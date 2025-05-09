package com.islamyolu.app.game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InfoGameActivity extends AppCompatActivity {
    private TextView timerText;
    private TextView questionText, gold_status;
    private Button option1, option2, option3, option4;
    private Button joker50, jokerHint, jokerDouble;
    private TextView goldAmountText;
    private TextView costCoinLabel, costCoinLabel_2, costCoinLabel_3;

    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int hataliYanitSayisiCarpani = 1;
    private int timeLeft = 150; // 60 saniye geri sayım süresi
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean joker50Used = false;
    private boolean jokerHintUsed = false;
    private boolean jokerDoubleUsed = false;
    private boolean firstAnswerSelected = false;
    private boolean ciftCevapKullanildi = false;

    private final int JOKER_YUZDE_50 = 120;
    private final int JOKER_IPUCU = 180;
    private final int JOKER_CIFT_CEVAP = 200;
    private final int ADD_COIN_EASY_Q = 80;
    private final int ADD_COIN_MEDIUM_Q = 100;
    private final int ADD_COIN_HARD_Q = 150;
    private final int HATALI_YANIT_DEVAM = 1000;

    private Dialog loadingDialog, mDialog, fDialog;

    private Question currentQuestion;
    private int correctAnswerIndex;
    private ConstraintLayout costCoinLayout, costCoinLayout_2, costCoinLayout_3;
    private StepView stepView;
    private List<Question> finalQuestions;
    private Animation buttonClickAnim;
    private GoldService goldService;
    private SoundService soundService;
    private BadgeService badgeService;
    private boolean jokerKullanmamaRozet = true;
    private int profit = 0;
    private boolean isAnimationRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_game); // XML dosyanızın adını uygun şekilde güncelleyin
        // ActionBar'ı gizle
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        initializeLoading();
        initializeViews();
        initializeUserGold();
        initializeServices();
        fetchQuestions();
        setupJokerClickListeners();

        // onBackPressedCallback tanımlıyoruz
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Timer'ı durdur
                stopTimer();
                // Çıkış onayı için bir AlertDialog oluştur
                new AlertDialog.Builder(InfoGameActivity.this)
                        .setTitle(getString(R.string.exit_game))
                        .setMessage(getString(R.string.exit_game_label))
                        .setPositiveButton(getString(R.string.evet), (dialog, which) -> endGamePopup(null, false, false, true))
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


    private void initializeLoading() {
        // Splash ekranını başlat
        loadingDialog = new Dialog(InfoGameActivity.this);
        loadingDialog.setContentView(R.layout.game_loading_splash);
        loadingDialog.setCancelable(false); // Geri tuşu ile iptal edilemesin
        loadingDialog.show();
    }

    private void initializeViews() {
        timerText = findViewById(R.id.timer_text);
        questionText = findViewById(R.id.question_text);
        option1 = findViewById(R.id.option_1);
        option2 = findViewById(R.id.option_2);
        option3 = findViewById(R.id.option_3);
        option4 = findViewById(R.id.option_4);
        joker50 = findViewById(R.id.joker_50); // %50 joker butonu
        jokerHint = findViewById(R.id.joker_hint); // İpucu joker butonu
        jokerDouble = findViewById(R.id.joker_double); // Çift cevap joker butonu
        goldAmountText = findViewById(R.id.gold_amount_text); // Altın miktarını gösteren TextView

        costCoinLabel = findViewById(R.id.costCoinLabel);
        costCoinLabel_2 = findViewById(R.id.costCoinLabel_2);
        costCoinLabel_3 = findViewById(R.id.costCoinLabel_3);
        costCoinLabel.setText(String.valueOf(JOKER_YUZDE_50));
        costCoinLabel_2.setText(String.valueOf(JOKER_IPUCU));
        costCoinLabel_3.setText(String.valueOf(JOKER_CIFT_CEVAP));

        costCoinLayout = findViewById(R.id.costCoinLayout);
        costCoinLayout_2 = findViewById(R.id.costCoinLayout_2);
        costCoinLayout_3 = findViewById(R.id.costCoinLayout_3);

        stepView = findViewById(R.id.step_view);
        // Animasyonu yükleyin
        buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click_anim);
        gold_status = findViewById(R.id.gold_status);
    }


    private void initializeUserGold() {
        goldService = new GoldService(this);
        saveGoldAmountText();
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

    private void prepareQuestions() {
        // Kolay ve orta seviyedeki soruları ayır
        List<Question> easyQuestions = new ArrayList<>();
        List<Question> mediumQuestions = new ArrayList<>();
        List<Question> hardQuestions = new ArrayList<>();

        for (Question question : questionList) {
            if (question.getZorlukSeviyesi() == 1) {
                easyQuestions.add(question);
            } else if (question.getZorlukSeviyesi() == 2) {
                mediumQuestions.add(question);
            } else if (question.getZorlukSeviyesi() == 3) {
                hardQuestions.add(question);
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
        showQuestion();
    }

    private void showQuestion() {
        if (currentQuestionIndex < finalQuestions.size()) {
            currentQuestion = finalQuestions.get(currentQuestionIndex);
            updateTextSize(currentQuestion.getSoruMetni());

            // Şıkları bir listeye koy ve karıştır
            List<String> options = new ArrayList<>(Arrays.asList(currentQuestion.getSiklar()));
            Collections.shuffle(options); // Şıkları karıştır

            // Karıştırılan şıkları ayarlayın
            option1.setText(options.get(0));
            option2.setText(options.get(1));
            option3.setText(options.get(2));
            option4.setText(options.get(3));

            // Doğru cevabın indeksini bul
            correctAnswerIndex = options.indexOf(currentQuestion.getDogruCevap()); // Yeni doğru cevabın indeksini al
            // StepView'i güncelle
            stepView.go(currentQuestionIndex, true); // Mevcut soru adımını güncelle (sarı renk)

            setupOptionClickListeners(currentQuestion);
        } else {
            // Soru bittiğinde ana menüye dön
            endGamePopup(null, false, true, false);
            stepView.done(true);

        }
    }

    private void updateTextSize(String text) {
        questionText.setText(text); // Metni ayarla

        // Eğer metin 200 karakterden fazlaysa yazı tipi boyutunu değiştir
        if (text.length() > 200) {
            questionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); // Boyutu 15sp yap
        } else {
            // 200 karakterden az ise boyutu 20sp olarak ayarla
            questionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }
    }

    private void setupOptionClickListeners(Question currentQuestion) {
        View.OnClickListener optionClickListener = view -> {
            Button selectedButton = (Button) view;
            String selectedAnswer = selectedButton.getText().toString();
            selectedButton.startAnimation(buttonClickAnim);
//            // Eğer kullanıcı daha önce çift cevap kullanmadıysa, diğer butonları devre dışı bırak
            if (!ciftCevapKullanildi) {
                disableAllJokerOptions(); // Diğer tüm butonları devre dışı bırak
            }
            // Seçilen yanıt kontrol edilir
            if (selectedAnswer.equals(currentQuestion.getDogruCevap())) {
                handleCorrectAnswer(selectedButton);
            } else {
                handleIncorrectAnswer(selectedButton, currentQuestion);
                return;
            }

            // 3 saniye bekle ve sonraki soruya geç
            handler.postDelayed(() -> {
                resetButtonColors();
                currentQuestionIndex++;
                showQuestion();
            }, 1000);
        };

        // Buton tıklama dinleyicileri ayarlandı
        option1.setOnClickListener(optionClickListener);
        option2.setOnClickListener(optionClickListener);
        option3.setOnClickListener(optionClickListener);
        option4.setOnClickListener(optionClickListener);
    }

    // Tüm seçenekleri devre dışı bırak
    private void disableAllJokerOptions() {
        option1.setEnabled(false);
        option2.setEnabled(false);
        option3.setEnabled(false);
        option4.setEnabled(false);
        joker50.setEnabled(false);
        jokerHint.setEnabled(false);
        jokerDouble.setEnabled(false);
    }

    private void handleCorrectAnswer(Button selectedButton) {
        soundService.successAnswer();
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.correctAnswer)); // Doğru cevap
        gold_status.setTextColor(ContextCompat.getColor(this, R.color.correctAnswer));
        if (currentQuestion.getZorlukSeviyesi() == 1) {
            goldService.addGoldAmount(ADD_COIN_EASY_Q); // Doğru cevapta 100 altın ekle
            profit += ADD_COIN_EASY_Q;
            gold_status.setText(getString(R.string.gold_status_text, ADD_COIN_EASY_Q));
        } else if (currentQuestion.getZorlukSeviyesi() == 2) {
            goldService.addGoldAmount(ADD_COIN_MEDIUM_Q);
            profit += ADD_COIN_MEDIUM_Q;
            gold_status.setText(getString(R.string.gold_status_text, ADD_COIN_MEDIUM_Q));
        } else if (currentQuestion.getZorlukSeviyesi() == 3) {
            goldService.addGoldAmount(ADD_COIN_HARD_Q);
            profit += ADD_COIN_HARD_Q;
            gold_status.setText(getString(R.string.gold_status_text, ADD_COIN_HARD_Q));
        }
        goldAnimation();
        saveGoldAmountText();
        validationRozet(false);
    }

    private void goldAnimation() {
        if (isAnimationRunning) {
            // Animasyon zaten çalışıyor, yeni bir animasyon başlatma
            return;
        }
        isAnimationRunning = true;
        gold_status.setVisibility(View.VISIBLE);

        // Giriş animasyonları: fade-in ve scale-in

        ObjectAnimator fadeInLabel = ObjectAnimator.ofFloat(gold_status, "alpha", 0f, 1f);
        ObjectAnimator scaleInXLabel = ObjectAnimator.ofFloat(gold_status, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleInYLabel = ObjectAnimator.ofFloat(gold_status, "scaleY", 0.8f, 1f);

        // Çıkış animasyonları: fade-out ve scale-out
        ObjectAnimator fadeOutLabel = ObjectAnimator.ofFloat(gold_status, "alpha", 1f, 0f);
        ObjectAnimator scaleOutXLabel = ObjectAnimator.ofFloat(gold_status, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleOutYLabel = ObjectAnimator.ofFloat(gold_status, "scaleY", 1f, 0.8f);

        // Giriş animasyonu süresi (500 ms)
        fadeInLabel.setDuration(500);
        scaleInXLabel.setDuration(500);
        scaleInYLabel.setDuration(500);

        // Çıkış animasyonu süresi (500 ms)
        fadeOutLabel.setDuration(500);
        scaleOutXLabel.setDuration(500);
        scaleOutYLabel.setDuration(500);

        // Giriş animasyonlarını aynı anda başlatmak için AnimatorSet kullan
        AnimatorSet fadeInSet = new AnimatorSet();
        fadeInSet.playTogether(fadeInLabel, scaleInXLabel, scaleInYLabel);
        fadeInSet.start();

        // 2 saniye sonra çıkış animasyonunu başlatmak için Handler kullan
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet fadeOutSet = new AnimatorSet();
            fadeOutSet.playTogether(fadeOutLabel, scaleOutXLabel, scaleOutYLabel);
            fadeOutSet.start();

            // Çıkış animasyonu bittiğinde görünürlükleri GONE yap
            fadeOutSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    gold_status.setVisibility(View.GONE);
                    isAnimationRunning = false; // Animasyon bitti, bayrağı sıfırla
                }
            });
        }, 700); // Görünme süresi (2 saniye)

    }

    private void goldAnimationMinus(int value) {
        gold_status.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswer));
        gold_status.setText(getString(R.string.gold_status_minus_text, value));
        goldAnimation();
    }

    private void validationRozet(boolean yarismaKazandiMi) {
        if (stepView.getCurrentStep() == 2) {
            badgeService.enableBadge(BadgeType.BILGI_YARISMA_1);
        }
        if (stepView.getCurrentStep() == 9) {
            badgeService.enableBadge(BadgeType.BILGI_YARISMA_2);
        }
        if (yarismaKazandiMi && timeLeft >= 20) {
            badgeService.enableBadge(BadgeType.BILGI_YARISMA_3);
            badgeService.enableBadge(BadgeType.BILGI_YARISMA_KAZANAN);
        }
        if (yarismaKazandiMi) {
            badgeService.enableBadge(BadgeType.BILGI_YARISMA_KAZANAN);
        }
        if (yarismaKazandiMi && jokerKullanmamaRozet) {
            badgeService.enableBadge(BadgeType.KELIME_YARISMA_4);
        }
    }

    private void handleIncorrectAnswer(Button selectedButton, Question currentQuestion) {
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.wrongAnswer)); // Yanlış cevap seçilen buton
        highlightCorrectAnswer(currentQuestion);
        if (currentQuestion.getZorlukSeviyesi() >= 2) {
            handler.postDelayed(() -> hataliCevapPopup(currentQuestion), 2000);
        } else {
            handler.postDelayed(() -> endGamePopup(currentQuestion, false, false, false), 2000);
        }
    }

    private void hataliCevapPopup(Question currentQuestion) {
        ProgressBar countdownProgressBar;
        TextView countdownText, totalProfitLabel;
        CountDownTimer countDownTimer;
        stopTimer();
        fDialog = new Dialog(InfoGameActivity.this);
        fDialog.setContentView(R.layout.hatali_yanit_pop_up);
        fDialog.setCancelable(false);
        Objects.requireNonNull(fDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int hataliYanitDeger = Math.multiplyExact(HATALI_YANIT_DEVAM, hataliYanitSayisiCarpani);

        TextView correctAnswerCount = fDialog.findViewById(R.id.correctAnswerCount);
        correctAnswerCount.setText(getString((R.string.hataliYanitMesaj), hataliYanitDeger));

        Button kabulEtButtonEndGame = fDialog.findViewById(R.id.kabulEtButtonEndGame);
        Button redEtButtonEndGame = fDialog.findViewById(R.id.redEtButtonEndGame);
        countdownProgressBar = fDialog.findViewById(R.id.countdownProgressBar);
        countdownText = fDialog.findViewById(R.id.countdownText);
        countdownText.setText(String.valueOf(timeLeft));
        totalProfitLabel = fDialog.findViewById(R.id.totalProfitLabel);
        totalProfitLabel.setText(String.valueOf(goldService.getGoldAmount()));


        // 7 saniye boyunca geri sayım yapacak bir CountDownTimer
        countDownTimer = new CountDownTimer(7000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // Geri sayımın kaç saniye kaldığını TextView'de göster
                int secondsLeft = (int) (millisUntilFinished / 1000);
                countdownText.setText(String.valueOf(secondsLeft));

                // Geri sayım çubuğunu güncelle
                countdownProgressBar.setProgress(secondsLeft);
            }

            @Override
            public void onFinish() {
                // Geri sayım bittiğinde yapılacak işlemler
                if (fDialog != null && fDialog.isShowing()) {
                    countdownText.setText("0");
                    countdownProgressBar.setProgress(0);
                    endGamePopup(currentQuestion, false, false, false);
                }

                // Burada başka bir işlem yapabilirsiniz (örneğin, oyuna devam et)
            }
        };

        countDownTimer.start(); // Geri sayımı başlat

        kabulEtButtonEndGame.setOnClickListener(v -> {
            if (goldService.getGoldAmount() >= hataliYanitDeger) {
                countDownTimer.cancel();
                goldService.minusGoldAmount(hataliYanitDeger);
                saveGoldAmountText();
                goldAnimationMinus(hataliYanitDeger);
                hataliYanitSayisiCarpani++;
                nextQuestionFailAnswer();
                fDialog.dismiss();
                startTimer();
            } else {
                Toast.makeText(fDialog.getContext(), R.string.yetersiz_kaynak_label_short, Toast.LENGTH_SHORT).show();
            }
        });

        redEtButtonEndGame.setOnClickListener(v -> {
            countDownTimer.cancel();
            endGamePopup(this.currentQuestion, false, false, false);
        });

        fDialog.show();
    }

    private void highlightCorrectAnswer(Question currentQuestion) {
        Button[] options = {option1, option2, option3, option4};
        for (Button option : options) {
            if (option.getText().toString().equals(currentQuestion.getDogruCevap())) {
                option.setBackgroundColor(ContextCompat.getColor(this, R.color.correctAnswer)); // Doğru cevabı yeşil yap
            }
        }
    }


    private void resetButtonColors() {
        int defaultColor = -1;
        option1.setBackgroundColor(defaultColor);
        option2.setBackgroundColor(defaultColor);
        option3.setBackgroundColor(defaultColor);
        option4.setBackgroundColor(defaultColor);

        option1.setEnabled(true);
        option2.setEnabled(true);
        option3.setEnabled(true);
        option4.setEnabled(true);

        // Butonların görsel durumunu sıfırla (örneğin opasiteyi geri al)
        option1.setAlpha(1.0f);
        option2.setAlpha(1.0f);
        option3.setAlpha(1.0f);
        option4.setAlpha(1.0f);


        if (!joker50Used) {
            joker50.setEnabled(true);
            costCoinLayout.setAlpha(1.0f);

        }
        if (!jokerHintUsed) {
            jokerHint.setEnabled(true);
            costCoinLayout_2.setAlpha(1.0f);

        }
        if (!jokerDoubleUsed) {
            jokerDouble.setEnabled(true);
            costCoinLayout_3.setAlpha(1.0f);
        }
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
                    endGamePopup(null, true, false, false);
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

    private void saveGoldAmountText() {
        goldAmountText.setText(String.format(Locale.getDefault(), "%d", goldService.getGoldAmount()));
    }

    private void setupJokerClickListeners() {
        joker50.setOnClickListener(v -> handleJoker50());
        costCoinLayout.setOnClickListener(v -> handleJoker50());
        jokerHint.setOnClickListener(v -> handleJokerHint());
        costCoinLayout_2.setOnClickListener(v -> handleJokerHint());
        jokerDouble.setOnClickListener(v -> handleJokerDouble());
        costCoinLayout_3.setOnClickListener(v -> handleJokerDouble());
    }

    private void handleJoker50() {
        if (!joker50Used && goldService.getGoldAmount() >= JOKER_YUZDE_50) {
            soundService.jokerEffect();
            jokerKullanmamaRozet = false;
            joker50Used = true;
            joker50.setEnabled(false);
            costCoinLayout.setAlpha(0.0f);
            goldService.minusGoldAmount(JOKER_YUZDE_50);
            saveGoldAmountText();
            goldAnimationMinus(JOKER_YUZDE_50);
            // Doğru cevabın indeksini kullanarak yanlış seçenekleri devre dışı bırak
            disableTwoWrongOptions(correctAnswerIndex);
        } else {
            soundService.noUsedJokerEffect();
            goldService.yetersizKaynakDialog(this);
        }
    }


    private void handleJokerHint() {
        if (!jokerHintUsed && goldService.getGoldAmount() >= JOKER_IPUCU) {
            soundService.jokerEffect();
            jokerKullanmamaRozet = false;
            jokerHintUsed = true;
            jokerHint.setEnabled(false);
            costCoinLayout_2.setAlpha(0.0f);
            goldService.minusGoldAmount(JOKER_IPUCU);
            saveGoldAmountText();
            goldAnimationMinus(JOKER_IPUCU);
            showHintPopup();
        } else {
            soundService.noUsedJokerEffect();
            goldService.yetersizKaynakDialog(this);
        }
    }

    private void handleJokerDouble() {
        if (!jokerDoubleUsed && goldService.getGoldAmount() >= JOKER_CIFT_CEVAP) {
            soundService.jokerEffect();
            jokerKullanmamaRozet = false;
            jokerDoubleUsed = true;
            jokerDouble.setEnabled(false);
            joker50.setEnabled(false);
            costCoinLayout_3.setAlpha(0.0f);
            goldService.minusGoldAmount(JOKER_CIFT_CEVAP);
            saveGoldAmountText();
            goldAnimationMinus(JOKER_CIFT_CEVAP);
            enableDoubleAnswer();
        } else {
            soundService.noUsedJokerEffect();
            goldService.yetersizKaynakDialog(this);
        }
    }

    private void disableTwoWrongOptions(int correctAnswerIndex) {
        // Yanlış cevapların indekslerini depolamak için bir liste oluştur
        List<Integer> wrongOptionsIndices = new ArrayList<>();

        // Yanlış cevapların indekslerini ekle
        for (int i = 0; i < 4; i++) { // 4 şıkkımız var
            if (i != correctAnswerIndex) {
                wrongOptionsIndices.add(i);
            }
        }

        // Rastgele iki yanlış seçenek seç
        Collections.shuffle(wrongOptionsIndices); // Yanlış seçenekleri karıştır

        // İki yanlış seçeneği devre dışı bırak
        for (int i = 0; i < 2 && i < wrongOptionsIndices.size(); i++) {
            int index = wrongOptionsIndices.get(i);
            disableOption(index); // Yanlış seçeneği devre dışı bırak
        }
    }


    private void disableOption(int index) {
        switch (index) {
            case 0:
                option1.setEnabled(false);
                option1.setAlpha(0.5f); // Görsel olarak pasif hale getir
                break;
            case 1:
                option2.setEnabled(false);
                option2.setAlpha(0.5f); // Görsel olarak pasif hale getir
                break;
            case 2:
                option3.setEnabled(false);
                option3.setAlpha(0.5f); // Görsel olarak pasif hale getir
                break;
            case 3:
                option4.setEnabled(false);
                option4.setAlpha(0.5f); // Görsel olarak pasif hale getir
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void endGamePopup(Question currentQuestion, boolean sureDoldumu, boolean yarismaKazandiMi, boolean cikisYapildi) {
        if (currentQuestion == null) {
            currentQuestion = this.currentQuestion;
        }
        if (fDialog != null && fDialog.isShowing()) {
            fDialog.dismiss();
        }
        // Yeni bir Dialog oluştur
        stopTimer();
        mDialog = new Dialog(InfoGameActivity.this);
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

        if (Boolean.FALSE.equals(sureDoldumu) && Boolean.FALSE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)) {
            //hint text set
            hitpouptext.setText(this.currentQuestion.getIpucu());
            correctAnswerEndGame.setText(getString(R.string.correctAnswerEndgameLabel) + "\n" + currentQuestion.getDogruCevap());
            imagePopUp.setImageResource(R.drawable.success);
            soundService.timeoutEffect();
        } else if (Boolean.FALSE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)) {
            soundService.timeoutEffect();
            hitpouptext.setText(getString(R.string.timeupLabel));
            imagePopUp.setImageResource(R.drawable.time_passing);
        } else if (cikisYapildi) {
            soundService.timeoutEffect();
            imagePopUp.setImageResource(R.drawable.leave);
        }

        if (stepView.getCurrentStep() == 0 && Boolean.FALSE.equals(sureDoldumu) && Boolean.FALSE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)) {
            soundService.wrongAnswer();
            imagePopUp.setImageResource(R.drawable.incorrect);
        }
        int currentStep;
        if (Boolean.TRUE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)) {
            soundService.winGameEffect();
            imagePopUp.setImageResource(R.drawable.winner);
            hitpouptext.setText(getString(R.string.winnerLabel));
            currentStep = stepView.getCurrentStep() + 1;
            validationRozet(true);
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
        badgeService.showInAppReview(InfoGameActivity.this);
    }

    private void showHintPopup() {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(InfoGameActivity.this);
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.custom_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view -> {
            soundService.commonButtonEffect();
            mDialog.dismiss();
        });
        //hint text set
        TextView hitpouptext = mDialog.findViewById(R.id.hintPopUpText);
        hitpouptext.setText(currentQuestion.getIpucu());
        // Dialog'u göster
        mDialog.show();
    }

    private void enableDoubleAnswer() {
        firstAnswerSelected = false; // İlk cevap hakkını sıfırlıyoruz

        // Seçeneklere tıklama dinleyicilerini ayarlıyoruz
        option1.setOnClickListener(v -> handleDoubleAnswer(0, option1));
        option2.setOnClickListener(v -> handleDoubleAnswer(1, option2));
        option3.setOnClickListener(v -> handleDoubleAnswer(2, option3));
        option4.setOnClickListener(v -> handleDoubleAnswer(3, option4));
    }

    private void handleDoubleAnswer(int selectedIndex, Button selectedButton) {
        if (!firstAnswerSelected) {
            // İlk cevap hakkı
            firstAnswerSelected = true;

            if (selectedIndex == correctAnswerIndex) {
                // Doğru cevap ise
                handleCorrectAnswer(selectedButton);
                nextQuestionWithDelay(); // Sonraki soruya geç
            } else {
                // Yanlış cevap ise
                selectedButton.setEnabled(false);
                selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.firstAnswer)); // Sarı yap
            }
        } else {
            // İkinci cevap hakkı
            if (selectedIndex == correctAnswerIndex) {
                // Doğru cevap ise
                handleCorrectAnswer(selectedButton);
            } else {
                // Yanlış cevap ise
                handleIncorrectAnswer(selectedButton, currentQuestion);
            }
            nextQuestionWithDelay(); // Sonraki soruya geç
        }
    }

    // 3 saniye bekle ve sonraki soruya geç
    private void nextQuestionWithDelay() {
        handler.postDelayed(() -> {
            resetButtonColors();
            currentQuestionIndex++;
            showQuestion();
        }, 2000);
    }

    // 3 saniye bekle ve sonraki soruya geç
    private void nextQuestionFailAnswer() {
        handler.postDelayed(() -> {
            resetButtonColors();
            showQuestion();
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Handler'ı temizle
    }
}
