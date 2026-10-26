#!/usr/bin/env python3
"""
Diyanet AwqatSalah -> GitHub Pages statik veri botu.

Her çalışmada, verisi en eski / en erken bitecek ilçelerden en fazla DAILY_LIMIT
kadarı için DateRange ile 1 yıllık vakit çeker ve SITE_DIR altına yazar:

  v1/places.json          il/ilçe listesi (uygulamada eşleştirme için normalize anahtarlarla)
  v1/index.json           ilçe başına validFrom/validTo/updatedAt (botun durum dosyası da budur)
  v1/times/{cityId}.json  ilçenin 1 yıllık vakitleri

Kota aşılırsa (429) bot durur, o ana kadarki ilerleme kaydedilir; kalan ilçeler
bir sonraki çalışmada öncelikli olarak alınır.

Kimlik bilgileri yalnızca ortam değişkeninden okunur (GitHub Secrets).
"""
import json
import logging
import os
import re
import sys
import time
import unicodedata
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

import requests

TZ = ZoneInfo("Europe/Istanbul")


def env_int(name: str, default: int) -> int:
    value = os.getenv(name, "").strip()
    return int(value) if value else default


BASE_URL = os.getenv("DIYANET_BASE_URL", "https://awqatsalah.diyanet.gov.tr").rstrip("/")
COUNTRY_ID = env_int("COUNTRY_ID", 2)                 # 2 = Türkiye
SITE_DIR = Path(os.getenv("SITE_DIR", "site"))
MODE = os.getenv("MODE", "auto").strip() or "auto"     # auto | places | times
# Kota cityId başına günde 100 istek olduğundan tüm ilçeler tek çalışmada çekilebilir.
DAILY_LIMIT = env_int("DAILY_LIMIT", 2000)             # bir çalışmada en fazla kaç ilçe
DAYS_AHEAD = env_int("DAYS_AHEAD", 365)                # kaç günlük veri istenecek
REFRESH_DAYS = env_int("REFRESH_DAYS", 25)             # aylık çalışmada hepsi yenilensin diye < 30
MIN_FORWARD_DAYS = env_int("MIN_FORWARD_DAYS", 300)    # ileri veri bundan azsa öncelikli yenile
MIN_VALID_DAYS = env_int("MIN_VALID_DAYS", 60)         # bundan az gün dönerse yanıtı reddet
PLACES_MAX_AGE_DAYS = env_int("PLACES_MAX_AGE_DAYS", 90)
REQUEST_DELAY = float(os.getenv("REQUEST_DELAY", "1.0"))
CHECK_QUOTA = os.getenv("CHECK_QUOTA", "1") == "1"

# --- DateRange isteğinin biçimi ---------------------------------------------
# Postman'de çalışan isteğinizle aynı değilse yalnızca bu iki ayarı ve
# build_date_range_request() fonksiyonunu değiştirmeniz yeterli.
DATERANGE_METHOD = os.getenv("DATERANGE_METHOD", "POST").upper()   # POST (JSON gövde) | GET (query)
DATERANGE_DATE_FORMAT = os.getenv("DATERANGE_DATE_FORMAT", "%Y-%m-%d")

PATHS = {
    "login": "/Auth/Login",
    "quota": "/api/Quota/My",
    "states": "/api/Place/States/{country_id}",
    "cities": "/api/Place/Cities/{state_id}",
    "date_range": "/api/PrayerTime/DateRange",
}

V1 = SITE_DIR / "v1"
TIMES_DIR = V1 / "times"
PLACES_FILE = V1 / "places.json"
INDEX_FILE = V1 / "index.json"

log = logging.getLogger("diyanet-bot")


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
    """'Devrekâni' / 'DEVREKANİ' / 'devrekani' -> 'DEVREKANI'. Uygulamada da aynı kural kullanılmalı."""
    s = (text or "").translate(_TR_UPPER).upper().translate(_FOLD)
    s = unicodedata.normalize("NFKD", s)
    return "".join(ch for ch in s if ch.isascii() and ch.isalnum())


_SENSITIVE = re.compile(r"mail|user|token|password|phone|tckn", re.I)


def mask(obj):
    """Public repo'da Actions logları herkese açıktır; kişisel alanları loglamadan önce maskeler."""
    if isinstance(obj, dict):
        return {k: ("***" if _SENSITIVE.search(str(k)) else mask(v)) for k, v in obj.items()}
    if isinstance(obj, list):
        return [mask(v) for v in obj]
    return obj


