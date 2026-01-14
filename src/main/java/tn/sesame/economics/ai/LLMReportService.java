package tn.sesame.economics.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ProductType;
import tn.sesame.economics.service.ReportGenerator;
import tn.sesame.economics.exception.ModelException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Service de génération de rapports utilisant des modèles de langage (LLM).
 * Intègre LangChain4j pour générer des analyses de marché intelligentes.
 *
 * @since Java 25
 */
public class LLMReportService implements ReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(LLMReportService.class.getName());
    private final ChatLanguageModel chatModel;
    private final boolean useLocalModel;

    /**
     * Constructeur du service LLM.
     *
     * @param useLocalModel true pour utiliser Ollama (local), false pour OpenAI
     * @throws ModelException si l'initialisation échoue
     */
    public LLMReportService(boolean useLocalModel) throws ModelException {
        this.useLocalModel = useLocalModel;

        try {
            if (useLocalModel) {
                LOGGER.info("Initialisation d'Ollama pour LLM local...");
                chatModel = OllamaChatModel.builder()
                        .baseUrl("http://localhost:11434")
                        .modelName("llama2")
                        .temperature(0.7)
                        // .maxTokens() n'est pas disponible dans cette version
                        // Utilisez plutôt .maxRetries() ou laissez par défaut
                        .timeout(java.time.Duration.ofSeconds(60))
                        .build();
                LOGGER.info("Ollama initialisé avec le modèle llama2");
            } else {
                LOGGER.info("Initialisation d'OpenAI...");
                String apiKey = System.getenv("OPENAI_API_KEY");
                if (apiKey == null || apiKey.isBlank()) {
                    throw new ModelException("Clé API OpenAI non trouvée. Définissez la variable OPENAI_API_KEY.");
                }
                chatModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-3.5-turbo")
                        .temperature(0.7)
                        .timeout(java.time.Duration.ofSeconds(60))
                        .build();
                LOGGER.info("OpenAI initialisé avec gpt-3.5-turbo");
            }
        } catch (Exception e) {
            throw new ModelException("Échec de l'initialisation du service LLM", e);
        }
    }

    /**
     * Génère un rapport complet d'analyse de marché.
     *
     * @param predictions Les prédictions à analyser
     * @return Rapport d'analyse de marché formaté
     */
    @Override
    public String generateMarketReport(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            LOGGER.warning("Aucune prédiction fournie pour le rapport");
            return "Aucune donnée disponible pour générer un rapport.";
        }

        try {
            LOGGER.info("Génération du rapport de marché pour " + predictions.size() + " prédictions...");

            // Préparation des données pour le prompt
            String dataSummary = prepareDataSummary(predictions);
            String trendAnalysis = analyzeTrends(predictions);

            // Construction du prompt en français
            String prompt = buildMarketReportPrompt(dataSummary, trendAnalysis);

            // Génération via LLM
            LOGGER.fine("Envoi du prompt au LLM...");
            String report = chatModel.generate(prompt);

            LOGGER.info("Rapport généré avec succès");
            return formatReport(report);

        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la génération du rapport: " + e.getMessage());
            return generateFallbackReport(predictions);
        }
    }

    /**
     * Génère un résumé exécutif des prédictions.
     *
     * @param predictions Les prédictions à résumer
     * @return Résumé exécutif formaté
     */
    @Override
    public String generateSummaryReport(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return "Aucune prédiction à résumer.";
        }

        try {
            LOGGER.info("Génération du résumé exécutif...");

            String dataSummary = prepareDataSummary(predictions);

            String prompt = String.format(
                    "En tant qu'analyste économique spécialisé dans les exportations agricoles tunisiennes, " +
                            "génère un résumé exécutif concis (3-4 phrases maximum) basé sur les prédictions suivantes:\n\n" +
                            "%s\n\n" +
                            "Le résumé doit:\n" +
                            "1. Identifier la tendance générale des prix\n" +
                            "2. Mentionner les produits les plus prometteurs\n" +
                            "3. Donner une recommandation stratégique principale\n" +
                            "Format: Texte concis et professionnel en français.",
                    dataSummary
            );

            String summary = chatModel.generate(prompt);
            LOGGER.info("Résumé exécutif généré avec succès");

            return formatSummary(summary);

        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la génération du résumé: " + e.getMessage());
            return generateSimpleSummary(predictions);
        }
    }

    /**
     * Prépare un résumé structuré des données de prédiction.
     *
     * @param predictions Liste des prédictions
     * @return Résumé formaté des données
     */
    private String prepareDataSummary(List<PricePrediction> predictions) {
        StringBuilder summary = new StringBuilder();
        summary.append("PRÉDICTIONS DE PRIX DES EXPORTATIONS AGRICOLES TUNISIENNES\n");
        summary.append("=".repeat(60)).append("\n\n");

        // Groupement par type de produit
        var byProduct = predictions.stream()
                .collect(Collectors.groupingBy(PricePrediction::productType));

        for (var entry : byProduct.entrySet()) {
            var product = entry.getKey();
            var productPredictions = entry.getValue();

            double avgPrice = productPredictions.stream()
                    .mapToDouble(PricePrediction::predictedPrice)
                    .average()
                    .orElse(0.0);

            double avgConfidence = productPredictions.stream()
                    .mapToDouble(PricePrediction::confidence)
                    .average()
                    .orElse(0.0);

            summary.append(String.format("• %s:\n", product.getFrenchName()));
            summary.append(String.format("  - Prix moyen prédit: %.2f TND/tonne\n", avgPrice));
            summary.append(String.format("  - Confiance moyenne: %.1f%%\n", avgConfidence * 100));
            summary.append(String.format("  - Nombre de prédictions: %d\n", productPredictions.size()));

            // Détail des prédictions individuelles
            for (int i = 0; i < Math.min(3, productPredictions.size()); i++) {
                var pred = productPredictions.get(i);
                summary.append(String.format("    * Prédiction %d: %.2f TND (confiance: %.1f%%)\n",
                        i + 1, pred.predictedPrice(), pred.confidence() * 100));
            }
            summary.append("\n");
        }

        // Statistiques globales
        double globalAvgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);

        long highConfidenceCount = predictions.stream()
                .filter(p -> p.confidence() >= 0.8)
                .count();

        summary.append("STATISTIQUES GLOBALES:\n");
        summary.append(String.format("- Prix moyen global: %.2f TND/tonne\n", globalAvgPrice));
        summary.append(String.format("- Prédictions à haute confiance: %d/%d (%.1f%%)\n",
                highConfidenceCount, predictions.size(),
                (highConfidenceCount * 100.0 / predictions.size())));
        summary.append(String.format("- Nombre total de prédictions: %d\n", predictions.size()));

        return summary.toString();
    }

    /**
     * Analyse les tendances dans les prédictions.
     *
     * @param predictions Liste des prédictions
     * @return Analyse des tendances
     */
    private String analyzeTrends(List<PricePrediction> predictions) {
        if (predictions.size() < 2) {
            return "Données insuffisantes pour l'analyse des tendances.";
        }

        // Analyse simple des tendances
        var byProduct = predictions.stream()
                .collect(Collectors.groupingBy(PricePrediction::productType));

        StringBuilder trends = new StringBuilder("ANALYSE DES TENDANCES:\n");

        for (var entry : byProduct.entrySet()) {
            var productPredictions = entry.getValue();
            if (productPredictions.size() >= 2) {
                // Calcul de la variation moyenne
                double firstPrice = productPredictions.get(0).predictedPrice();
                double lastPrice = productPredictions.get(productPredictions.size() - 1).predictedPrice();
                double variation = ((lastPrice - firstPrice) / firstPrice) * 100;

                String trend;
                if (variation > 5) trend = "FORTE HAUSSE";
                else if (variation > 2) trend = "HAUSSE MODÉRÉE";
                else if (variation < -5) trend = "FORTE BAISSE";
                else if (variation < -2) trend = "BAISSE MODÉRÉE";
                else trend = "STABLE";

                trends.append(String.format("• %s: %.1f%% (%s)\n",
                        entry.getKey().getFrenchName(), variation, trend));
            }
        }

        return trends.toString();
    }

    /**
     * Construit le prompt pour le rapport de marché.
     *
     * @param dataSummary Résumé des données
     * @param trendAnalysis Analyse des tendances
     * @return Prompt complet
     */
    private String buildMarketReportPrompt(String dataSummary, String trendAnalysis) {
        return String.format(
                "En tant qu'expert en intelligence économique spécialisé dans l'agriculture tunisienne, " +
                        "analyse les données de prédiction suivantes et génère un rapport détaillé.\n\n" +
                        "CONTEXTE: Ces prédictions concernent les exportations agricoles tunisiennes (huile d'olive, " +
                        "dattes, agrumes, blé, tomates, piments) vers les marchés internationaux.\n\n" +
                        "DONNÉES DE PRÉDICTION:\n%s\n\n" +
                        "ANALYSE DES TENDANCES:\n%s\n\n" +
                        "STRUCTURE DU RAPPORT (en français):\n" +
                        "1. SYNOPSIS EXÉCUTIF (3-4 phrases résumant les conclusions principales)\n" +
                        "2. ANALYSE DES MARCHÉS PAR PRODUIT\n" +
                        "   - Pour chaque produit: tendance, opportunités, risques spécifiques\n" +
                        "3. RECOMMANDATIONS STRATÉGIQUES\n" +
                        "   - Actions prioritaires pour les exportateurs\n" +
                        "   - Timing optimal pour les ventes\n" +
                        "   - Stratégies de diversification\n" +
                        "4. RISQUES IDENTIFIÉS\n" +
                        "   - Risques de marché\n" +
                        "   - Facteurs externes (climat, géopolitique, etc.)\n" +
                        "   - Mesures d'atténuation\n" +
                        "5. PERSPECTIVES À COURT ET MOYEN TERME\n" +
                        "   - Projections pour les 3-6 prochains mois\n" +
                        "   - Scénarios optimistes/pessimistes\n\n" +
                        "STYLE: Professionnel, basé sur les données, orienté vers l'action. " +
                        "Utilise des termes économiques appropriés mais accessibles aux décideurs.",
                dataSummary, trendAnalysis
        );
    }

    /**
     * Formate le rapport généré.
     *
     * @param rawReport Rapport brut du LLM
     * @return Rapport formaté
     */
    private String formatReport(String rawReport) {
        return "=".repeat(80) + "\n" +
                "RAPPORT D'INTELLIGENCE DE MARCHÉ - EXPORTATIONS AGRICOLES TUNISIENNES\n" +
                "Date: " + java.time.LocalDate.now() + "\n" +
                "=".repeat(80) + "\n\n" +
                rawReport + "\n\n" +
                "-".repeat(80) + "\n" +
                "Ce rapport a été généré automatiquement par le système d'intelligence économique.\n" +
                "Les prédictions sont basées sur des modèles d'IA et doivent être validées par des experts.";
    }

    /**
     * Formate le résumé exécutif.
     *
     * @param rawSummary Résumé brut du LLM
     * @return Résumé formaté
     */
    private String formatSummary(String rawSummary) {
        return "RÉSUMÉ EXÉCUTIF\n" +
                "Date: " + java.time.LocalDate.now() + "\n" +
                "-".repeat(50) + "\n" +
                rawSummary.trim();
    }

    /**
     * Génère un rapport de secours en cas d'échec du LLM.
     *
     * @param predictions Liste des prédictions
     * @return Rapport de secours basique
     */
    private String generateFallbackReport(List<PricePrediction> predictions) {
        LOGGER.warning("Génération d'un rapport de secours...");

        StringBuilder report = new StringBuilder();
        report.append("RAPPORT DE SECOURS - ANALYSE MANUELLE\n");
        report.append("=".repeat(60)).append("\n\n");

        // Analyse basique
        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);

        var bestProduct = predictions.stream()
                .max(java.util.Comparator.comparingDouble(PricePrediction::predictedPrice))
                .map(PricePrediction::productType)
                .orElse(tn.sesame.economics.model.ProductType.OLIVE_OIL);  // Correction ici

        var worstProduct = predictions.stream()
                .min(java.util.Comparator.comparingDouble(PricePrediction::predictedPrice))
                .map(PricePrediction::productType)
                .orElse(tn.sesame.economics.model.ProductType.WHEAT);  // Correction ici

        report.append("ANALYSE BASIQUE:\n");
        report.append(String.format("- Prix moyen prédit: %.2f TND/tonne\n", avgPrice));
        report.append(String.format("- Produit le plus prometteur: %s\n", bestProduct.getFrenchName()));
        report.append(String.format("- Produit nécessitant attention: %s\n", worstProduct.getFrenchName()));
        report.append(String.format("- Nombre de prédictions analysées: %d\n\n", predictions.size()));

        report.append("RECOMMANDATIONS GÉNÉRIQUES:\n");
        report.append("1. Surveiller les prix de ").append(bestProduct.getFrenchName()).append("\n");
        report.append("2. Diversifier les exportations\n");
        report.append("3. Consulter un expert pour validation détaillée\n");

        return report.toString();
    }

    /**
     * Génère un résumé simple en cas d'échec du LLM.
     *
     * @param predictions Liste des prédictions
     * @return Résumé simple
     */
    private String generateSimpleSummary(List<PricePrediction> predictions) {
        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);

        return String.format(
                "Résumé basique: %d prédictions avec un prix moyen de %.2f TND/tonne. " +
                        "Consultez le rapport complet pour l'analyse détaillée.",
                predictions.size(), avgPrice
        );
    }

    /**
     * Vérifie si le service LLM est disponible.
     *
     * @return true si le service est opérationnel
     */
    public boolean isAvailable() {
        try {
            // Test simple de connexion
            chatModel.generate("Test de connexion - ignorez ce message.");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Service LLM non disponible: " + e.getMessage());
            return false;
        }
    }
}