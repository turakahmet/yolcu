package com.myapp.yolcu.ui.Service;

import android.content.Context;
import android.media.MediaPlayer;

import com.myapp.yolcu.R;

public class SoundService {
    private MediaPlayer mediaPlayer;
    private Context context;

    // Constructor: Context alır ve SoundService'i başlatır
    public SoundService(Context context) {
        this.context = context;
    }

    // Ses oynatma metodu
    public void playSound(int soundResourceId) {
        // Daha önce bir MediaPlayer nesnesi çalışıyorsa, önce durdur ve serbest bırak
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // Yeni MediaPlayer nesnesi başlat ve ses dosyasını çal
        mediaPlayer = MediaPlayer.create(context, soundResourceId);
        mediaPlayer.start();

        // Ses dosyası tamamlandığında MediaPlayer'i serbest bırak
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
        });
    }

    // Ses durdurma metodu
    public void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // SoundService'i temizleme metodu
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    public void successAnswer() {
        playSound(R.raw.success);
    }
    public void wrongAnswer() {
        playSound(R.raw.failure);
    }
    public void wgHintJokerEffect() {
        playSound(R.raw.wg_hint_joker);
    }

    public void jokerEffect() {
        playSound(R.raw.joker);
    }
    public void noUsedJokerEffect() {
        playSound(R.raw.no_used_joker);
    }

    public void timeoutEffect() {
        playSound(R.raw.timeout);
    }
    public void winGameEffect() {
        playSound(R.raw.win_game);
    }
    public void playGameButtonEffect() {
        playSound(R.raw.play_game);
    }
    public void commonButtonEffect() {
        playSound(R.raw.common_button);
    }


    public void successRozet() {playSound(R.raw.rozet_success);
    }

    public void matchesSuccessButtonEffect() {
        playSound(R.raw.matches_success);
    }
    public void matchesFailButtonEffect() {
        playSound(R.raw.matches_fail);
    }
}

