package com.islamyolu.app.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Uygulamanın yerel tercihlerine (SharedPreferences) tek giriş noktası.
 * <pre>
 * AppPreferences prefs = AppPreferences.from(context);
 * prefs.settings().getReminderMinutes(SettingsPreferences.KEY_NOTIFY_OGLE);
 * prefs.location().getPrayerCitySelection();
 * </pre>
 * Dosya adları ve anahtarlar yalnızca bu pakette bilinir; başka sınıflar
 * {@code getSharedPreferences} çağırmaz. Nesne hafiftir (Android dosyaları kendisi önbelleğe
 * alır), bu yüzden singleton tutulmaz; her yerde {@link #from(Context)} ile alınır.
 */
public final class AppPreferences {

    private final Context appContext;

    private AppPreferences(@NonNull Context context) {
        Context app = context.getApplicationContext();
        this.appContext = app != null ? app : context;
    }

    @NonNull
    public static AppPreferences from(@NonNull Context context) {
        return new AppPreferences(context);
    }

    /** Bildirim, ezan sesi ve hatırlatma ayarları. */
    @NonNull
    public SettingsPreferences settings() {
        return new SettingsPreferences(open(PreferenceFiles.SETTINGS));
    }

    /** Seçili namaz vakti şehri, eski il kaydı ve son bilinen konum. */
    @NonNull
    public LocationPreferences location() {
        return new LocationPreferences(open(PreferenceFiles.LOCATION));
    }

    /** Kullanıcı adı ve profil resmi. */
    @NonNull
    public UserPreferences user() {
        return new UserPreferences(open(PreferenceFiles.USER));
    }

    /** Altın miktarı ve yarışma katılım sayaçları. */
    @NonNull
    public GamePreferences game() {
        return new GamePreferences(open(PreferenceFiles.GAME), open(PreferenceFiles.GAME_JOIN));
    }

    /** Kazanılan rozetler. */
    @NonNull
    public BadgePreferences badges() {
        return new BadgePreferences(open(PreferenceFiles.BADGE));
    }

    /** Hava durumu önbelleği. */
    @NonNull
    public WeatherPreferences weather() {
        return new WeatherPreferences(open(PreferenceFiles.WEATHER));
    }

    /** Widget görünüm ayarları. */
    @NonNull
    public WidgetPreferences widget() {
        return new WidgetPreferences(open(PreferenceFiles.WIDGET));
    }

    /** Tanıtım ekranının tamamlanma durumu. */
    @NonNull
    public IntroPreferences intro() {
        return new IntroPreferences(open(PreferenceFiles.INTRO));
    }

    /** Aynı vakit bildiriminin iki kez gösterilmemesi için gönderim kayıtları. */
    @NonNull
    public SentNotificationPreferences sentNotifications() {
        return new SentNotificationPreferences(open(PreferenceFiles.SENT_NOTIFICATIONS));
    }

    private SharedPreferences open(String name) {
        return appContext.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
}
