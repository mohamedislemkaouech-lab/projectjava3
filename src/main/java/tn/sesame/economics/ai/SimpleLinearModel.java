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
    private int numFeaturesTrained = 0; // Track number of features used in training

    /**
     * Entraîne un modèle de régression linéaire simple
     */
    public void train(List<double[]> features, double[] targets, int epochs, double learningRate) {
        if (features.isEmpty() || features.size() != targets.length) {
            throw new IllegalArgumentException("Données d'entraînement invalides");
        }

        numFeaturesTrained = features.get(0).length;
        weights = new double[numFeaturesTrained];
        bias = 0.0;

        Random random = new Random();
        for (int i = 0; i < numFeaturesTrained; i++) {
            weights[i] = random.nextDouble() * 0.1 - 0.05; // Petites valeurs initiales
        }

        log.info("Début de l'entraînement sur {} échantillons, {} features",
                features.size(), numFeaturesTrained);

        // Descente de gradient (batch) avec learning rate decay
        double currentLearningRate = learningRate;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0.0;
            double[] weightGradients = new double[numFeaturesTrained];
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
                for (int j = 0; j < numFeaturesTrained; j++) {
                    weightGradients[j] += error * x[j];
                }
                biasGradient += error;
            }

            // Mise à jour des poids avec learning rate decay
            if (epoch % 100 == 0 && epoch > 0) {
                currentLearningRate *= 0.95; // Réduction du learning rate
            }

            for (int j = 0; j < numFeaturesTrained; j++) {
                weights[j] -= currentLearningRate * weightGradients[j] / features.size();
            }
            bias -= currentLearningRate * biasGradient / features.size();

            // Log périodique
            if (epoch % 100 == 0 || epoch == epochs - 1) {
                double mse = totalLoss / features.size();
                double rmse = Math.sqrt(mse);
                log.info("Epoch {}: MSE = {:.4f}, RMSE = {:.2f}, LR = {:.6f}",
                        epoch, mse, rmse, currentLearningRate);
            }
        }

        // Calcul de la précision finale
        calculateAccuracy(features, targets);

        isTrained = true;
        log.info("Entraînement terminé. Précision: {:.2f}%", trainingAccuracy * 100);
        log.info("Poids finaux: bias={:.4f}, weights={}", bias,
                java.util.Arrays.toString(weights));
    }

    /**
     * Prédit le prix pour un vecteur de features
     */
    public double predict(double[] features) {
        if (!isTrained) {
            throw new IllegalStateException("Modèle non entraîné");
        }

        if (features.length != numFeaturesTrained) {
            throw new IllegalArgumentException(
                    String.format("Nombre de features incorrect. Attendu: %d, Reçu: %d",
                            numFeaturesTrained, features.length)
            );
        }

        return predictSingle(features);
    }

    private double predictSingle(double[] features) {
        double prediction = bias;
        for (int i = 0; i < features.length; i++) {
            prediction += weights[i] * features[i];
        }

        // Ensure the prediction is reasonable (positive price for exports)
        if (prediction < 0) {
            prediction = Math.abs(prediction);
        }

        // Apply a minimum price (Tunisian exports typically > 500 TND)
        return Math.max(500.0, prediction);
    }

    /**
     * Calcule la précision du modèle (R² score)
     */
    private void calculateAccuracy(List<double[]> features, double[] targets) {
        if (features.isEmpty() || targets.length == 0) {
            trainingAccuracy = 0.0;
            return;
        }

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

        // Calculate R² score, but ensure it's between 0 and 1
        if (totalSumSquares == 0) {
            trainingAccuracy = 0.0;
        } else {
            double r2 = 1.0 - (residualSumSquares / totalSumSquares);
            // R² can be negative for very bad models, so clamp it
            trainingAccuracy = Math.max(0.0, Math.min(1.0, r2));
        }
    }

    /**
     * Évalue le modèle sur des données de test
     */
    public ModelEvaluation evaluate(List<double[]> testFeatures, double[] testTargets) {
        if (!isTrained) {
            throw new IllegalStateException("Modèle non entraîné");
        }

        if (testFeatures.isEmpty() || testFeatures.size() != testTargets.length) {
            throw new IllegalArgumentException("Données de test invalides");
        }

        double mae = 0.0;  // Mean Absolute Error
        double mse = 0.0;  // Mean Squared Error
        double[] predictions = new double[testFeatures.size()];

        // Check feature dimensions
        int expectedFeatures = numFeaturesTrained;
        for (int i = 0; i < testFeatures.size(); i++) {
            if (testFeatures.get(i).length != expectedFeatures) {
                throw new IllegalArgumentException(
                        String.format("Feature dimension mismatch at index %d. Expected: %d, Got: %d",
                                i, expectedFeatures, testFeatures.get(i).length)
                );
            }
        }

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
            if (testTargets[i] > 0) { // Avoid division by zero
                mape += Math.abs(predictions[i] - testTargets[i]) / testTargets[i];
                validCount++;
            }
        }
        mape = (validCount > 0) ? (mape / validCount) * 100 : 0;

        return new ModelEvaluation(mae, mse, rmse, mape, predictions);
    }

    /**
     * Sauvegarde les paramètres du modèle dans un fichier
     */
    public void saveModel(String filePath) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter(filePath);
            writer.write("SimpleLinearModel Parameters\n");
            writer.write("=".repeat(30) + "\n");
            writer.write(String.format("Trained: %b\n", isTrained));
            writer.write(String.format("Accuracy: %.4f\n", trainingAccuracy));
            writer.write(String.format("NumFeatures: %d\n", numFeaturesTrained));
            writer.write(String.format("Bias: %.6f\n", bias));
            writer.write("Weights:\n");
            for (int i = 0; i < weights.length; i++) {
                writer.write(String.format("  Feature[%d]: %.6f\n", i, weights[i]));
            }
            writer.close();
            log.info("Modèle sauvegardé dans: {}", filePath);
        } catch (java.io.IOException e) {
            log.error("Erreur lors de la sauvegarde du modèle: {}", e.getMessage());
        }
    }

    /**
     * Charge les paramètres du modèle depuis un fichier
     */
    public void loadModel(String filePath) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));

            // Parse the file
            for (String line : lines) {
                if (line.startsWith("Trained:")) {
                    isTrained = Boolean.parseBoolean(line.split(":")[1].trim());
                } else if (line.startsWith("Accuracy:")) {
                    trainingAccuracy = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.startsWith("NumFeatures:")) {
                    numFeaturesTrained = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.startsWith("Bias:")) {
                    bias = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.startsWith("Feature[")) {
                    // Parse weights
                    int start = line.indexOf('[') + 1;
                    int end = line.indexOf(']');
                    int index = Integer.parseInt(line.substring(start, end));
                    double weight = Double.parseDouble(line.split(":")[1].trim());

                    if (weights == null) {
                        weights = new double[numFeaturesTrained];
                    }
                    if (index < weights.length) {
                        weights[index] = weight;
                    }
                }
            }

            log.info("Modèle chargé depuis: {}", filePath);
        } catch (Exception e) {
            log.error("Erreur lors du chargement du modèle: {}", e.getMessage());
        }
    }

    /**
     * Affiche un résumé du modèle
     */
    public void printModelSummary() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 RÉSUMÉ DU MODÈLE LINÉAIRE SIMPLE");
        System.out.println("=".repeat(50));
        System.out.printf("• Entraîné: %s\n", isTrained ? "✅ OUI" : "❌ NON");
        System.out.printf("• Précision (R²): %.2f%%\n", trainingAccuracy * 100);
        System.out.printf("• Nombre de features: %d\n", numFeaturesTrained);
        System.out.printf("• Biais (intercept): %.4f\n", bias);

        if (weights != null) {
            System.out.println("\n• Poids des features:");
            for (int i = 0; i < weights.length; i++) {
                String featureName = getFeatureName(i);
                System.out.printf("  %-15s: %.6f\n", featureName, weights[i]);
            }
        }

        System.out.println("=".repeat(50));
    }

    private String getFeatureName(int index) {
        String[] featureNames = {
                "Mois",
                "Type Produit",
                "Volume",
                "Indicateur",
                "Volatilité",
                "Taux Change",
                "Pays",
                "Jour de l'année"
        };

        return (index < featureNames.length) ? featureNames[index] : "Feature_" + index;
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
            System.out.printf("MAE  (Erreur Absolue Moyenne): %.2f TND/tonne%n", mae);
            System.out.printf("MSE  (Erreur Quadratique Moyenne): %.2f%n", mse);
            System.out.printf("RMSE (Racine MSE): %.2f TND/tonne%n", rmse);
            System.out.printf("MAPE (Erreur Pourcentage Moyenne): %.2f%%%n", mape);

            // Statistiques des prédictions
            if (predictions.length > 0) {
                double minPred = java.util.Arrays.stream(predictions).min().orElse(0);
                double maxPred = java.util.Arrays.stream(predictions).max().orElse(0);
                double avgPred = java.util.Arrays.stream(predictions).average().orElse(0);
                System.out.println("\n📈 STATISTIQUES DES PRÉDICTIONS:");
                System.out.printf("  Min: %.2f TND/tonne%n", minPred);
                System.out.printf("  Max: %.2f TND/tonne%n", maxPred);
                System.out.printf("  Moyenne: %.2f TND/tonne%n", avgPred);
            }

            // Interprétation
            System.out.println("\n🎯 INTERPRÉTATION:");
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