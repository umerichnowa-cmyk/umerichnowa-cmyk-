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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenTrainingActivity extends Activity {
    private static final int REQUEST_CAPTURE = 7103;
    private static final int REQUEST_NOTIFICATIONS = 7104;
    private static final int SIMULATIONS = 5000;
    private static final int HORIZONS = 6;
    private static final String PREFS = "crashstat";
    private static final String KEY_INITIALIZED = "dataset_initialized";
    private static final String KEY_NOTICE_V3 = "notice_seen_v3";

    private final double[] initialValues = {
            2.92,8.65,3.20,1.00,2.72,1.81,2.89,2.32,1.13,1.98,
            3.62,1.75,1.97,1.80,2.34,2.94,2.40,8.87,1.23,12.37,
            1.55,1.06,3.01,1.99,1.17,1.03,1.05,1.00,1.93,1.08,
            1.93,2.42,2.27,2.64,11.87,16.99,3.91
    };

    private final ArrayList<Double> values = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger forecastGeneration = new AtomicInteger(0);

    private EditText input;
    private TextView latest;
    private TextView risk;
    private TextView predictionValue;
    private TextView predictionZone;
    private TextView predictionProbabilities;
    private TextView predictionConfidence;
    private TextView futurePredictions;
    private TextView summary;
    private TextView recent;
    private TextView screenStatus;
    private TextView trainingInfo;
    private LinearLayout details;
    private Button compactButton;
    private boolean compact = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ScreenReaderService.ACTION_ROUND_CAPTURED.equals(intent.getAction())) {
                double value = intent.getDoubleExtra(ScreenReaderService.EXTRA_VALUE, 0.0);
                loadValues();
                refresh();
                if (value >= 1.0) {
                    screenStatus.setText(String.format(Locale.FRANCE,
                            "Lecture active • %.2fx ajouté automatiquement", value));
                }
            } else if (ScreenReaderService.ACTION_READER_STATUS.equals(intent.getAction())) {
                boolean active = intent.getBooleanExtra(ScreenReaderService.EXTRA_ACTIVE, false);
                updateReaderStatus(active);
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
        refresh();
        updateReaderStatus(RoundStore.isReaderActive(this));
        showNoticeOnce();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screenStatus != null) updateReaderStatus(RoundStore.isReaderActive(this));
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
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

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7,17,31));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(14),dp(14),dp(28));
        scroll.addView(root);

        root.addView(text("CrashStat AI Trainer",27,Color.WHITE,true));
        TextView subtitle = text("Collecte OCR et apprentissage local continu",14,Color.rgb(151,170,195),false);
        subtitle.setPadding(0,0,0,dp(12));
        root.addView(subtitle);

        LinearLayout readerCard = card();
        readerCard.addView(text("LECTURE AUTOMATIQUE DU JEU",17,Color.rgb(255,181,75),true));
        TextView readerHelp = text(
                "Optimisée pour le jeu affiché dans la moitié inférieure, comme dans votre vidéo.",
                13,Color.rgb(202,214,230),false);
        readerHelp.setPadding(0,dp(5),0,dp(9));
        readerCard.addView(readerHelp);

        LinearLayout readerActions = new LinearLayout(this);
        readerActions.setOrientation(LinearLayout.HORIZONTAL);
        Button startReader = button("DÉMARRER",Color.rgb(28,126,92));
        Button stopReader = button("ARRÊTER",Color.rgb(160,48,58));
        readerActions.addView(startReader,new LinearLayout.LayoutParams(0,dp(52),1));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0,dp(52),1);
        stopParams.setMargins(dp(9),0,0,0);
        readerActions.addView(stopReader,stopParams);
        readerCard.addView(readerActions);

        screenStatus = text("Prêt à lire l’écran",14,Color.rgb(158,177,201),true);
        screenStatus.setPadding(0,dp(9),0,0);
        screenStatus.setGravity(Gravity.CENTER);
        readerCard.addView(screenStatus);

        trainingInfo = text("",13,Color.rgb(158,177,201),false);
        trainingInfo.setPadding(0,dp(5),0,0);
        trainingInfo.setGravity(Gravity.CENTER);
        readerCard.addView(trainingInfo);
        root.addView(readerCard,spaced());

        startReader.setOnClickListener(v -> startScreenReading());
        stopReader.setOnClickListener(v -> stopScreenReading());

        LinearLayout entryCard = card();
        entryCard.addView(text("Ajouter manuellement le dernier multiplicateur",16,Color.WHITE,true));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(9),0,0);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ex. 2,45");
        input.setHintTextColor(Color.rgb(115,135,160));
        input.setTextColor(Color.WHITE);
        input.setTextSize(21);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(dp(14),0,dp(14),0);
        input.setBackground(round(Color.rgb(18,38,62),13));
        row.addView(input,new LinearLayout.LayoutParams(0,dp(54),1));

        Button add = button("AJOUTER",Color.rgb(255,138,0));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(108),dp(54));
        addParams.setMargins(dp(9),0,0,0);
        row.addView(add,addParams);
        add.setOnClickListener(v -> addValue());
        input.setOnEditorActionListener((v,actionId,event) -> { addValue(); return true; });
        entryCard.addView(row);
        root.addView(entryCard,spaced());

        latest = text("",21,Color.WHITE,true);
        latest.setGravity(Gravity.CENTER);
        latest.setPadding(dp(8),dp(5),dp(8),dp(7));
        root.addView(latest);

        LinearLayout predictionCard = card();
        predictionCard.setBackground(round(Color.rgb(24,42,67),17));
        TextView predictionTitle = text("PRÉVISION PROBABILISTE M+1",16,Color.rgb(255,181,75),true);
        predictionTitle.setGravity(Gravity.CENTER);
        predictionCard.addView(predictionTitle);

        predictionValue = text("Calcul en cours…",31,Color.WHITE,true);
        predictionValue.setGravity(Gravity.CENTER);
        predictionValue.setPadding(0,dp(7),0,dp(3));
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

        risk = text("",15,Color.WHITE,true);
        risk.setGravity(Gravity.CENTER);
        risk.setPadding(dp(11),dp(11),dp(11),dp(11));
        root.addView(risk,spaced());

        LinearLayout futureCard = card();
        futureCard.addView(text("Projection probabiliste M+1 à M+6",17,Color.WHITE,true));
        futurePredictions = text("Calcul en cours…",15,Color.rgb(255,181,75),true);
        futurePredictions.setPadding(0,dp(8),0,0);
        futureCard.addView(futurePredictions);
        root.addView(futureCard,spaced());

        LinearLayout recentCard = card();
        recentCard.addView(text("12 dernières manches enregistrées",17,Color.WHITE,true));
        recent = text("",16,Color.rgb(255,181,75),true);
        recent.setPadding(0,dp(8),0,0);
        recentCard.addView(recent);
        root.addView(recentCard,spaced());

        details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        root.addView(details);

        LinearLayout summaryCard = card();
        summaryCard.addView(text("Données d’entraînement",17,Color.WHITE,true));
        summary = text("",15,Color.rgb(224,233,244),false);
        summary.setPadding(0,dp(8),0,0);
        summaryCard.addView(summary);
        details.addView(summaryCard,spaced());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button undo = button("ANNULER",Color.rgb(38,64,92));
        compactButton = button("MODE COMPACT",Color.rgb(38,64,92));
        Button session = button("SESSION",Color.rgb(151,47,55));
        actions.addView(undo,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0,dp(50),1.25f);
        middle.setMargins(dp(7),0,dp(7),0);
        actions.addView(compactButton,middle);
        actions.addView(session,new LinearLayout.LayoutParams(0,dp(50),1));
        root.addView(actions,spaced());

        undo.setOnClickListener(v -> undoLast());
        compactButton.setOnClickListener(v -> toggleCompact());
        session.setOnClickListener(v -> openSessionDialog());

        TextView foot = text(
                "L’OCR enregistre uniquement les multiplicateurs visibles. Il ne lit ni le code ni les graines du serveur et ne garantit aucun résultat futur.",
                12,Color.rgb(150,165,185),false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot);

        setContentView(scroll);
    }

    private void startScreenReading() {
        if (RoundStore.isReaderActive(this)) {
            toast("La lecture est déjà active");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        screenStatus.setText("Autorisez la capture de l’écran dans la fenêtre Android…");
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            screenStatus.setText("Autorisation refusée • lecture inactive");
            return;
        }

        Intent serviceIntent = new Intent(this, ScreenReaderService.class)
                .setAction(ScreenReaderService.ACTION_START)
                .putExtra(ScreenReaderService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ScreenReaderService.EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent);
        else startService(serviceIntent);
        screenStatus.setText("Démarrage de la lecture OCR…");
    }

    private void stopScreenReading() {
        Intent stopIntent = new Intent(this, ScreenReaderService.class)
                .setAction(ScreenReaderService.ACTION_STOP);
        startService(stopIntent);
        updateReaderStatus(false);
    }

    private void updateReaderStatus(boolean active) {
        if (active) {
            screenStatus.setText("Lecture active • laissez le jeu visible en bas de l’écran");
            screenStatus.setTextColor(Color.rgb(87,211,155));
        } else {
            screenStatus.setText("Lecture inactive • appuyez sur DÉMARRER");
            screenStatus.setTextColor(Color.rgb(158,177,201));
        }
        updateTrainingInfo();
    }

    private void updateTrainingInfo() {
        if (trainingInfo == null) return;
        trainingInfo.setText(String.format(Locale.FRANCE,
                "%d manches dans le modèle • %d capturées automatiquement",
                values.size(), RoundStore.automaticCount(this)));
    }

    private void addValue() {
        String raw = input.getText().toString().trim().replace(',','.');
        if (raw.isEmpty()) { toast("Entrez un multiplicateur"); return; }
        try {
            double value = Double.parseDouble(raw);
            if (value < 1.0 || value > 100000.0) {
                toast("Valeur autorisée : 1,00 à 100000");
                return;
            }
            RoundStore.appendManual(this, value, System.currentTimeMillis());
            input.setText("");
            loadValues();
            refresh();
        } catch (NumberFormatException error) {
            toast("Multiplicateur non valide");
        }
    }

    private void undoLast() {
        loadValues();
        if (values.isEmpty()) { toast("Aucune saisie à annuler"); return; }
        values.remove(values.size()-1);
        RoundStore.save(this, values);
        refresh();
    }

    private void toggleCompact() {
        compact = !compact;
        details.setVisibility(compact ? View.GONE : View.VISIBLE);
        compactButton.setText(compact ? "MODE COMPLET" : "MODE COMPACT");
    }

    private void openSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Gérer les données")
                .setMessage("Effacer vide le modèle pour une nouvelle collecte. Restaurer remet les 37 manches extraites de la première vidéo.")
                .setNegativeButton("Annuler",null)
                .setNeutralButton("Restaurer",(dialog,which) -> {
                    values.clear();
                    for (double value : initialValues) values.add(value);
                    RoundStore.save(this, values);
                    refresh();
                })
                .setPositiveButton("Effacer",(dialog,which) -> {
                    values.clear();
                    RoundStore.save(this, values);
                    refresh();
                })
                .show();
    }

    private void loadValues() {
        values.clear();
        values.addAll(RoundStore.load(this));
    }

    private void refresh() {
        updateTrainingInfo();
        if (values.isEmpty()) {
            latest.setText("Aucune manche enregistrée");
            recent.setText("—");
            summary.setText("Démarrez la lecture ou ajoutez au moins 6 manches.");
            setRisk("Données insuffisantes",Color.rgb(38,64,92));
            showInsufficientPrediction();
            return;
        }

        double sum=0,min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for (double value:values) {
            sum+=value;
            min=Math.min(min,value);
            max=Math.max(max,value);
        }
        double mean=sum/values.size();
        List<Double> sorted=new ArrayList<>(values);
        Collections.sort(sorted);
        int n=sorted.size();
        double median=n%2==0
                ?(sorted.get(n/2-1)+sorted.get(n/2))/2.0
                :sorted.get(n/2);
        double last=values.get(values.size()-1);

        latest.setText(String.format(Locale.FRANCE,"Dernier résultat : %.2fx",last));
        summary.setText(String.format(Locale.FRANCE,
                "Manches apprises : %d\nCaptures OCR : %d\nMoyenne : %.2fx\nMédiane : %.2fx\nMinimum : %.2fx\nMaximum : %.2fx",
                values.size(),RoundStore.automaticCount(this),mean,median,min,max));

        StringBuilder list=new StringBuilder();
        int start=Math.max(0,values.size()-12);
        for (int i=values.size()-1;i>=start;i--) {
            list.append(String.format(Locale.FRANCE,"%.2fx",values.get(i)));
            if (i>start) list.append("   ");
        }
        recent.setText(list.toString());

        updateRisk();
        requestForecast();
    }

    private void updateRisk() {
        int sampleStart=Math.max(0,values.size()-12);
        int low=0,veryLow=0;
        for (int i=sampleStart;i<values.size();i++) {
            if (values.get(i)<2.0) low++;
            if (values.get(i)<1.5) veryLow++;
        }
        int streak=0;
        for (int i=values.size()-1;i>=0 && values.get(i)<2.0;i--) streak++;
        double rate=low/(double)Math.max(1,values.size()-sampleStart);
        if (veryLow>=8 || streak>=5 || rate>=0.82) {
            setRisk("Séquence basse • forte incertitude",Color.rgb(177,48,57));
        } else if (veryLow>=5 || streak>=3 || rate>=0.62) {
            setRisk("Séquence modérée • incertitude élevée",Color.rgb(197,112,20));
        } else {
            setRisk("Séquence diversifiée",Color.rgb(28,126,92));
        }
    }

    private void requestForecast() {
        if (values.size()<6) {
            showInsufficientPrediction();
            return;
        }

        int generation=forecastGeneration.incrementAndGet();
        ArrayList<Double> snapshot=new ArrayList<>(values);
        predictionValue.setText("Calcul…");
        predictionZone.setText("Apprentissage des séquences de 1 à 5 manches");
        predictionProbabilities.setText("");
        predictionConfidence.setText(String.format(Locale.FRANCE,
                "%,d simulations en préparation",SIMULATIONS));
        futurePredictions.setText("Calcul des trajectoires M+1 à M+6…");

        new Thread(() -> {
            PredictionEngine.Forecast forecast=
                    PredictionEngine.calculate(snapshot,SIMULATIONS,HORIZONS);
            mainHandler.post(() -> {
                if (generation!=forecastGeneration.get()) return;
                renderForecast(forecast);
            });
        },"CrashStatForecast").start();
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
    }

    private void showInsufficientPrediction() {
        predictionValue.setText("6 manches requises");
        predictionZone.setText("La prévision sera recalculée après chaque capture.");
        predictionProbabilities.setText("");
        predictionConfidence.setText("Le modèle apprend uniquement à partir des résultats visibles.");
        futurePredictions.setText("Les projections M+1 à M+6 apparaîtront ici.");
    }

    private void setRisk(String label,int color) {
        risk.setText(label);
        risk.setBackground(round(color,14));
    }

    private void showNoticeOnce() {
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if (prefs.getBoolean(KEY_NOTICE_V3,false)) return;
        new AlertDialog.Builder(this)
                .setTitle("Lecture d’écran et confidentialité")
                .setMessage("CrashStat ne sauvegarde pas la vidéo. Avec votre autorisation Android, il analyse localement la zone inférieure, attend qu’un multiplicateur final reste stable, puis enregistre uniquement cette valeur. Vous devez relancer l’autorisation à chaque nouvelle session de capture.")
                .setCancelable(false)
                .setPositiveButton("J’ai compris",(dialog,which) ->
                        prefs.edit().putBoolean(KEY_NOTICE_V3,true).apply())
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
