package com.islamyolu.app.sound;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Vakit girince seçilen ezan sesini, açıksa ardından duayı çalan ön plan servisi (mediaPlayback).
 * <p>
 * Neden servis: eskiden ses kısa ömürlü bir alarm alıcısında çalıyordu; uzun ezan (~3 dk) sırasında
 * sistem süreci kapatabiliyordu. Ön plan servisi ses bitene kadar yaşar.
 * <p>
 * Susturma (telefon cepteyken de): <b>ses kısma tuşu</b> ezanı ve duayı durdurur. İki yoldan yakalanır:
 * <ol>
 *   <li>Medya oturumu + {@link VolumeProvider}: çalarken ses tuşları doğrudan buraya gelir (ses en
 *       düşükteyken bile basış algılanır).</li>
 *   <li>Yedek: bazı üreticiler tuşu oturuma yönlendirmezse medya sesi düşer; bu düşüş algılanınca durur.</li>
 * </ol>
 * Bildirimdeki "Durdur" da aynı işi yapar. Arama gelirse (ses odağı kaybı) ezan susar.
 * Eski "ekran açılıp kapanınca dur" davranışı kaldırıldı: ekran kendiliğinden yanınca ezan kesiliyordu.
 */
public class PrayerAudioService extends Service {

    private static final String TAG = "PrayerAudioService";
    private static final String CHANNEL_ID = "ezan_playback_channel";
    private static final int NOTIFICATION_ID = 64302;
    private static final String EXTRA_EZAN_RES = "ezanRes";
    private static final String EXTRA_DUA_RES = "duaRes";
    private static final String ACTION_STOP = "com.islamyolu.app.action.STOP_EZAN";
    /** Uzun ezan + dua ve meal için yeterli üst sınır; takılma olursa servis kendini kapatır */
    private static final long MAX_PLAYBACK_MS = 15 * 60 * 1000L;
    /** VolumeProvider'ın gösterdiği sanal ses ölçeği (yalnızca tuş basışını yakalamak için) */
    private static final int VIRTUAL_VOLUME_MAX = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable maxDurationGuard = this::stopPlayback;
    private final Deque<Integer> queue = new ArrayDeque<>();

    private AudioManager audioManager;
    @Nullable private MediaPlayer player;
    @Nullable private MediaSession session;
    @Nullable private AudioFocusRequest focusRequest;
    @Nullable private ContentObserver volumeObserver;
    private int startMusicVolume;
    private boolean stopping;

