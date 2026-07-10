package com.crashstat.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class PredictionEngine {
    private PredictionEngine() {}

    static final class Forecast {
        final double[] estimates;
        final double[] lows;
        final double[] highs;
        final double p15;
        final double p20;
        final double p30;
        final double p50;
        final int confidence;
        final int simulations;
        final int exactPatternMatches;

        Forecast(double[] estimates, double[] lows, double[] highs,
                 double p15, double p20, double p30, double p50,
                 int confidence, int simulations, int exactPatternMatches) {
            this.estimates = estimates;
            this.lows = lows;
            this.highs = highs;
            this.p15 = p15;
            this.p20 = p20;
            this.p30 = p30;
            this.p50 = p50;
            this.confidence = confidence;
            this.simulations = simulations;
            this.exactPatternMatches = exactPatternMatches;
        }
    }

    private static final int MAX_CONTEXT = 5;

    static Forecast calculate(List<Double> history, int simulationCount, int horizons) {
        if (history == null || history.size() < 6) return null;

        int simulations = Math.max(1000, simulationCount);
        int steps = Math.max(1, horizons);
        double[][] paths = new double[steps][simulations];
        Model model = new Model(history);
        Random random = new Random(seedFromHistory(history));
        ArrayList<Integer> initialState = new ArrayList<>();
        int stateStart = Math.max(0, history.size() - MAX_CONTEXT);
        for (int i = stateStart; i < history.size(); i++) {
            initialState.add(bucket(history.get(i)));
        }

        for (int simulation = 0; simulation < simulations; simulation++) {
            ArrayList<Integer> state = new ArrayList<>(initialState);
            for (int horizon = 0; horizon < steps; horizon++) {
                double next = model.sample(state, random);
                paths[horizon][simulation] = next;
                state.add(bucket(next));
                if (state.size() > MAX_CONTEXT) state.remove(0);
            }
        }

        double[] estimates = new double[steps];
        double[] lows = new double[steps];
        double[] highs = new double[steps];
        for (int horizon = 0; horizon < steps; horizon++) {
            Arrays.sort(paths[horizon]);
            estimates[horizon] = quantileSorted(paths[horizon], 0.50);
            lows[horizon] = quantileSorted(paths[horizon], 0.20);
            highs[horizon] = quantileSorted(paths[horizon], 0.80);
        }

        double p15 = probabilityAtLeast(paths[0], 1.50);
        double p20 = probabilityAtLeast(paths[0], 2.00);
        double p30 = probabilityAtLeast(paths[0], 3.00);
        double p50 = probabilityAtLeast(paths[0], 5.00);

        int matches = model.matchCount(initialState);
        double dataScore = Math.min(28.0, Math.log10(Math.max(10, history.size())) * 11.0);
        double matchScore = Math.min(22.0, Math.sqrt(matches) * 5.0);
        int confidence = (int) Math.round(18.0 + dataScore + matchScore);
        confidence = Math.max(20, Math.min(72, confidence));

        return new Forecast(estimates, lows, highs, p15, p20, p30, p50,
                confidence, simulations, matches);
    }

    private static final class Model {
        private final Map<String, ArrayList<Double>>[] contexts;
        private final ArrayList<Double> global = new ArrayList<>();
        private final ArrayList<Double> recent = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Model(List<Double> history) {
            contexts = new Map[MAX_CONTEXT + 1];
            for (int k = 1; k <= MAX_CONTEXT; k++) contexts[k] = new HashMap<>();

            global.addAll(history);
            int recentStart = Math.max(0, history.size() - 150);
            recent.addAll(history.subList(recentStart, history.size()));

            for (int nextIndex = 1; nextIndex < history.size(); nextIndex++) {
                int maxK = Math.min(MAX_CONTEXT, nextIndex);
                for (int k = 1; k <= maxK; k++) {
                    String key = keyFromHistory(history, nextIndex - k, nextIndex);
                    contexts[k]
                            .computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(history.get(nextIndex));
                }
            }
        }

        double sample(List<Integer> state, Random random) {
            ArrayList<Double> conditional = null;
            for (int k = Math.min(MAX_CONTEXT, state.size()); k >= 1; k--) {
                String key = keyFromState(state, state.size() - k, state.size());
                ArrayList<Double> found = contexts[k].get(key);
                if (found != null && found.size() >= 2) {
                    conditional = found;
                    break;
                }
            }

            double selector = random.nextDouble();
            double sampled;
            if (conditional != null && selector < 0.72) {
                sampled = conditional.get(random.nextInt(conditional.size()));
            } else if (!recent.isEmpty() && selector < 0.93) {
                double biased = 1.0 - Math.pow(random.nextDouble(), 2.2);
                int index = Math.min(recent.size() - 1,
                        (int) Math.floor(biased * recent.size()));
                sampled = recent.get(index);
            } else {
                sampled = global.get(random.nextInt(global.size()));
            }

            double jitter = Math.exp(random.nextGaussian() * 0.018);
            return Math.max(1.0, sampled * jitter);
        }

        int matchCount(List<Integer> state) {
            for (int k = Math.min(MAX_CONTEXT, state.size()); k >= 1; k--) {
                String key = keyFromState(state, state.size() - k, state.size());
                ArrayList<Double> found = contexts[k].get(key);
                if (found != null && !found.isEmpty()) return found.size();
            }
            return 0;
        }
    }

    private static String keyFromHistory(List<Double> history, int start, int end) {
        StringBuilder key = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (key.length() > 0) key.append('-');
            key.append(bucket(history.get(i)));
        }
        return key.toString();
    }

    private static String keyFromState(List<Integer> state, int start, int end) {
        StringBuilder key = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (key.length() > 0) key.append('-');
            key.append(state.get(i));
        }
        return key.toString();
    }

    private static int bucket(double value) {
        if (value < 1.10) return 0;
        if (value < 1.20) return 1;
        if (value < 1.50) return 2;
        if (value < 2.00) return 3;
        if (value < 3.00) return 4;
        if (value < 5.00) return 5;
        if (value < 10.00) return 6;
        if (value < 20.00) return 7;
        return 8;
    }

    private static double probabilityAtLeast(double[] values, double threshold) {
        int count = 0;
        for (double value : values) if (value >= threshold) count++;
        return count * 100.0 / values.length;
    }

    private static double quantileSorted(double[] sorted, double q) {
        if (sorted.length == 0) return 1.0;
        double position = q * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) return sorted[lower];
        double fraction = position - lower;
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
    }

    private static long seedFromHistory(List<Double> history) {
        long hash = 1469598103934665603L;
        for (double value : history) {
            hash ^= Double.doubleToLongBits(value);
            hash *= 1099511628211L;
        }
        return hash;
    }
}
