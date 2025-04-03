package com.islamyolu.app.feedback;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.islamyolu.app.R;

import java.util.Objects;

public class FeedbackActivity extends AppCompatActivity {
    private TextInputEditText nameInput, descriptionInput;
    private Spinner topicSpinner;

    private Button submitButton;
//    private ReviewManager reviewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_form);
        // ActionBar'ı gizle
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        initializeViews();

// "Bir konu seçiniz" gibi hint metnini ekle
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.feedback_topic, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(adapter);

// Hint olarak "Bir konu seçiniz" metnini ekle
        topicSpinner.setSelection(0, false); // Hint olarak eklediğimiz ilk öğeyi otomatik seçme

        topicSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Eğer hint öğesi (ilk öğe) seçili ise, gri renk yap
                if (topicSpinner.getSelectedItemPosition() == 0) {
                    // Spinner'ın altındaki TextView'ı al
                    TextView selectedView = (TextView) topicSpinner.getSelectedView();
                    selectedView.setTextColor(Color.GRAY);  // Gri renk yap
                }

                // Erişilebilirlik için performClick çağrısı
                v.performClick();  // Tıklama olayını düzgün şekilde işleme alır.
                return false; // Bu olayın başka bir yerde işlenmesini engelleme
            }
        });


        showInAppReview();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Spinner'daki seçimi kontrol et
                if (topicSpinner.getSelectedItemPosition() == 0) {
                    Toast.makeText(FeedbackActivity.this, "Lütfen bir konu seçiniz", Toast.LENGTH_SHORT).show();
                    return;  // Konu seçilmediyse, işlem durdurulsun
                }

                // Ad alanının boş olup olmadığını kontrol et
                TextInputEditText nameInput = findViewById(R.id.nameInput);
                if (Objects.requireNonNull(nameInput.getText()).toString().trim().isEmpty()) {
                    Toast.makeText(FeedbackActivity.this, "Adınızı giriniz", Toast.LENGTH_SHORT).show();
                    return;  // Ad alanı boşsa işlem durdurulsun
                }

                // Açıklama alanının boş olup olmadığını kontrol et
                TextInputEditText descriptionInput = findViewById(R.id.descriptionInput);
                if (Objects.requireNonNull(descriptionInput.getText()).toString().trim().isEmpty()) {
                    Toast.makeText(FeedbackActivity.this, "Açıklama kısmını doldurunuz", Toast.LENGTH_SHORT).show();
                    return;  // Açıklama alanı boşsa işlem durdurulsun
                }
                sendEmail();
            }
        });

    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        submitButton = findViewById(R.id.submitButton);
        topicSpinner = findViewById(R.id.topicSpinner);

    }

    private void showInAppReview() {
        // ReviewManager oluştur
        ReviewManager manager = ReviewManagerFactory.create(this);

        // İnceleme bilgilerini talep et
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Başarılı bir şekilde ReviewInfo alındı
                ReviewInfo reviewInfo = task.getResult();

                // İnceleme akışını başlat
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // İnceleme akışı tamamlandı
                    // Kullanıcının puanlama yapıp yapmadığı bilgisi API tarafından sağlanmaz
                    Log.d("InAppReview", "İnceleme akışı tamamlandı.");
                });
            } else {
                // Hata durumunda güvenli hata kontrolü
                Exception exception = task.getException();
                if (exception instanceof ReviewException) {
                    int reviewErrorCode = ((ReviewException) exception).getErrorCode();
                    Log.e("InAppReview", "İnceleme isteği başarısız oldu. Hata kodu: " + reviewErrorCode);
                } else {
                    Log.e("InAppReview", "Bilinmeyen hata: " + exception);
                }
            }
        });
    }

    private void sendEmail() {
        String name = Objects.requireNonNull(nameInput.getText()).toString();

        String description = Objects.requireNonNull(descriptionInput.getText()).toString();

        String subject = "İslam Yolu App Bildirim ";
        String message = getString(R.string.feedBackAd) + ": " + name + "\n" + getString(R.string.feeedBackTopicLabel) + ": " + topicSpinner.getSelectedItem().toString() + "\n" + getString(R.string.FeedBackAciklama) + ": " + description + "\n" + getString(R.string.feedBackAltYazi);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // Yalnızca e-posta uygulamalarını filtreler
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"techturak@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.feedBackEmailSecimBaslik)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(FeedbackActivity.this, getString(R.string.feedBackEmailSecimYok), Toast.LENGTH_LONG).show();
        }
    }
}
