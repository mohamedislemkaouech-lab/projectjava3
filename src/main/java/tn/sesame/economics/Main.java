package tn.sesame.economics;

import tn.sesame.economics.ai.*;
import tn.sesame.economics.model.*;
import tn.sesame.economics.service.EconomicIntelligenceService;
import tn.sesame.economics.service.ReportGenerator;
import tn.sesame.economics.exception.ModelException;
import tn.sesame.economics.exception.PredictionException;
import tn.sesame.economics.util.DataLoader;
import tn.sesame.economics.integration.TinyLlamaService;
import tn.sesame.economics.ai.DJLRealModel;
import tn.sesame.economics.ai.SimpleLinearModel;
import tn.sesame.economics.ai.SimpleLinearPredictionService;
import tn.sesame.economics.ai.ONNXRuntimeService;

import java.util.Random;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Classe principale de l'application d'intelligence économique tunisienne.
 * Utilise DJL Réel pour les prédictions deep learning.
 *
 * @since Java 25
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final Scanner scanner = new Scanner(System.in);
    private static EconomicIntelligenceService intelligenceService;

    /**
     * Point d'entrée principal de l'application.
     *
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        LOGGER.info("=== Système d'Intelligence Économique Tunisienne ===");

        // Setup project structure first
        setupProjectStructure();

        LOGGER.info("Initialisation en cours...");

        try {
            // Initialisation des services
            initializeServices();

            // Message spécial pour DJL Réel
            System.out.println("\n" + "🎉" + "=".repeat(58) + "🎉");
            System.out.println("  🚀 DJL RÉEL ACTIVÉ - MODÈLE DEEP LEARNING EN FONCTIONNEMENT");
            System.out.println("  📊 Prêt à analyser vos données CSV avec un vrai réseau de neurones");
            System.out.println("🎉" + "=".repeat(58) + "🎉\n");

            // Menu principal
            boolean running = true;
            while (running) {
                displayMainMenu();
                int choice = readIntInput("Votre choix: ");

                switch (choice) {
                    case 1 -> analyzeHistoricalData();
                    case 2 -> performCustomAnalysis();
                    case 3 -> trainAIModel();
                    case 4 -> generateMarketReport();
                    case 5 -> generateExecutiveSummary();
                    case 6 -> testTinyLlama();
                    case 7 -> displaySystemInfo();
                    case 8 -> exportPredictions();
                    case 9 -> changeAIModel();
                    case 0 -> {
                        running = false;
                        cleanupServices();
                        LOGGER.info("Arrêt du système...");
                    }
                    default -> LOGGER.warning("Choix invalide. Veuillez réessayer.");
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur critique dans l'application: " + e.getMessage(), e);
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            LOGGER.info("Application terminée.");
        }
    }

    /**
     * Initialise tous les services nécessaires.
     */
    private static void initializeServices() throws ModelException {
        LOGGER.info("Initialisation des services...");

        // Initialiser les modèles d'IA
        initializeAIServices();

        LOGGER.info("Services initialisés avec succès");
    }

    /**
     * Initialise les services d'IA avec DJL Réel par défaut.
     */
    private static void initializeAIServices() throws ModelException {
        System.out.println("\n=== SÉLECTION DU MODÈLE D'IA ===");
        System.out.println("1. DJL Réel (Deep Learning - Modèle principal)");
        System.out.println("2. ONNX Runtime (Optimisé production)");
        System.out.println("3. Modèle simple (régression linéaire)");
        System.out.println("4. Utiliser TinyLlama pour les rapports");
        System.out.print("Votre choix (1-4): ");

        String aiModelChoice = scanner.nextLine();
        BaseAIModel predictionService;

        switch (aiModelChoice) {
            case "2" -> {
                System.out.println("Initialisation ONNX Runtime...");
                predictionService = new ONNXRuntimeService();
            }
            case "3" -> {
                System.out.println("Création modèle simple...");
                predictionService = new SimpleLinearPredictionService();
            }
            case "4" -> {
                System.out.println("Configuration TinyLlama...");
                predictionService = new SimpleLinearPredictionService(); // Fallback
            }
            default -> {
                System.out.println("🚀 Initialisation DJL Réel (Deep Learning)...");
                predictionService = new DJLRealModel();
            }
        }

        // Vérifier les fichiers CSV pour DJL Réel
        if (predictionService instanceof DJLRealModel) {
            System.out.println("\n🔍 Vérification des fichiers CSV pour l'entraînement...");
            checkCSVFiles();
        }

        // Chargement du modèle
        LOGGER.info("Chargement du modèle: " + predictionService.getModelName());
        try {
            predictionService.loadModel();
            System.out.println("✅ Modèle chargé avec succès!");

            // Afficher les informations du modèle si c'est DJL Réel
            if (predictionService instanceof DJLRealModel) {
                ((DJLRealModel) predictionService).printModelInfo();
            }
        } catch (ModelException e) {
            System.out.println("❌ Erreur lors du chargement du modèle: " + e.getMessage());
            System.out.println("🔄 Tentative avec ONNX Runtime comme fallback...");

            // Fallback vers ONNX Runtime
            predictionService = new ONNXRuntimeService();
            predictionService.loadModel();
            System.out.println("✅ ONNX Runtime chargé comme fallback");
        }

        // Initialisation du service LLM
        LOGGER.info("Initialisation du service LLM (TinyLlama)...");
        ReportGenerator reportService = new TinyLlamaService();

        intelligenceService = new EconomicIntelligenceService(predictionService, reportService);
    }

    /**
     * Permet de changer de modèle d'IA pendant l'exécution.
     */
    private static void changeAIModel() {
        System.out.println("\n🔄 CHANGEMENT DE MODÈLE D'IA");
        System.out.println("=".repeat(40));

        String currentModelName = intelligenceService.getPredictionModel().getModelName();
        System.out.println("Modèle actuel: " + currentModelName);

        System.out.println("\nChoisissez le nouveau modèle:");
        System.out.println("1. DJL Réel (Deep Learning - recommandé)");
        System.out.println("2. ONNX Runtime");
        System.out.println("3. Modèle simple (régression linéaire)");
        System.out.println("4. Annuler");
        System.out.print("Votre choix: ");

        int choice = readIntInput("");

        if (choice == 4) {
            System.out.println("Changement annulé.");
            return;
        }

        try {
            // Décharger l'ancien modèle
            System.out.println("\n🔧 Déchargement de l'ancien modèle...");
            if (intelligenceService.getPredictionModel() != null) {
                intelligenceService.getPredictionModel().unloadModel();
            }

            // Créer le nouveau modèle
            BaseAIModel newModel;
            switch (choice) {
                case 2:
                    newModel = new ONNXRuntimeService();
                    System.out.println("🔄 Passage à ONNX Runtime...");
                    break;
                case 3:
                    newModel = new SimpleLinearPredictionService();
                    System.out.println("🔄 Passage au modèle simple...");
                    break;
                default:
                    newModel = new DJLRealModel();
                    System.out.println("🚀 Passage à DJL Réel...");
                    if (newModel instanceof DJLRealModel) {
                        checkCSVFiles();
                    }
                    break;
            }

            // Charger le nouveau modèle
            newModel.loadModel();

            // Mettre à jour le service
            intelligenceService = new EconomicIntelligenceService(
                    newModel,
                    new TinyLlamaService()
            );

            System.out.println("✅ Modèle changé avec succès!");
            System.out.println("Nouveau modèle: " + newModel.getModelName());

        } catch (Exception e) {
            System.out.println("❌ Erreur lors du changement: " + e.getMessage());
            System.out.println("Retour au modèle précédent...");
        }
    }

    /**
     * Vérifie la présence des fichiers CSV nécessaires.
     */
    private static void checkCSVFiles() {
        String[] requiredFiles = {
                "exports_historical.csv",
                "exports_training.csv",
                "exports_test.csv"
        };

        System.out.println("📂 Vérification des fichiers CSV...");

        boolean allFilesFound = true;
        for (String file : requiredFiles) {
            boolean found = false;
            String foundLocation = "";

            // Chercher dans plusieurs emplacements
            String[] locations = {
                    file,
                    "data/" + file,
                    "src/main/resources/data/" + file,
                    "src/main/resources/" + file
            };

            for (String location : locations) {
                java.nio.file.Path path = java.nio.file.Paths.get(location);
                if (java.nio.file.Files.exists(path)) {
                    found = true;
                    foundLocation = location;
                    break;
                }
            }

            if (found) {
                System.out.println("✅ " + file + " trouvé: " + foundLocation);

                // Afficher le nombre de lignes si possible
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(foundLocation));
                    int dataLines = Math.max(0, lines.size() - 1); // Exclure l'en-tête
                    System.out.println("   📊 " + dataLines + " enregistrements");
                } catch (Exception e) {
                    // Ignorer l'erreur de lecture
                }
            } else {
                System.out.println("❌ " + file + " NON TROUVÉ");
                System.out.println("   Placez-le dans: src/main/resources/data/ ou data/");
                allFilesFound = false;
            }
        }

        if (!allFilesFound) {
            System.out.println("\n⚠️  ATTENTION: Certains fichiers CSV sont manquants.");
            System.out.println("DJL Réel va générer des données d'entraînement synthétiques.");
            System.out.println("Appuyez sur Entrée pour continuer...");
            scanner.nextLine();
        }
    }

    private static void generateExecutiveSummary() {
        System.out.println("\n📈 GÉNÉRATION DE RÉSUMÉ EXÉCUTIF");
        System.out.println("=".repeat(40));

        System.out.println("1. Utiliser les données historiques");
        System.out.println("2. Utiliser un échantillon aléatoire");
        System.out.print("Votre choix: ");

        int choice = readIntInput("");

        try {
            List<ExportData> data;
            if (choice == 2) {
                data = loadCSVFile("exports_historical.csv");
                if (data.size() > 10) {
                    Collections.shuffle(data);
                    data = data.subList(0, 10);
                }
            } else {
                data = loadCSVFile("exports_training.csv");
            }

            if (data.isEmpty()) {
                System.out.println("❌ Aucune donnée disponible");
                return;
            }

            // Faire des prédictions
            List<PricePrediction> predictions = intelligenceService.analyzeExports(data);

            // Générer le résumé exécutif
            String report = intelligenceService.generateIntelligenceReport(predictions);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("📈 RÉSUMÉ EXÉCUTIF");
            System.out.println("=".repeat(60));
            System.out.println(report);

            // Sauvegarde optionnelle
            System.out.print("\n💾 Sauvegarder le résumé? (o/n): ");
            String saveChoice = scanner.nextLine();

            if (saveChoice.equalsIgnoreCase("o")) {
                String fileName = "resume_executif_" + LocalDate.now() + ".txt";
                java.nio.file.Files.writeString(
                        java.nio.file.Paths.get(fileName),
                        report
                );
                System.out.println("✅ Résumé sauvegardé dans: " + fileName);
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Affiche le menu principal amélioré.
     */
    private static void displayMainMenu() {
        String currentModel = "DJL Simulé";
        if (intelligenceService != null && intelligenceService.getPredictionModel() != null) {
            currentModel = intelligenceService.getPredictionModel().getModelName();
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("           🤖 INTELLIGENCE ÉCONOMIQUE TUNISIENNE");
        System.out.println("           Modèle actuel: " + currentModel);
        System.out.println("=".repeat(60));
        System.out.println("1.  Analyser les données historiques");
        System.out.println("2.  Effectuer une analyse personnalisée");
        System.out.println("3.  Entraîner le modèle IA");
        System.out.println("4.  Générer rapport de marché (TinyLlama)");
        System.out.println("5.  Générer résumé exécutif (TinyLlama)");
        System.out.println("6.  Tester TinyLlama");
        System.out.println("7.  Informations système");
        System.out.println("8.  Exporter les prédictions");
        System.out.println("9.  Changer de modèle d'IA");
        System.out.println("0.  Quitter");
        System.out.print("Votre choix: ");
    }

    /**
     * Setup project directories and check file structure.
     */
    private static void setupProjectStructure() {
        System.out.println("\n=== CONFIGURATION DU PROJET ===");

        // Create directories if they don't exist
        String[] directories = {
                "src/main/resources/data",
                "src/main/resources",
                "data",
                "resources/data"
        };

        for (String dir : directories) {
            java.nio.file.Path dirPath = java.nio.file.Paths.get(dir);
            if (!java.nio.file.Files.exists(dirPath)) {
                try {
                    java.nio.file.Files.createDirectories(dirPath);
                    System.out.println("✓ Dossier créé: " + dir);
                } catch (Exception e) {
                    System.out.println("✗ Impossible de créer: " + dir + " - " + e.getMessage());
                }
            } else {
                System.out.println("✓ Dossier existe déjà: " + dir);
            }
        }

        // Check for CSV files
        System.out.println("\n=== VÉRIFICATION DES FICHIERS CSV ===");
        String[] csvFiles = {
                "exports_historical.csv",
                "exports_training.csv",
                "exports_test.csv"
        };

        boolean allFilesFound = true;
        for (String file : csvFiles) {
            boolean found = false;
            for (String dir : directories) {
                java.nio.file.Path filePath = java.nio.file.Paths.get(dir, file);
                if (java.nio.file.Files.exists(filePath)) {
                    System.out.println("✓ " + file + " trouvé dans: " + dir + "/");
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("❌ " + file + " NON TROUVÉ");
                System.out.println("   Placez ce fichier dans un de ces dossiers:");
                System.out.println("   - src/main/resources/data/");
                System.out.println("   - src/main/resources/");
                System.out.println("   - data/ (à la racine du projet)");
                allFilesFound = false;
            }
        }

        if (!allFilesFound) {
            System.out.println("\n⚠️  ATTENTION: Certains fichiers CSV sont manquants.");
            System.out.println("L'application peut ne pas fonctionner correctement.");
            System.out.println("Appuyez sur Entrée pour continuer...");
            scanner.nextLine();
        }
    }

    /**
     * Teste la connexion à TinyLlama.
     */
    private static void testTinyLlama() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           🔧 TEST TINYLLAMA");
        System.out.println("=".repeat(50));

        TinyLlamaService tinyLlama = new TinyLlamaService();

        System.out.println("\n🔍 Vérification de la connexion...");
        String testResult = tinyLlama.testConnection();

        System.out.println(testResult);

        if (testResult.contains("✅")) {
            System.out.print("\n📊 Voulez-vous tester les différents types de rapports? (o/n): ");
            String choice = scanner.nextLine();

            if (choice.equalsIgnoreCase("o")) {
                // Créer des données de test
                List<PricePrediction> testData = generateDemoPredictions(3);

                System.out.println("\n🧪 Test 1: Rapport de marché");
                System.out.println("-".repeat(30));
                String marketReport = tinyLlama.generateMarketReport(testData);
                System.out.println(marketReport.substring(0, Math.min(200, marketReport.length())) + "...");

                System.out.println("\n🧪 Test 2: Résumé exécutif");
                System.out.println("-".repeat(30));
                String summary = tinyLlama.generateSummaryReport(testData);
                System.out.println(summary);

                System.out.println("\n🧪 Test 3: Rapport formaté (Markdown)");
                System.out.println("-".repeat(30));
                String formatted = tinyLlama.generateReport(testData, ReportGenerator.ReportFormat.MARKDOWN);
                System.out.println(formatted.substring(0, Math.min(150, formatted.length())) + "...");
            }
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Appuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }

    /**
     * Charge un fichier CSV en essayant plusieurs emplacements.
     */
    private static List<ExportData> loadCSVFile(String fileName) {
        System.out.println("\n🔍 Recherche du fichier: " + fileName);

        // First, try to load directly without any path prefix
        System.out.print("Essai direct: " + fileName + "... ");
        try {
            List<ExportData> directData = DataLoader.loadCSVData(fileName);
            if (!directData.isEmpty()) {
                System.out.println("SUCCÈS (" + directData.size() + " enregistrements)");
                return directData;
            }
        } catch (Exception e) {
            System.out.println("ÉCHEC");
        }

        // List of possible locations (most common first)
        String[] possiblePaths = {
                fileName,
                "data/" + fileName,
                "src/main/resources/data/" + fileName,
                "src/main/resources/" + fileName,
                "resources/data/" + fileName,
                "resources/" + fileName
        };

        for (String path : possiblePaths) {
            try {
                System.out.print("  Essai: " + path + "... ");
                List<ExportData> data = DataLoader.loadCSVData(path);
                if (!data.isEmpty()) {
                    System.out.println("SUCCÈS (" + data.size() + " enregistrements)");
                    return data;
                } else {
                    System.out.println("VIDE");
                }
            } catch (Exception e) {
                System.out.println("ÉCHEC: " + e.getMessage());
            }
        }

        System.out.println("\n❌ ERREUR: Fichier '" + fileName + "' non trouvé!");
        System.out.println("\nVeuillez placer le fichier dans un de ces emplacements:");
        System.out.println("1. À la racine du projet: " + fileName);
        System.out.println("2. Dans le dossier 'data/': data/" + fileName);
        System.out.println("3. Dans 'src/main/resources/data/': src/main/resources/data/" + fileName);

        // Show current directory for debugging
        System.out.println("\nRépertoire courant: " + System.getProperty("user.dir"));

        return Collections.emptyList();
    }

    /**
     * Analyse les données historiques CSV.
     */
    private static void analyzeHistoricalData() {
        LOGGER.info("Analyse des données historiques CSV...");

        System.out.println("\n=== ANALYSE DES DONNÉES HISTORIQUES ===");
        System.out.println("1. Analyser exports_historical.csv (complet)");
        System.out.println("2. Analyser exports_training.csv (entraînement)");
        System.out.println("3. Analyser exports_test.csv (test)");
        System.out.print("Votre choix (1-3): ");

        int datasetChoice = readIntInput("");
        String fileName;

        switch (datasetChoice) {
            case 2 -> fileName = "exports_training.csv";
            case 3 -> fileName = "exports_test.csv";
            default -> fileName = "exports_historical.csv";
        }

        // Use the utility method to load CSV file
        List<ExportData> historicalData = loadCSVFile(fileName);

        if (historicalData.isEmpty()) {
            System.out.println("\n⚠️  Analyse impossible sans données.");
            return;
        }

        // Afficher les statistiques
        DataLoader.displayDatasetStatistics(historicalData);

        // Vérifier si on utilise DJL Réel
        if (intelligenceService.getPredictionModel() instanceof DJLRealModel) {
            System.out.println("\n🎯 MODÈLE DJL RÉEL DÉTECTÉ");
            System.out.println("Les prédictions utiliseront un réseau de neurones entraîné.");
        }

        // Menu d'analyse avancée
        System.out.println("\n=== OPTIONS D'ANALYSE ===");
        System.out.println("1. Faire des prédictions sur ces données");
        System.out.println("2. Analyser par produit spécifique");
        System.out.println("3. Voir les tendances temporelles");
        System.out.println("4. Retour au menu principal");
        System.out.print("Votre choix: ");

        int analysisChoice = readIntInput("");

        switch (analysisChoice) {
            case 1 -> makePredictionsOnData(historicalData);
            case 2 -> analyzeByProduct(historicalData);
            case 3 -> showTimeTrends(historicalData);
            default -> System.out.println("Retour au menu principal.");
        }
    }

    /**
     * Fait des prédictions sur les données chargées avec DJL Réel.
     */
    private static void makePredictionsOnData(List<ExportData> data) {
        LOGGER.info("Prédictions sur " + data.size() + " enregistrements...");

        // Afficher le modèle utilisé
        boolean isDJLReal = intelligenceService.getPredictionModel() instanceof DJLRealModel;

        System.out.println("\n" + "=".repeat(60));
        if (isDJLReal) {
            System.out.println("🎯 PRÉDICTIONS AVEC DJL RÉEL");
            System.out.println("Modèle: Deep Learning (MLP 7→12→8→4→1)");
        } else {
            System.out.println("🔮 PRÉDICTIONS");
            System.out.println("Modèle: " + intelligenceService.getPredictionModel().getModelName());
        }
        System.out.println("=".repeat(60));

        try {
            List<PricePrediction> predictions = intelligenceService.analyzeExports(data);

            // Afficher les résultats
            displayPredictions(predictions);

            // Statistiques améliorées
            displayEnhancedStatistics(predictions, isDJLReal);

            // Demander si on veut sauvegarder
            System.out.print("\n💾 Voulez-vous exporter ces prédictions en CSV? (o/n): ");
            String exportChoice = scanner.nextLine();

            if (exportChoice.equalsIgnoreCase("o")) {
                String fileName = "predictions_" + LocalDate.now() + ".csv";
                DataLoader.exportPredictionsToCSV(predictions, fileName);
                System.out.println("✅ Prédictions exportées dans: " + fileName);

                // Afficher un extrait du fichier
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(fileName));
                    System.out.println("\n📄 Extrait du fichier exporté:");
                    System.out.println("-".repeat(80));
                    lines.stream().limit(5).forEach(System.out::println);
                    if (lines.size() > 5) {
                        System.out.println("... et " + (lines.size() - 5) + " lignes supplémentaires");
                    }
                } catch (Exception e) {
                    // Ignorer
                }
            }

        } catch (PredictionException e) {
            LOGGER.log(Level.SEVERE, "Erreur de prédiction: " + e.getMessage(), e);
            System.err.println("Erreur d'analyse: " + e.getMessage());
            System.out.println("Utilisation de prédictions de démonstration...");

            List<PricePrediction> demoPredictions = generateDemoPredictions(data);
            displayPredictions(demoPredictions);
            displayStatistics(demoPredictions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue: " + e.getMessage(), e);
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    /**
     * Affiche des statistiques améliorées pour DJL Réel.
     */
    private static void displayEnhancedStatistics(List<PricePrediction> predictions, boolean isDJLReal) {
        if (predictions.isEmpty()) {
            System.out.println("Aucune statistique disponible.");
            return;
        }

        System.out.println("\n📊 STATISTIQUES AVANCÉES");
        if (isDJLReal) {
            System.out.println("(Modèle DJL Réel - Deep Learning)");
        }
        System.out.println("=".repeat(50));

        // Statistiques de base
        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);

        double maxPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .max()
                .orElse(0.0);

        double minPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .min()
                .orElse(0.0);

        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0.0);

        // Calcul de la variance
        double variance = predictions.stream()
                .mapToDouble(p -> Math.pow(p.predictedPrice() - avgPrice, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        System.out.printf("• Nombre de prédictions: %d%n", predictions.size());
        System.out.printf("• Prix moyen prédit: %.2f TND/tonne%n", avgPrice);
        System.out.printf("• Écart-type: %.2f TND/tonne%n", stdDev);
        System.out.printf("• Fourchette: %.2f - %.2f TND/tonne%n", minPrice, maxPrice);
        System.out.printf("• Confiance moyenne: %.2f%%%n", avgConfidence * 100);

        // Analyse par produit
        System.out.println("\n📦 ANALYSE PAR PRODUIT:");
        Map<ProductType, List<PricePrediction>> byProduct = predictions.stream()
                .collect(Collectors.groupingBy(PricePrediction::productType));

        byProduct.forEach((product, productPredictions) -> {
            double productAvg = productPredictions.stream()
                    .mapToDouble(PricePrediction::predictedPrice)
                    .average()
                    .orElse(0.0);

            double productConfidence = productPredictions.stream()
                    .mapToDouble(PricePrediction::confidence)
                    .average()
                    .orElse(0.0);

            System.out.printf("  • %-15s: %5.0f TND (confiance: %5.1f%%, %d prédictions)%n",
                    product.getFrenchName(),
                    productAvg,
                    productConfidence * 100,
                    productPredictions.size());
        });

        // Distribution des confiances
        long highConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.8)
                .count();

        long mediumConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.6 && p.confidence() < 0.8)
                .count();

        long lowConfidence = predictions.stream()
                .filter(p -> p.confidence() < 0.6)
                .count();

        System.out.println("\n🎯 DISTRIBUTION DES CONFIANCES:");
        System.out.printf("  • Haute confiance (≥80%%): %d (%.1f%%)%n",
                highConfidence, (highConfidence * 100.0 / predictions.size()));
        System.out.printf("  • Confiance moyenne (60-80%%): %d (%.1f%%)%n",
                mediumConfidence, (mediumConfidence * 100.0 / predictions.size()));
        System.out.printf("  • Basse confiance (<60%%): %d (%.1f%%)%n",
                lowConfidence, (lowConfidence * 100.0 / predictions.size()));

        // Avis sur la qualité des prédictions
        System.out.println("\n💡 INTERPRÉTATION:");
        if (isDJLReal) {
            System.out.println("✅ Prédictions basées sur un modèle Deep Learning entraîné");
        }

        if (avgConfidence >= 0.8) {
            System.out.println("✅ Excellente qualité des prédictions");
        } else if (avgConfidence >= 0.6) {
            System.out.println("👍 Bonne qualité des prédictions");
        } else {
            System.out.println("⚠️  Qualité modérée - à utiliser avec précaution");
        }
    }

    /**
     * Analyse les données par produit spécifique.
     */
    private static void analyzeByProduct(List<ExportData> data) {
        System.out.println("\n=== ANALYSE PAR PRODUIT ===");

        // Afficher la distribution des produits
        Map<ProductType, Long> productDistribution = data.stream()
                .collect(Collectors.groupingBy(ExportData::productType, Collectors.counting()));

        System.out.println("Produits disponibles:");
        int i = 1;
        List<ProductType> productList = new ArrayList<>();
        for (Map.Entry<ProductType, Long> entry : productDistribution.entrySet()) {
            System.out.printf("%d. %-15s (%d enregistrements)%n",
                    i++, entry.getKey().getFrenchName(), entry.getValue());
            productList.add(entry.getKey());
        }

        System.out.print("Choisissez un produit (1-" + productList.size() + "): ");
        int choice = readIntInput("");

        if (choice < 1 || choice > productList.size()) {
            System.out.println("Choix invalide.");
            return;
        }

        ProductType selectedProduct = productList.get(choice - 1);
        List<ExportData> filteredData = data.stream()
                .filter(d -> d.productType() == selectedProduct)
                .collect(Collectors.toList());

        System.out.println("\n📊 ANALYSE POUR: " + selectedProduct.getFrenchName());
        System.out.println("=".repeat(40));

        // Statistiques pour ce produit
        double avgPrice = filteredData.stream()
                .mapToDouble(ExportData::pricePerTon)
                .average()
                .orElse(0);

        double minPrice = filteredData.stream()
                .mapToDouble(ExportData::pricePerTon)
                .min()
                .orElse(0);

        double maxPrice = filteredData.stream()
                .mapToDouble(ExportData::pricePerTon)
                .max()
                .orElse(0);

        double totalVolume = filteredData.stream()
                .mapToDouble(ExportData::volume)
                .sum();

        System.out.printf("Prix moyen: %.2f TND/tonne%n", avgPrice);
        System.out.printf("Fourchette de prix: %.2f - %.2f TND/tonne%n", minPrice, maxPrice);
        System.out.printf("Volume total: %.2f tonnes%n", totalVolume);

        // Distribution par pays
        Map<String, Long> countryDist = filteredData.stream()
                .collect(Collectors.groupingBy(ExportData::destinationCountry, Collectors.counting()));

        System.out.println("\n🌍 PRINCIPAUX PAYS D'EXPORTATION:");
        countryDist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry ->
                        System.out.printf("  • %-12s: %d exportations%n", entry.getKey(), entry.getValue()));

        // Prédiction pour ce produit
        System.out.print("\n🤖 Voulez-vous une prédiction de prix pour ce produit? (o/n): ");
        String predictChoice = scanner.nextLine();

        if (predictChoice.equalsIgnoreCase("o")) {
            // Utiliser la dernière donnée comme base pour la prédiction
            ExportData latestData = filteredData.stream()
                    .max(Comparator.comparing(ExportData::date))
                    .orElse(null);

            if (latestData != null) {
                try {
                    PricePrediction prediction = intelligenceService.getPredictionModel().predictPrice(latestData);
                    System.out.printf("\n🔮 PRÉDICTION:%n");
                    System.out.printf("Produit: %s%n", prediction.productType().getFrenchName());
                    System.out.printf("Prix prédit: %.2f TND/tonne%n", prediction.predictedPrice());
                    System.out.printf("Confiance: %.1f%%%n", prediction.confidence() * 100);
                    System.out.printf("Date de prédiction: %s%n", prediction.predictionDate());
                } catch (Exception e) {
                    System.out.println("❌ Erreur lors de la prédiction: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Affiche les tendances temporelles.
     */
    private static void showTimeTrends(List<ExportData> data) {
        System.out.println("\n📈 TENDANCES TEMPORELLES");
        System.out.println("=".repeat(40));

        // Grouper par année
        Map<Integer, Double> yearlyAvgPrice = data.stream()
                .collect(Collectors.groupingBy(
                        d -> d.date().getYear(),
                        Collectors.averagingDouble(ExportData::pricePerTon)
                ));

        // Trier par année
        List<Map.Entry<Integer, Double>> sortedEntries = new ArrayList<>(yearlyAvgPrice.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        System.out.println("Évolution du prix moyen par année:");
        System.out.println("Année | Prix Moyen | Tendance");
        System.out.println("------|------------|----------");

        Double previousPrice = null;
        for (Map.Entry<Integer, Double> entry : sortedEntries) {
            String trend = "";
            if (previousPrice != null) {
                double change = ((entry.getValue() - previousPrice) / previousPrice) * 100;
                if (change > 5) trend = "↗️";
                else if (change < -5) trend = "↘️";
                else trend = "➡️";
            }

            System.out.printf("%5d | %9.2fTND | %s%n", entry.getKey(), entry.getValue(), trend);
            previousPrice = entry.getValue();
        }

        // Graphique ASCII simple
        System.out.println("\n📊 GRAPHIQUE DES PRIX (simplifié):");
        double minPrice = sortedEntries.stream()
                .mapToDouble(Map.Entry::getValue)
                .min()
                .orElse(0);

        double maxPrice = sortedEntries.stream()
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(1000);

        for (Map.Entry<Integer, Double> entry : sortedEntries) {
            int barLength = (int) ((entry.getValue() - minPrice) / (maxPrice - minPrice) * 30);
            System.out.printf("%5d: %s %.0fTND%n",
                    entry.getKey(),
                    "█".repeat(Math.max(0, barLength)),
                    entry.getValue());
        }
    }

    /**
     * Entraîne le modèle IA sur les datasets.
     */
    private static void trainAIModel() {
        LOGGER.info("Entraînement du modèle IA...");

        System.out.println("\n🤖 ENTRAÎNEMENT DU MODÈLE IA");
        System.out.println("=".repeat(50));

        try {
            // Charger les données d'entraînement
            List<ExportData> trainingData = loadCSVFile("exports_training.csv");

            if (trainingData.isEmpty()) {
                System.out.println("❌ Impossible de trouver le fichier exports_training.csv");
                System.out.println("Veuillez placer le fichier dans src/main/resources/data/");
                return;
            }

            if (trainingData.size() < 50) {
                System.out.println("⚠️  Attention: peu de données d'entraînement (" + trainingData.size() + " enregistrements)");
                System.out.println("Utilisez exports_historical.csv pour plus de données.");
            }

            System.out.printf("• %d enregistrements chargés%n", trainingData.size());

            // Préparer les données
            System.out.println("• Préparation des données...");
            Map<String, Object> preparedData = DataLoader.prepareDataForTraining(trainingData);

            // Créer et entraîner un modèle simple
            System.out.println("• Création du modèle...");
            SimpleLinearModel model = new SimpleLinearModel();

            @SuppressWarnings("unchecked")
            List<double[]> features = (List<double[]>) preparedData.get("features");
            double[] targets = (double[]) preparedData.get("targets");

            System.out.println("• Début de l'entraînement...");
            System.out.print("Nombre d'époques [500]: ");
            int epochs = readIntInput("");
            if (epochs <= 0) epochs = 500;

            System.out.print("Taux d'apprentissage [0.01]: ");
            double learningRate = readDoubleInput("");
            if (learningRate <= 0) learningRate = 0.01;

            model.train(features, targets, epochs, learningRate);

            System.out.println("✅ Entraînement terminé!");
            System.out.printf("Précision du modèle: %.1f%%%n", model.getTrainingAccuracy() * 100);

            // Évaluer sur les données de test
            System.out.print("\n📊 Évaluer sur les données de test? (o/n): ");
            String evalChoice = scanner.nextLine();

            if (evalChoice.equalsIgnoreCase("o")) {
                evaluateTrainedModel(model);
            }

            // Option pour sauvegarder le modèle
            System.out.print("\n💾 Sauvegarder le modèle entraîné? (o/n): ");
            String saveChoice = scanner.nextLine();

            if (saveChoice.equalsIgnoreCase("o")) {
                saveTrainedModel(model, preparedData);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'entraînement: " + e.getMessage(), e);
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Évalue le modèle entraîné sur les données de test.
     */
    private static void evaluateTrainedModel(SimpleLinearModel model) {
        try {
            List<ExportData> testData = loadCSVFile("exports_test.csv");

            if (testData.isEmpty()) {
                System.out.println("❌ Aucune donnée de test trouvée");
                return;
            }

            System.out.println("\n📈 ÉVALUATION SUR LES DONNÉES DE TEST");
            System.out.println("=".repeat(40));
            System.out.printf("• %d enregistrements de test%n", testData.size());

            Map<String, Object> testDataPrepared = DataLoader.prepareDataForTraining(testData);

            @SuppressWarnings("unchecked")
            List<double[]> testFeatures = (List<double[]>) testDataPrepared.get("features");
            double[] testTargets = (double[]) testDataPrepared.get("targets");

            SimpleLinearModel.ModelEvaluation evaluation = model.evaluate(testFeatures, testTargets);
            evaluation.printReport();

            // Exemple de prédiction
            System.out.println("\n🔮 EXEMPLE DE PRÉDICTION:");
            if (!testData.isEmpty()) {
                ExportData sample = testData.get(0);
                double[] features = DataLoader.encodeFeatures(sample);
                double prediction = model.predict(features);

                System.out.printf("Produit: %s%n", sample.productType().getFrenchName());
                System.out.printf("Prix réel: %.2f TND/tonne%n", sample.pricePerTon());
                System.out.printf("Prix prédit: %.2f TND/tonne%n", prediction);
                System.out.printf("Erreur: %.2f TND (%.1f%%)%n",
                        Math.abs(prediction - sample.pricePerTon()),
                        Math.abs(prediction - sample.pricePerTon()) / sample.pricePerTon() * 100);
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'évaluation: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde le modèle entraîné.
     */
    private static void saveTrainedModel(SimpleLinearModel model, Map<String, Object> trainingData) {
        try {
            String timestamp = LocalDate.now().toString();
            String modelName = "tunisian_export_model_" + timestamp;

            // Créer un fichier de métadonnées
            String metadata = String.format(
                    "Modèle: %s%n" +
                            "Date d'entraînement: %s%n" +
                            "Précision: %.2f%%%n" +
                            "Nombre d'échantillons: %d%n" +
                            "Poids: %s%n" +
                            "Biais: %.4f%n",
                    modelName,
                    timestamp,
                    model.getTrainingAccuracy() * 100,
                    ((List<?>) trainingData.get("features")).size(),
                    Arrays.toString(model.getWeights()),
                    model.getBias()
            );

            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get(modelName + ".txt"),
                    metadata
            );

            System.out.println("✅ Modèle sauvegardé dans: " + modelName + ".txt");

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Génère un rapport détaillé.
     */
    private static void generateDetailedReport() {
        try {
            // Charger toutes les données
            List<ExportData> allData = loadCSVFile("exports_historical.csv");

            if (allData.isEmpty()) {
                System.out.println("❌ Aucune donnée disponible pour le rapport");
                return;
            }

            // Générer un sous-ensemble pour le rapport
            List<ExportData> reportData = allData.stream()
                    .sorted((a, b) -> b.date().compareTo(a.date()))
                    .limit(50)
                    .collect(Collectors.toList());

            // Faire des prédictions
            List<PricePrediction> predictions = intelligenceService.analyzeExports(reportData);

            // Générer le rapport
            String report = intelligenceService.generateIntelligenceReport(predictions);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("📋 RAPPORT D'INTELLIGENCE ÉCONOMIQUE");
            System.out.println("=".repeat(60));
            System.out.println(report);

            // Sauvegarde
            System.out.print("\n💾 Sauvegarder le rapport? (o/n): ");
            String saveChoice = scanner.nextLine();

            if (saveChoice.equalsIgnoreCase("o")) {
                String fileName = "rapport_economique_" + LocalDate.now() + ".txt";
                java.nio.file.Files.writeString(
                        java.nio.file.Paths.get(fileName),
                        report
                );
                System.out.println("✅ Rapport sauvegardé dans: " + fileName);
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la génération du rapport: " + e.getMessage());
        }
    }

    /**
     * Exporte les prédictions en CSV.
     */
    private static void exportPredictions() {
        System.out.println("\n💾 EXPORT DES PRÉDICTIONS");
        System.out.println("=".repeat(40));

        System.out.println("1. Exporter les prédictions existantes");
        System.out.println("2. Générer de nouvelles prédictions à exporter");
        System.out.print("Votre choix: ");

        int choice = readIntInput("");

        try {
            if (choice == 2) {
                // Charger des données et faire des prédictions
                List<ExportData> data = loadCSVFile("exports_test.csv");
                if (data.isEmpty()) {
                    System.out.println("❌ Aucune donnée trouvée");
                    return;
                }

                List<PricePrediction> predictions = intelligenceService.analyzeExports(data);
                displayPredictions(predictions);

                System.out.print("\nNom du fichier de sortie [predictions_export.csv]: ");
                String fileName = scanner.nextLine();
                if (fileName.isEmpty()) fileName = "predictions_export.csv";

                DataLoader.exportPredictionsToCSV(predictions, fileName);
                System.out.println("✅ " + predictions.size() + " prédictions exportées dans: " + fileName);

            } else {
                // Utiliser un fichier existant
                System.out.print("Nom du fichier CSV à exporter: ");
                String fileName = scanner.nextLine();

                if (!fileName.endsWith(".csv")) {
                    fileName += ".csv";
                }

                System.out.println("Format attendu: prediction_date,product_type,predicted_price,confidence,model_name,status");
                System.out.println("Le fichier sera créé dans le dossier courant");

                // Demander des prédictions simples à exporter
                System.out.print("Nombre de prédictions à générer [10]: ");
                int count = readIntInput("");
                if (count <= 0) count = 10;

                List<PricePrediction> demoPredictions = generateDemoPredictions(count);
                DataLoader.exportPredictionsToCSV(demoPredictions, fileName);

                System.out.println("✅ " + demoPredictions.size() + " prédictions exportées");
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'export: " + e.getMessage());
        }
    }

    /**
     * Nettoie les services avant la fermeture.
     */
    private static void cleanupServices() {
        try {
            if (intelligenceService != null && intelligenceService.getPredictionModel() != null) {
                intelligenceService.getPredictionModel().unloadModel();
            }
            LOGGER.info("Services nettoyés");
        } catch (Exception e) {
            LOGGER.warning("Erreur lors du nettoyage: " + e.getMessage());
        }
    }

    // === MÉTHODES EXISTANTES (adaptées) ===

    private static void performCustomAnalysis() {
        System.out.println("\n=== ANALYSE PERSONNALISÉE ===");
        System.out.println("1. Saisir manuellement des données");
        System.out.println("2. Utiliser un échantillon aléatoire du dataset");
        System.out.print("Votre choix: ");

        int choice = readIntInput("");

        if (choice == 2) {
            // Utiliser un échantillon aléatoire du dataset
            try {
                List<ExportData> allData = loadCSVFile("exports_historical.csv");
                if (allData.isEmpty()) {
                    System.out.println("❌ Aucune donnée disponible");
                    return;
                }

                Collections.shuffle(allData);
                List<ExportData> sample = allData.stream().limit(5).collect(Collectors.toList());

                System.out.println("\n📋 ÉCHANTILLON ALÉATOIRE:");
                for (int i = 0; i < sample.size(); i++) {
                    ExportData data = sample.get(i);
                    System.out.printf("%d. %s - %s - %.2fTND/tonne%n",
                            i+1, data.date(), data.productType().getFrenchName(), data.pricePerTon());
                }

                System.out.print("\nAnalyser cet échantillon? (o/n): ");
                String analyzeChoice = scanner.nextLine();

                if (analyzeChoice.equalsIgnoreCase("o")) {
                    makePredictionsOnData(sample);
                }

            } catch (Exception e) {
                System.out.println("❌ Erreur: " + e.getMessage());
            }
        } else {
            System.out.println("\nCette fonctionnalité sera implémentée ultérieurement.");
            System.out.println("Utilisez l'option 2 pour analyser un échantillon du dataset.");
        }
    }

    private static void generateMarketReport() {
        System.out.println("\n=== GÉNÉRATION DE RAPPORT DE MARCHÉ ===");
        System.out.println("1. Générer un rapport rapide");
        System.out.println("2. Générer un rapport détaillé");
        System.out.print("Votre choix: ");

        int choice = readIntInput("");

        if (choice == 2) {
            generateDetailedReport();
        } else {
            // Générer un rapport rapide
            try {
                List<ExportData> reportData = loadCSVFile("exports_training.csv");
                if (reportData.size() > 20) {
                    reportData = reportData.subList(0, 20); // Limiter à 20 enregistrements
                }

                var predictions = intelligenceService.analyzeExports(reportData);
                var report = intelligenceService.generateIntelligenceReport(predictions);

                System.out.println("\n" + "=".repeat(60));
                System.out.println("📋 RAPPORT RAPIDE DE MARCHÉ");
                System.out.println("=".repeat(60));
                System.out.println(report);

                System.out.print("\n💾 Sauvegarder ce rapport? (o/n): ");
                String saveChoice = scanner.nextLine();
                if (saveChoice.equalsIgnoreCase("o")) {
                    String fileName = "rapport_marche_" + LocalDate.now() + ".txt";
                    java.nio.file.Files.writeString(
                            java.nio.file.Paths.get(fileName),
                            report
                    );
                    System.out.println("✅ Rapport sauvegardé dans: " + fileName);
                }

            } catch (Exception e) {
                System.out.println("❌ Erreur lors de la génération du rapport: " + e.getMessage());
                System.out.println("Essayer de générer un rapport détaillé à la place...");
                generateDetailedReport();
            }
        }
    }

    // === MÉTHODES UTILITAIRES ===

    private static List<PricePrediction> generateDemoPredictions(List<ExportData> exports) {
        return exports.stream()
                .limit(10)
                .map(export -> new PricePrediction(
                        LocalDate.now().plusDays(30),
                        export.productType(),
                        export.pricePerTon() * (0.9 + Math.random() * 0.2),
                        0.85,
                        "Modèle de démonstration",
                        PredictionStatus.COMPLETED
                ))
                .collect(Collectors.toList());
    }

    private static List<PricePrediction> generateDemoPredictions(int count) {
        List<PricePrediction> predictions = new ArrayList<>();
        ProductType[] products = ProductType.values();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            ProductType product = products[random.nextInt(products.length)];
            predictions.add(new PricePrediction(
                    LocalDate.now().plusDays(random.nextInt(30)),
                    product,
                    1000 + random.nextDouble() * 3000,
                    0.7 + random.nextDouble() * 0.3,
                    "DemoModel",
                    PredictionStatus.COMPLETED
            ));
        }
        return predictions;
    }

    private static int readIntInput(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            LOGGER.warning("Entrée invalide. Utilisation de la valeur par défaut 0.");
            return 0;
        }
    }

    private static double readDoubleInput(String prompt) {
        System.out.print(prompt);
        try {
            return Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException e) {
            LOGGER.warning("Entrée invalide. Utilisation de la valeur par défaut 0.0.");
            return 0.0;
        }
    }

    private static void displayPredictions(List<PricePrediction> predictions) {
        if (predictions.isEmpty()) {
            System.out.println("Aucune prédiction à afficher.");
            return;
        }

        System.out.println("\n=== RÉSULTATS DES PRÉDICTIONS ===");
        System.out.printf("%-5s %-15s %-12s %-10s %-15s%n",
                "N°", "Produit", "Prix Prédit", "Confiance", "Statut");
        System.out.println("-".repeat(60));

        for (int i = 0; i < predictions.size(); i++) {
            var pred = predictions.get(i);
            System.out.printf("%-5d %-15s %-12.2f %-10.2f %-15s%n",
                    i + 1,
                    pred.productType().getFrenchName(),
                    pred.predictedPrice(),
                    pred.confidence() * 100,
                    pred.status()
            );
        }
    }

    private static void displayStatistics(List<PricePrediction> predictions) {
        if (predictions.isEmpty()) {
            System.out.println("Aucune statistique disponible.");
            return;
        }

        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);

        double maxPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .max()
                .orElse(0.0);

        double minPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .min()
                .orElse(0.0);

        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0.0);

        System.out.println("\n=== STATISTIQUES ===");
        System.out.printf("Nombre de prédictions: %d%n", predictions.size());
        System.out.printf("Prix moyen prédit: %.2f TND/tonne%n", avgPrice);
        System.out.printf("Prix maximum: %.2f TND/tonne%n", maxPrice);
        System.out.printf("Prix minimum: %.2f TND/tonne%n", minPrice);
        System.out.printf("Confiance moyenne: %.2f%%%n", avgConfidence * 100);

        var productCount = predictions.stream()
                .collect(Collectors.groupingBy(
                        PricePrediction::productType,
                        Collectors.counting()
                ));

        System.out.println("\nDistribution par produit:");
        productCount.forEach((product, count) ->
                System.out.printf("  • %s: %d prédictions%n", product.getFrenchName(), count)
        );
    }

    private static void displaySystemInfo() {
        System.out.println("\n=== INFORMATIONS SYSTÈME ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));
        System.out.println("Processeurs disponibles: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Mémoire totale: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
        System.out.println("Mémoire libre: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");

        // Informations sur les datasets
        System.out.println("\n=== INFORMATIONS DATASETS ===");

        String[] files = {"exports_historical.csv", "exports_training.csv", "exports_test.csv"};
        for (String file : files) {
            try {
                List<ExportData> data = loadCSVFile(file);
                System.out.printf("%-25s: %,d enregistrements%n", file, data.size());

                if (!data.isEmpty()) {
                    LocalDate minDate = data.stream()
                            .map(ExportData::date)
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());
                    LocalDate maxDate = data.stream()
                            .map(ExportData::date)
                            .max(LocalDate::compareTo)
                            .orElse(LocalDate.now());
                    System.out.printf("  Période: %s à %s%n", minDate, maxDate);
                }
            } catch (Exception e) {
                System.out.printf("%-25s: NON TROUVÉ%n", file);
            }
        }
    }
}