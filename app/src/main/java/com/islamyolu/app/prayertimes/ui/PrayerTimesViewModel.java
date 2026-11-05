package com.islamyolu.app.prayertimes.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.Service.NetworkServiceUtil;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.PrayerTimesRepository;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.prayertimes.sync.PrayerTimesSyncScheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Ekranların namaz vakitlerini Room'dan LiveData ile okuduğu yer (§5). Sorgu ana thread'de çalışmaz;
 * Room'a yeni veri yazılınca (senkron, şehir değişimi) ekran kendiliğinden güncellenir.
 * Durumlar §6'ya göredir: veri varsa internet olmasa da normal çalışır, hiçbir uyarı göstermez.
 */
public class PrayerTimesViewModel extends AndroidViewModel {

    public enum Status {
        /** Vakitler gösterilebilir (internet gerekmez) */
        OK,
        /** Şehir hiç seçilmemiş (ilk kurulum) */
        NO_CITY,
        /** Bugünün verisi yok, internet var: arka planda yenileniyor */
        UPDATING,
        /** Bugünün verisi yok, internet yok: "Vakitler güncel değil, internete bağlanın" */
        OUTDATED_OFFLINE
    }

    public static final class State {
        @NonNull public final Status status;
        @Nullable public final PrayerCitySelection selection;
        @NonNull public final PrayerTimeline timeline;

        State(@NonNull Status status, @Nullable PrayerCitySelection selection, @NonNull PrayerTimeline timeline) {
            this.status = status;
            this.selection = selection;
            this.timeline = timeline;
        }
    }

    private final PrayerTimesRepository repository;
    private final LocationPreferences preferences;
    private final NetworkServiceUtil network;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();

    @Nullable private LiveData<List<PrayerDayEntity>> source;
    @Nullable private PrayerCitySelection selection;
    private boolean syncRequested;

    public PrayerTimesViewModel(@NonNull Application application) {
        super(application);
        repository = IslamYoluApplication.from(application).getPrayerTimesRepository();
        preferences = AppPreferences.from(application).location();
        network = NetworkServiceUtil.getInstance(application);
    }

    public LiveData<State> getState() {
        return state;
    }

    /**
     * Seçili şehri ve gün aralığını yeniden okur. Ekran her öne geldiğinde çağrılır
     * (şehir değişmiş veya gün dönmüş olabilir).
     *
     * @param daysAhead bugünden sonra kaç gün gerekli (ana ekran: yarının imsakı için 2, aylık takvim: 31)
     */
    public void refresh(int daysAhead) {
        selection = preferences.getPrayerCitySelection();
        if (source != null) {
            state.removeSource(source);
            source = null;
        }
        if (selection == null) {
            state.setValue(new State(Status.NO_CITY, null, PrayerTimeline.of(Collections.emptyList())));
            return;
        }
        LocalDate from = PrayerTimesRepository.timelineWindow(Instant.now())[0];
        LocalDate to = from.plusDays(daysAhead + 2L);
        source = repository.observeDays(selection.getCityId(), from, to);
        state.addSource(source, this::onDays);
    }

    private void onDays(@Nullable List<PrayerDayEntity> days) {
        PrayerTimeline timeline = PrayerTimeline.of(days != null ? days : Collections.emptyList());
        if (timeline.today(Instant.now()) != null) {
            state.setValue(new State(Status.OK, selection, timeline));
            return;
        }
        // Bugünün verisi yok (bitti veya hiç inmedi): mevcut veriye dokunmadan yenilemeyi dene
        if (network.isConnected()) {
            if (!syncRequested) {
                syncRequested = true;
                PrayerTimesSyncScheduler.requestSyncNow(getApplication());
            }
            state.setValue(new State(Status.UPDATING, selection, timeline));
        } else {
            state.setValue(new State(Status.OUTDATED_OFFLINE, selection, timeline));
        }
    }

    /** "Yenile": internet geldiyse indirmeyi yeniden dener. */
    public void retrySync() {
        syncRequested = false;
        if (source != null && source.getValue() != null) {
            onDays(source.getValue());
        }
    }
}
