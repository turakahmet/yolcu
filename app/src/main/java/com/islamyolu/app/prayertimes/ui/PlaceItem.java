package com.islamyolu.app.prayertimes.ui;

import androidx.annotation.NonNull;

import com.islamyolu.app.prayertimes.PlaceNameNormalizer;

/** Seçim listesindeki bir ülke, eyalet/il veya şehir/ilçe satırı. */
final class PlaceItem {

    final int id;
    @NonNull final String name;
    /** Aramada kullanılan normalize ad (ör. "Aladağ" araması "ALADAG" ile eşleşir). */
    @NonNull final String searchKey;
    /** Diyanet bu şehir için vakit yayımlamıyor (404). */
    final boolean unavailable;

    PlaceItem(int id, @NonNull String name, boolean unavailable) {
        this.id = id;
        this.name = name;
        this.searchKey = PlaceNameNormalizer.normalize(name);
        this.unavailable = unavailable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceItem)) return false;
        PlaceItem other = (PlaceItem) o;
        return id == other.id && unavailable == other.unavailable && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * id + name.hashCode()) + (unavailable ? 1 : 0);
    }
}
