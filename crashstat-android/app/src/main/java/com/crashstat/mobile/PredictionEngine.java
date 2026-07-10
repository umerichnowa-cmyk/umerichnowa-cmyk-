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
        final int patternLength;
        final int patternMatches;
        final int bestLag;
        final int patternStrength;

        Forecast(double[] estimates, double[] lows, double[] highs,
                 double p15, double p20, double p30, double p50,
                 int confidence, int simulations, int exactPatternMatches,
                 int patternLength, int patternMatches, int bestLag,
                 int patternStrength) {
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
            this.patternLength = patternLength;
            this.patternMatches = patternMatches;
            this.bestLag = bestLag;
            this.patternStrength = patternStrength;
        }
    }

    private static final int MAX_CONTEXT = 12;
    private static final int MAX_LAG = 60;

    static Forecast calculate(List<Double> history, int simulationCount, int horizons) {
        if (history == null || history.size() < 6) return null;

        int simulations = Math.max(1000, simulationCount);
        int steps = Math.max(1, horizons);
        double[][] paths = new double[steps][simulations];
        Model model = new Model(history);
        PatternInfo patternInfo = analyzePattern(history);
        Random random = new Random(seedFromHistory(history));

        ArrayList<Integer> initialState = new ArrayList<>();
        int stateStart = Math.max(0, history.size() - MAX_CONTEXT);
        for (int i = stateStart; i < history.size(); i++) {
            initialState.add(bucket(history.get(i)));
        }

        for (int simulation = 0; simulation < simulations; simulation++) {
            ArrayList<Integer> state = new ArrayList<>(initialState);
            for (int horizon = 0; horizon < steps; horizon++) {
                double next;
                if (horizon == 0 && !patternInfo.candidates.isEmpty()
                        && random.nextDouble() < patternInfo.useProbability) {
                    next = weightedSample(patternInfo.candidates, random);
                } else {
                    next = model.sample(state, random);
                }
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
        double dataScore = Math.min(24.0, Math.log10(Math.max(10, history.size())) * 9.0);
        double contextScore = Math.min(18.0, Math.sqrt(matches) * 4.0);
        double motifScore = patternInfo.patternLength * 1.4
                + Math.min(16.0, Math.sqrt(patternInfo.matches) * 4.0)
                + patternInfo.strength * 0.12;
        int confidence = (int) Math.round(12.0 + dataScore + contextScore + motifScore);
        confidence = Math.max(18, Math.min(78, confidence));

        return new Forecast(estimates, lows, highs, p15, p20, p30, p50,
                confidence, simulations, matches,
                patternInfo.patternLength, patternInfo.matches,
                patternInfo.bestLag, patternInfo.strength);
    }

    private static final class WeightedValue {
        final double value;
        final double weight;

        WeightedValue(double value, double weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private static final class PatternInfo {
        final ArrayList<WeightedValue> candidates = new ArrayList<>();
        int patternLength;
        int matches;
        int bestLag;
        int strength;
        double useProbability;
    }

    private static PatternInfo analyzePattern(List<Double> history) {
        PatternInfo info = new PatternInfo();
        int n = history.size();
        if (n < 8) return info;

        int longest = 0;
        int longestMatches = 0;
        int exact = 0;
        ArrayList<WeightedValue> selected = new ArrayList<>();

        int maxLength = Math.min(MAX_CONTEXT, n - 2);
        for (int length = maxLength; length >= 3; length--) {
            ArrayList<WeightedValue> candidates = new ArrayList<>();
            int matches = 0;
            int exactMatches = 0;
            int allowedDistance = Math.max(1, length / 6);

            for (int end = length; end < n; end++) {
                if (end >= n - 1) break;
                int distance = 0;
                for (int j = 0; j < length; j++) {
                    int historical = bucket(history.get(end - length + j));
                    int current = bucket(history.get(n - length + j));
                    if (historical != current) distance++;
                }
                if (distance <= allowedDistance) {
                    matches++;
                    if (distance == 0) exactMatches++;
                    double similarity = Math.exp(-1.35 * distance);
                    double lengthWeight = 1.0 + length * length * 0.12;
                    double recency = 0.45 + 0.55 * (end / (double) Math.max(1, n - 1));
                    candidates.add(new WeightedValue(history.get(end),
                            similarity * lengthWeight * recency));
                }
            }

            if (matches >= 2) {
                longest = length;
                longestMatches = matches;
                exact = exactMatches;
                selected = candidates;
                break;
            }
        }

        info.patternLength = longest;
        info.matches = longestMatches;
        info.candidates.addAll(selected);

        int bestLag = 0;
        double bestScore = 0.0;
        int maxLag = Math.min(MAX_LAG, n / 2);
        for (int lag = 2; lag <= maxLag; lag++) {
            int comparisons = Math.min(160, n - lag);
            if (comparisons < 12) continue;
            int start = n - comparisons;
            int equal = 0;
            for (int i = start; i < n; i++) {
                if (bucket(history.get(i)) == bucket(history.get(i - lag))) equal++;
            }
            double score = equal / (double) comparisons;
            if (score > bestScore) {
                bestScore = score;
                bestLag = lag;
            }
        }

        if (bestLag > 0 && bestScore >= 0.43 && n - bestLag >= 0) {
            double periodicValue = history.get(n - bestLag);
            info.candidates.add(new WeightedValue(periodicValue,
                    2.0 + bestScore * 8.0));
            info.bestLag = bestLag;
        }

        double supportScore = Math.min(1.0, longestMatches / 8.0);
        double lengthScore = longest / (double) MAX_CONTEXT;
        double exactScore = longestMatches == 0 ? 0.0 : exact / (double) longestMatches;
        double periodicScore = bestScore;
        double rawStrength = 100.0 * (0.34 * supportScore
                + 0.34 * lengthScore
                + 0.18 * exactScore
                + 0.14 * periodicScore);
        info.strength = (int) Math.round(Math.max(0.0, Math.min(100.0, rawStrength)));
        info.useProbability = Math.min(0.88,
                0.35 + 0.0045 * info.strength + 0.02 * longest);
        return info;
    }

    private static double weightedSample(List<WeightedValue> values, Random random) {
        double total = 0.0;
        for (WeightedValue value : values) total += Math.max(0.0001, value.weight);
        double target = random.nextDouble() * total;
        double cumulative = 0.0;
        for (WeightedValue value : values) {
            cumulative += Math.max(0.0001, value.weight);
            if (cumulative >= target) {
                double jitter = Math.exp(random.nextGaussian() * 0.012);
                return Math.max(1.0, value.value * jitter);
            }
        }
        return Math.max(1.0, values.get(values.size() - 1).value);
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
            int recentStart = Math.max(0, history.size() - 240);
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
            int matchedLength = 0;
            for (int k = Math.min(MAX_CONTEXT, state.size()); k >= 1; k--) {
                String key = keyFromState(state, state.size() - k, state.size());
                ArrayList<Double> found = contexts[k].get(key);
                if (found != null && found.size() >= 2) {
                    conditional = found;
                    matchedLength = k;
                    break;
                }
            }

            double selector = random.nextDouble();
            double conditionalProbability = Math.min(0.88, 0.60 + matchedLength * 0.025);
            double sampled;
            if (conditional != null && selector < conditionalProbability) {
                sampled = conditional.get(random.nextInt(conditional.size()));
            } else if (!recent.isEmpty() && selector < 0.95) {
                double biased = 1.0 - Math.pow(random.nextDouble(), 2.4);
                int index = Math.min(recent.size() - 1,
                        (int) Math.floor(biased * recent.size()));
                sampled = recent.get(index);
            } else {
                sampled = global.get(random.nextInt(global.size()));
            }

            double jitter = Math.exp(random.nextGaussian() * 0.016);
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
