package com.myapp.yolcu; // Uygulamanızın paketiyle değiştirin

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