    /**
     * Alarm alıcısından çağrılır. Tam zamanlı alarmdan ön plan servisi başlatmak Android 12+'da serbesttir.
     *
     * @return servis başlatılamadıysa false (çağıran eski yolla çalabilir)
     */
    public static boolean start(@NonNull Context context, @RawRes int ezanRes, @RawRes int duaRes) {
        if (ezanRes == 0 && duaRes == 0) return true; // çalınacak ses yok
        Intent intent = new Intent(context, PrayerAudioService.class)
                .putExtra(EXTRA_EZAN_RES, ezanRes)
                .putExtra(EXTRA_DUA_RES, duaRes);
        try {
            ContextCompat.startForegroundService(context, intent);
            return true;
        } catch (RuntimeException e) {
            // Android 12+: tam zamanlı alarm izni yoksa (yaklaşık alarm) arka plandan başlatılamaz
            Log.w(TAG, "Ezan servisi başlatılamadı", e);
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = getSystemService(AudioManager.class);
        createChannel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // startForegroundService sonrası birkaç saniye içinde ön plana geçilmeli
        try {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type);
        } catch (RuntimeException e) {
            Log.e(TAG, "Ön plana geçilemedi", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        // Yeni vakit geldiyse (öncekisi hâlâ çalıyorsa) baştan başlar
        stopping = false;
        handler.removeCallbacks(maxDurationGuard);
        releasePlayer();
        releaseListeners();
        queue.clear();
        int ezanRes = intent.getIntExtra(EXTRA_EZAN_RES, 0);
        int duaRes = intent.getIntExtra(EXTRA_DUA_RES, 0);
        if (ezanRes != 0) queue.add(ezanRes);
        if (duaRes != 0) queue.add(duaRes);

        if (!requestFocus()) {
            Log.d(TAG, "Ses odağı alınamadı (ör. arama sürüyor); ezan çalınmadı");
            stopPlayback();
            return START_NOT_STICKY;
        }
        startVolumeKeyListening();
        handler.postDelayed(maxDurationGuard, MAX_PLAYBACK_MS);
        playNext();
        return START_NOT_STICKY;
    }

    /** Sıradaki sesi çalar (önce ezan, sonra dua); kuyruk bitince servis kapanır. */
    private void playNext() {
        releasePlayer();
        Integer res = queue.poll();
        if (res == null || stopping) {
            stopPlayback();
            return;
        }
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // ayarlarda yazdığı gibi medya ses seviyesi
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        player = MediaPlayer.create(this, res, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (player == null) {
            Log.e(TAG, "Ses dosyası açılamadı: " + res);
            playNext();
            return;
        }
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnCompletionListener(mp -> playNext());
        player.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Oynatma hatası: " + what + "/" + extra);
            playNext();
            return true;
        });
        player.start();
    }

    // --- Ses kısma tuşu ---

    private void startVolumeKeyListening() {
        // 1) Medya oturumu: çalarken ses tuşu basışları buraya yönlendirilir
        session = new MediaSession(this, TAG);
        session.setPlaybackToRemote(new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE,
                VIRTUAL_VOLUME_MAX, VIRTUAL_VOLUME_MAX / 2) {
            @Override
            public void onAdjustVolume(int direction) {
                if (direction < 0) {
                    Log.d(TAG, "Ses kısma tuşu: ezan durduruluyor");
                    handler.post(PrayerAudioService.this::stopPlayback);
                }
            }
        });
        session.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .setActions(PlaybackState.ACTION_STOP)
                .build());
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onStop() {
                stopPlayback();
            }

            @Override
            public void onPause() {
                stopPlayback(); // kulaklık/medya düğmesi
            }
        });
        session.setActive(true);

        // 2) Yedek: tuş oturuma gelmezse sistem medya sesini düşürür; düşüş görülünce durulur
        startMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < startMusicVolume) {
                    Log.d(TAG, "Medya sesi kısıldı: ezan durduruluyor");
                    stopPlayback();
                }
            }
        };
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);
    }

    // --- Ses odağı ---

    private boolean requestFocus() {
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setOnAudioFocusChangeListener(change -> {
                    if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopPlayback(); // arama vb.
                    }
                }, handler)
                .build();
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // --- Kapatma ---

    private void stopPlayback() {
        if (stopping) return;
        stopping = true;
        queue.clear();
        handler.removeCallbacks(maxDurationGuard);
        releasePlayer();
        releaseListeners();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    /** Medya oturumu, ses gözlemcisi ve ses odağı bırakılır. */
    private void releaseListeners() {
        if (volumeObserver != null) {
            getContentResolver().unregisterContentObserver(volumeObserver);
            volumeObserver = null;
        }
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }
        if (focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // --- Bildirim ---

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        // Sessiz kanal: sesi zaten servis çalıyor; bildirim yalnızca durdurma yolunu gösterir
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.ezan_playback_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.ezan_playback_channel_description));
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        PendingIntent stopIntent = PendingIntent.getService(this, 0,
                new Intent(this, PrayerAudioService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.crescent)
                .setContentTitle(getString(R.string.ezan_playing_title))
                .setContentText(getString(R.string.ezan_playing_text))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.ezan_playing_text)))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setDeleteIntent(stopIntent)
                .addAction(R.drawable.ic_settings_pause, getString(R.string.ezan_stop), stopIntent)
                .build();
    }
}
