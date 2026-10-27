#!/usr/bin/env python3
"""
Bot çalışmasının sonucunu e-posta ile gönderir.

- Başarılı / yenilenecek ilçe yok  -> kısa özet
- Kısmi veya hatalı                -> ayrıntılı teşhis (hata ayıklamak için doğrudan
                                       Claude'a yapıştırılabilecek bir blok) + tam log eki

Yalnızca standart kütüphane kullanır; bot daha başlarken çökse bile (ör. eksik paket)
çalışır. Bu durumda report.json olmaz, teşhis bot.log'dan çıkarılır.

Ortam değişkenleri:
  SMTP_USER, SMTP_PASSWORD (workflow'da GMAIL_USER / GMAIL_PASS secret'larından), REPORT_TO
  SMTP_HOST=smtp.gmail.com, SMTP_PORT=465, MAIL_FROM=SMTP_USER
  REPORT_FILE=report.json, BOT_LOG=bot.log, SITE_DIR=site
  BOT_OUTCOME, PUBLISH_OUTCOME              (workflow adım sonuçları)
"""
import json
import os
import re
import smtplib
import ssl
import sys
from collections import Counter
from datetime import datetime, timezone
from email.message import EmailMessage
from pathlib import Path
from zoneinfo import ZoneInfo

TZ = ZoneInfo("Europe/Istanbul")
REPORT_FILE = Path(os.getenv("REPORT_FILE", "report.json"))
BOT_LOG = Path(os.getenv("BOT_LOG", "bot.log"))
SITE_DIR = Path(os.getenv("SITE_DIR", "site"))
DEPS_OUTCOME = os.getenv("DEPS_OUTCOME", "unknown")
FETCH_OUTCOME = os.getenv("FETCH_OUTCOME", "unknown")
BOT_OUTCOME = os.getenv("BOT_OUTCOME", "unknown")
PUBLISH_OUTCOME = os.getenv("PUBLISH_OUTCOME", "unknown")
LOG_TAIL_LINES = 80

_EMAIL_RE = re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+")
_JWT_RE = re.compile(r"eyJ[\w-]+\.[\w-]+\.[\w-]+")
_BEARER_RE = re.compile(r"(Bearer\s+)\S+", re.I)


def scrub(text: str) -> str:
    """E-posta adresi ve token benzeri değerleri maskeler."""
    text = _JWT_RE.sub("<token>", text)
    text = _BEARER_RE.sub(r"\1<token>", text)
    return _EMAIL_RE.sub("<e-posta>", text)


def read_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def read_log() -> str:
    try:
        return scrub(BOT_LOG.read_text(encoding="utf-8", errors="replace"))
    except FileNotFoundError:
        return ""


def fmt_dt(iso: str | None) -> str:
    if not iso:
        return "-"
    return datetime.fromisoformat(iso).astimezone(TZ).strftime("%d.%m.%Y %H:%M")


def fmt_duration(sec) -> str:
    if sec is None:
        return "-"
    m, s = divmod(int(sec), 60)
    return f"{m} dk {s} sn" if m else f"{s} sn"


def github_links() -> tuple[str, str]:
    server = os.getenv("GITHUB_SERVER_URL", "https://github.com")
    repo = os.getenv("GITHUB_REPOSITORY", "")
    run_url = f"{server}/{repo}/actions/runs/{os.getenv('GITHUB_RUN_ID', '')}" if repo else "-"
    pages_url = "-"
    if "/" in repo:
        owner, name = repo.split("/", 1)
        pages_url = f"https://{owner.lower()}.github.io/{name}/v1/"
    return run_url, pages_url


def site_summary() -> dict:
    """Yayımlanan verinin genel durumu (index.json'dan)."""
    index = read_json(SITE_DIR / "v1" / "index.json") or {}
    cities = index.get("cities") or {}
    if not cities:
        return {}
    valid_to = sorted(c["validTo"] for c in cities.values() if c.get("validTo"))
    updated = sorted(c["updatedAt"] for c in cities.values() if c.get("updatedAt"))
    return {"cities": len(cities), "earliestValidTo": valid_to[0] if valid_to else None,
            "oldestUpdate": updated[0] if updated else None, "pending": index.get("pending")}


def overall(report) -> str:
    if report is None or BOT_OUTCOME in ("failure", "cancelled") and report.get("status") != "partial":
        return "failed"
    if PUBLISH_OUTCOME not in ("success", "skipped"):
        return "failed"
    status = report.get("status")
    if status in ("success", "nothing_to_do"):
        return status
    return "partial" if status == "partial" else "failed"


