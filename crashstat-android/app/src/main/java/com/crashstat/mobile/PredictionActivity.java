package com.crashstat.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class PredictionActivity extends Activity {
    private static final String PREFS = "crashstat";
    private static final String KEY_VALUES = "values";
    private static final String KEY_NOTICE_V2 = "notice_seen_v2";
    private static final int SIMULATIONS = 5000;
    private static final int HORIZONS = 6;

    private final ArrayList<Double> values = new ArrayList<>();
    private final double[] initialValues = {
            2.92,8.65,3.20,1.00,2.72,1.81,2.89,2.32,1.13,1.98,
            3.62,1.75,1.97,1.80,2.34,2.94,2.40,8.87,1.23,12.37,
            1.55,1.06,3.01,1.99,1.17,1.03,1.05,1.00,1.93,1.08,
            1.93,2.42,2.27,2.64,11.87,16.99,3.91
    };

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
    private TextView frequencies;
    private TextView recent;
    private LinearLayout details;
    private Button compactButton;
    private boolean compact = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7,17,31));
        getWindow().setNavigationBarColor(Color.rgb(7,17,31));
        loadValues();
        buildUi();
        refresh();
        showNoticeOnce();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7,17,31));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(14),dp(14),dp(28));
        scroll.addView(root);

        root.addView(text("CrashStat Mobile 2",27,Color.WHITE,true));
        TextView subtitle = text("Prévisions probabilistes hors ligne",14,Color.rgb(151,170,195),false);
        subtitle.setPadding(0,0,0,dp(12));
        root.addView(subtitle);

        LinearLayout entryCard = card();
        entryCard.addView(text("Ajouter le dernier multiplicateur",17,Color.WHITE,true));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(10),0,0);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ex. 2,45");
        input.setHintTextColor(Color.rgb(115,135,160));
        input.setTextColor(Color.WHITE);
        input.setTextSize(22);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(dp(14),0,dp(14),0);
        input.setBackground(round(Color.rgb(18,38,62),13));
        row.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));

        Button add = button("AJOUTER",Color.rgb(255,138,0));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(110),dp(56));
        addParams.setMargins(dp(9),0,0,0);
        row.addView(add,addParams);
        add.setOnClickListener(v -> addValue());
        input.setOnEditorActionListener((v,actionId,event) -> { addValue(); return true; });
        entryCard.addView(row);
        root.addView(entryCard,spaced());

        latest = text("",22,Color.WHITE,true);
        latest.setGravity(Gravity.CENTER);
        latest.setPadding(dp(8),dp(6),dp(8),dp(8));
        root.addView(latest);

        LinearLayout predictionCard = card();
        predictionCard.setBackground(round(Color.rgb(24,42,67),17));
        TextView predictionTitle = text("PRÉVISION DE LA PROCHAINE MANCHE",16,Color.rgb(255,181,75),true);
        predictionTitle.setGravity(Gravity.CENTER);
        predictionCard.addView(predictionTitle);

        predictionValue = text("Calcul en cours…",31,Color.WHITE,true);
        predictionValue.setGravity(Gravity.CENTER);
        predictionValue.setPadding(0,dp(8),0,dp(3));
        predictionCard.addView(predictionValue);

        predictionZone = text("",15,Color.rgb(221,230,241),false);
        predictionZone.setGravity(Gravity.CENTER);
        predictionCard.addView(predictionZone);

        predictionProbabilities = text("",14,Color.rgb(221,230,241),false);
        predictionProbabilities.setGravity(Gravity.CENTER);
        predictionProbabilities.setPadding(0,dp(9),0,0);
        predictionCard.addView(predictionProbabilities);

        predictionConfidence = text("",12,Color.rgb(155,173,197),false);
        predictionConfidence.setGravity(Gravity.CENTER);
        predictionConfidence.setPadding(0,dp(8),0,0);
        predictionCard.addView(predictionConfidence);
        root.addView(predictionCard,spaced());

        risk = text("",16,Color.WHITE,true);
        risk.setGravity(Gravity.CENTER);
        risk.setPadding(dp(12),dp(12),dp(12),dp(12));
        root.addView(risk,spaced());

        LinearLayout futureCard = card();
        futureCard.addView(text("Projection probabiliste des 6 prochaines manches",17,Color.WHITE,true));
        futurePredictions = text("Calcul en cours…",15,Color.rgb(255,181,75),true);
        futurePredictions.setPadding(0,dp(8),0,0);
        futureCard.addView(futurePredictions);
        root.addView(futureCard,spaced());

        LinearLayout recentCard = card();
        recentCard.addView(text("12 dernières manches",17,Color.WHITE,true));
        recent = text("",16,Color.rgb(255,181,75),true);
        recent.setPadding(0,dp(8),0,0);
        recentCard.addView(recent);
        root.addView(recentCard,spaced());

        details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        root.addView(details);

        LinearLayout summaryCard = card();
        summaryCard.addView(text("Résumé statistique",17,Color.WHITE,true));
        summary = text("",15,Color.rgb(224,233,244),false);
        summary.setPadding(0,dp(8),0,0);
        summaryCard.addView(summary);
        details.addView(summaryCard,spaced());

        LinearLayout freqCard = card();
        freqCard.addView(text("Fréquences observées",17,Color.WHITE,true));
        frequencies = text("",15,Color.rgb(224,233,244),false);
        frequencies.setPadding(0,dp(8),0,0);
        freqCard.addView(frequencies);
        details.addView(freqCard,spaced());

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

        TextView foot = text("Cette estimation analyse des séquences et simule des scénarios. Elle ne connaît pas le résultat généré par le serveur et ne garantit aucun gain.",12,Color.rgb(150,165,185),false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot);

        setContentView(scroll);
    }

    private void addValue() {
        String raw = input.getText().toString().trim().replace(',','.');
        if (raw.isEmpty()) { toast("Entrez un multiplicateur"); return; }
        try {
            double value = Double.parseDouble(raw);
            if (value < 1.0 || value > 100000.0) { toast("Valeur autorisée : 1,00 à 100000"); return; }
            values.add(value);
            saveValues();
            input.setText("");
            refresh();
        } catch (NumberFormatException e) {
            toast("Multiplicateur non valide");
        }
    }

    private void undoLast() {
        if (values.isEmpty()) { toast("Aucune saisie à annuler"); return; }
        values.remove(values.size()-1);
        saveValues();
        refresh();
    }

    private void toggleCompact() {
        compact = !compact;
        details.setVisibility(compact ? View.GONE : View.VISIBLE);
        compactButton.setText(compact ? "MODE COMPLET" : "MODE COMPACT");
    }

    private void openSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Gérer la session")
                .setMessage("Effacer permet de saisir uniquement les 6 ou 12 dernières manches d’une nouvelle session. Vous pouvez aussi restaurer les résultats extraits de la vidéo.")
                .setNegativeButton("Annuler",null)
                .setNeutralButton("Données vidéo",(d,w) -> {
                    values.clear();
                    for (double value : initialValues) values.add(value);
                    saveValues();
                    refresh();
                })
                .setPositiveButton("Effacer tout",(d,w) -> {
                    values.clear();
                    saveValues();
                    refresh();
                })
                .show();
    }

    private void refresh() {
        if (values.isEmpty()) {
            latest.setText("Aucune manche enregistrée");
            recent.setText("—");
            summary.setText("Ajoutez au moins 6 manches pour activer la prévision.");
            frequencies.setText("—");
            setRisk("Données insuffisantes",Color.rgb(38,64,92));
            showInsufficientPrediction();
            return;
        }

        double sum=0,min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for (double value:values) { sum+=value; min=Math.min(min,value); max=Math.max(max,value); }
        double mean=sum/values.size();
        List<Double> sorted=new ArrayList<>(values);
        Collections.sort(sorted);
        int n=sorted.size();
        double median=n%2==0?(sorted.get(n/2-1)+sorted.get(n/2))/2.0:sorted.get(n/2);
        double last=values.get(values.size()-1);

        latest.setText(String.format(Locale.FRANCE,"Dernier résultat : %.2fx",last));
        summary.setText(String.format(Locale.FRANCE,
                "Manches : %d\nMoyenne : %.2fx\nMédiane : %.2fx\nMinimum : %.2fx\nMaximum : %.2fx",
                values.size(),mean,median,min,max));

        double[] levels={1.20,1.50,2.00,3.00,5.00,10.00};
        StringBuilder freq=new StringBuilder();
        for (int i=0;i<levels.length;i++) {
            int reached=0;
            for (double value:values) if (value>=levels[i]) reached++;
            freq.append(String.format(Locale.FRANCE,"≥ %.2fx : %.1f%%",levels[i],reached*100.0/values.size()));
            if (i<levels.length-1) freq.append('\n');
        }
        frequencies.setText(freq.toString());

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
        int sampleStart=Math.max(0,values.size()-10);
        int low=0,veryLow=0;
        for (int i=sampleStart;i<values.size();i++) {
            if (values.get(i)<2.0) low++;
            if (values.get(i)<1.5) veryLow++;
        }
        int streak=0;
        for (int i=values.size()-1;i>=0 && values.get(i)<2.0;i--) streak++;
        double rate=low/(double)Math.max(1,values.size()-sampleStart);
        if (veryLow>=6 || streak>=5 || rate>=0.80) setRisk("Séquence récente très basse — forte incertitude",Color.rgb(177,48,57));
        else if (veryLow>=4 || streak>=3 || rate>=0.60) setRisk("Séquence récente modérée",Color.rgb(197,112,20));
        else setRisk("Séquence récente diversifiée",Color.rgb(28,126,92));
    }

    private void requestForecast() {
        if (values.size()<6) {
            showInsufficientPrediction();
            return;
        }

        int generation=forecastGeneration.incrementAndGet();
        ArrayList<Double> snapshot=new ArrayList<>(values);
        predictionValue.setText("Calcul…");
        predictionZone.setText("Analyse des séquences similaires");
        predictionProbabilities.setText("");
        predictionConfidence.setText(String.format(Locale.FRANCE,"%,d simulations en préparation",SIMULATIONS));
        futurePredictions.setText("Calcul des 6 trajectoires…");

        new Thread(() -> {
            PredictionEngine.Forecast forecast=PredictionEngine.calculate(snapshot,SIMULATIONS,HORIZONS);
            mainHandler.post(() -> {
                if (generation!=forecastGeneration.get()) return;
                renderForecast(forecast);
            });
        }).start();
    }

    private void renderForecast(PredictionEngine.Forecast forecast) {
        if (forecast==null) {
            showInsufficientPrediction();
            return;
        }

        predictionValue.setText(String.format(Locale.FRANCE,"≈ %.2fx",forecast.estimates[0]));
        predictionZone.setText(String.format(Locale.FRANCE,"Zone probable centrale : %.2fx à %.2fx",forecast.lows[0],forecast.highs[0]));
        predictionProbabilities.setText(String.format(Locale.FRANCE,
                "≥1,50x : %.0f%%   •   ≥2x : %.0f%%\n≥3x : %.0f%%   •   ≥5x : %.0f%%",
                forecast.p15,forecast.p20,forecast.p30,forecast.p50));
        predictionConfidence.setText(String.format(Locale.FRANCE,
                "Confiance du modèle : %d%% • %,d simulations • %d séquences proches",
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
        predictionZone.setText("Ajoutez les derniers multiplicateurs pour lancer le calcul.");
        predictionProbabilities.setText("");
        predictionConfidence.setText("La prévision se recalculera automatiquement après chaque ajout.");
        futurePredictions.setText("Les projections M+1 à M+6 apparaîtront ici.");
    }

    private void setRisk(String label,int color) {
        risk.setText(label);
        risk.setBackground(round(color,14));
    }

    private void loadValues() {
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        String saved=prefs.getString(KEY_VALUES,"");
        if (saved==null || saved.trim().isEmpty()) {
            for (double value:initialValues) values.add(value);
            saveValues();
            return;
        }
        try {
            for (String part:saved.split(",")) if (!part.trim().isEmpty()) values.add(Double.parseDouble(part.trim()));
        } catch (Exception e) {
            values.clear();
            for (double value:initialValues) values.add(value);
            saveValues();
        }
    }

    private void saveValues() {
        StringBuilder out=new StringBuilder();
        for (int i=0;i<values.size();i++) {
            if (i>0) out.append(',');
            out.append(values.get(i));
        }
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_VALUES,out.toString()).apply();
    }

    private void showNoticeOnce() {
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        if (prefs.getBoolean(KEY_NOTICE_V2,false)) return;
        new AlertDialog.Builder(this)
                .setTitle("Prévision probabiliste")
                .setMessage("Cette version produit une estimation après chaque manche à partir des séquences passées et de 5 000 simulations. Un jeu crash peut toutefois être aléatoire et généré côté serveur : la valeur affichée n’est jamais une certitude.")
                .setCancelable(false)
                .setPositiveButton("J’ai compris",(d,w) -> prefs.edit().putBoolean(KEY_NOTICE_V2,true).apply())
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
