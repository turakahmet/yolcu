package com.islamyolu.app.Service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.islamyolu.app.R;

/**
 * İnternet bağlantısı kontrolü ve "internet yok" pencereleri için tek merkez; ekranlar
 * ConnectivityManager'a doğrudan erişmez.
 */
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

    /** Arka plan ve widget gibi örnek tutmayan yerler için kısa yol. */
    public static boolean isConnected(Context context) {
        return getInstance(context).isConnected();
    }

    /**
     * Etkin ağ internet sunuyor mu. "VALIDATED" şartı aranmaz: Google bağlantı denetimine
     * erişemeyen cihazlarda (ör. GMS'siz Huawei/Honor) internet olduğu halde yok sayılıyordu.
     */
    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }

    // Popup-1: Sadece uyarı göster
    public void showNoInternetDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle(R.string.network_error_title)
                .setMessage(R.string.no_Internet)
                .setPositiveButton(R.string.tamam, null)
                .setCancelable(false)
                .show();
    }

    // Popup-2: Kullanıcıya devam etmek istiyor musun sorusu
    public void showConfirmNoInternetDialog(Activity activity, OnUserChoiceListener listener) {
        if (activity == null || activity.isFinishing()) return;
        showConfirmNoInternetDialog(activity, activity.getString(R.string.no_internet_title),
                activity.getString(R.string.no_internet_continue_message), listener);
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
                .setPositiveButton(R.string.evet, (dialog, which) -> {
                    if (listener != null) listener.onContinue();
                })
                .setNegativeButton(R.string.hayir, (dialog, which) -> {
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
