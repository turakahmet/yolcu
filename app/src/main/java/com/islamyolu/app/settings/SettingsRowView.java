package com.islamyolu.app.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.islamyolu.app.R;

/**
 * Tüm ayar ekranlarında kullanılan ortak satır: ikon + başlık + açıklama + sağda ok/anahtar.
 * <p>
 * Anahtarlı satırda dinleyici yalnızca KULLANICI değiştirdiğinde çağrılır; {@link #setChecked}
 * ile koddan verilen değer dinleyiciyi tetiklemez (eski ekrandaki isPressed() kontrolüne gerek kalmaz).
 * Erişilebilirlik: satırın tamamı tek bir öğe olarak okunur (başlık, açıklama ve açık/kapalı durumu).
 */
public class SettingsRowView extends LinearLayout {

    /** Kullanıcı anahtarı değiştirdiğinde çağrılır. */
    public interface OnToggleListener {
        void onToggle(@NonNull SettingsRowView row, boolean checked);
    }

    private static final int TYPE_NAVIGATION = 0;
    private static final int TYPE_TOGGLE = 1;
    private static final int TYPE_INFO = 2;

    private final View iconContainer;
    private final ImageView icon;
    private final TextView title;
    private final TextView subtitle;
    private final MaterialSwitch toggle;
    private final ImageView chevron;
    private final View divider;
    private final int type;

    @Nullable private OnToggleListener toggleListener;
    /** Koddan değer verilirken dinleyiciyi bastırır */
    private boolean bindingValue;

    public SettingsRowView(@NonNull Context context) {
        this(context, null);
    }

    public SettingsRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_settings_row, this, true);
        iconContainer = findViewById(R.id.settingsRowIconContainer);
        icon = findViewById(R.id.settingsRowIcon);
        title = findViewById(R.id.settingsRowTitle);
        subtitle = findViewById(R.id.settingsRowSubtitle);
        toggle = findViewById(R.id.settingsRowSwitch);
        chevron = findViewById(R.id.settingsRowChevron);
        divider = findViewById(R.id.settingsRowDivider);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingsRowView);
        try {
            setIcon(a.getDrawable(R.styleable.SettingsRowView_rowIcon));
            title.setText(a.getText(R.styleable.SettingsRowView_rowTitle));
            setSubtitle(a.getText(R.styleable.SettingsRowView_rowSubtitle));
            type = a.getInt(R.styleable.SettingsRowView_rowType, TYPE_NAVIGATION);
            divider.setVisibility(a.getBoolean(R.styleable.SettingsRowView_rowDivider, false) ? VISIBLE : GONE);
        } finally {
            a.recycle();
        }

        chevron.setVisibility(type == TYPE_NAVIGATION ? VISIBLE : GONE);
        toggle.setVisibility(type == TYPE_TOGGLE ? VISIBLE : GONE);
        if (type == TYPE_TOGGLE) {
            // Satıra dokunmak anahtarı değiştirir; anahtarın kendisi ayrıca odak almaz
            toggle.setClickable(false);
            toggle.setFocusable(false);
            super.setOnClickListener(v -> toggle.toggle());
            toggle.setOnCheckedChangeListener(this::onSwitchChanged);
        }
        if (type != TYPE_INFO) {
            setBackgroundResource(resolveSelectableBackground(context));
            setClickable(true);
            setFocusable(true);
        }
    }

    private static int resolveSelectableBackground(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId;
    }

    private void onSwitchChanged(CompoundButton button, boolean checked) {
        if (!bindingValue && toggleListener != null) {
            toggleListener.onToggle(this, checked);
        }
    }

    public void setIcon(@Nullable Drawable drawable) {
        icon.setImageDrawable(drawable);
        iconContainer.setVisibility(drawable != null ? VISIBLE : GONE);
    }

    public void setIcon(@DrawableRes int resId) {
        setIcon(resId != 0 ? AppCompatResources.getDrawable(getContext(), resId) : null);
    }

    public void setTitle(@StringRes int resId) {
        title.setText(resId);
    }

    public void setSubtitle(@Nullable CharSequence text) {
        subtitle.setText(text);
        subtitle.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
    }

    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    /** Koddan değer verir; {@link OnToggleListener} çağrılmaz. */
    public void setChecked(boolean checked) {
        bindingValue = true;
        toggle.setChecked(checked);
        bindingValue = false;
    }

    public boolean isChecked() {
        return toggle.isChecked();
    }

    public void setOnToggleListener(@Nullable OnToggleListener listener) {
        this.toggleListener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.5f);
        toggle.setEnabled(enabled);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return type == TYPE_TOGGLE ? Switch.class.getName() : super.getAccessibilityClassName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (type == TYPE_TOGGLE) {
            info.setCheckable(true);
            setCheckedState(info, toggle.isChecked());
        }
        info.setText(subtitle.getVisibility() == VISIBLE
                ? title.getText() + ", " + subtitle.getText()
                : title.getText());
    }

    /** Android 16 (API 36) üç durumlu işaret API'si; öncesinde eski boolean API (yalnızca orada). */
    @SuppressWarnings("deprecation")
    private static void setCheckedState(AccessibilityNodeInfo info, boolean checked) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            info.setChecked(checked ? AccessibilityNodeInfo.CHECKED_STATE_TRUE : AccessibilityNodeInfo.CHECKED_STATE_FALSE);
        } else {
            info.setChecked(checked);
        }
    }
}
