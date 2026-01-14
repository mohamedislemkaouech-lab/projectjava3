package tn.sesame.economics.util;

import tn.sesame.economics.model.*;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DataLoader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Charge les données CSV et les convertit en objets ExportData
     */
    public static List<ExportData> loadCSVData(String fileName) {
        List<ExportData> data = new ArrayList<>();
        String filePath = "src/main/resources/data/" + fileName;

        try {
            log.info("Chargement du fichier CSV: {}", filePath);
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            boolean isFirstLine = true;
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                if (!line.trim().isEmpty()) {
                    ExportData exportData = parseCSVLine(line);
                    if (exportData != null) {
                        data.add(exportData);
                    }
                }
            }

            log.info("{} lignes chargées depuis {}", data.size(), fileName);

        } catch (IOException e) {
            log.error("Erreur lors du chargement du fichier CSV: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur de parsing du CSV: {}", e.getMessage());
        }

        return data;
    }
    private static void checkFileLocations() {
        System.out.println("\n=== VÉRIFICATION DES FICHIERS ===");

        String[] files = {
                "exports_historical.csv",
                "exports_training.csv",
                "exports_test.csv"
        };

        for (String file : files) {
            // Check multiple possible locations
            String[] paths = {
                    file,
                    "data/" + file,
                    "src/main/resources/data/" + file,
                    "resources/data/" + file,
                    "src/main/resources/" + file,
                    "resources/" + file
            };

            System.out.println("\nRecherche de: " + file);
            boolean found = false;

            for (String path : paths) {
                java.nio.file.Path filePath = java.nio.file.Paths.get(path);
                if (java.nio.file.Files.exists(filePath)) {
                    System.out.println("✓ Trouvé à: " + filePath.toAbsolutePath());
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("❌ Fichier non trouvé");
            }
        }
    }
    /**
     * Parse une ligne CSV en objet ExportData
     */
    private static ExportData parseCSVLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 6) {
                log.warn("Ligne invalide ({} champs): {}", parts.length, line);
                return null;
            }

            return new ExportData(
                    LocalDate.parse(parts[0].trim(), DATE_FORMATTER),
                    ProductType.valueOf(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()),
                    parts[4].trim(),
                    MarketIndicator.valueOf(parts[5].trim())
            );
        } catch (Exception e) {
            log.error("Erreur de parsing de la ligne: {} - {}", line, e.getMessage());
            return null;
        }
    }

    /**
     * Affiche les statistiques du dataset
     */
    public static void displayDatasetStatistics(List<ExportData> dataset) {
        if (dataset.isEmpty()) {
            System.out.println("⚠️  Dataset vide");
            return;
        }

        System.out.println("\n📊 STATISTIQUES DU DATASET");
        System.out.println("=".repeat(50));

        // Nombre total d'enregistrements
        System.out.printf("Nombre total d'exportations: %,d%n", dataset.size());

        // Période couverte
        LocalDate minDate = dataset.stream()
                .map(ExportData::date)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate maxDate = dataset.stream()
                .map(ExportData::date)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        System.out.printf("Période: %s à %s%n", minDate, maxDate);

        // Prix moyen
        double avgPrice = dataset.stream()
                .mapToDouble(ExportData::pricePerTon)
                .average()
                .orElse(0.0);
        System.out.printf("Prix moyen par tonne: %.2f TND%n", avgPrice);

        // Volume total
        double totalVolume = dataset.stream()
                .mapToDouble(ExportData::volume)
                .sum();
        System.out.printf("Volume total exporté: %.2f tonnes%n", totalVolume);

        // Distribution par produit
        System.out.println("\n📦 DISTRIBUTION PAR PRODUIT:");
        Map<ProductType, Long> productCount = dataset.stream()
                .collect(Collectors.groupingBy(ExportData::productType, Collectors.counting()));

        productCount.forEach((product, count) -> {
            double percentage = (count * 100.0) / dataset.size();
            System.out.printf("  • %-15s: %3d (%.1f%%)%n",
                    product.getFrenchName(), count, percentage);
        });

        // Distribution par pays
        System.out.println("\n🌍 TOP 5 PAYS DESTINATION:");
        Map<String, Long> countryCount = dataset.stream()
                .collect(Collectors.groupingBy(ExportData::destinationCountry, Collectors.counting()));

        countryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry ->
                        System.out.printf("  • %-12s: %3d exportations%n", entry.getKey(), entry.getValue()));
    }

    /**
     * Exporte les prédictions vers un fichier CSV
     */
    public static void exportPredictionsToCSV(List<PricePrediction> predictions, String outputFileName) {
        String filePath = "src/main/resources/data/" + outputFileName;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Écrire l'en-tête
            writer.println("prediction_date,product_type,predicted_price,confidence,model_name,status");

            // Écrire les données
            for (PricePrediction prediction : predictions) {
                writer.printf("%s,%s,%.2f,%.2f,%s,%s%n",
                        prediction.predictionDate(),
                        prediction.productType(),
                        prediction.predictedPrice(),
                        prediction.confidence(),
                        prediction.modelName(),
                        prediction.status()
                );
            }

            log.info("{} prédictions exportées vers {}", predictions.size(), outputFileName);

        } catch (IOException e) {
            log.error("Erreur lors de l'export CSV: {}", e.getMessage());
        }
    }

    /**
     * Prépare les données pour l'entraînement (normalisation, encodage, etc.)
     */
    public static Map<String, Object> prepareDataForTraining(List<ExportData> trainingData) {
        log.info("Préparation des données pour l'entraînement ({} enregistrements)", trainingData.size());

        Map<String, Object> preparedData = new HashMap<>();

        // 1. Extraire les features (caractéristiques)
        List<double[]> features = trainingData.stream()
                .map(data -> encodeFeatures(data))
                .collect(Collectors.toList());

        // 2. Extraire les targets (prix réels)
        double[] targets = trainingData.stream()
                .mapToDouble(ExportData::pricePerTon)
                .toArray();

        // 3. Calculer les statistiques pour la normalisation
        double[] featureMeans = calculateFeatureMeans(features);
        double[] featureStdDevs = calculateFeatureStdDevs(features, featureMeans);

        // 4. Normaliser les features
        List<double[]> normalizedFeatures = normalizeFeatures(features, featureMeans, featureStdDevs);

        preparedData.put("features", normalizedFeatures);
        preparedData.put("targets", targets);
        preparedData.put("feature_means", featureMeans);
        preparedData.put("feature_stddevs", featureStdDevs);
        preparedData.put("original_data", trainingData);

        log.info("Données préparées: {} features, {} targets",
                features.size(), targets.length);

        return preparedData;
    }

    /**
     * Encode les caractéristiques d'une exportation en vecteur numérique
     */
    public static double[] encodeFeatures(ExportData data) {
        // Créer un vecteur de features
        double[] features = new double[10]; // Ajuster selon le nombre de features

        // 1. Encodage du mois (0-11)
        features[0] = data.date().getMonthValue() / 12.0;

        // 2. Encodage du produit (one-hot simplifié)
        int productIndex = data.productType().ordinal();
        features[1 + productIndex % 5] = 1.0; // Utilise les positions 1-5

        // 3. Encodage du volume (normalisé)
        features[6] = data.volume() / 1000.0; // Normalisation approximative

        // 4. Encodage du pays (simplifié)
        int countryHash = Math.abs(data.destinationCountry().hashCode() % 10);
        features[7] = countryHash / 10.0;

        // 5. Encodage de l'indicateur de marché
        features[8] = data.indicator().ordinal() / 4.0; // Normalisé 0-1

        // 6. Année normalisée (0-1 sur 20 ans)
        int year = data.date().getYear();
        features[9] = (year - 2005) / 20.0; // 2005-2025

        return features;
    }

    private static double[] calculateFeatureMeans(List<double[]> features) {
        int numFeatures = features.get(0).length;
        double[] means = new double[numFeatures];

        for (double[] featureVector : features) {
            for (int i = 0; i < numFeatures; i++) {
                means[i] += featureVector[i];
            }
        }

        for (int i = 0; i < numFeatures; i++) {
            means[i] /= features.size();
        }

        return means;
    }

    private static double[] calculateFeatureStdDevs(List<double[]> features, double[] means) {
        int numFeatures = features.get(0).length;
        double[] variances = new double[numFeatures];

        for (double[] featureVector : features) {
            for (int i = 0; i < numFeatures; i++) {
                double diff = featureVector[i] - means[i];
                variances[i] += diff * diff;
            }
        }

        double[] stdDevs = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            stdDevs[i] = Math.sqrt(variances[i] / features.size());
            // Éviter la division par zéro
            if (stdDevs[i] < 0.0001) {
                stdDevs[i] = 1.0;
            }
        }

        return stdDevs;
    }

    private static List<double[]> normalizeFeatures(List<double[]> features,
                                                    double[] means, double[] stdDevs) {
        return features.stream()
                .map(featureVector -> {
                    double[] normalized = new double[featureVector.length];
                    for (int i = 0; i < featureVector.length; i++) {
                        normalized[i] = (featureVector[i] - means[i]) / stdDevs[i];
                    }
                    return normalized;
                })
                .collect(Collectors.toList());
    }
}