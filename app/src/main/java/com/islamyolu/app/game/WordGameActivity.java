package com.islamyolu.app.game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.Entity.Question;
import com.islamyolu.app.R;
import com.islamyolu.app.Rozet.BadgeType;
import com.islamyolu.app.Service.APIService;
import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.Service.GoldService;
import com.islamyolu.app.Service.SoundService;
import com.islamyolu.app.Utils.APIClient;
import com.shuhart.stepview.StepView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WordGameActivity extends AppCompatActivity {
    private TextView timerText;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int timeLeft = 240; // 60 saniye geri sayım süresi
    private TextView questionTextView, questionSpaceHint, costCoinLabel;
    private GridLayout answerGrid;
    private Button letterJokerButton, confirmButton;
    private int currentQuestionIndex = 0;
    private String currentAnswer;
    private List<Question> questionList;
    private Dialog loadingDialog, mDialog;
    private TextView goldAmountText;
    private List<Question> finalQuestions;
    private StepView stepView;
    private final int JOKER_LETTER = 100;
    private final int ADD_COIN = 30;
    private ConstraintLayout costCoinLayout, confirmButtonLayout;
    private GoldService goldService;
    private SoundService soundService;
    private BadgeService badgeService;
    private ImageView gifImageView;
    private TextView gifImageSuccessViewLabel;
    private int profit = 0;
    private boolean harfAlindimiRozet = false;
    private boolean jokerKullanmamaRozet = true;
    private int toplamAcilanHarfSayisi = 0;
    private int sorudanAlinanPuan = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_game);
        // ActionBar'ı gizle
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        initializeLoading();
        initializeViews();
        initializeUserGold();
        initializeServices();
        fetchQuestions();
        buttonClicker();// Soru çekme metodu

        // onBackPressedCallback tanımlama
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Timer'ı durdur
                stopTimer();
                // Çıkış onayı için bir AlertDialog oluştur
                new AlertDialog.Builder(WordGameActivity.this)
                        .setTitle(getString(R.string.exit_game))
                        .setMessage(getString(R.string.exit_game_label))
                        .setPositiveButton(getString(R.string.evet), (dialog, which) -> endGamePopup(false, false, true))
                        .setNegativeButton(getString(R.string.hayir), (dialog, which) -> {
                            dialog.dismiss(); // "Hayır" seçilirse dialog kapanır
                            startTimer(); // "Hayır" seçildiğinde timer yeniden başlar
                        })
                        .setCancelable(false)
                        .create()
                        .show();

            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeServices() {
        soundService = new SoundService(this);
        badgeService = new BadgeService(this);
    }

    private void buttonClicker() {
        letterJokerButton.setOnClickListener(v -> giveLetter());
        costCoinLayout.setOnClickListener(v -> giveLetter());
        confirmButton.setOnClickListener(v -> checkAnswer());
    }

    private void initializeLoading() {
        // Splash ekranını başlat
        loadingDialog = new Dialog(WordGameActivity.this);
        loadingDialog.setContentView(R.layout.game_loading_splash);
        loadingDialog.setCancelable(false); // Geri tuşu ile iptal edilemesin
        loadingDialog.show();
    }


    private void initializeUserGold() {
        goldService = new GoldService(this);
        saveGoldAmountText();
    }

    private void saveGoldAmountText() {
        goldAmountText.setText(String.format(Locale.getDefault(), "%d", goldService.getGoldAmount()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundService != null) {
            soundService.cleanup();
        }
    }

    private void fetchQuestions() {
        APIService apiService = APIClient.getClient().create(APIService.class);
        apiService.getAllQuestionList().enqueue(new Callback<List<Question>>() {
            @Override
            public void onResponse(@NonNull Call<List<Question>> call, @NonNull Response<List<Question>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body();
                    prepareQuestions();
                    loadingDialog.dismiss();
                } else {
                    Log.d("servis hata", "Response başarılı ama içeriği null.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Question>> call, @NonNull Throwable t) {
                Log.d("servis hata", Objects.requireNonNull(t.getMessage()));
                loadingDialog.dismiss();
            }
        });
    }

    private void initializeViews() {
        timerText = findViewById(R.id.timer_text);
        questionTextView = findViewById(R.id.questionTextView);
        answerGrid = findViewById(R.id.answerGrid);
        letterJokerButton = findViewById(R.id.letterJokerButton);
        stepView = findViewById(R.id.step_view_word_game);
        goldAmountText = findViewById(R.id.gold_amount_text_word);
        confirmButton = findViewById(R.id.confirmButton);
        questionSpaceHint = findViewById(R.id.questionSpaceHint);
        gifImageView = findViewById(R.id.gifImageSuccessView);
        costCoinLabel = findViewById(R.id.costCoinLabel);
        costCoinLayout = findViewById(R.id.costCoinLayout);
        confirmButtonLayout = findViewById(R.id.confirmButtonLayout);
        gifImageSuccessViewLabel = findViewById(R.id.gifImageSuccessViewLabel);

        costCoinLabel.setText(String.valueOf(JOKER_LETTER));


    }

    private void startTimer() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    timerText.setText(String.valueOf(timeLeft));
                    handler.postDelayed(this, 1000);
                } else {
                    // Süre dolduğunda ana menüye dön
                    endGamePopup(true, false, false);
                }
            }
        }, 1000);
    }

    private void stopTimer() {
        handler.removeCallbacksAndMessages(null);  // Tüm gecikmeli işlemleri iptal eder
    }

    // Activity kapanırken zamanlayıcıyı durdurun
    @Override
    protected void onStop() {
        super.onStop();
        stopTimer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //sureyi baslat
        startTimer();
    }

    @SuppressLint("SetTextI18n")
    private void endGamePopup(boolean sureDoldumu, boolean yarismaKazandiMi, boolean cikisYapildi) {
        stopTimer();
        mDialog = new Dialog(WordGameActivity.this);
        mDialog.setContentView(R.layout.end_game_popup);
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //pop-up resim degistir
        ImageView imagePopUp = mDialog.findViewById(R.id.imagePopUpEndGame);
        // end_game_popup.xml layoutunu dialog'a yükle
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(false);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButtonEndGame);
        closeButton.setOnClickListener(view -> {
            soundService.commonButtonEffect();
            mDialog.dismiss();
            finish();
        });
        TextView hitpouptext = mDialog.findViewById(R.id.hintPopUpTextEndGame);
        TextView correctAnswerEndGame = mDialog.findViewById(R.id.correctAnswerEndGame);

        if (Boolean.TRUE.equals(sureDoldumu) && Boolean.FALSE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)) {
            hitpouptext.setText(getString(R.string.timeupLabel));
            correctAnswerEndGame.setText(getString(R.string.correctAnswerEndgameLabel) + "\n" + currentAnswer);
            imagePopUp.setImageResource(R.drawable.time_passing);
            soundService.timeoutEffect();
        } else if (cikisYapildi) {
            soundService.timeoutEffect();
            imagePopUp.setImageResource(R.drawable.leave);
        }
        int currentStep;
        if (Boolean.TRUE.equals(yarismaKazandiMi)) {
            soundService.winGameEffect();
            imagePopUp.setImageResource(R.drawable.winner);
            hitpouptext.setText(getString(R.string.winnerLabel));
            currentStep = stepView.getCurrentStep() + 1;
            validationRozet(yarismaKazandiMi);
        } else {
            currentStep = stepView.getCurrentStep();
        }

        TextView correctAnswerCount = mDialog.findViewById(R.id.correctAnswerCount);
        // ValueAnimator ile 0'dan correctAnswers sayısına kadar animasyon oluştur
        ValueAnimator animator = ValueAnimator.ofInt(0, currentStep);

        animator.setDuration(1800); // Animasyon süresi (2000ms = 2 saniye)
        animator.addUpdateListener(animation -> {
            // TextView'de sayıyı yavaş yavaş artırarak göster
            correctAnswerCount.setText(animation.getAnimatedValue().toString());
        });
        animator.start(); // Animasyonu başlat

        TextView totalProfitLabel = mDialog.findViewById(R.id.totalProfitLabel);

        ValueAnimator animatorProfit = ValueAnimator.ofInt(0, profit);

        animatorProfit.setDuration(1800); // Animasyon süresi (2000ms = 2 saniye)
        animatorProfit.addUpdateListener(animation -> {
            // TextView'de sayıyı yavaş yavaş artırarak göster
            totalProfitLabel.setText("+" + animation.getAnimatedValue().toString());
        });
        animatorProfit.start();

        // Dialog'u göster
        mDialog.show();
        //app review cagir
        badgeService.showInAppReview(WordGameActivity.this);
    }

    private void prepareQuestions() {
        // Kolay ve orta seviyedeki soruları ayır
        List<Question> easyQuestions = new ArrayList<>();
        List<Question> mediumQuestions = new ArrayList<>();
        List<Question> hardQuestions = new ArrayList<>();

        for (Question question : questionList) {
            if (question.isDogruCevapWordGame()) {
                if (question.getZorlukSeviyesi() == 1) {
                    easyQuestions.add(question);
                } else if (question.getZorlukSeviyesi() == 2) {
                    mediumQuestions.add(question);
                } else if (question.getZorlukSeviyesi() == 3) {
                    hardQuestions.add(question);
                }
            }
        }

        // Kolay seviyeden 5 rastgele soru seç
        Collections.shuffle(easyQuestions);
        List<Question> selectedEasyQuestions = easyQuestions.subList(0, Math.min(5, easyQuestions.size()));

        // Orta seviyeden 3 rastgele soru seç
        Collections.shuffle(mediumQuestions);
        List<Question> selectedMediumQuestions = mediumQuestions.subList(0, Math.min(5, mediumQuestions.size()));

        // Zor seviyeden 3 rastgele soru seç
        Collections.shuffle(hardQuestions);
        List<Question> selectedHardQuestions = hardQuestions.subList(0, Math.min(5, hardQuestions.size()));


        // Seçilen soruları birleştir
        finalQuestions = new ArrayList<>();
        finalQuestions.addAll(selectedEasyQuestions);
        finalQuestions.addAll(selectedMediumQuestions);
        finalQuestions.addAll(selectedHardQuestions);


        // Toplam soru sayısı kadar adım oluştur
        List<String> stepLabels = new ArrayList<>();
        for (int i = 1; i <= finalQuestions.size(); i++) {
            stepLabels.add("");
        }
        // StepView adımlarını ayarla
        stepView.setSteps(stepLabels);
        prepareQuestionByWordGame();
    }

    private void prepareQuestionByWordGame() {
        Iterator<Question> iterator = finalQuestions.iterator();
        while (iterator.hasNext()) {
            Question question = iterator.next();
            showQuestion(question);
            iterator.remove(); // Gösterilen soruyu listeden kaldır
            break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void showQuestion(Question question) {
        // StepView'i güncelle
        stepView.go(currentQuestionIndex, true); // Mevcut soru adımını güncelle (sarı renk)
        updateTextSize(question.getSoruMetni());
        currentAnswer = question.getDogruCevap().toUpperCase(new Locale("tr", "TR")).replace(" ", ""); // Doğru cevabı büyük harfle al
        questionSpaceHint.setText(question.questionSpaceHint());
        answerGrid.removeAllViews(); // Önceki yanıt kutucuklarını temizle
        // Satır sayacını
        for (int i = 0; i < currentAnswer.length(); i++) {

            EditText answerBox = new EditText(this);
            answerBox.setHint("_"); // Boşluk
            answerBox.setBackgroundResource(R.drawable.hexagon);
            answerBox.setLayoutParams(new GridLayout.LayoutParams());
            answerBox.setMaxWidth(100);
            answerBox.setTextSize(28);
            answerBox.setTypeface(answerBox.getTypeface(), Typeface.BOLD);
            answerBox.setCursorVisible(false);
            answerBox.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            answerBox.setTextColor(Color.BLACK);
            answerBox.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)}); // Sadece 1 karakter al

            // Sorunun sayısal olup olmadığını kontrol et
            if (question.isNumeric()) {
                answerBox.setInputType(InputType.TYPE_CLASS_NUMBER); // Sayısal klavye aç
            } else {
                answerBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS); // Büyük harf karakter girişi
            }


            // Odaklanma durumunu dinlemek için OnFocusChangeListener ekleyin
            answerBox.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Odaklandığında arka planı farklı bir renge veya drawable'a ayarla
                    answerBox.setBackgroundResource(R.drawable.hexagon_focused);
                } else {
                    // Odak dışına çıktığında varsayılan arka plana geri dön
                    answerBox.setBackgroundResource(R.drawable.hexagon);
                }
            });

            // Geri tuşu olayını dinleyin
            int[] indexArray = {i}; // `index`'i dizide sakla
            answerBox.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    int index = answerGrid.indexOfChild(v); // Hangi EditText'e basıldığını bul
                    EditText currentEditText = (EditText) answerGrid.getChildAt(index);

                    // Eğer şu anki EditText ipucu harfi içeriyorsa, silmeye izin verme
                    if (!currentEditText.isEnabled()) {
                        return true; // Tuş olayını işlediğimizi belirt
                    }

                    // Şu anki EditText içindeki metni sil
                    String currentText = currentEditText.getText().toString();
                    if (!currentText.isEmpty()) {
                        currentEditText.setText(""); // Mevcut metni temizle
                    } else if (index > 0) {
                        // Eğer mevcut EditText boşsa ve ilk kutucuk değilse, önceki kutucuğa geç
                        EditText previousEditText = (EditText) answerGrid.getChildAt(index - 1);
                        while (!previousEditText.isEnabled() && index > 1) {
                            index--;
                            previousEditText = (EditText) answerGrid.getChildAt(index - 1);
                        }

                        // Önceki EditText'e odaklan
                        previousEditText.requestFocus();
                    }
                    return true; // Tuş olayını işlediğimizi belirt
                }
                return false; // Olayın başka bir dinleyici tarafından işlenmesine izin ver
            });

            answerGrid.addView(answerBox);

            // Her bir kutucuğa tıklama ve metin değişikliği olayı
            answerBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && indexArray[0] < currentAnswer.length() - 1) {
                        // Eğer bir karakter yazılmışsa ve son kutucuk değilse, bir sonraki kutucuğa geç
                        EditText nextEditText = (EditText) answerGrid.getChildAt(indexArray[0] + 1);

                        // Eğer ipucu verilen bir karakter ise, sonraki karaktere geç
                        while (nextEditText != null && !nextEditText.isEnabled() && indexArray[0] < currentAnswer.length() - 1) {
                            indexArray[0]++;
                            nextEditText = (EditText) answerGrid.getChildAt(indexArray[0] + 1);
                        }

                        // `nextEditText`'in null olup olmadığını kontrol et
                        if (nextEditText != null && nextEditText.isEnabled()) {
                            nextEditText.requestFocus(); // Sonraki kutucuğa odaklan
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String input = s.toString();
                    if (!input.equals(input.toUpperCase())) {
                        // Eğer küçük harf girilmişse, büyük harfe çevir
                        answerBox.setText(input.toUpperCase());
                        answerBox.setSelection(answerBox.getText().length()); // İmleci sona al
                    }
                }
            });
        }
    }

    private void updateTextSize(String text) {
        questionTextView.setText(text); // Metni ayarla

        // Eğer metin 200 karakterden fazlaysa yazı tipi boyutunu değiştir
        if (text.length() > 200) {
            questionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); // Boyutu 15sp yap
        } else {
            // 200 karakterden az ise boyutu 20sp olarak ayarla
            questionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }
    }

    private void resetButtonColors() {
        letterJokerButton.setEnabled(true);
        confirmButton.setEnabled(true);
        costCoinLayout.setAlpha(1.0f);
        confirmButtonLayout.setAlpha(1.0f);
        harfAlindimiRozet = false;
        toplamAcilanHarfSayisi = 0;
    }

    private void checkAnswer() {
        StringBuilder userAnswer = new StringBuilder();
        for (int i = 0; i < answerGrid.getChildCount(); i++) {
            EditText editText = (EditText) answerGrid.getChildAt(i);
            userAnswer.append(editText.getText().toString());
        }
        if (userAnswer.toString().equalsIgnoreCase(currentAnswer)) {
            int kalanHarfSayisi = currentAnswer.length() - toplamAcilanHarfSayisi;
            sorudanAlinanPuan = ADD_COIN * kalanHarfSayisi;
            goldService.addGoldAmount(sorudanAlinanPuan);
            showAnimation(true);
            soundService.successAnswer();
            saveGoldAmountText();
            validationRozet(false);
            profit += sorudanAlinanPuan;
            currentQuestionIndex++;
            confirmButtonLayout.setAlpha(0.0f);
            confirmButton.setEnabled(false);
            if (!harfAlindimiRozet) {
                badgeService.enableBadge(BadgeType.KELIME_YARISMA_1);
            }

            if (!finalQuestions.isEmpty()) {
                nextQuestionWithDelay();
            } else {
                endGamePopup(false, true, false);
                stepView.done(true);
            }
        } else {
            showAnimation(false);
            soundService.wrongAnswer();
        }
    }

    private void validationRozet(boolean yarismaKazandiMi) {
        if (stepView.getCurrentStep() == 9) {
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_2);
        }
        if (yarismaKazandiMi && timeLeft >= 20) {
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_3);
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_KAZANAN);
        }
        if (yarismaKazandiMi) {
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_KAZANAN);
        }
        if (yarismaKazandiMi && jokerKullanmamaRozet) {
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_4);
        }

    }

    private void nextQuestionWithDelay() {
        handler.postDelayed(() -> {
            resetButtonColors();
            prepareQuestionByWordGame();
        }, 2000);
    }

    @SuppressLint("SetTextI18n")
    private void showAnimation(boolean dogruYanit) {
        // Doğru veya yanlış yanıt duruma göre ayarla
        if (dogruYanit) {
            gifImageSuccessViewLabel.setText(getString(R.string.wordGameCorrectLabel) + "+" + sorudanAlinanPuan);
            gifImageSuccessViewLabel.setTextColor(ContextCompat.getColor(this, R.color.correctAnswer));
            gifImageView.setImageResource(R.drawable.confirm);
        } else {
            gifImageSuccessViewLabel.setText(getString(R.string.wordGameWrongLabel));
            gifImageSuccessViewLabel.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswer));
            gifImageView.setImageResource(R.drawable.wrong);
        }

        // Görünür yap
        gifImageView.setVisibility(View.VISIBLE);
        gifImageSuccessViewLabel.setVisibility(View.VISIBLE);

        // Giriş animasyonları: fade-in ve scale-in
        ObjectAnimator fadeInImage = ObjectAnimator.ofFloat(gifImageView, "alpha", 0f, 1f);
        ObjectAnimator scaleInXImage = ObjectAnimator.ofFloat(gifImageView, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleInYImage = ObjectAnimator.ofFloat(gifImageView, "scaleY", 0.8f, 1f);

        ObjectAnimator fadeInLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "alpha", 0f, 1f);
        ObjectAnimator scaleInXLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleInYLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "scaleY", 0.8f, 1f);

        // Çıkış animasyonları: fade-out ve scale-out
        ObjectAnimator fadeOutImage = ObjectAnimator.ofFloat(gifImageView, "alpha", 1f, 0f);
        ObjectAnimator fadeOutLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "alpha", 1f, 0f);
        ObjectAnimator scaleOutXImage = ObjectAnimator.ofFloat(gifImageView, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleOutYImage = ObjectAnimator.ofFloat(gifImageView, "scaleY", 1f, 0.8f);
        ObjectAnimator scaleOutXLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleOutYLabel = ObjectAnimator.ofFloat(gifImageSuccessViewLabel, "scaleY", 1f, 0.8f);

        // Giriş animasyonu süresi (500 ms)
        fadeInImage.setDuration(500);
        fadeInLabel.setDuration(500);
        scaleInXImage.setDuration(500);
        scaleInYImage.setDuration(500);
        scaleInXLabel.setDuration(500);
        scaleInYLabel.setDuration(500);

        // Çıkış animasyonu süresi (500 ms)
        fadeOutImage.setDuration(500);
        fadeOutLabel.setDuration(500);
        scaleOutXImage.setDuration(500);
        scaleOutYImage.setDuration(500);
        scaleOutXLabel.setDuration(500);
        scaleOutYLabel.setDuration(500);

        // Giriş animasyonlarını aynı anda başlatmak için AnimatorSet kullan
        AnimatorSet fadeInSet = new AnimatorSet();
        fadeInSet.playTogether(fadeInImage, scaleInXImage, scaleInYImage, fadeInLabel, scaleInXLabel, scaleInYLabel);
        fadeInSet.start();

        // 2 saniye sonra çıkış animasyonunu başlatmak için Handler kullan
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet fadeOutSet = new AnimatorSet();
            fadeOutSet.playTogether(fadeOutImage, scaleOutXImage, scaleOutYImage, fadeOutLabel, scaleOutXLabel, scaleOutYLabel);
            fadeOutSet.start();

            // Çıkış animasyonu bittiğinde görünürlükleri GONE yap
            fadeOutSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    gifImageView.setVisibility(View.GONE);
                    gifImageSuccessViewLabel.setVisibility(View.GONE);
                }
            });
        }, 2000); // Görünme süresi (2 saniye)
    }


    private void giveLetter() {

        if (goldService.getGoldAmount() >= JOKER_LETTER) {
            goldService.minusGoldAmount(JOKER_LETTER);
            saveGoldAmountText();
            Random random = new Random();
            int index;
            harfAlindimiRozet = true;
            jokerKullanmamaRozet = false;
            EditText hintEditText;
            // Buton sesini çal
            soundService.wgHintJokerEffect();

            // Sadece henüz açılmamış olan bir EditText bulana kadar döngü devam eder
            do {
                index = random.nextInt(currentAnswer.length()); // Rastgele bir indeks seç
                hintEditText = (EditText) answerGrid.getChildAt(index); // O indeksteki EditText'i bul
            } while (!hintEditText.isEnabled()); // Eğer EditText zaten açılmışsa yeniden seç

            // Doğru cevaptan alınacak harf
            char hintChar = currentAnswer.charAt(index);
            hintEditText.setEnabled(false); // EditText'i düzenlenemez yap
            toplamAcilanHarfSayisi++;

            // Alfabeden rasgele harfler gösterme animasyonu
            Handler handler = new Handler(Looper.getMainLooper());
            EditText finalHintEditText = hintEditText;
            Runnable runnable = new Runnable() {
                final int animDuration = 1300; // Animasyonun süresi (2 saniye)
                final int interval = 70; // Harflerin değişme aralığı (100 ms)
                final long startTime = System.currentTimeMillis();

                @Override
                public void run() {
                    if (System.currentTimeMillis() - startTime < animDuration) {
                        // Rasgele bir harf seç
                        char randomChar = (char) (random.nextInt(26) + 'A');
                        finalHintEditText.setText(String.valueOf(randomChar));

                        // Animasyonu tekrar çalıştır
                        handler.postDelayed(this, interval);
                    } else {
                        // Animasyon süresi tamamlandı, doğru harfi yerleştir
                        finalHintEditText.setText(String.valueOf(hintChar));

                    }
                }
            };

            // Animasyonu başlat
            handler.post(runnable);

            // Tüm EditText bileşenlerinin kapalı olup olmadığını kontrol edin
            boolean allDisabled = true;
            for (int i = 0; i < currentAnswer.length(); i++) {
                EditText editText = (EditText) answerGrid.getChildAt(i);
                if (editText.isEnabled()) {
                    allDisabled = false;
                    break;
                }
            }

            if (allDisabled) {
                costCoinLayout.setAlpha(0.0f);
                letterJokerButton.setEnabled(false);
            }
        } else {
            soundService.noUsedJokerEffect();
            letterJokerButton.setEnabled(false);
            goldService.yetersizKaynakDialog(this);
        }
    }


}
