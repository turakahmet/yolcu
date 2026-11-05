package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Sıradaki vakte geri sayımın tek hesabı. Ana ekran, aylık takvim, kalıcı bildirim ve widget aynı
 * değeri bu sınıftan alır. Saf Java.
 * <ul>
 *   <li>Sıradaki vakit {@link PrayerTimeline#next(Instant)}: yatsıdan sonra ertesi günün imsakı,
 *       boş (null) vakitler atlanır; her gün kendi gmt farkıyla.</li>
 *   <li>Kalan süre hiçbir zaman eksi olmaz; sıfıra ulaşan vakit anında bir sonraki vakte geçilir.</li>
 *   <li>Saniye aşağı yuvarlanır. Uygulama ve bildirim "03:20:11" (saat bitince "00:20:11"),
 *       widget saniyesiz "03:20" gösterir; saat ve dakika her yerde aynıdır.</li>
 * </ul>
 */
public final class PrayerCountdown {

    private static final long HOUR_MS = 3_600_000L;

    @NonNull public final PrayerTimeline.Moment next;
    /** Kalan süre (ms, ≥ 0). */
    public final long remainingMillis;
    /** Kalan tam saniye (≥ 0). */
    public final long remainingSeconds;
    /** Gösterilen saniyenin değişmesine kalan süre (1..1000 ms); ekranlar bu anda yeniden çizer. */
    public final long millisUntilTick;

    private PrayerCountdown(@NonNull PrayerTimeline.Moment next, long remainingMillis) {
        this.next = next;
        long millis = Math.max(0, remainingMillis);
        this.remainingMillis = millis;
        this.remainingSeconds = millis / 1000;
        long sub = millis % 1000;
        this.millisUntilTick = sub == 0 ? 1000 : sub;
    }

    /** Sıradaki vakte geri sayım; eldeki veri bittiyse (sıradaki vakit yok) null. */
    @Nullable
    public static PrayerCountdown of(@NonNull PrayerTimeline timeline, @NonNull Instant now) {
        PrayerTimeline.Moment next = timeline.next(now);
        if (next == null) {
            return null;
        }
        return new PrayerCountdown(next, Duration.between(now, next.instant).toMillis());
    }

    /** Kalan süre (saniyeye yuvarlanmış, ≥ 0). */
    @NonNull
    public Duration remaining() {
        return Duration.ofSeconds(remainingSeconds);
    }

    /** Uygulama ve bildirim metni: "03:20:11". */
    @NonNull
    public String text() {
        return format(remainingSeconds);
    }

    /** Widget metni (saniyesiz): "03:20". Saat ve dakika {@link #text()} ile aynıdır. */
    @NonNull
    public String textWithoutSeconds() {
        return formatWithoutSeconds(remainingSeconds);
    }

    /** Gösterilen dakikanın değişmesine kalan süre (1..60000 ms); widget bu anda yeniden çizilir. */
    public long millisUntilMinuteTick() {
        long intoMinute = (remainingSeconds % 60) * 1000 + (millisUntilTick % 1000);
        return intoMinute == 0 ? 60_000 : intoMinute;
    }

    // --- Kalıcı bildirim: saat ayrı metin, "DD:ss" sistem kronometresi ---
    // Kronometre saati çift haneli gösteremediği için "03:" + "20:11" olarak ikiye bölünür. Kronometre
    // yalnızca saatin içindeki kalan süreyi sayar; saat değişince bildirim bir kez yenilenir.

    /** Tam saat sayısı (03:20:11 → 3). */
    public long hours() {
        return remainingMillis / HOUR_MS;
    }

    /** Bildirimdeki saat kısmı: "03:". */
    @NonNull
    public String hoursPrefix() {
        return String.format(Locale.ROOT, "%02d:", hours());
    }

    /** Saatin içinde kalan süre (ms); kronometre bunu "DD:ss" olarak geri sayar. */
    public long millisWithinHour() {
        return remainingMillis % HOUR_MS;
    }

    /** "SS:DD:ss", her alan çift haneli. */
    @NonNull
    public static String format(long seconds) {
        long total = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60);
    }

    /** "SS:DD", her alan çift haneli; saniyeler atılır (aşağı yuvarlama). */
    @NonNull
    public static String formatWithoutSeconds(long seconds) {
        long total = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", total / 3600, (total % 3600) / 60);
    }
}
