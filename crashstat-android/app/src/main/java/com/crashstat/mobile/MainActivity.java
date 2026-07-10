package com.crashstat.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

public class MainActivity extends Activity {
    private static final String PREFS = "crashstat";
    private static final String KEY_VALUES = "values";
    private static final String KEY_NOTICE = "notice_seen";

    private final ArrayList<Double> values = new ArrayList<>();
    private final double[] initialValues = {
            2.92,8.65,3.20,1.00,2.72,1.81,2.89,2.32,1.13,1.98,
            3.62,1.75,1.97,1.80,2.34,2.94,2.40,8.87,1.23,12.37,
            1.55,1.06,3.01,1.99,1.17,1.03,1.05,1.00,1.93,1.08,
            1.93,2.42,2.27,2.64,11.87,16.99,3.91
    };

    private EditText input;
    private TextView latest;
    private TextView risk;
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
        root.setPadding(dp(16),dp(16),dp(16),dp(28));
        scroll.addView(root);

        root.addView(text("CrashStat Mobile",28,Color.WHITE,true));
        TextView subtitle = text("Analyse statistique hors ligne",14,Color.rgb(151,170,195),false);
        subtitle.setPadding(0,0,0,dp(14));
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
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(112),dp(56));
        addParams.setMargins(dp(10),0,0,0);
        row.addView(add,addParams);
        add.setOnClickListener(v -> addValue());
        input.setOnEditorActionListener((v,actionId,event) -> { addValue(); return true; });
        entryCard.addView(row);
        root.addView(entryCard,spaced());

        latest = text("",23,Color.WHITE,true);
        latest.setGravity(Gravity.CENTER);
        latest.setPadding(dp(8),dp(8),dp(8),dp(8));
        root.addView(latest);

        risk = text("",17,Color.WHITE,true);
        risk.setGravity(Gravity.CENTER);
        risk.setPadding(dp(14),dp(14),dp(14),dp(14));
        root.addView(risk,spaced());

        details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        root.addView(details);

        LinearLayout summaryCard = card();
        summaryCard.addView(text("Résumé",18,Color.WHITE,true));
        summary = text("",16,Color.rgb(224,233,244),false);
        summary.setPadding(0,dp(8),0,0);
        summaryCard.addView(summary);
        details.addView(summaryCard,spaced());

        LinearLayout freqCard = card();
        freqCard.addView(text("Fréquences observées",18,Color.WHITE,true));
        frequencies = text("",15,Color.rgb(224,233,244),false);
        frequencies.setPadding(0,dp(8),0,0);
        freqCard.addView(frequencies);
        details.addView(freqCard,spaced());

        LinearLayout recentCard = card();
        recentCard.addView(text("12 dernières manches",18,Color.WHITE,true));
        recent = text("",16,Color.rgb(255,181,75),true);
        recent.setPadding(0,dp(8),0,0);
        recentCard.addView(recent);
        root.addView(recentCard,spaced());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button undo = button("ANNULER",Color.rgb(38,64,92));
        compactButton = button("MODE COMPACT",Color.rgb(38,64,92));
        Button reset = button("RÉINITIALISER",Color.rgb(151,47,55));
        actions.addView(undo,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0,dp(50),1.25f);
        middle.setMargins(dp(7),0,dp(7),0);
        actions.addView(compactButton,middle);
        actions.addView(reset,new LinearLayout.LayoutParams(0,dp(50),1.15f));
        root.addView(actions,spaced());

        undo.setOnClickListener(v -> undoLast());
        compactButton.setOnClickListener(v -> toggleCompact());
        reset.setOnClickListener(v -> confirmReset());

        TextView foot = text("Estimation statistique uniquement. Aucun résultat futur ni gain n’est garanti.",12,Color.rgb(150,165,185),false);
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

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Réinitialiser l’historique ?")
                .setMessage("Les 37 résultats extraits de la vidéo seront restaurés.")
                .setNegativeButton("Annuler",null)
                .setPositiveButton("Réinitialiser",(d,w) -> {
                    values.clear();
                    for (double v:initialValues) values.add(v);
                    saveValues();
                    refresh();
                }).show();
    }

    private void refresh() {
        if (values.isEmpty()) {
            latest.setText("Aucune manche enregistrée");
            summary.setText("Ajoutez un multiplicateur pour commencer.");
            frequencies.setText("—");
            recent.setText("—");
            setRisk("Données insuffisantes",Color.rgb(38,64,92));
            return;
        }

        double sum=0,min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for (double v:values) { sum+=v; min=Math.min(min,v); max=Math.max(max,v); }
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
            for (double v:values) if (v>=levels[i]) reached++;
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

        int sampleStart=Math.max(0,values.size()-10);
        int low=0,veryLow=0;
        for (int i=sampleStart;i<values.size();i++) {
            if (values.get(i)<2.0) low++;
            if (values.get(i)<1.5) veryLow++;
        }
        int streak=0;
        for (int i=values.size()-1;i>=0 && values.get(i)<2.0;i--) streak++;
        double rate=low/(double)(values.size()-sampleStart);
        if (veryLow>=6 || streak>=5 || rate>=0.80) setRisk("Séquence récente très basse — prudence",Color.rgb(177,48,57));
        else if (veryLow>=4 || streak>=3 || rate>=0.60) setRisk("Séquence récente modérée",Color.rgb(197,112,20));
        else setRisk("Séquence récente diversifiée",Color.rgb(28,126,92));
    }

    private void setRisk(String label,int color) {
        risk.setText(label);
        risk.setBackground(round(color,14));
    }

    private void loadValues() {
        SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        String saved=prefs.getString(KEY_VALUES,"");
        if (saved==null || saved.trim().isEmpty()) {
            for (double v:initialValues) values.add(v);
            saveValues();
            return;
        }
        try {
            for (String part:saved.split(",")) if (!part.trim().isEmpty()) values.add(Double.parseDouble(part.trim()));
        } catch (Exception e) {
            values.clear();
            for (double v:initialValues) values.add(v);
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
        if (prefs.getBoolean(KEY_NOTICE,false)) return;
        new AlertDialog.Builder(this)
                .setTitle("Information importante")
                .setMessage("CrashStat analyse seulement l’historique. Un jeu crash est généré côté serveur : aucune application ne peut garantir le prochain résultat ni un gain.")
                .setCancelable(false)
                .setPositiveButton("J’ai compris",(d,w) -> prefs.edit().putBoolean(KEY_NOTICE,true).apply())
                .show();
    }

    private LinearLayout card() {
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(15),dp(16),dp(15));
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
        Button b=new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(5),0,dp(5),0);
        b.setBackground(round(color,12));
        return b;
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