# --- API istemcisi -----------------------------------------------------------
class Client:
    TOKEN_TTL = 25 * 60  # access token 30 dk; 5 dk pay

    def __init__(self, email: str, password: str):
        self.session = requests.Session()
        self.session.headers["Accept"] = "application/json"
        self._email, self._password = email, password
        self._token = None
        self._token_at = 0.0

    def login(self) -> None:
        r = self.session.post(BASE_URL + PATHS["login"],
                              json={"email": self._email, "password": self._password}, timeout=30)
        if r.status_code == 429:
            raise QuotaExceeded("login")
        if r.status_code != 200:
            raise ApiError(f"Login başarısız: HTTP {r.status_code}")
        body = r.json()
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
                r = self.session.request(method, BASE_URL + path, timeout=60,
                                         headers={"Authorization": f"Bearer {self._token}"}, **kwargs)
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
            body = r.json()
            if not body.get("success"):
                msg = str(body.get("message") or "")
                if re.search(r"kota|quota|limit", msg, re.I):
                    raise QuotaExceeded(msg)
                raise ApiError(msg or "success=false")
            return body.get("data")
        raise ApiError("3 deneme başarısız")


# --- işler ---------------------------------------------------------------------
def _quota_entries(obj):
    """Quota/My yanıtındaki {controllerName, resultName, ...} kayıtlarını bulur."""
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
    entries = list(_quota_entries(data))
    if not entries:
        log.info("Kota bilgisi: %s", json.dumps(mask(data), ensure_ascii=False)[:1000])
        return
    for q in entries:
        lines = q.get("lines") or []
        low = min((ln.get("remainingUse", 0) for ln in lines), default=None)
        log.info("Kota %s/%s (%s, parametre başına %s): kullanılan %s, aşım %s, parametre sayısı %d, en düşük kalan %s",
                 q.get("controllerName"), q.get("resultName"), q.get("period"), q.get("benefit"),
                 q.get("totalUsed"), q.get("totalExceeded"), len(lines), low)


def places_outdated(doc, today: date) -> bool:
    if not doc or not doc.get("places"):
        return True
    generated = datetime.fromisoformat(doc["generatedAt"]).date()
    return (today - generated).days >= PLACES_MAX_AGE_DAYS


def fetch_places(client: Client) -> dict:
    log.info("İl/ilçe listesi çekiliyor...")
    states = client.request("GET", PATHS["states"].format(country_id=COUNTRY_ID)) or []
    places = []
    for st in states:
        cities = client.request("GET", PATHS["cities"].format(state_id=st["id"])) or []
        for ct in cities:
            places.append({
                "id": int(ct["id"]), "name": ct["name"], "key": norm(ct["name"]),
                "stateId": int(st["id"]), "state": st["name"], "stateKey": norm(st["name"]),
            })
        time.sleep(REQUEST_DELAY / 2)
    if not places:
        raise ApiError("İlçe listesi boş döndü")
    places.sort(key=lambda p: (p["stateKey"], p["key"]))
    doc = {"version": 1, "countryId": COUNTRY_ID, "generatedAt": now_iso(),
           "count": len(places), "places": places}
    write_json(PLACES_FILE, doc)  # yarım liste asla yazılmaz: hata olursa buraya gelinmez
    log.info("%d il, %d ilçe kaydedildi.", len(states), len(places))
    return doc


def build_date_range_request(city_id: int, start: date, end: date) -> dict:
    payload = {
        "cityId": city_id,
        "startDate": start.strftime(DATERANGE_DATE_FORMAT),
        "endDate": end.strftime(DATERANGE_DATE_FORMAT),
    }
    return {"json": payload} if DATERANGE_METHOD == "POST" else {"params": payload}


_TIME_RE = re.compile(r"^\d{2}:\d{2}$")
_TIME_FIELDS = ("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")


def parse_day(item: dict) -> dict:
    d = datetime.strptime(item["gregorianDateShort"], "%d.%m.%Y").date()
    return {
        "date": d.isoformat(),
        **{k: item.get(k) for k in _TIME_FIELDS},
        "hijri": item.get("hijriDateShort"),
        "hijriLong": item.get("hijriDateLong"),
        "qibla": item.get("qiblaTime"),
    }


