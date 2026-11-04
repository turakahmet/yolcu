package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Ülke/şehir listesi gibi yalnızca okunan uzak verinin sonucu. */
public final class RemoteResult<T> {

    public enum Status {
        SUCCESS,
        /** Bağlantı/zaman aşımı */
        NETWORK_ERROR,
        /** HTTP hatası veya okunamayan gövde */
        SERVER_ERROR
    }

    private final Status status;
    @Nullable private final T data;

    private RemoteResult(Status status, @Nullable T data) {
        this.status = status;
        this.data = data;
    }

    static <T> RemoteResult<T> success(@NonNull T data) {
        return new RemoteResult<>(Status.SUCCESS, data);
    }

    static <T> RemoteResult<T> failure(@NonNull Status status) {
        return new RemoteResult<>(status, null);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** Yalnızca {@link Status#SUCCESS} durumunda dolu. */
    @Nullable
    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
