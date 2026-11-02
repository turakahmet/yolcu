#!/usr/bin/env python3
"""
Diyanet AwqatSalah -> GitHub Pages statik veri botu (tüm ülkeler, veri biçimi v2).

Hiyerarşi: Ülke -> Eyalet (Türkiye'de il) -> Şehir (Türkiye'de ilçe).
Her gün çalışır; MAX_RUN_MINUTES içinde, verisi olmayan / eskiyen şehirleri öncelik
sırasıyla yeniler ve SITE_DIR altına yazar:

  v2/countries.json            ülke listesi
  v2/places/{countryId}.json   ülkenin eyaletleri ve şehirleri
  v2/cities.json               tüm şehirlerin düz listesi (arama ve botun ana listesi)
  v2/times/{cityId}.json       şehrin 1 yıllık vakitleri (kompakt satırlar)
  v2/index.json                botun durum dosyası (güncellik, hatalı şehirler)

Kota cityId (parametre) başına günde 100 istektir; şehir başına ayda ~1 istek kullanılır.
Kimlik bilgileri yalnızca ortam değişkeninden okunur (GitHub Secrets).
"""
import json
import logging
import os
import platform
import re
import shutil
import signal
import sys
import time
import traceback
import unicodedata
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

import requests

TZ = ZoneInfo("Europe/Istanbul")
BOT_VERSION = "2026.11.02"
DATA_VERSION = 2


def env_int(name: str, default: int) -> int:
    value = os.getenv(name, "").strip()
    return int(value) if value else default


def env_float(name: str, default: float) -> float:
    value = os.getenv(name, "").strip()
    return float(value) if value else default


BASE_URL = os.getenv("DIYANET_BASE_URL", "https://awqatsalah.diyanet.gov.tr").rstrip("/")
SITE_DIR = Path(os.getenv("SITE_DIR", "site"))
MODE = os.getenv("MODE", "auto").strip() or "auto"     # auto | places | times
# Boşsa tüm ülkeler; test için ör. "2" (yalnızca Türkiye) veya "2,13"
COUNTRY_FILTER = {int(x) for x in os.getenv("COUNTRY_IDS", "").replace(" ", "").split(",") if x}
DAILY_LIMIT = env_int("DAILY_LIMIT", 100000)           # bir çalışmada en fazla kaç şehir
DAYS_BEHIND = env_int("DAYS_BEHIND", 1)                # batıdaki saat dilimleri için dünden başla
DAYS_AHEAD = env_int("DAYS_AHEAD", 365)                # bugünden itibaren kaç gün
REFRESH_DAYS = env_int("REFRESH_DAYS", 25)             # şehir kaç günde bir yenilensin
MIN_FORWARD_DAYS = env_int("MIN_FORWARD_DAYS", 300)    # ileri veri bundan azsa öncelikli yenile
MIN_VALID_DAYS = env_int("MIN_VALID_DAYS", 60)         # bundan az gün dönerse yanıtı reddet
MAX_NULL_RATIO = env_float("MAX_NULL_RATIO", 0.5)      # boş vakit oranı bunu aşarsa reddet (kutup bölgeleri)
PLACES_MAX_AGE_DAYS = env_int("PLACES_MAX_AGE_DAYS", 90)
FAILED_RETRY_DAYS = env_int("FAILED_RETRY_DAYS", 7)    # hata veren şehri kaç gün sonra tekrar dene
MAX_CONSECUTIVE_ERRORS = env_int("MAX_CONSECUTIVE_ERRORS", 20)
REQUEST_DELAY = env_float("REQUEST_DELAY", 1.0)
MAX_RUN_MINUTES = env_int("MAX_RUN_MINUTES", 150)      # bu süre dolunca yeni şehir başlatma
REQUEST_DEADLINE = env_int("REQUEST_DEADLINE", 90)     # tek isteğin toplam süre sınırı (sn)
CHECK_QUOTA = os.getenv("CHECK_QUOTA", "1") == "1"
KEEP_V1 = os.getenv("KEEP_V1", "0") == "1"             # eski v1 klasörü, v2 dolunca silinir
REPORT_FILE = Path(os.getenv("REPORT_FILE", "report.json"))

