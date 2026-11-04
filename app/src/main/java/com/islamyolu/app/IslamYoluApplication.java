package com.islamyolu.app;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.islamyolu.app.data.AppDatabase;

/**
 * Uygulama düzeyinde paylaşılan nesneler (gereksiz singleton yerine tek örnek burada tutulur).
 * Veritabanı ilk ihtiyaç anında oluşturulur; açılışta disk işlemi yapılmaz.
 */
public class IslamYoluApplication extends Application {

    private volatile AppDatabase database;

    @NonNull
    public static IslamYoluApplication from(@NonNull Context context) {
        return (IslamYoluApplication) context.getApplicationContext();
    }

    @NonNull
    public AppDatabase getDatabase() {
        AppDatabase result = database;
        if (result == null) {
            synchronized (this) {
                result = database;
                if (result == null) {
                    result = AppDatabase.create(this);
                    database = result;
                }
            }
        }
        return result;
    }
}
