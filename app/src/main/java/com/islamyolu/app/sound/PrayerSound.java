package com.islamyolu.app.sound;

import androidx.annotation.RawRes;

import com.islamyolu.app.R;

/**
 * Ayarlardaki ezan sesi ve ezan duası seçimlerinin ses dosyası karşılıkları (tek tanım).
 * Seçim sırası R.array.ezan_sound_options / ezan_dua_options ile aynıdır.
 */
public final class PrayerSound {

    private PrayerSound() {
    }

    /** Ezan sesi: 0 Ezan, 1 Kısa ezan, 2 Muhabbet, 3 Kısa bildirim, 4 Kapalı → dosya; kapalıysa 0. */
    @RawRes
    public static int ezanRes(int ezanType) {
        switch (ezanType) {
            case 0:
                return R.raw.ezan_uzun;
            case 1:
                return R.raw.ezan_kisa;
            case 2:
                return R.raw.ezan_bildirim;
            case 3:
                return R.raw.ezan_bildirim_2;
            default:
                return 0;
        }
    }

    /** Ezan duası: 0 Dua, 1 Dua ve meal, 2 Kapalı → dosya; kapalıysa 0. */
    @RawRes
    public static int duaRes(int duaType) {
        switch (duaType) {
            case 0:
                return R.raw.ezan_dua;
            case 1:
                return R.raw.ezan_dua_meal;
            default:
                return 0;
        }
    }
}
