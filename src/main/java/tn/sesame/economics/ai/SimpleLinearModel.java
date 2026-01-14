package tn.sesame.economics.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Random;


@Slf4j
@Data
public class SimpleLinearModel {

    private double[] weights;
    private double bias;
    private boolean isTrained = false;
    private double trainingAccuracy;

    /**
     * Entraîne un modèle de régression linéaire simple
     */
    public void train(List<double[]> features, double[] targets, int epochs, double learningRate) {
        if (features.isEmpty() || features.size() != targets.length) {
            throw new IllegalArgumentException("Données d'entraînement invalides");
        }

        int numFeatures = features.get(0).length;
        weights = new double[numFeatures];
        bias = 0.0;

        Random random = new Random();
        for (int i = 0; i < numFeatures; i++) {
            weights[i] = random.nextDouble() * 0.1 - 0.05; // Petites valeurs initiales
        }

        log.info("Début de l'entraînement sur {} échantillons, {} features",
                features.size(), numFeatures);

        // Descente de gradient (batch)
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0.0;
            double[] weightGradients = new double[numFeatures];
            double biasGradient = 0.0;

            for (int i = 0; i < features.size(); i++) {
                double[] x = features.get(i);
                double y = targets[i];

                // Prédiction
                double prediction = predictSingle(x);

                // Erreur
                double error = prediction - y;
                totalLoss += error * error;

                // Calcul des gradients
                for (int j = 0; j < numFeatures; j++) {
                    weightGradients[j] += error * x[j];
                }
                biasGradient += error;
            }

            // Mise à jour des poids
            for (int j = 0; j < numFeatures; j++) {
                weights[j] -= learningRate * weightGradients[j] / features.size();
            }
            bias -= learningRate * biasGradient / features.size();

            // Log périodique
            if (epoch % 100 == 0) {
                double mse = totalLoss / features.size();
                log.info("Epoch {}: MSE = {:.4f}", epoch, mse);
            }
        }

        // Calcul de la précision finale
        calculateAccuracy(features, targets);

        isTrained = true;
        log.info("Entraînement terminé. Précision: {:.2f}%", trainingAccuracy * 100);
    }

    /**
     * Prédit le prix pour un vecteur de features
     */
    public double predict(double[] features) {
        if (!isTrained) {
            throw new IllegalStateException("Modèle non entraîné");
        }

        return predictSingle(features);
    }

    private double predictSingle(double[] features) {
        double prediction = bias;
        for (int i = 0; i < features.length; i++) {
            prediction += weights[i] * features[i];
        }
        return prediction;
    }

    /**
     * Calcule la précision du modèle (R² score)
     */
    private void calculateAccuracy(List<double[]> features, double[] targets) {
        double totalSumSquares = 0.0;
        double residualSumSquares = 0.0;

        double meanTarget = 0.0;
        for (double target : targets) {
            meanTarget += target;
        }
        meanTarget /= targets.length;

        for (int i = 0; i < features.size(); i++) {
            double prediction = predictSingle(features.get(i));
            double target = targets[i];

            totalSumSquares += Math.pow(target - meanTarget, 2);
            residualSumSquares += Math.pow(target - prediction, 2);
        }

        trainingAccuracy = 1.0 - (residualSumSquares / totalSumSquares);
    }

    /**
     * Évalue le modèle sur des données de test
     */
    public ModelEvaluation evaluate(List<double[]> testFeatures, double[] testTargets) {
        if (!isTrained) {
            throw new IllegalStateException("Modèle non entraîné");
        }

        double mae = 0.0;  // Mean Absolute Error
        double mse = 0.0;  // Mean Squared Error
        double[] predictions = new double[testFeatures.size()];

        for (int i = 0; i < testFeatures.size(); i++) {
            double prediction = predict(testFeatures.get(i));
            double target = testTargets[i];

            predictions[i] = prediction;
            mae += Math.abs(prediction - target);
            mse += Math.pow(prediction - target, 2);
        }

        mae /= testFeatures.size();
        mse /= testFeatures.size();
        double rmse = Math.sqrt(mse);

        // Calcul MAPE (Mean Absolute Percentage Error)
        double mape = 0.0;
        int validCount = 0;
        for (int i = 0; i < testFeatures.size(); i++) {
            if (testTargets[i] != 0) {
                mape += Math.abs(predictions[i] - testTargets[i]) / testTargets[i];
                validCount++;
            }
        }
        mape = (validCount > 0) ? (mape / validCount) * 100 : 0;

        return new ModelEvaluation(mae, mse, rmse, mape, predictions);
    }

    /**
     * Classe pour stocker les résultats d'évaluation
     */
    @Data
    public static class ModelEvaluation {
        private final double mae;      // Erreur absolue moyenne
        private final double mse;      // Erreur quadratique moyenne
        private final double rmse;     // Racine de l'erreur quadratique moyenne
        private final double mape;     // Erreur en pourcentage moyenne
        private final double[] predictions;

        public void printReport() {
            System.out.println("\n📊 RAPPORT D'ÉVALUATION DU MODÈLE");
            System.out.println("=".repeat(40));
            System.out.printf("MAE  (Erreur Absolue Moyenne): %.2f €/tonne%n", mae);
            System.out.printf("MSE  (Erreur Quadratique Moyenne): %.2f%n", mse);
            System.out.printf("RMSE (Racine MSE): %.2f €/tonne%n", rmse);
            System.out.printf("MAPE (Erreur Pourcentage Moyenne): %.2f%%%n", mape);

            // Interprétation
            System.out.println("\n📈 INTERPRÉTATION:");
            if (mape < 10) {
                System.out.println("✅ Excellente précision (erreur < 10%)");
            } else if (mape < 20) {
                System.out.println("👍 Bonne précision (erreur 10-20%)");
            } else if (mape < 30) {
                System.out.println("⚠️  Précision acceptable (erreur 20-30%)");
            } else {
                System.out.println("❌ Précision faible (erreur > 30%)");
            }
        }
    }
}