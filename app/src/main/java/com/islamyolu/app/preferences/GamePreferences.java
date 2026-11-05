package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Oyun verileri: altın miktarı ("GamePreferences") ve yarışma katılım sayaçları ("GameJoinPreferences").
 */
public final class GamePreferences {

    private static final String KEY_GOLD = "userGold";
    private static final int NO_GOLD = -1;

    private final SharedPreferences gold;
    private final SharedPreferences joins;

    GamePreferences(@NonNull SharedPreferences gold, @NonNull SharedPreferences joins) {
        this.gold = gold;
        this.joins = joins;
    }

    public int getGold(int defaultValue) {
        return gold.getInt(KEY_GOLD, defaultValue);
    }

    public void setGold(int amount) {
        gold.edit().putInt(KEY_GOLD, amount).apply();
    }

    /** Altın hiç kaydedilmemişse (ilk açılış) verilen değeri yazar. */
    public void initGoldIfMissing(int initialAmount) {
        if (gold.getInt(KEY_GOLD, NO_GOLD) == NO_GOLD) {
            setGold(initialAmount);
        }
    }

    /** Yarışmaya katılım sayısı. */
    public int getJoinCount(@NonNull String gameKey) {
        return joins.getInt(gameKey, 0);
    }

    /** Katılım sayısını bir artırır ve yeni değeri döndürür. */
    public int incrementJoinCount(@NonNull String gameKey) {
        int next = getJoinCount(gameKey) + 1;
        joins.edit().putInt(gameKey, next).apply();
        return next;
    }
}