# --- DateRange isteğinin biçimi ---------------------------------------------
DATERANGE_METHOD = os.getenv("DATERANGE_METHOD", "POST").upper()   # POST (JSON gövde) | GET (query)
DATERANGE_DATE_FORMAT = os.getenv("DATERANGE_DATE_FORMAT", "%Y-%m-%d")

PATHS = {
    "login": "/Auth/Login",
    "quota": "/api/Quota/My",
    "countries": "/api/Place/Countries",
    "states": "/api/Place/States/{country_id}",
    "cities": "/api/Place/Cities/{state_id}",
    "date_range": "/api/PrayerTime/DateRange",
}

V1 = SITE_DIR / "v1"
V2 = SITE_DIR / "v2"
TIMES_DIR = V2 / "times"
PLACES_DIR = V2 / "places"
COUNTRIES_FILE = V2 / "countries.json"
CITIES_FILE = V2 / "cities.json"
INDEX_FILE = V2 / "index.json"

TIME_FIELDS = ("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
DAY_FIELDS = ["date", *TIME_FIELDS, "hijri", "qibla", "gmt"]
CITY_FIELDS = ["id", "name", "key", "stateId", "state", "countryId", "country"]

log = logging.getLogger("diyanet-bot")

REPORT = {
    "botVersion": BOT_VERSION,
    "status": "running",            # success | nothing_to_do | partial | failed
    "startedAt": None, "finishedAt": None, "durationSec": None,
    "mode": MODE,
    "placesRefreshed": False, "countriesCount": None, "citiesCount": None,
    "due": 0, "attempted": 0, "ok": 0, "failed": 0, "pending": None,
    "skippedBackoff": 0, "failedTotal": 0, "removedCities": 0, "removedV1": False,
    "stopReason": None,             # quota | time_limit | consecutive_errors | signal
    "failures": [],                 # bu çalışmadaki hatalar [{cityId, city, error}]
    "quota": [],
    "fatal": None,
    "config": {
        "baseUrl": BASE_URL, "countryFilter": sorted(COUNTRY_FILTER), "dailyLimit": DAILY_LIMIT,
        "daysAhead": DAYS_AHEAD, "refreshDays": REFRESH_DAYS, "minForwardDays": MIN_FORWARD_DAYS,
        "maxNullRatio": MAX_NULL_RATIO, "failedRetryDays": FAILED_RETRY_DAYS,
        "requestDeadline": REQUEST_DEADLINE, "maxRunMinutes": MAX_RUN_MINUTES,
        "dateRangeMethod": DATERANGE_METHOD, "dateRangeDateFormat": DATERANGE_DATE_FORMAT,
        "python": platform.python_version(), "requests": requests.__version__,
    },
}
MAX_REPORTED_FAILURES = 100


class QuotaExceeded(Exception):
    pass


class ApiError(Exception):
    pass


# --- yardımcılar ---------------------------------------------------------------
def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def read_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    tmp.replace(path)


_TR_UPPER = str.maketrans({"i": "İ", "ı": "I"})
_FOLD = str.maketrans("ÇĞİÖŞÜÂÎÛ", "CGIOSUAIU")


def norm(text: str) -> str:
    """'Devrekâni' / 'DEVREKANİ' / 'München' -> 'DEVREKANI' / 'MUNCHEN'. Uygulamada da aynı kural kullanılmalı."""
    s = (text or "").translate(_TR_UPPER).upper().translate(_FOLD)
    s = unicodedata.normalize("NFKD", s)
    return "".join(ch for ch in s if ch.isascii() and ch.isalnum())


# --- API istemcisi -----------------------------------------------------------
class _Deadline(BaseException):
    """BaseException: kütüphanelerin 'except Exception' blokları tarafından yutulmasın diye."""


def _on_deadline(signum, frame):
    raise _Deadline()


def _with_deadline(fn):
    """requests'in timeout'u bayt başına beklemedir; SIGALRM ile isteğin toplam süresini sınırlar."""
    previous = signal.signal(signal.SIGALRM, _on_deadline)
    signal.alarm(REQUEST_DEADLINE)
    try:
        return fn()
    except _Deadline:
        raise requests.Timeout(f"istek {REQUEST_DEADLINE} sn içinde tamamlanmadı") from None
    finally:
        signal.alarm(0)
        signal.signal(signal.SIGALRM, previous)


def _json(r):
    try:
        return r.json()
    except ValueError:
        raise ApiError(f"JSON olmayan yanıt (HTTP {r.status_code}): {r.text[:200]!r}") from None


class Client:
    TOKEN_TTL = 25 * 60  # access token 30 dk; 5 dk pay

    def __init__(self, email: str, password: str):
        self.session = requests.Session()
        self.session.headers["Accept"] = "application/json"
        self._email, self._password = email, password
        self._token = None
        self._token_at = 0.0

    def login(self) -> None:
        try:
            r = _with_deadline(lambda: self.session.post(
                BASE_URL + PATHS["login"], json={"email": self._email, "password": self._password},
                timeout=(10, 30)))
        except requests.RequestException as e:
            raise ApiError(f"Login isteği başarısız ({type(e).__name__})")
        if r.status_code == 429:
            raise QuotaExceeded("login")
        if r.status_code != 200:
            raise ApiError(f"Login başarısız: HTTP {r.status_code}")
        body = _json(r)
        if not body.get("success") or not body.get("data"):
            raise ApiError(f"Login başarısız: {body.get('message')}")
        self._token = body["data"]["accessToken"]
        self._token_at = time.monotonic()
        log.info("Login başarılı.")

    def request(self, method: str, path: str, **kwargs):
        if not self._token or time.monotonic() - self._token_at > self.TOKEN_TTL:
            self.login()
        relogged = False
        attempt = 0
        while attempt < 3:
            attempt += 1
            try:
                r = _with_deadline(lambda: self.session.request(
                    method, BASE_URL + path, timeout=(10, 60),
                    headers={"Authorization": f"Bearer {self._token}"}, **kwargs))
            except requests.RequestException as e:
                log.warning("%s %s ağ hatası (%s), deneme %d/3", method, path, type(e).__name__, attempt)
                time.sleep(5 * attempt)
                continue
            if r.status_code == 401 and not relogged:
                self.login()
                relogged = True
                attempt -= 1
                continue
            if r.status_code == 429:
                raise QuotaExceeded(path)
            if r.status_code >= 500:
                log.warning("%s %s HTTP %d, deneme %d/3", method, path, r.status_code, attempt)
                time.sleep(5 * attempt)
                continue
            if r.status_code != 200:
                raise ApiError(f"HTTP {r.status_code}: {r.text[:300]}")
            body = _json(r)
            if not body.get("success"):
                msg = str(body.get("message") or "")
                if re.search(r"kota|quota|limit", msg, re.I):
                    raise QuotaExceeded(msg)
                raise ApiError(msg or "success=false")
            return body.get("data")
        raise ApiError("3 deneme başarısız")


# --- kota ------------------------------------------------------------------------
def _quota_entries(obj):
    if isinstance(obj, dict):
        if "resultName" in obj:
            yield obj
        for v in obj.values():
            yield from _quota_entries(v)
    elif isinstance(obj, list):
        for v in obj:
            yield from _quota_entries(v)


def log_quota(client: Client) -> None:
    try:
        data = client.request("GET", PATHS["quota"])
    except ApiError as e:
        log.warning("Kota bilgisi alınamadı: %s", e)
        return
    for q in _quota_entries(data):
        lines = q.get("lines") or []
        low = min((ln.get("remainingUse", 0) for ln in lines), default=None)
        log.info("Kota %s/%s (%s, parametre başına %s): kullanılan %s, aşım %s, parametre sayısı %d, en düşük kalan %s",
                 q.get("controllerName"), q.get("resultName"), q.get("period"), q.get("benefit"),
                 q.get("totalUsed"), q.get("totalExceeded"), len(lines), low)
        REPORT["quota"].append({"endpoint": f"{q.get('controllerName')}/{q.get('resultName')}",
                                "period": q.get("period"), "perParameter": q.get("benefit"),
                                "totalUsed": q.get("totalUsed"), "totalExceeded": q.get("totalExceeded"),
                                "parameters": len(lines), "lowestRemaining": low})


# --- yer listesi -----------------------------------------------------------------
def places_outdated(doc, today: date) -> bool:
    if not doc or not doc.get("rows"):
        return True
    generated = datetime.fromisoformat(doc["generatedAt"]).date()
    return (today - generated).days >= PLACES_MAX_AGE_DAYS


def fetch_places(client: Client) -> dict:
    """Ülke -> eyalet -> şehir hiyerarşisini çeker. Hepsi başarıyla gelmeden hiçbir dosya yazılmaz."""
    log.info("Ülke/eyalet/şehir listesi çekiliyor...")
    countries = client.request("GET", PATHS["countries"]) or []
    if COUNTRY_FILTER:
        countries = [c for c in countries if int(c["id"]) in COUNTRY_FILTER]
    if not countries:
        raise ApiError("Ülke listesi boş döndü")

    country_docs, rows, seen = [], [], set()
    for c in countries:
        cid = int(c["id"])
        states = client.request("GET", PATHS["states"].format(country_id=cid)) or []
        state_docs = []
        for st in states:
            sid = int(st["id"])
            cities = client.request("GET", PATHS["cities"].format(state_id=sid)) or []
            city_docs = []
            for ct in cities:
                city_id = int(ct["id"])
                if city_id in seen:           # aynı şehir iki eyalette dönerse ilkini tut
                    continue
                seen.add(city_id)
                city_docs.append({"id": city_id, "name": ct["name"], "code": ct.get("code"), "key": norm(ct["name"])})
                rows.append([city_id, ct["name"], norm(ct["name"]), sid, st["name"], cid, c["name"]])
            city_docs.sort(key=lambda x: x["key"])
            state_docs.append({"id": sid, "name": st["name"], "code": st.get("code"),
                               "key": norm(st["name"]), "cities": city_docs})
            time.sleep(REQUEST_DELAY / 2)
        state_docs.sort(key=lambda s: s["key"])
        country_docs.append({"id": cid, "name": c["name"], "code": c.get("code"),
                             "key": norm(c["name"]), "codeKey": norm(c.get("code") or ""), "states": state_docs})
        log.info("%s: %d eyalet/il, %d şehir/ilçe", c["name"], len(state_docs),
                 sum(len(s["cities"]) for s in state_docs))
    if not rows:
        raise ApiError("Hiç şehir dönmedi")

    generated = now_iso()
    keep = set()
    for cd in country_docs:
        path = PLACES_DIR / f"{cd['id']}.json"
        keep.add(path.name)
        write_json(path, {"version": DATA_VERSION, "generatedAt": generated, "countryId": cd["id"],
                          "name": cd["name"], "code": cd["code"], "states": cd["states"]})
    for f in PLACES_DIR.glob("*.json"):
        if f.name not in keep:
            f.unlink()
    country_docs.sort(key=lambda x: x["key"])
    write_json(COUNTRIES_FILE, {
        "version": DATA_VERSION, "generatedAt": generated, "count": len(country_docs),
        "countries": [{"id": cd["id"], "name": cd["name"], "code": cd["code"], "key": cd["key"],
                       "codeKey": cd["codeKey"], "states": len(cd["states"]),
                       "cities": sum(len(s["cities"]) for s in cd["states"])} for cd in country_docs]})
    rows.sort(key=lambda r: (norm(r[6]), norm(r[4]), r[2]))
    doc = {"version": DATA_VERSION, "generatedAt": generated, "count": len(rows), "fields": CITY_FIELDS, "rows": rows}
    write_json(CITIES_FILE, doc)
    REPORT["placesRefreshed"] = True
    log.info("%d ülke, %d şehir kaydedildi.", len(country_docs), len(rows))
    return doc


def city_list(doc: dict) -> list:
    return [dict(zip(doc["fields"], row)) for row in doc["rows"]]


def remove_orphans(cities: list, index_doc: dict) -> None:
    """Artık listede olmayan şehirlerin vakit dosyalarını ve durum kayıtlarını siler."""
    ids = {str(c["id"]) for c in cities}
    removed = 0
    if TIMES_DIR.exists():
        for f in TIMES_DIR.glob("*.json"):
            if f.stem not in ids:
                f.unlink()
                removed += 1
    for section in ("cities", "failed"):
        for k in [k for k in index_doc.get(section, {}) if k not in ids]:
            del index_doc[section][k]
    if removed:
        log.info("Listede olmayan %d şehrin dosyası silindi.", removed)
    REPORT["removedCities"] = removed


# --- vakitler --------------------------------------------------------------------
_TIME_RE = re.compile(r"^\d{2}:\d{2}$")


def _time(value):
    value = (value or "").strip()
    return value if _TIME_RE.match(value) else None


def build_date_range_request(city_id: int, start: date, end: date) -> dict:
    payload = {
        "cityId": city_id,
        "startDate": start.strftime(DATERANGE_DATE_FORMAT),
        "endDate": end.strftime(DATERANGE_DATE_FORMAT),
    }
    return {"json": payload} if DATERANGE_METHOD == "POST" else {"params": payload}


def parse_day(item: dict) -> list:
    d = datetime.strptime(item["gregorianDateShort"], "%d.%m.%Y").date()
    return [d.isoformat(), *(_time(item.get(k)) for k in TIME_FIELDS),
            item.get("hijriDateShort"), _time(item.get("qiblaTime")), item.get("greenwichMeanTimeZone")]


def validate(days: list, start: date) -> int:
    """Geçersizse ApiError; geçerliyse boş vakit sayısını döndürür."""
    if len(days) < MIN_VALID_DAYS:
        raise ApiError(f"yetersiz gün sayısı: {len(days)}")
    if days[0][0] != start.isoformat():
        raise ApiError(f"ilk gün {days[0][0]}, beklenen {start}")
    prev = None
    for day in days:
        current = date.fromisoformat(day[0])
        if prev and current - prev != timedelta(days=1):
            raise ApiError(f"tarih boşluğu/tekrarı: {prev} -> {current}")
        prev = current
    nulls = sum(1 for day in days for v in day[1:7] if v is None)
    if nulls > len(days) * 6 * MAX_NULL_RATIO:
        raise ApiError(f"boş vakit oranı çok yüksek: {nulls}/{len(days) * 6}")
    return nulls


def fetch_city(client: Client, city_id: int, start: date, end: date) -> tuple:
    data = client.request(DATERANGE_METHOD, PATHS["date_range"],
                          **build_date_range_request(city_id, start, end)) or []
    days = sorted((parse_day(x) for x in data), key=lambda x: x[0])
    days = [x for x in days if x[0] >= start.isoformat()]
    nulls = validate(days, start)
    return days, nulls


def select_due(cities: list, entries: dict, failed: dict, today: date) -> list:
    """Öncelik: hiç denenmemiş > yenilenmesi gereken (en erken biten önce) > beklemesi biten hatalılar."""
    due = []
    for c in cities:
        cid = str(c["id"])
        f = failed.get(cid)
        if f and (today - date.fromisoformat(f["lastTry"])).days < FAILED_RETRY_DAYS:
            REPORT["skippedBackoff"] += 1
            continue
        e = entries.get(cid)
        if not e:
            due.append(((2 if f else 0, "", ""), c))
            continue
        valid_to = date.fromisoformat(e["validTo"])
        updated = datetime.fromisoformat(e["updatedAt"]).date()
        if (today - updated).days >= REFRESH_DAYS or (valid_to - today).days < MIN_FORWARD_DAYS:
            due.append(((2 if f else 1, e["validTo"], e["updatedAt"]), c))
    due.sort(key=lambda t: t[0])
    return [c for _, c in due]


def run_times(client: Client, cities: list, index_doc: dict, today: date) -> tuple:
    entries = index_doc.setdefault("cities", {})
    failed_map = index_doc.setdefault("failed", {})
    due = select_due(cities, entries, failed_map, today)
    batch = due[:DAILY_LIMIT]
    log.info("Yenilenmesi gereken şehir: %d, bu çalışmada en fazla: %d, hata beklemesinde: %d",
             len(due), len(batch), REPORT["skippedBackoff"])
    REPORT["due"] = len(due)

    start = today - timedelta(days=DAYS_BEHIND)
    end = today + timedelta(days=DAYS_AHEAD - 1)
    ok = failed = consecutive = 0
    run_started = time.monotonic()
    try:
        for c in batch:
            cid = str(c["id"])
            label = f"{c['country']}/{c['state']}/{c['name']} ({c['id']})"
            if time.monotonic() - run_started > MAX_RUN_MINUTES * 60:
                log.info("%d dakikalık çalışma süresi doldu; kalanlar sonraki çalışmada.", MAX_RUN_MINUTES)
                REPORT["stopReason"] = "time_limit"
                break
            REPORT["attempted"] += 1
            try:
                days, nulls = fetch_city(client, c["id"], start, end)
            except ApiError as e:
                failed += 1
                consecutive += 1
                log.error("%s: %s", label, e)
                prev = failed_map.get(cid, {})
                failed_map[cid] = {"lastTry": today.isoformat(), "count": prev.get("count", 0) + 1,
                                   "error": str(e)[:300]}
                if len(REPORT["failures"]) < MAX_REPORTED_FAILURES:
                    REPORT["failures"].append({"cityId": c["id"], "city": label, "error": str(e)[:500]})
                if consecutive >= MAX_CONSECUTIVE_ERRORS:
                    log.error("Art arda %d hata; çalışma durduruluyor.", consecutive)
                    REPORT["stopReason"] = "consecutive_errors"
                    break
                continue
            consecutive = 0
            updated_at = now_iso()
            write_json(TIMES_DIR / f"{cid}.json", {
                "version": DATA_VERSION, "cityId": c["id"], "name": c["name"], "state": c["state"],
                "stateId": c["stateId"], "country": c["country"], "countryId": c["countryId"],
                "updatedAt": updated_at, "validFrom": days[0][0], "validTo": days[-1][0],
                "fields": DAY_FIELDS, "days": days,
            })
            entries[cid] = {"validFrom": days[0][0], "validTo": days[-1][0],
                            "updatedAt": updated_at, "days": len(days), "nullTimes": nulls}
            failed_map.pop(cid, None)
            ok += 1
            log.info("%s: %d gün%s", label, len(days), f", {nulls} boş vakit" if nulls else "")
            time.sleep(REQUEST_DELAY)
    finally:
        index_doc["generatedAt"] = now_iso()
        # hata veren şehirler beklemeye alındığı için bekleyen = hiç denenmemiş olanlar
        index_doc["pending"] = max(len(due) - REPORT["attempted"], 0)
        write_json(INDEX_FILE, index_doc)
        REPORT.update(ok=ok, failed=failed, pending=index_doc["pending"], failedTotal=len(failed_map))
        log.info("Özet: %d başarılı, %d hatalı, bekleyen %d, toplam hatalı şehir %d",
                 ok, failed, index_doc["pending"], len(failed_map))
    return ok, failed


def maybe_remove_v1(index_doc: dict, cities: list) -> None:
    """v2 ilk doldurması bitince eski v1 klasörünü siler."""
    if KEEP_V1 or not V1.exists():
        return
    filled = len(index_doc.get("cities", {}))
    if cities and filled >= len(cities) - len(index_doc.get("failed", {})) and not index_doc.get("pending"):
        shutil.rmtree(V1)
        REPORT["removedV1"] = True
        log.info("v2 doldurması tamamlandı; eski v1 klasörü silindi.")


# --- ana akış --------------------------------------------------------------------
def _on_sigterm(signum, frame):
    REPORT["stopReason"] = "signal"
    raise SystemExit(f"sinyal {signum} alındı (iptal veya zaman aşımı)")


def _finalize_report(started: float) -> None:
    REPORT["finishedAt"] = now_iso()
    REPORT["durationSec"] = round(time.monotonic() - started)
    if REPORT["status"] == "running":
        if REPORT["fatal"] or (REPORT["failed"] and not REPORT["ok"]):
            REPORT["status"] = "failed"
        elif REPORT["failed"] or REPORT["stopReason"] in ("quota", "consecutive_errors", "signal"):
            REPORT["status"] = "partial"
        elif REPORT["ok"] == 0 and not REPORT["placesRefreshed"]:
            REPORT["status"] = "nothing_to_do"
        else:
            REPORT["status"] = "success"   # süre sınırıyla kalan varsa 'pending' ile bildirilir
    try:
        REPORT_FILE.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2), encoding="utf-8")
    except OSError as e:
        log.error("Rapor yazılamadı: %s", e)