def validate(days: list, start: date) -> None:
    if len(days) < MIN_VALID_DAYS:
        raise ApiError(f"yetersiz gün sayısı: {len(days)}")
    if days[0]["date"] != start.isoformat():
        raise ApiError(f"ilk gün {days[0]['date']}, beklenen {start}")
    prev = None
    for day in days:
        current = date.fromisoformat(day["date"])
        if prev and current - prev != timedelta(days=1):
            raise ApiError(f"tarih boşluğu/tekrarı: {prev} -> {current}")
        for k in _TIME_FIELDS:
            if not _TIME_RE.match(day.get(k) or ""):
                raise ApiError(f"{day['date']} {k} geçersiz: {day.get(k)!r}")
        prev = current


def fetch_city(client: Client, city_id: int, start: date, end: date) -> list:
    data = client.request(DATERANGE_METHOD, PATHS["date_range"],
                          **build_date_range_request(city_id, start, end)) or []
    days = sorted((parse_day(x) for x in data), key=lambda x: x["date"])
    days = [x for x in days if x["date"] >= start.isoformat()]
    validate(days, start)
    return days


def select_due(places: list, entries: dict, today: date) -> list:
    """Hiç verisi olmayanlar önce, sonra ileri verisi en erken biten / en eski güncellenenler."""
    due = []
    for p in places:
        e = entries.get(str(p["id"]))
        if not e:
            due.append(((0, "", ""), p))
            continue
        valid_to = date.fromisoformat(e["validTo"])
        updated = datetime.fromisoformat(e["updatedAt"]).date()
        if (today - updated).days >= REFRESH_DAYS or (valid_to - today).days < MIN_FORWARD_DAYS:
            due.append(((1, e["validTo"], e["updatedAt"]), p))
    due.sort(key=lambda t: t[0])
    return [p for _, p in due]


def run_times(client: Client, places: list, today: date) -> tuple:
    index_doc = read_json(INDEX_FILE) or {"version": 1, "cities": {}}
    entries = index_doc.setdefault("cities", {})
    due = select_due(places, entries, today)
    batch = due[:DAILY_LIMIT]
    log.info("Güncellenmesi gereken ilçe: %d, bu çalışmada: %d", len(due), len(batch))

    start, end = today, today + timedelta(days=DAYS_AHEAD - 1)
    ok = failed = consecutive = 0
    try:
        for p in batch:
            label = f"{p['state']}/{p['name']} ({p['id']})"
            try:
                days = fetch_city(client, p["id"], start, end)
            except ApiError as e:
                failed += 1
                consecutive += 1
                log.error("%s: %s", label, e)
                if consecutive >= 5:
                    log.error("Art arda 5 hata; çalışma durduruluyor.")
                    break
                continue
            consecutive = 0
            updated_at = now_iso()
            write_json(TIMES_DIR / f"{p['id']}.json", {
                "version": 1, "cityId": p["id"], "name": p["name"], "state": p["state"],
                "updatedAt": updated_at, "validFrom": days[0]["date"], "validTo": days[-1]["date"],
                "days": days,
            })
            entries[str(p["id"])] = {"validFrom": days[0]["date"], "validTo": days[-1]["date"],
                                     "updatedAt": updated_at, "days": len(days)}
            ok += 1
            log.info("%s: %d gün (%s → %s)", label, len(days), days[0]["date"], days[-1]["date"])
            time.sleep(REQUEST_DELAY)
    finally:
        index_doc["generatedAt"] = now_iso()
        index_doc["pending"] = max(len(due) - ok, 0)
        write_json(INDEX_FILE, index_doc)
        log.info("Özet: %d başarılı, %d hatalı, bekleyen %d", ok, failed, index_doc["pending"])
    return ok, failed


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    email, password = os.getenv("DIYANET_EMAIL"), os.getenv("DIYANET_PASSWORD")
    if not email or not password:
        log.error("DIYANET_EMAIL / DIYANET_PASSWORD ortam değişkenleri tanımlı değil.")
        return 2

    client = Client(email, password)
    today = datetime.now(TZ).date()
    try:
        if CHECK_QUOTA:
            log_quota(client)
        places_doc = read_json(PLACES_FILE)
        if MODE == "places" or (MODE == "auto" and places_outdated(places_doc, today)):
            places_doc = fetch_places(client)
            if MODE == "places":
                return 0
        if not places_doc:
            log.error("places.json yok; önce MODE=places ile çalıştırın.")
            return 1
        ok, failed = run_times(client, places_doc["places"], today)
        if failed and not ok:
            return 1
    except QuotaExceeded as e:
        log.warning("Kota sınırına ulaşıldı (%s). İlerleme kaydedildi; kalanlar sonraki çalışmada.", e)
    except ApiError as e:
        log.error("Hata: %s", e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
