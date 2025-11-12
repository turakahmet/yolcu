package com.islamyolu.app.profile;

import com.islamyolu.app.R;

public enum Avatar {
    MAN_1(R.drawable.man_1),
    MAN_2(R.drawable.man_2),
    MAN_3(R.drawable.man_3),
    MAN_4(R.drawable.man_4),
    MAN_5(R.drawable.man_5),
    MAN_6(R.drawable.man_6),
    WOMEN_1(R.drawable.women_1),
    WOMEN_2(R.drawable.women_2),
    WOMEN_3(R.drawable.women_3),
    WOMEN_4(R.drawable.women_4),
    WOMEN_5(R.drawable.women_5),
    WOMEN_6(R.drawable.women_6);

    private final int resId;

    Avatar(int resId) {
        this.resId = resId;
    }

    public int getResId() {
        return resId;
    }

    // Enum adı ile drawable ID alma
    public static Avatar fromName(String name) {
        try {
            return Avatar.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return MAN_1; // default avatar
        }
    }
}
