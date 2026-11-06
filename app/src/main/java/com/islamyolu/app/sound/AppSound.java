package com.islamyolu.app.sound;

import androidx.annotation.RawRes;

import com.islamyolu.app.R;

/** Uygulama içi kısa efekt sesleri (oyunlar, düğmeler, rozet). Çalmak için {@link SoundManager#play}. */
public enum AppSound {
    COMMON_BUTTON(R.raw.common_button),
    PLAY_GAME(R.raw.play_game),
    SUCCESS(R.raw.success),
    FAILURE(R.raw.failure),
    JOKER(R.raw.joker),
    NO_JOKER(R.raw.no_used_joker),
    HINT_JOKER(R.raw.wg_hint_joker),
    TIMEOUT(R.raw.timeout),
    WIN_GAME(R.raw.win_game),
    BADGE(R.raw.rozet_success),
    MATCH_SUCCESS(R.raw.matches_success),
    MATCH_FAIL(R.raw.matches_fail);

    @RawRes final int res;

    AppSound(@RawRes int res) {
        this.res = res;
    }
}
