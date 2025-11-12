package com.islamyolu.app.Service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkServiceUtil {

    private static NetworkServiceUtil instance;
    private final Context appContext;

    private NetworkServiceUtil(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized NetworkServiceUtil getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkServiceUtil(context);
        }
        return instance;
    }

    // İnternet var mı kontrol et
    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    // Popup-1: Sadece uyarı göster
    public void showNoInternetDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Bağlantı Hatası")
                .setMessage("İnternet bağlantınızı kontrol edin.")
                .setPositiveButton("Tamam", null)
                .setCancelable(false)
                .show();
    }

    // Popup-2: Kullanıcıya devam etmek istiyor musun sorusu
    public void showConfirmNoInternetDialog(Activity activity, OnUserChoiceListener listener) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle("İnternet Yok")
                .setMessage("İnternet bağlantınız yok. Devam etmek istiyor musunuz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    if (listener != null) listener.onContinue();
                })
                .setNegativeButton("Hayır", (dialog, which) -> {
                    if (listener != null) listener.onCancel();
                })
                .setCancelable(false)
                .show();
    }

    // Popup-2: Parametreli (mesajı dışarıdan alıyor)
    public void showConfirmNoInternetDialog(Activity activity,
                                            String title,
                                            String message,
                                            OnUserChoiceListener listener) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Evet", (dialog, which) -> {
                    if (listener != null) listener.onContinue();
                })
                .setNegativeButton("Hayır", (dialog, which) -> {
                    if (listener != null) listener.onCancel();
                })
                .setCancelable(false)
                .show();
    }

    // Callback interface
    public interface OnUserChoiceListener {
        void onContinue();

        void onCancel();
    }
}


