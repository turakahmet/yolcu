package com.islamyolu.app.preferences;

/**
 * Uygulamadaki tüm SharedPreferences dosya adları tek yerde.
 * <p>
 * Adlar önceki sürümlerle birebir aynıdır; değiştirilirse kullanıcının kayıtlı ayarları, puanı,
 * rozetleri ve seçili şehri kaybolur. Yeni dosya gerekiyorsa buraya eklenir.
 */
final class PreferenceFiles {

    static final String SETTINGS = "SettingPrefs";
    static final String LOCATION = "location_prefs";
    static final String USER = "user_prefs";
    static final String GAME = "GamePreferences";
    static final String GAME_JOIN = "GameJoinPreferences";
    static final String BADGE = "BadgePreferences";
    static final String WEATHER = "WeatherPrefs";
    static final String WIDGET = "com.islamyolu.app.widget.PrayerTimesWidget";
    static final String INTRO = "IntroPrefs";
    static final String SENT_NOTIFICATIONS = "SentNotifications";

    private PreferenceFiles() {
    }
}
