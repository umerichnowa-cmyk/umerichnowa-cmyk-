package com.crashstat.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final class Candidate {
        final double value;
        final double weight;

        Candidate(double value, double weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private static final class CandidateSet {
        final ArrayList<Candidate> candidates;
        final int exactMatches;

        CandidateSet(ArrayList<Candidate> candidates, int exactMatches) {
            this.candidates = candidates;
            this.exactMatches = exactMatches;
        }
    }

    static Forecast calculate(List<Double> history, int simulationCount, int horizons) {
        if (history == null || history.size() < 6) return null;

        int simulations = Math.max(1000, simulationCount);
        int steps = Math.max(1, horizons);
        double[][] paths = new double[steps][simulations];
        long seed = seedFromHistory(history);
        Random random = new Random(seed);
        CandidateSet initial = buildCandidates(history);

        for (int s = 0; s < simulations; s++) {
            ArrayList<Double> simulatedHistory = new ArrayList<>(history);
            for (int h = 0; h < steps; h++) {
                CandidateSet set = buildCandidates(simulatedHistory);
                double next = weightedSample(set.candidates, random);
                paths[h][s] = next;
                simulatedHistory.add(next);
            }
        }

        double[] estimates = new double[steps];
        double[] lows = new double[steps];
        double[] highs = new double[steps];
        for (int h = 0; h < steps; h++) {
            Arrays.sort(paths[h]);
            estimates[h] = quantileSorted(paths[h], 0.50);
            lows[h] = quantileSorted(paths[h], 0.20);
            highs[h] = quantileSorted(paths[h], 0.80);
        }

        double p15 = probabilityAtLeast(paths[0], 1.50);
        double p20 = probabilityAtLeast(paths[0], 2.00);
        double p30 = probabilityAtLeast(paths[0], 3.00);
        double p50 = probabilityAtLeast(paths[0], 5.00);

        int confidence = 18 + Math.min(30, history.size()) + initial.exactMatches * 5;
        if (history.size() < 12) confidence = Math.min(confidence, 42);
        confidence = Math.max(20, Math.min(78, confidence));

        return new Forecast(estimates, lows, highs, p15, p20, p30, p50,
                confidence, simulations, initial.exactMatches);
    }

    private static CandidateSet buildCandidates(List<Double> history) {
        int n = history.size();
        ArrayList<Candidate> out = new ArrayList<>();
        if (n == 0) return new CandidateSet(out, 0);

        int patternLength = Math.min(3, n - 1);
        int exactMatches = 0;

        if (patternLength > 0) {
            for (int i = patternLength; i < n; i++) {
                int distance = 0;
                for (int j = 0; j < patternLength; j++) {
                    int historicalBucket = bucket(history.get(i - patternLength + j));
                    int currentBucket = bucket(history.get(n - patternLength + j));
                    distance += Math.abs(historicalBucket - currentBucket);
                }

                if (distance == 0) exactMatches++;
                double similarity = Math.exp(-0.78 * distance);
                double patternBonus = distance == 0 ? 4.2 : (distance <= 2 ? 2.0 : 1.0);
                double recency = 0.62 + 0.38 * (i / (double) Math.max(1, n - 1));
                double weight = similarity * patternBonus * recency;
                if (weight >= 0.015) out.add(new Candidate(history.get(i), weight));
            }
        }

        int recentStart = Math.max(0, n - 12);
        for (int i = recentStart; i < n; i++) {
            double recency = 0.22 + 0.22 * ((i - recentStart + 1) / (double) Math.max(1, n - recentStart));
            out.add(new Candidate(history.get(i), recency));
        }

        for (double value : history) out.add(new Candidate(value, 0.07));

        return new CandidateSet(out, exactMatches);
    }

    private static int bucket(double value) {
        if (value < 1.20) return 0;
        if (value < 1.50) return 1;
        if (value < 2.00) return 2;
        if (value < 3.00) return 3;
        if (value < 5.00) return 4;
        if (value < 10.00) return 5;
        return 6;
    }

    private static double weightedSample(List<Candidate> candidates, Random random) {
        if (candidates.isEmpty()) return 1.0;
        double total = 0.0;
        for (Candidate candidate : candidates) total += candidate.weight;
        double target = random.nextDouble() * total;
        double cumulative = 0.0;
        for (Candidate candidate : candidates) {
            cumulative += candidate.weight;
            if (cumulative >= target) return candidate.value;
        }
        return candidates.get(candidates.size() - 1).value;
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