# --- e-posta gövdeleri -----------------------------------------------------------
def success_body(report: dict, level: str) -> tuple[str, str]:
    run_url, pages_url = github_links()
    site = site_summary()
    today = datetime.now(TZ).strftime("%d.%m.%Y")
    if level == "nothing_to_do":
        subject = f"✅ Namaz vakitleri: yenilenecek ilçe yoktu ({today})"
        what = "Tüm ilçelerin verisi güncel olduğu için bu çalışmada Diyanet'ten vakit çekilmedi."
    else:
        subject = f"✅ Namaz vakitleri güncellendi: {report['ok']}/{report['due']} ilçe ({today})"
        what = f"{report['ok']} ilçenin 1 yıllık vakitleri yenilendi."
    lines = [
        what,
        "",
        f"Çalışma     : {fmt_dt(report.get('startedAt'))}, süre {fmt_duration(report.get('durationSec'))}",
        f"İlçe listesi: {report.get('placesCount')} ilçe" + (" (bu çalışmada yenilendi)" if report.get("placesRefreshed") else ""),
    ]
    if site:
        lines.append(f"Yayındaki veri: {site['cities']} ilçe, en erken biten {site['earliestValidTo']}, "
                     f"en eski güncelleme {fmt_dt(site['oldestUpdate'])}")
    for q in report.get("quota") or []:
        lines.append(f"Kota ({q['endpoint']}): parametre başına {q['perParameter']}/gün, "
                     f"bugün kullanılan {q['totalUsed']}, aşım {q['totalExceeded']}")
    lines += ["", f"Çalışma detayı: {run_url}", f"Yayın adresi  : {pages_url}"]
    return subject, "\n".join(lines)


def failure_body(report, level: str, log_text: str) -> tuple[str, str]:
    run_url, pages_url = github_links()
    today = datetime.now(TZ).strftime("%d.%m.%Y %H:%M")
    r = report or {}
    if level == "partial":
        subject = f"⚠️ Namaz vakitleri kısmen güncellendi: {r.get('ok', 0)} başarılı, {r.get('failed', 0)} hatalı ({today})"
        intro = ("Çalışma tamamlandı ama bazı ilçeler güncellenemedi veya çalışma erken durdu. "
                 "Güncellenemeyen ilçelerin eski verisi yayında kalmaya devam ediyor; "
                 "kalanlar bir sonraki çalışmada (ayın 1'i veya 15'i) tekrar denenecek.")
    else:
        subject = f"❌ Namaz vakitleri botu HATA verdi ({today})"
        intro = ("Çalışma başarısız oldu. Yayındaki mevcut veri değiştirilmedi (veya yalnızca başarılı "
                 "ilçeler yayımlandı); uygulama eski veriyle çalışmaya devam eder.")

    log_lines = log_text.splitlines()
    tail = "\n".join(log_lines[-LOG_TAIL_LINES:]) if log_lines else "(bot.log boş veya yok)"
    failures = r.get("failures") or []
    groups = Counter(re.sub(r"\d", "#", f["error"])[:160] for f in failures)

    d = []
    d.append("=== HATA AYIKLAMA İÇİN CLAUDE'A YAPIŞTIRILACAK TEŞHİS BİLGİSİ ===")
    d.append("Bağlam: Diyanet AwqatSalah API'sinden il/ilçe ve 1 yıllık namaz vakitlerini çekip "
             "GitHub Pages'e (gh-pages branch, v1/places.json, v1/index.json, v1/times/{cityId}.json) "
             "yazan Python botu (bot/pry_bot.py). GitHub Actions'ta ayın 1'i ve 15'i çalışır. "
             "Login: POST /Auth/Login; diğerleri /api/... altında (Place/States, Place/Cities, "
             "PrayerTime/DateRange, Quota/My). Kota cityId başına günde 100 istek.")
    d.append("")
    d.append(f"Genel durum      : {level}")
    d.append(f"Bot durumu       : {r.get('status', 'rapor yok')}")
    d.append(f"Adım sonuçları   : bağımlılık={DEPS_OUTCOME}, gh-pages klonlama={FETCH_OUTCOME}, "
             f"bot={BOT_OUTCOME}, yayın={PUBLISH_OUTCOME}")
    d.append(f"Bot sürümü       : {r.get('botVersion', '-')}")
    d.append(f"Tetikleyici      : {os.getenv('GITHUB_EVENT_NAME', '-')}, commit {os.getenv('GITHUB_SHA', '-')[:7]}, "
             f"deneme {os.getenv('GITHUB_RUN_ATTEMPT', '-')}")
    d.append(f"Başlangıç / süre : {fmt_dt(r.get('startedAt'))} / {fmt_duration(r.get('durationSec'))}")
    if report:
        d.append(f"Sayılar          : yenilenmesi gereken {r.get('due')}, denenen {r.get('attempted')}, "
                 f"başarılı {r.get('ok')}, hatalı {r.get('failed')}, bekleyen {r.get('pending')}")
        d.append(f"Durma sebebi     : {r.get('stopReason') or '-'}")
        d.append(f"İlçe listesi     : {r.get('placesCount')} ilçe, bu çalışmada yenilendi mi: {r.get('placesRefreshed')}")
    elif BOT_OUTCOME == "skipped":
        failed_step = ("bağımlılık kurulumu (requirements.txt / pip install)" if DEPS_OUTCOME == "failure"
                       else "gh-pages klonlama" if FETCH_OUTCOME == "failure" else "bot'tan önceki bir adım")
        d.append(f"Rapor            : bot adımı hiç çalışmadı; {failed_step} başarısız oldu. "
                 "Hata mesajı bu adımın Actions logunda (bağlantı yukarıda). Yayındaki veriye dokunulmadı.")
    else:
        d.append("Rapor            : report.json üretilmedi; bot büyük ihtimalle başlarken çöktü "
                 "(sözdizimi hatası, yanlış dosya yolu, import hatası vb.). Ayrıntı aşağıdaki logda.")

    fatal = r.get("fatal")
    if fatal:
        d += ["", f"ÖLÜMCÜL HATA: {fatal['type']}: {scrub(fatal['message'])}", "Traceback:",
              scrub(fatal.get("traceback") or "-")]

    if failures:
        d += ["", f"HATA TÜRLERİ ({len(failures)} kayıt, rakamlar # ile gruplandı):"]
        d += [f"  {n} x {msg}" for msg, n in groups.most_common(10)]
        d += ["", f"HATALI İLÇELER (ilk {len(failures)}):"]
        d += [f"  {f['cityId']} {f['city']}: {scrub(f['error'])}" for f in failures]

    if r.get("quota"):
        d += ["", "KOTA:"] + [f"  {json.dumps(q, ensure_ascii=False)}" for q in r["quota"]]
    if r.get("config"):
        d += ["", "YAPILANDIRMA:", "  " + json.dumps(r["config"], ensure_ascii=False)]

    d += ["", f"SON {LOG_TAIL_LINES} LOG SATIRI:", tail]
    d.append("=== TEŞHİS BİLGİSİ SONU ===")

    body = "\n".join([
        intro, "",
        "Aşağıdaki bloğun tamamını kopyalayıp Claude'a yapıştırarak hatanın sebebini ve çözümünü sorabilirsiniz. "
        "Tam log ve rapor ekte. E-posta adresleri ve token'lar maskelenmiştir.", "",
        f"Çalışma detayı: {run_url}", f"Yayın adresi  : {pages_url}", "",
        *d,
    ])
    return subject, body


