package com.islamyolu.app.prayertimes.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.LocationAccess;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.databinding.ActivityCitySelectionBinding;
import com.islamyolu.app.databinding.ItemSelectionStepBinding;
import com.islamyolu.app.prayertimes.PlaceNameNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Diyanet namaz vakitleri için şehir seçimi: Ülke → Eyalet/İl → Şehir/İlçe.
 * Şehir seçilince 1 yıllık vakitler indirilip Room'a yazılır, seçim tercihlere kaydedilir.
 * Mevcut konuma göre il bulma akışından (Ayarlar > Güncelle) bağımsızdır.
 */
public class CitySelectionActivity extends AppCompatActivity {

    private ActivityCitySelectionBinding binding;
    @Nullable private AlertDialog suggestionDialog;
    private CitySelectionViewModel viewModel;
    private PlaceAdapter adapter;
    private List<PlaceItem> allItems = Collections.emptyList();
    private CitySelectionViewModel.Level shownLevel;
    private CitySelectionViewModel.Level itemsLevel;
    private boolean scrollToTopOnNextList;
    private boolean filterHasNoResult;
    private boolean allStepsDone;

    /** İzin, konum açık/kapalı ve ayarlara yönlendirme merkezi LocationAccess'te. Sonuç ViewModel'e gider. */
    private final LocationAccess locationAccess = LocationAccess.register(this,
            new LocationAccess.Options()
                    .fresh(true) // kullanıcı yer değiştirmiş olabilir; eski konum kullanılmaz
                    .servicesOffMessage(R.string.city_selection_location_off_message)
                    .declineLabel(R.string.city_selection_location_confirm_no),
            new LocationAccess.Callback() {
                @Override
                public void onLocation(@NonNull Location location) {
                    viewModel.onLocationReceived(location.getLatitude(), location.getLongitude());
                }

                @Override
                public void onUnavailable(@NonNull LocationService.Unavailable reason) {
                    viewModel.onLocationUnavailable(reason);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCitySelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(CitySelectionViewModel.class);
        // Ekran okuyucu adım başlığını başlık olarak duyurur (API 27 dahil)
        ViewCompat.setAccessibilityHeading(binding.stepTitle, true);

        adapter = new PlaceAdapter(viewModel::onItemClicked);
        int columns = getResources().getInteger(R.integer.city_selection_columns);
        binding.placeList.setLayoutManager(new GridLayoutManager(this, columns));
        binding.placeList.setAdapter(adapter);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scrollToTopOnNextList = true; // arama sonucu en üstten gösterilir
                applyFilter();
            }
        });
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.useLocationButton.setOnClickListener(v -> startLocationFlow());

