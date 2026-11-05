package com.islamyolu.app.main; // Uygulamanızın paketiyle değiştirin

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.NetworkServiceUtil;

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
        return NetworkServiceUtil.isConnected(this);
    }
}
