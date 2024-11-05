package com.myapp.yolcu.ui.Rozet;

public class Badge {
    private BadgeType badgeType;
    private boolean isEnabled;

    public Badge(BadgeType badgeType, boolean isEnabled) {
        this.badgeType = badgeType;
        this.isEnabled = isEnabled;
    }

    public BadgeType getBadgeType() { return badgeType; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}