# --- gönderim --------------------------------------------------------------------
def send(subject: str, body: str, attachments: list[tuple[str, bytes]]) -> None:
    host = os.getenv("SMTP_HOST", "smtp.gmail.com")
    port = int(os.getenv("SMTP_PORT", "465"))
    user, password = os.getenv("SMTP_USER"), os.getenv("SMTP_PASSWORD")
    to = os.getenv("REPORT_TO") or user   # REPORT_TO tanımlı değilse gönderici adrese gönderilir
    if not (user and password):
        raise SystemExit("GMAIL_USER / GMAIL_PASS secret'ları tanımlı değil; e-posta gönderilmedi.")

    msg = EmailMessage()
    msg["Subject"] = subject
    msg["From"] = os.getenv("MAIL_FROM") or user
    msg["To"] = to
    msg.set_content(body)
    for name, data in attachments:
        msg.add_attachment(data, maintype="text", subtype="plain", filename=name)

    context = ssl.create_default_context()
    if port == 465:
        with smtplib.SMTP_SSL(host, port, context=context, timeout=60) as s:
            s.login(user, password)
            s.send_message(msg)
    else:
        with smtplib.SMTP(host, port, timeout=60) as s:
            s.starttls(context=context)
            s.login(user, password)
            s.send_message(msg)


def main() -> int:
    report = read_json(REPORT_FILE)
    level = overall(report)
    log_text = read_log()

    if level in ("success", "nothing_to_do"):
        subject, body = success_body(report, level)
        attachments = []
    else:
        subject, body = failure_body(report, level, log_text)
        attachments = [("bot-log.txt", log_text.encode("utf-8"))]
        if report:
            attachments.append(("report.json.txt",
                                scrub(json.dumps(report, ensure_ascii=False, indent=2)).encode("utf-8")))

    try:
        send(subject, body, attachments)
    except (smtplib.SMTPException, OSError) as e:
        print(f"E-posta gönderilemedi: {type(e).__name__}: {e}", file=sys.stderr)
        return 1
    print(f"E-posta gönderildi: {subject}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
