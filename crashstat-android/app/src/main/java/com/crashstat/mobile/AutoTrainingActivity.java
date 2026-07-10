package com.crashstat.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoTrainingActivity extends Activity {
    private static final int REQUEST_CAPTURE = 8103;
    private static final int REQUEST_NOTIFICATIONS = 8104;
    private static final int SIMULATIONS = 5000;
    private static final int HORIZONS = 6;
    private static final String PREFS = "crashstat";
    private static final String KEY_INITIALIZED = "dataset_initialized";
    private static final String KEY_NOTICE_V4 = "notice_seen_v4";

    private final double[] initialValues = {
            2.92,8.65,3.20,1.00,2.72,1.81,2.89,2.32,1.13,1.98,
            3.62,1.75,1.97,1.80,2.34,2.94,2.40,8.87,1.23,12.37,
            1.55,1.06,3.01,1.99,1.17,1.03,1.05,1.00,1.93,1.08,
            1.93,2.42,2.27,2.64,11.87,16.99,3.91
    };

    private final ArrayList<Double> values = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger forecastGeneration = new AtomicInteger(0);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE);

    private TextView readerBadge;
    private TextView readerActivity;
    private TextView trainingState;
    private TextView trainingCounters;
    private TextView latest;
    private TextView predictionValue;
    private TextView predictionZone;
    private TextView predictionProbabilities;
    private TextView predictionConfidence;
    private TextView futurePredictions;
    private TextView recent;
    private TextView dataSummary;
    private LinearLayout details;
    private Button compactButton;
    private boolean compact = false;
    private boolean awaitingForecastAfterCapture = false;
    private boolean pulse = false;

    private final Runnable statusTicker = new Runnable() {
        @Override
        public void run() {
            updateLiveStatus();
            mainHandler.postDelayed(this, 1000L);
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ScreenReaderService.ACTION_ROUND_CAPTURED.equals(action)) {
                double value = intent.getDoubleExtra(ScreenReaderService.EXTRA_VALUE, 0.0);
                awaitingForecastAfterCapture = true;
                trainingState.setText(String.format(Locale.FRANCE,
                        "NOUVELLE MANCHE %.2fx • apprentissage et recalcul de M+1…", value));
                trainingState.setTextColor(Color.rgb(255,181,75));
                loadValues();
                refreshAll();
            } else if (ScreenReaderService.ACTION_READER_STATUS.equals(action)) {
                boolean active = intent.getBooleanExtra(ScreenReaderService.EXTRA_ACTIVE, false);
                updateReaderState(active);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7,17,31));
        getWindow().setNavigationBarColor(Color.rgb(7,17,31));
        initializeDataset();
        loadValues();
        buildUi();
        registerScreenReceiver();
        refreshAll();
        updateReaderState(RoundStore.isReaderActive(this));
        mainHandler.post(statusTicker);
        showNoticeOnce();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadValues();
        if (readerBadge != null) {
            refreshAll();
            updateReaderState(RoundStore.isReaderActive(this));
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(statusTicker);
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void initializeDataset() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ArrayList<Double> existing = RoundStore.load(this);
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            if (existing.isEmpty()) {
                for (double value : initialValues) existing.add(value);
                RoundStore.save(this, existing);
            }
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
        }
    }

    private void registerScreenReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScreenReaderService.ACTION_ROUND_CAPTURED);
        filter.addAction(ScreenReaderService.ACTION_READER_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7,17,31));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(14),dp(14),dp(28));
        scroll.addView(root);

        root.addView(text("CrashStat Auto AI",28,Color.WHITE,true));
        TextView subtitle = text("Collecte, entraînement et prévision entièrement automatiques",14,Color.rgb(151,170,195),false);
        subtitle.setPadding(0,0,0,dp(12));
        root.addView(subtitle);

        LinearLayout readerCard = card();
        readerCard.addView(text("LECTURE AUTOMATIQUE DU JEU",17,Color.rgb(255,181,75),true));

        readerBadge = text("LECTURE INACTIVE",18,Color.WHITE,true);
        readerBadge.setGravity(Gravity.CENTER);
        readerBadge.setPadding(dp(12),dp(11),dp(12),dp(11));
        readerBadge.setBackground(round(Color.rgb(112,54,62),14));
        LinearLayout.LayoutParams badgeParams = spaced();
        badgeParams.setMargins(0,dp(9),0,dp(7));
        readerCard.addView(readerBadge,badgeParams);

        readerActivity = text("Appuyez sur DÉMARRER pour autoriser la lecture.",14,Color.rgb(202,214,230),false);
        readerActivity.setGravity(Gravity.CENTER);
        readerCard.addView(readerActivity);

        trainingState = text("Aucun entraînement automatique en cours.",14,Color.rgb(158,177,201),true);
        trainingState.setGravity(Gravity.CENTER);
        trainingState.setPadding(0,dp(8),0,0);
        readerCard.addView(trainingState);

        trainingCounters = text("",13,Color.rgb(158,177,201),false);
        trainingCounters.setGravity(Gravity.CENTER);
        trainingCounters.setPadding(0,dp(6),0,dp(10));
        readerCard.addView(trainingCounters);

        LinearLayout readerActions = new LinearLayout(this);
        readerActions.setOrientation(LinearLayout.HORIZONTAL);
        Button startReader = button("DÉMARRER",Color.rgb(28,126,92));
        Button stopReader = button("ARRÊTER",Color.rgb(160,48,58));
        readerActions.addView(startReader,new LinearLayout.LayoutParams(0,dp(52),1));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0,dp(52),1);
        stopParams.setMargins(dp(9),0,0,0);
        readerActions.addView(stopReader,stopParams);
        readerCard.addView(readerActions);
        root.addView(readerCard,spaced());

        startReader.setOnClickListener(v -> startScreenReading());
        stopReader.setOnClickListener(v -> stopScreenReading());

        latest = text("",22,Color.WHITE,true);
        latest.setGravity(Gravity.CENTER);
        latest.setPadding(dp(8),dp(5),dp(8),dp(8));
        root.addView(latest);

        LinearLayout predictionCard = card();
        predictionCard.setBackground(round(Color.rgb(24,42,67),17));
        TextView predictionTitle = text("PRÉVISION PROBABILISTE M+1",16,Color.rgb(255,181,75),true);
        predictionTitle.setGravity(Gravity.CENTER);
        predictionCard.addView(predictionTitle);

        predictionValue = text("Calcul en cours…",32,Color.WHITE,true);
        predictionValue.setGravity(Gravity.CENTER);
        predictionValue.setPadding(0,dp(8),0,dp(3));
        predictionCard.addView(predictionValue);

        predictionZone = text("",15,Color.rgb(221,230,241),false);
        predictionZone.setGravity(Gravity.CENTER);
        predictionCard.addView(predictionZone);

        predictionProbabilities = text("",14,Color.rgb(221,230,241),false);
        predictionProbabilities.setGravity(Gravity.CENTER);
        predictionProbabilities.setPadding(0,dp(8),0,0);
        predictionCard.addView(predictionProbabilities);

        predictionConfidence = text("",12,Color.rgb(155,173,197),false);
        predictionConfidence.setGravity(Gravity.CENTER);
        predictionConfidence.setPadding(0,dp(8),0,0);
        predictionCard.addView(predictionConfidence);
        root.addView(predictionCard,spaced());

        LinearLayout futureCard = card();
        futureCard.addView(text("Projection probabiliste M+1 à M+6",17,Color.WHITE,true));
        futurePredictions = text("Calcul en cours…",15,Color.rgb(255,181,75),true);
        futurePredictions.setPadding(0,dp(8),0,0);
        futureCard.addView(futurePredictions);
        root.addView(futureCard,spaced());

        LinearLayout recentCard = card();
        recentCard.addView(text("12 dernières manches ajoutées automatiquement",17,Color.WHITE,true));
        recent = text("",16,Color.rgb(255,181,75),true);
        recent.setPadding(0,dp(8),0,0);
        recentCard.addView(recent);
        root.addView(recentCard,spaced());

        details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        root.addView(details);

        LinearLayout dataCard = card();
        dataCard.addView(text("État du modèle",17,Color.WHITE,true));
        dataSummary = text("",15,Color.rgb(224,233,244),false);
        dataSummary.setPadding(0,dp(8),0,0);
        dataCard.addView(dataSummary);
        details.addView(dataCard,spaced());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        compactButton = button("MODE COMPACT",Color.rgb(38,64,92));
        Button reset = button("RÉINITIALISER",Color.rgb(151,47,55));
        actions.addView(compactButton,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0,dp(50),1);
        resetParams.setMargins(dp(9),0,0,0);
        actions.addView(reset,resetParams);
        root.addView(actions,spaced());

        compactButton.setOnClickListener(v -> toggleCompact());
        reset.setOnClickListener(v -> confirmReset());

        TextView foot = text(
                "Aucune saisie manuelle n’est nécessaire. Une valeur n’est ajoutée que lorsque l’OCR détecte un multiplicateur final stable. Les prévisions restent probabilistes.",
                12,Color.rgb(150,165,185),false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot);

        setContentView(scroll);
    }

    private void startScreenReading() {
        if (RoundStore.isReaderActive(this)) {
            toast("La lecture automatique est déjà active");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        readerActivity.setText("Autorisez maintenant la capture dans la fenêtre Android…");
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            readerActivity.setText("Autorisation refusée • lecture inactive");
            updateReaderState(false);
            return;
        }

        Intent serviceIntent = new Intent(this, ScreenReaderService.class)
                .setAction(ScreenReaderService.ACTION_START)
                .putExtra(ScreenReaderService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ScreenReaderService.EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent);
        else startService(serviceIntent);
        readerActivity.setText("Démarrage du scanner OCR…");
    }

    private void stopScreenReading() {
        Intent stopIntent = new Intent(this, ScreenReaderService.class)
                .setAction(ScreenReaderService.ACTION_STOP);
        startService(stopIntent);
        updateReaderState(false);
    }

    private void updateReaderState(boolean active) {
        if (active) {
            readerBadge.setText("● LECTURE ACTIVE");
            readerBadge.setBackground(round(Color.rgb(28,126,92),14));
            readerActivity.setText("OCR actif • le jeu doit rester visible dans la moitié inférieure");
            if (!awaitingForecastAfterCapture) {
                trainingState.setText("En attente du prochain multiplicateur final stable…");
                trainingState.setTextColor(Color.rgb(158,177,201));
            }
        } else {
            readerBadge.setText("LECTURE INACTIVE");
            readerBadge.setBackground(round(Color.rgb(112,54,62),14));
            readerActivity.setText("Appuyez sur DÉMARRER pour autoriser la lecture.");
            if (!awaitingForecastAfterCapture) {
                trainingState.setText("L’apprentissage automatique est arrêté.");
                trainingState.setTextColor(Color.rgb(158,177,201));
            }
        }
        updateTrainingCounters();
    }

    private void updateLiveStatus() {
        if (readerBadge == null) return;
        boolean active = RoundStore.isReaderActive(this);
        if (active && !awaitingForecastAfterCapture) {
            pulse = !pulse;
            readerBadge.setText(pulse ? "● LECTURE ACTIVE" : "◉ LECTURE ACTIVE");
            long lastTime = RoundStore.lastAutomaticTime(this);
            if (lastTime > 0L) {
                long seconds = Math.max(0L,(System.currentTimeMillis()-lastTime)/1000L);
                readerActivity.setText(String.format(Locale.FRANCE,
                        "OCR actif • dernier multiplicateur capturé il y a %d s",seconds));
            } else {
                readerActivity.setText("OCR actif • recherche du premier multiplicateur final…");
            }
        }
        updateTrainingCounters();
    }

    private void updateTrainingCounters() {
        if (trainingCounters == null) return;
        int automatic = RoundStore.automaticCount(this);
        long lastTime = RoundStore.lastAutomaticTime(this);
        double lastValue = RoundStore.lastAutomaticValue(this);
        String last = lastTime > 0L
                ? String.format(Locale.FRANCE,"Dernier entraînement : %.2fx à %s",
                lastValue,timeFormat.format(new Date(lastTime)))
                : "Aucun multiplicateur OCR encore enregistré";
        trainingCounters.setText(String.format(Locale.FRANCE,
                "%d manches dans le modèle • %d capturées par OCR\n%s",
                values.size(),automatic,last));
    }

    private void loadValues() {
        values.clear();
        values.addAll(RoundStore.load(this));
    }

    private void refreshAll() {
        updateTrainingCounters();
        if (values.isEmpty()) {
            latest.setText("Aucune manche enregistrée");
            recent.setText("—");
            dataSummary.setText("Démarrez la lecture automatique et laissez le jeu visible.");
            showInsufficientPrediction();
            return;
        }

        double last = values.get(values.size()-1);
        latest.setText(String.format(Locale.FRANCE,"Dernier résultat appris : %.2fx",last));

        StringBuilder list = new StringBuilder();
        int start = Math.max(0,values.size()-12);
        for (int i=values.size()-1;i>=start;i--) {
            list.append(String.format(Locale.FRANCE,"%.2fx",values.get(i)));
            if (i>start) list.append("   ");
        }
        recent.setText(list.toString());

        dataSummary.setText(String.format(Locale.FRANCE,
                "Manches apprises : %d\nCaptures automatiques : %d\nRéentraînement : après chaque nouvelle valeur OCR\nSimulations par prévision : %,d",
                values.size(),RoundStore.automaticCount(this),SIMULATIONS));

        requestForecast();
    }

    private void requestForecast() {
        if (values.size()<6) {
            showInsufficientPrediction();
            return;
        }

        int generation=forecastGeneration.incrementAndGet();
        ArrayList<Double> snapshot=new ArrayList<>(values);
        predictionValue.setText("Calcul…");
        predictionZone.setText("Le modèle intègre la nouvelle manche");
        predictionProbabilities.setText("");
        predictionConfidence.setText(String.format(Locale.FRANCE,
                "%,d simulations en cours",SIMULATIONS));
        futurePredictions.setText("Actualisation automatique de M+1 à M+6…");

        new Thread(() -> {
            PredictionEngine.Forecast forecast=
                    PredictionEngine.calculate(snapshot,SIMULATIONS,HORIZONS);
            mainHandler.post(() -> {
                if (generation!=forecastGeneration.get()) return;
                renderForecast(forecast);
            });
        },"CrashStatAutoForecast").start();
    }

    private void renderForecast(PredictionEngine.Forecast forecast) {
        if (forecast==null) {
            showInsufficientPrediction();
            return;
        }

        predictionValue.setText(String.format(Locale.FRANCE,"≈ %.2fx",forecast.estimates[0]));
        predictionZone.setText(String.format(Locale.FRANCE,
                "Zone centrale simulée : %.2fx à %.2fx",
                forecast.lows[0],forecast.highs[0]));
        predictionProbabilities.setText(String.format(Locale.FRANCE,
                "≥1,50x : %.0f%%   •   ≥2x : %.0f%%\n≥3x : %.0f%%   •   ≥5x : %.0f%%",
                forecast.p15,forecast.p20,forecast.p30,forecast.p50));
        predictionConfidence.setText(String.format(Locale.FRANCE,
                "Stabilité du modèle : %d%% • %,d simulations • %d suites comparables",
                forecast.confidence,forecast.simulations,forecast.exactPatternMatches));

        StringBuilder future=new StringBuilder();
        for (int i=0;i<forecast.estimates.length;i++) {
            future.append(String.format(Locale.FRANCE,
                    "M+%d : ≈ %.2fx   (%.2f–%.2fx)",
                    i+1,forecast.estimates[i],forecast.lows[i],forecast.highs[i]));
            if (i<forecast.estimates.length-1) future.append('\n');
        }
        futurePredictions.setText(future.toString());

        if (awaitingForecastAfterCapture) {
            awaitingForecastAfterCapture = false;
            trainingState.setText("✓ ENTRAÎNEMENT TERMINÉ • M+1 actualisée automatiquement");
            trainingState.setTextColor(Color.rgb(87,211,155));
        }
    }

    private void showInsufficientPrediction() {
        predictionValue.setText("6 manches requises");
        predictionZone.setText("La prévision apparaîtra automatiquement.");
        predictionProbabilities.setText("");
        predictionConfidence.setText("Aucune saisie manuelle nécessaire.");
        futurePredictions.setText("Les projections M+1 à M+6 apparaîtront ici.");
    }

    private void toggleCompact() {
        compact = !compact;
        details.setVisibility(compact ? View.GONE : View.VISIBLE);
        compactButton.setText(compact ? "MODE COMPLET" : "MODE COMPACT");
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Réinitialiser les données ?")
                .setMessage("Les 37 valeurs initiales seront restaurées. La collecte OCR pourra ensuite continuer automatiquement.")
                .setNegativeButton("Annuler",null)
                .setPositiveButton("Réinitialiser",(dialog,which) -> {
                    values.clear();
                    for (double value : initialValues) values.add(value);
                    RoundStore.save(this,values);
                    refreshAll();
                })
                .show();
    }

    private void showNoticeOnce() {
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if (prefs.getBoolean(KEY_NOTICE_V4,false)) return;
        new AlertDialog.Builder(this)
                .setTitle("Fonctionnement automatique")
                .setMessage("Après DÉMARRER, CrashStat détecte chaque multiplicateur final stable, l’ajoute automatiquement au modèle, relance l’entraînement puis actualise M+1 et M+1 à M+6. Aucun champ de saisie manuelle n’est nécessaire.")
                .setCancelable(false)
                .setPositiveButton("J’ai compris",(dialog,which) ->
                        prefs.edit().putBoolean(KEY_NOTICE_V4,true).apply())
                .show();
    }

    private LinearLayout card() {
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15),dp(14),dp(15),dp(14));
        card.setBackground(round(Color.rgb(14,31,52),16));
        return card;
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0,1.15f);
        if (bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }

    private Button button(String label,int color) {
        Button button=new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(5),0,dp(5),0);
        button.setBackground(round(color,12));
        return button;
    }

    private GradientDrawable round(int color,int radiusDp) {
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,dp(10));
        return params;
    }

    private int dp(int value) {
        return Math.round(value*getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
}
