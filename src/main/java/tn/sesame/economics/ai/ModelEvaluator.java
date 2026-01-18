package tn.sesame.economics.ai;

import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
public class ModelEvaluator {

    @Data
    public static class EvaluationResult {
        private final String modelName;
        private final double mae;  // Mean Absolute Error
        private final double mse;  // Mean Squared Error
        private final double rmse; // Root Mean Squared Error
        private final double mape; // Mean Absolute Percentage Error
        private final double r2;   // R-squared
        private final int sampleSize;
        private final long inferenceTimeMs;

        public void printReport() {
            log.info("\n📊 ÉVALUATION DU MODÈLE: {}", modelName);
            log.info("=".repeat(50));
            log.info("Taille de l'échantillon: {}", sampleSize);
            log.info("MAE  (Erreur Absolue Moyenne): {:.2f} TND", mae);
            log.info("MSE  (Erreur Quadratique Moyenne): {:.2f}", mse);
            log.info("RMSE (Racine MSE): {:.2f} TND", rmse);
            log.info("MAPE (Erreur Pourcentage Moyenne): {:.2f}%", mape);
            log.info("R²   (Coefficient de détermination): {:.4f}", r2);
            log.info("Temps d'inférence moyen: {} ms", inferenceTimeMs / sampleSize);

            // Interprétation
            log.info("\n📈 INTERPRÉTATION:");
            if (mape < 10) {
                log.info("✅ Excellente précision (erreur < 10%)");
            } else if (mape < 20) {
                log.info("👍 Bonne précision (erreur 10-20%)");
            } else if (mape < 30) {
                log.info("⚠️  Précision acceptable (erreur 20-30%)");
            } else {
                log.info("❌ Précision faible (erreur > 30%)");
            }
        }
    }

    /**
     * Évalue un modèle sur des données de test
     */
    public static EvaluationResult evaluateModel(
            BaseAIModel model,
            List<ExportData> testData,
            List<Double> actualPrices) {

        if (testData.size() != actualPrices.size()) {
            throw new IllegalArgumentException("Les données de test et les prix réels doivent avoir la même taille");
        }

        long startTime = System.currentTimeMillis();

        // Générer les prédictions
        List<PricePrediction> predictions = model.predictBatch(testData);

        long endTime = System.currentTimeMillis();
        long inferenceTime = endTime - startTime;

        // Extraire les prix prédits
        double[] predicted = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .toArray();

        // Convertir les prix réels
        double[] actual = actualPrices.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        // Calculer les métriques
        double mae = calculateMAE(actual, predicted);
        double mse = calculateMSE(actual, predicted);
        double rmse = Math.sqrt(mse);
        double mape = calculateMAPE(actual, predicted);
        double r2 = calculateR2(actual, predicted);

        return new EvaluationResult(
                model.getModelName(),
                mae, mse, rmse, mape, r2,
                testData.size(),
                inferenceTime
        );
    }

    /**
     * Compare plusieurs modèles
     */
    public static Map<String, EvaluationResult> compareModels(
            Map<String, BaseAIModel> models,
            List<ExportData> testData,
            List<Double> actualPrices) {

        Map<String, EvaluationResult> results = new HashMap<>();

        for (Map.Entry<String, BaseAIModel> entry : models.entrySet()) {
            try {
                EvaluationResult result = evaluateModel(entry.getValue(), testData, actualPrices);
                results.put(entry.getKey(), result);
                result.printReport();
            } catch (Exception e) {
                log.error("Erreur lors de l'évaluation du modèle {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }

        // Afficher le classement
        printRanking(results);

        return results;
    }

    private static double calculateMAE(double[] actual, double[] predicted) {
        double sum = 0.0;
        for (int i = 0; i < actual.length; i++) {
            sum += Math.abs(actual[i] - predicted[i]);
        }
        return sum / actual.length;
    }

    private static double calculateMSE(double[] actual, double[] predicted) {
        double sum = 0.0;
        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - predicted[i];
            sum += error * error;
        }
        return sum / actual.length;
    }

    private static double calculateMAPE(double[] actual, double[] predicted) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != 0) {
                sum += Math.abs((actual[i] - predicted[i]) / actual[i]);
                count++;
            }
        }

        return (sum / count) * 100.0;
    }

    private static double calculateR2(double[] actual, double[] predicted) {
        double mean = 0.0;
        for (double value : actual) {
            mean += value;
        }
        mean /= actual.length;

        double ssTotal = 0.0;
        double ssResidual = 0.0;

        for (int i = 0; i < actual.length; i++) {
            ssTotal += Math.pow(actual[i] - mean, 2);
            ssResidual += Math.pow(actual[i] - predicted[i], 2);
        }

        return 1.0 - (ssResidual / ssTotal);
    }

    private static void printRanking(Map<String, EvaluationResult> results) {
        log.info("\n🏆 CLASSEMENT DES MODÈLES (par MAPE)");
        log.info("=".repeat(50));

        results.entrySet().stream()
                .sorted((a, b) -> Double.compare(a.getValue().getMape(), b.getValue().getMape()))
                .forEach(entry -> {
                    EvaluationResult result = entry.getValue();
                    log.info("{:<20} : MAPE = {:6.2f}% | R² = {:.4f} | Temps = {} ms",
                            entry.getKey(),
                            result.getMape(),
                            result.getR2(),
                            result.getInferenceTimeMs() / result.getSampleSize());
                });
    }
}