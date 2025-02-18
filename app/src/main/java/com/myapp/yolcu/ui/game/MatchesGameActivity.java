package com.myapp.yolcu.ui.game;

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
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Entity.EsmaHusna;
import com.myapp.yolcu.ui.Rozet.BadgeType;
import com.myapp.yolcu.ui.Service.BadgeService;
import com.myapp.yolcu.ui.Service.GoldService;
import com.myapp.yolcu.ui.Service.SoundService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MatchesGameActivity extends AppCompatActivity {

    private Button mEsma1, mEsma2, mEsma3, mEsma4;
    private Button mAnswer1, mAnswer2, mAnswer3, mAnswer4;
    private ArrayList<EsmaHusna> esmaHusnas;
    private ArrayList<EsmaHusna> unansweredEsmas;
    private HashSet<EsmaHusna> answeredEsmas;
    private EsmaHusna currentEsma = new EsmaHusna();
    private Dialog loadingDialog, mDialog;
    private GoldService goldService;
    private SoundService soundService;
    private BadgeService badgeService;
    private TextView goldAmountText,timerText,gold_status;
    private Button selectedEsmaButton; // Seçilen Esma butonunu tutar
    private Button selectedAnswerButton; // Seçilen Anlam butonunu tutar
    private List<Button> allButtonList = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());;
    private int timeLeft = 500; // 60 saniye geri sayım süresi
    private List<Button> mEsmaButtons;
    private List<Button> mAnswerButtons;
    private Animation buttonClickAnim;
    private final int ADD_COIN=50;
    private final int MINUS_COIN=20;
    private boolean isAnimationRunning = false;
    private int dogruBilinenSoruAdet=0,profit=0;
    private boolean yanlisYanitVerildiMi=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches_game);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        initializeLoading();
        initializeViews();
        initializeUserGold();
        initializeServices();
        fetchQuestions();
        buttonClicker();//

        // onBackPressedCallback tanımlama
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Timer'ı durdur
                stopTimer();
                // Çıkış onayı için bir AlertDialog oluştur
                new AlertDialog.Builder(MatchesGameActivity.this)
                        .setTitle(getString(R.string.exit_game))
                        .setMessage(getString(R.string.exit_game_label))
                        .setPositiveButton(getString(R.string.evet), (dialog, which) -> endGamePopup(false,false,true))
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

    private void buttonClicker() {
        // Butonlara onClickListener ekleyerek tıklama olaylarını işliyoruz
        setOptionClickListener(mEsma1, mAnswer1);
        setOptionClickListener(mEsma2, mAnswer2);
        setOptionClickListener(mEsma3, mAnswer3);
        setOptionClickListener(mEsma4, mAnswer4);
    }

    private void fetchQuestions() {
        // Başlangıçta soruları karıştır
        shuffleQuestions();
        loadingDialog.dismiss();
    }

    private void initializeServices() {
        soundService = new SoundService(this);
        badgeService = new BadgeService(this);
    }

    private void initializeUserGold() {
        goldService = new GoldService(this);
        saveGoldAmountText();
    }

    private void saveGoldAmountText() {
        goldAmountText.setText(String.format(Locale.getDefault(), "%d", goldService.getGoldAmount()));
    }

    private void initializeViews() {
        // Butonları referans alıyoruz
        goldAmountText=findViewById(R.id.gold_amount_text_matches);
        timerText=findViewById(R.id.timer_text);
        gold_status=findViewById(R.id.gold_status);

        mEsma1 = findViewById(R.id.m_esma_1);
        mEsma2 = findViewById(R.id.m_esma_2);
        mEsma3 = findViewById(R.id.m_esma_3);
        mEsma4 = findViewById(R.id.m_esma_4);
        allButtonList.add(mEsma1);
        allButtonList.add(mEsma2);
        allButtonList.add(mEsma3);
        allButtonList.add(mEsma4);

        mAnswer1 = findViewById(R.id.m_answer_1);
        mAnswer2 = findViewById(R.id.m_answer_2);
        mAnswer3 = findViewById(R.id.m_answer_3);
        mAnswer4 = findViewById(R.id.m_answer_4);
        allButtonList.add(mAnswer1);
        allButtonList.add(mAnswer2);
        allButtonList.add(mAnswer3);
        allButtonList.add(mAnswer4);

        mEsmaButtons = Arrays.asList(mEsma1, mEsma2, mEsma3, mEsma4);
        mAnswerButtons = Arrays.asList(mAnswer1, mAnswer2, mAnswer3, mAnswer4);
        // Esma-i Hüsna verilerini oluştur
        esmaHusnas = new EsmaHusna().getEsmaHusnaListesiniAl();


        // Yanıtlanan Esma-i Hüsna'ları tutacak yapı
        answeredEsmas = new HashSet<>();
        unansweredEsmas = new ArrayList<>(esmaHusnas);

        buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click_anim);

    }

    private void initializeLoading() {
        // Splash ekranını başlat
        loadingDialog = new Dialog(MatchesGameActivity.this);
        loadingDialog.setContentView(R.layout.game_loading_splash);
        loadingDialog.setCancelable(false); // Geri tuşu ile iptal edilemesin
        loadingDialog.show();
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
                    endGamePopup(true,false,false);
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
        mDialog = new Dialog(MatchesGameActivity.this);
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
        TextView correctAnswerCountLevelLabel=mDialog.findViewById(R.id.correctAnswerCountLevelLabel);
        correctAnswerCountLevelLabel.setText(getString(R.string.wordGameCorrectLabel));

        if (Boolean.TRUE.equals(sureDoldumu) && Boolean.FALSE.equals(yarismaKazandiMi) && Boolean.FALSE.equals(cikisYapildi)){
            hitpouptext.setText(getString(R.string.timeupLabel));
            imagePopUp.setImageResource(R.drawable.time_passing);
            soundService.timeoutEffect();
        }else if(cikisYapildi){
            soundService.timeoutEffect();
            imagePopUp.setImageResource(R.drawable.leave);
        }
        if(Boolean.TRUE.equals(yarismaKazandiMi)){
            soundService.winGameEffect();
            imagePopUp.setImageResource(R.drawable.winner);
            hitpouptext.setText(getString(R.string.winnerLabel));
            validationRozet(yarismaKazandiMi);
        }

        TextView correctAnswerCount = mDialog.findViewById(R.id.correctAnswerCount);
        // ValueAnimator ile 0'dan correctAnswers sayısına kadar animasyon oluştur
        ValueAnimator animator = ValueAnimator.ofInt(0, dogruBilinenSoruAdet);

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
            totalProfitLabel.setText("+"+animation.getAnimatedValue().toString());
        });
        animatorProfit.start();

        // Dialog'u göster
        mDialog.show();
        //app review cagir
        badgeService.showInAppReview(MatchesGameActivity.this);
    }

    private void validationRozet(boolean yarismaKazandiMi) {
        if(yarismaKazandiMi && yanlisYanitVerildiMi){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_KAZANAN);
        }
        if(yarismaKazandiMi && !yanlisYanitVerildiMi){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_5);
        }
        if(dogruBilinenSoruAdet==50 && !yanlisYanitVerildiMi){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_4);
        }
        if(dogruBilinenSoruAdet==33){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_1);
        }
        if(dogruBilinenSoruAdet==66){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_2);
        }
        if(dogruBilinenSoruAdet==99){
            badgeService.enableBadge(BadgeType.ESMA_YARISMA_3);
        }
    }

    private void setOptionClickListener(Button esmaButton, Button answerButton) {
        esmaButton.setOnClickListener(v -> handleEsmaSelection(esmaButton));
        answerButton.setOnClickListener(v -> handleAnswerSelection(answerButton));
    }

    private void handleEsmaSelection(Button esmaButton) {
        selectedEsmaButton = esmaButton;
        selectedEsmaButton.startAnimation(buttonClickAnim);
        disabledEsmaButtons();
        selectedEsmaButton.setAlpha(1.0f);// Seçilen esma butonunun şeffaflık değerini değiştir
        if (selectedAnswerButton != null) {
            secimleriKontrolEt();
        }
    }

    private void handleAnswerSelection(Button answerButton) {
        selectedAnswerButton = answerButton;
        selectedAnswerButton.startAnimation(buttonClickAnim);
        disabledAnswerButtons();
        selectedAnswerButton.setAlpha(1.0f);
        if (selectedEsmaButton != null) {
            secimleriKontrolEt();
        }
    }

    private void secimleriKontrolEt() {
        // Seçilen cevabı kontrol et
        String selectedAnswer = selectedAnswerButton.getText().toString();
        String correctAnswer = currentEsma.getMeaningbyName(String.valueOf(selectedEsmaButton.getText()), esmaHusnas);

        if (selectedAnswer.equals(correctAnswer)) {
            soundService.matchesSuccessButtonEffect();
            // Doğru cevap verildiğinde, her iki butonun arka planını yeşil yap
            selectedEsmaButton.setBackgroundColor(ContextCompat.getColor(this, R.color.correctAnswer));
            selectedAnswerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.correctAnswer));
            selectedEsmaButton.setAlpha(0.5f);
            selectedEsmaButton.setEnabled(false);
            selectedAnswerButton.setAlpha(0.5f);
            selectedAnswerButton.setEnabled(false);
            enableOtherButtons();
            showAnimation(true);
            dogruBilinenSoruAdet+=1;
            goldService.addGoldAmount(ADD_COIN);
            saveGoldAmountText();

            // 4 soruyu doğru cevapladıysak, yeni 4 soruya geç
            answeredEsmas.add(currentEsma);
            validationRozet(false);
            if (isAllButtonDisable()) {
                shuffleQuestions();
            }
        } else {
            // Yanlış cevap verildiğinde, butonları eski haline döndür
            soundService.matchesFailButtonEffect();
            selectedEsmaButton.setAlpha(1.0f); // Esma butonunu aktif yap
            selectedAnswerButton.setAlpha(1.0f);
            enableOtherButtons();
            yanlisYanitVerildiMi=true;
            showAnimation(false);
            if(goldService.getGoldAmount()>MINUS_COIN) {
                goldService.minusGoldAmount(MINUS_COIN);
                saveGoldAmountText();
            }
        }

        // Cevap seçildikten sonra Esma ve anlam butonlarının durumunu sıfırla
        selectedEsmaButton = null;
        selectedAnswerButton = null;
    }
    @SuppressLint("SetTextI18n")
    private void showAnimation(boolean dogruYanit){
        if (isAnimationRunning) {
            // Animasyon zaten çalışıyor, yeni bir animasyon başlatma
            return;
        }

        isAnimationRunning = true; // Animasyon başlatıldı

        // Doğru veya yanlış yanıt duruma göre ayarla
        if (dogruYanit) {
            gold_status.setText("+"+ADD_COIN);
            gold_status.setTextColor(ContextCompat.getColor(this, R.color.correctAnswer));
            profit+=ADD_COIN;
        } else {
            gold_status.setText("-"+MINUS_COIN);
            gold_status.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswer));
        }
        // Görünür yap
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
    private void enableOtherButtons() {
        for (Button button : allButtonList) {
            if (button.isEnabled()) {
                button.setAlpha(1.0f);
            }
        }
    }

    private boolean isAllButtonDisable() {
        for (Button button : allButtonList) {
            if (button.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    private void disabledEsmaButtons() {
        mEsma1.setAlpha(0.5f);
        mEsma2.setAlpha(0.5f);
        mEsma3.setAlpha(0.5f);
        mEsma4.setAlpha(0.5f);
    }

    private void disabledAnswerButtons() {
        mAnswer1.setAlpha(0.5f);
        mAnswer2.setAlpha(0.5f);
        mAnswer3.setAlpha(0.5f);
        mAnswer4.setAlpha(0.5f);
    }

    private void enableButtons() {
        // Diğer butonları tekrar aktif hale getir
        mEsma1.setAlpha(1.0f);
        mEsma2.setAlpha(1.0f);
        mEsma3.setAlpha(1.0f);
        mEsma4.setAlpha(1.0f);

        mEsma1.setEnabled(true);
        mEsma2.setEnabled(true);
        mEsma3.setEnabled(true);
        mEsma4.setEnabled(true);

        mAnswer1.setAlpha(1.0f);
        mAnswer2.setAlpha(1.0f);
        mAnswer3.setAlpha(1.0f);
        mAnswer4.setAlpha(1.0f);

        mAnswer1.setEnabled(true);
        mAnswer2.setEnabled(true);
        mAnswer3.setEnabled(true);
        mAnswer4.setEnabled(true);
        int defaultColor = -1;
        mEsma1.setBackgroundColor(defaultColor);
        mEsma2.setBackgroundColor(defaultColor);
        mEsma3.setBackgroundColor(defaultColor);
        mEsma4.setBackgroundColor(defaultColor);

        mAnswer1.setBackgroundColor(defaultColor);
        mAnswer2.setBackgroundColor(defaultColor);
        mAnswer3.setBackgroundColor(defaultColor);
        mAnswer4.setBackgroundColor(defaultColor);


    }

    private void shuffleQuestions() {
        // Eğer tüm Esma-i Hüsna'lar cevaplandıysa, tekrar baştan başla
        if (answeredEsmas.size() == esmaHusnas.size()) {
            answeredEsmas.clear();
        }

        // Henüz yanıtlanmamış sorulardan 4 tane al
        unansweredEsmas.clear();
        for (EsmaHusna esma : esmaHusnas) {
            if (!esma.isDahaOnceSorulduMu()) {
                unansweredEsmas.add(esma);
            }
        }
        if(unansweredEsmas.isEmpty()){
            endGamePopup(false,true,false);
            return;
        }

        Collections.shuffle(unansweredEsmas); // Soruları karıştır

        // İlk 4 değeri alın
        List<EsmaHusna> firstForthUnansweredEsmas = unansweredEsmas.subList(0, Math.min(4, unansweredEsmas.size()));


        // Esma ve anlamları butonlara yerleştir
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> meanings = new ArrayList<>();

        for (EsmaHusna esma : firstForthUnansweredEsmas) {
            esma.setDahaOnceSorulduMu(true);
            names.add(esma.getName());
            meanings.add(esma.getMeaning());
        }

        Collections.shuffle(names);  // İsimleri karıştır
        Collections.shuffle(meanings); // Anlamları karıştır

        // Butonlara isimleri ve anlamları yerleştir
        for (int i = 0; i < mEsmaButtons.size(); i++) {
            if (i < names.size()) {
                mEsmaButtons.get(i).setText(names.get(i));
            } else {
                mEsmaButtons.get(i).setVisibility(View.GONE);
            }
        }

        for (int i = 0; i < mAnswerButtons.size(); i++) {
            if (i < meanings.size()) {
                mAnswerButtons.get(i).setText(meanings.get(i));
            } else {
                mAnswerButtons.get(i).setVisibility(View.GONE);
            }
        }

        // Butonları aktif yap
        enableButtons();
    }
}