        // Alt seviyedeyken geri hareketi bir üst listeye döner; ülke listesinde ekran kapanır
        OnBackPressedCallback backToParent = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.goBack();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToParent);

        viewModel.getLevel().observe(this, level -> {
            backToParent.setEnabled(level != CitySelectionViewModel.Level.COUNTRY);
            binding.stepTitle.setText(stepTitle(level));
            if (shownLevel != null && shownLevel != level) {
                binding.searchInput.setText(null);
            }
            shownLevel = level;
            renderSteps();
            updateLocationButton();
        });
        viewModel.getSelection().observe(this, selected -> renderSteps());
        bindStepClick(binding.stepCountry, CitySelectionViewModel.Level.COUNTRY);
        bindStepClick(binding.stepState, CitySelectionViewModel.Level.STATE);
        viewModel.getItems().observe(this, items -> {
            // Yeni seviyenin listesi gelince en üstten gösterilir (aynı listede güncellemede kaydırma korunur)
            if (itemsLevel != viewModel.getLevel().getValue()) {
                itemsLevel = viewModel.getLevel().getValue();
                scrollToTopOnNextList = true;
            }
            allItems = items;
            applyFilter();
        });
        viewModel.getLoading().observe(this, loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.placeList.setAlpha(loading ? 0.4f : 1f);
            binding.searchLayout.setEnabled(!loading);
            updateEmptyState();
            updateLocationButton();
        });
        viewModel.getLocationSuggestion().observe(this, this::showLocationSuggestion);
        viewModel.getMessage().observe(this, this::showMessage);
        viewModel.getCompletion().observe(this, completion -> {
            if (completion != null && completion.consume()) {
                allStepsDone = true;
                renderSteps();
                Toast.makeText(this, getString(R.string.city_selection_success, completion.cityName),
                        Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        // Ekran 1. adımdan (ülke) açılır; konum yalnızca "Mevcut konumumu bul" ile aranır
        viewModel.start();
    }

    // --- Adım göstergesi ---

    private void renderSteps() {
        int current = levelIndex(viewModel.getLevel().getValue());
        String[] selected = viewModel.getSelection().getValue();
        if (selected == null) {
            selected = new String[2];
        }
        ItemSelectionStepBinding[] steps = {binding.stepCountry, binding.stepState, binding.stepCity};
        String[] labels = {
                selected[0] != null ? selected[0] : getString(R.string.city_selection_step_label_country),
                selected[1] != null ? selected[1] : getString(R.string.city_selection_step_label_state),
                getString(R.string.city_selection_step_label_city)};
        for (int i = 0; i < steps.length; i++) {
            boolean done = allStepsDone || i < current;
            boolean active = !done && i == current;
            renderStep(steps[i], i, labels[i], done, active, i <= current || allStepsDone, i < current || allStepsDone);
        }
    }

    private void renderStep(ItemSelectionStepBinding step, int index, String label, boolean done, boolean active,
                            boolean lineStartReached, boolean lineEndReached) {
        int color = ContextCompat.getColor(this, done ? R.color.step_done
                : active ? R.color.step_current : R.color.step_pending);
        step.circle.setBackgroundTintList(ColorStateList.valueOf(color));
        step.circle.setText(done ? "✓" : String.valueOf(index + 1));
        step.label.setText(label);
        step.label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        step.label.setAlpha(done || active ? 1f : 0.6f);

        // Bağlantı çizgileri: ilk adımın solunda ve son adımın sağında çizgi yok
        step.lineStart.setVisibility(index == 0 ? View.INVISIBLE : View.VISIBLE);
        step.lineEnd.setVisibility(index == 2 ? View.INVISIBLE : View.VISIBLE);
        step.lineStart.setBackgroundColor(ContextCompat.getColor(this, lineStartReached ? R.color.step_done : R.color.step_pending));
        step.lineEnd.setBackgroundColor(ContextCompat.getColor(this, lineEndReached ? R.color.step_done : R.color.step_pending));

        // Yalnızca tamamlanmış adıma dokunulup geri dönülebilir
        step.getRoot().setClickable(done && !allStepsDone);
        step.getRoot().setContentDescription(getString(R.string.city_selection_step_a11y, index + 1, label));
    }

    private void bindStepClick(ItemSelectionStepBinding step, CitySelectionViewModel.Level target) {
        step.getRoot().setOnClickListener(v -> {
            if (target.ordinal() < levelIndex(viewModel.getLevel().getValue())) {
                viewModel.goToLevel(target);
            }
        });
    }

    private static int levelIndex(@Nullable CitySelectionViewModel.Level level) {
        return level != null ? level.ordinal() : 0;
    }

    @Override
    protected void onDestroy() {
        // Pencere sızıntısını önle; öneri ViewModel'de durduğu için yeni ekranda tekrar gösterilir
        if (suggestionDialog != null) {
            suggestionDialog.setOnDismissListener(null);
            suggestionDialog.dismiss();
        }
        super.onDestroy();
    }

    private void startLocationFlow() {
        viewModel.onLocatingStarted();
        locationAccess.requestSingleLocation();
    }

    private void showLocationSuggestion(@Nullable CitySelectionViewModel.LocationSuggestion suggestion) {
        if (suggestionDialog != null) {
            suggestionDialog.setOnDismissListener(null);
            suggestionDialog.dismiss();
            suggestionDialog = null;
        }
        if (suggestion == null) {
            return;
        }
        suggestionDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.city_selection_location_confirm_title)
                .setMessage(locationSummary(suggestion))
                .setPositiveButton(R.string.city_selection_location_confirm_yes, (d, w) -> viewModel.acceptLocationSuggestion())
                .setNegativeButton(R.string.city_selection_location_confirm_no, (d, w) -> viewModel.rejectLocationSuggestion())
                .create();
        // Dışarı dokunup kapatmak "listeden seçeceğim" anlamına gelir
        suggestionDialog.setOnDismissListener(d -> {
            if (viewModel.getLocationSuggestion().getValue() != null) {
                viewModel.rejectLocationSuggestion();
            }
        });
        suggestionDialog.show();
    }

    /**
     * Ülke: Türkiye / Şehir: İstanbul / İlçe: Gaziosmanpaşa (yurt dışında Eyalet / Şehir).
     * İlçe Diyanet listesinde yoksa hangi yerin vakitlerinin kullanılacağı ayrıca yazılır.
     */
    private String locationSummary(CitySelectionViewModel.LocationSuggestion s) {
        Locale uiLocale = getResources().getConfiguration().getLocales().get(0);
        // Türkçe adlarda i/İ doğru dönüşsün diye Türkçe kural; yabancı yer adlarında dilden bağımsız kural
        Locale placeLocale = s.turkey ? Locale.forLanguageTag("tr") : Locale.ROOT;
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.location_line_country, displayCase(s.countryName, uiLocale)));
        if (s.stateName != null) {
            sb.append('\n').append(getString(s.turkey ? R.string.location_line_province : R.string.location_line_state,
                    displayCase(s.stateName, placeLocale)));
        }
        if (s.districtName != null) {
            sb.append('\n').append(getString(s.turkey ? R.string.location_line_district : R.string.location_line_city,
                    displayCase(s.districtName, placeLocale)));
        }
        if (s.centerName != null) {
            sb.append("\n\n").append(getString(R.string.location_line_prayer_center, displayCase(s.centerName, placeLocale)));
        }
        return sb.toString();
    }

    /** "GAZİOSMANPAŞA" → "Gaziosmanpaşa", "AFYONKARAHİSAR" → "Afyonkarahisar" */
    private static String displayCase(String value, Locale locale) {
        String lower = value.toLowerCase(locale);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean wordStart = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            sb.append(wordStart ? String.valueOf(c).toUpperCase(locale) : String.valueOf(c));
            wordStart = c == ' ' || c == '-' || c == '/' || c == '(';
        }
        return sb.toString();
    }

    private void updateLocationButton() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean countryLevel = viewModel.getLevel().getValue() == CitySelectionViewModel.Level.COUNTRY;
        binding.useLocationButton.setVisibility(countryLevel && !loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Arama: "aladag", "Aladağ" ve "ALADAĞ" aynı sonucu verir (botla aynı normalizasyon). */
    private void applyFilter() {
        Editable text = binding.searchInput.getText();
        String query = PlaceNameNormalizer.normalize(text != null ? text.toString() : null);
        List<PlaceItem> filtered = allItems;
        if (!query.isEmpty()) {
            filtered = new ArrayList<>();
            for (PlaceItem item : allItems) {
                if (item.searchKey.contains(query)) {
                    filtered.add(item);
                }
            }
        }
        filterHasNoResult = !query.isEmpty() && filtered.isEmpty() && !allItems.isEmpty();
        // submitList asenkron: kaydırma liste yerleştikten sonra yapılır, yoksa eski konum korunur
        // ve ilk satır (ör. ADANA) görünür alanın üstünde kalır
        boolean scrollToTop = scrollToTopOnNextList;
        scrollToTopOnNextList = false;
        adapter.submitList(filtered, () -> {
            if (scrollToTop) {
                binding.placeList.scrollToPosition(0);
            }
        });
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        binding.emptyText.setVisibility(filterHasNoResult && !loading ? View.VISIBLE : View.GONE);
    }

    private void showMessage(CitySelectionViewModel.Message message) {
        if (message == null) {
            binding.messagePanel.setVisibility(View.GONE);
            return;
        }
        binding.messagePanel.setVisibility(View.VISIBLE);
        binding.messageText.setText(message.argument != null
                ? getString(message.textRes, message.argument)
                : getString(message.textRes));
        binding.retryButton.setVisibility(message.retryable ? View.VISIBLE : View.GONE);
    }

    private int stepTitle(CitySelectionViewModel.Level level) {
        switch (level) {
            case STATE:
                return R.string.city_selection_step_state;
            case CITY:
                return R.string.city_selection_step_city;
            default:
                return R.string.city_selection_step_country;
        }
    }
}