def _run() -> int:
    email, password = os.getenv("DIYANET_EMAIL"), os.getenv("DIYANET_PASSWORD")
    if not email or not password:
        raise ApiError("DIYANET_EMAIL / DIYANET_PASSWORD ortam değişkenleri tanımlı değil (GitHub Secrets).")

    client = Client(email, password)
    today = datetime.now(TZ).date()
    if CHECK_QUOTA:
        log_quota(client)
    cities_doc = read_json(CITIES_FILE)
    if MODE == "places" or (MODE == "auto" and places_outdated(cities_doc, today)):
        cities_doc = fetch_places(client)
    if not cities_doc:
        raise ApiError("cities.json yok; önce MODE=places veya auto ile çalıştırın.")
    cities = city_list(cities_doc)
    REPORT["citiesCount"] = len(cities)
    REPORT["countriesCount"] = len({c["countryId"] for c in cities})

    index_doc = read_json(INDEX_FILE) or {"version": DATA_VERSION, "cities": {}, "failed": {}}
    remove_orphans(cities, index_doc)
    if MODE == "places":
        write_json(INDEX_FILE, index_doc)
        return 0
    ok, failed = run_times(client, cities, index_doc, today)
    maybe_remove_v1(index_doc, cities)
    return 1 if failed and not ok else 0


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    signal.signal(signal.SIGTERM, _on_sigterm)
    started = time.monotonic()
    REPORT["startedAt"] = now_iso()
    log.info("Bot sürümü %s, mod=%s, ülke filtresi=%s", BOT_VERSION, MODE, sorted(COUNTRY_FILTER) or "tümü")
    try:
        return _run()
    except QuotaExceeded as e:
        log.warning("Kota sınırına ulaşıldı (%s). İlerleme kaydedildi; kalanlar sonraki çalışmada.", e)
        REPORT["stopReason"] = "quota"
        return 0
    except BaseException as e:
        log.error("Hata: %s: %s", type(e).__name__, e)
        REPORT["fatal"] = {"type": type(e).__name__, "message": str(e)[:1000],
                           "traceback": traceback.format_exc()[-4000:]}
        return 1
    finally:
        _finalize_report(started)


if __name__ == "__main__":
    sys.exit(main())
