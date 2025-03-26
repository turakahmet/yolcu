package com.islamyolu.app.main; // Uygulamanızın paketiyle değiştirin

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.islamyolu.app.R;

public class NetworkCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_check);

        // İnternet bağlantısını kontrol et
        if (!isInternetAvailable()) {
            TextView messageTextView = findViewById(R.id.messageTextView);
            messageTextView.setText(R.string.no_Internet);
        } else {
            // Ana aktiviteyi başlat
            // Burada ana aktivitenizi başlatın, örneğin:
            // startActivity(new Intent(this, MainActivity.class));
            finish(); // Bu aktiviteyi kapat
        }
    }

    // İnternet bağlantısını kontrol eden metot
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // SDK 27 ve üstü için modern API kullanımı
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
