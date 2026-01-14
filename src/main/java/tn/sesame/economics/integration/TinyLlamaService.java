package tn.sesame.economics.integration;

import tn.sesame.economics.model.*;
import tn.sesame.economics.service.ReportGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service d'intégration avec TinyLlama via Ollama.
 * Fournit des fonctionnalités de génération de rapports en langage naturel.
 */
public class TinyLlamaService implements ReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(TinyLlamaService.class.getName());
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "tinyllama";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TinyLlamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateReport(List<PricePrediction> predictions, ReportFormat format) {
        try {
            String prompt = createAnalysisPrompt(predictions, format);
            String rawResponse = callOllamaAPI(prompt);

            // Format the response based on the requested format
            return formatResponse(rawResponse, format);

        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la génération du rapport: " + e.getMessage());
            return createFallbackReport(predictions, format);
        }
    }

    @Override
    public String generateMarketReport(List<PricePrediction> predictions) {
        String prompt = createMarketAnalysisPrompt(predictions);
        try {
            String response = callOllamaAPI(prompt);
            return "📊 RAPPORT DE MARCHÉ\n" +
                    "=".repeat(40) + "\n\n" +
                    response;
        } catch (Exception e) {
            return createFallbackMarketReport(predictions);
        }
    }

    @Override
    public String generateSummaryReport(List<PricePrediction> predictions) {
        String prompt = createExecutiveSummaryPrompt(predictions);
        try {
            String response = callOllamaAPI(prompt);
            return "📈 RÉSUMÉ EXÉCUTIF\n" +
                    "=".repeat(40) + "\n\n" +
                    response;
        } catch (Exception e) {
            return createFallbackSummary(predictions);
        }
    }

    /**
     * Teste la connexion à Ollama.
     */
    public String testConnection() {
        try {
            String testPrompt = "Réponds simplement '✅ Connecté' si tu reçois ce message.";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                                    MODEL_NAME, testPrompt)
                    ))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return "✅ Connecté à Ollama/TinyLlama";
            } else {
                return "❌ Échec de connexion. Code: " + response.statusCode();
            }
        } catch (Exception e) {
            return "❌ Erreur de connexion: " + e.getMessage() +
                    "\nVérifiez qu'Ollama est installé et démarré (ollama serve)";
        }
    }

    /**
     * Vérifie si Ollama est disponible.
     */
    public boolean isOllamaAvailable() {
        try {
            String result = testConnection();
            return result.contains("✅");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Appelle l'API Ollama avec un prompt.
     */
    private String callOllamaAPI(String prompt) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error: " + response.statusCode() + " - " + response.body());
        }

        // Parse the JSON response
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.body());
        return responseJson.get("response").asText();
    }

    /**
     * Crée un prompt d'analyse basé sur les prédictions.
     */
    private String createAnalysisPrompt(List<PricePrediction> predictions, ReportFormat format) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un analyste économique spécialisé dans les exportations tunisiennes. ");
        prompt.append("Analyse les données suivantes et fournis un rapport clair et concis.\n\n");

        prompt.append("Données à analyser:\n");
        for (int i = 0; i < predictions.size(); i++) {
            PricePrediction p = predictions.get(i);
            prompt.append(String.format("%d. %s - Prix prédit: %.2f TND/tonne - Confiance: %.1f%%\n",
                    i + 1, p.productType().getFrenchName(), p.predictedPrice(), p.confidence() * 100));
        }

        prompt.append("\nFormat de sortie: ");
        if (format == ReportFormat.MARKDOWN) {
            prompt.append("Utilise Markdown avec titres, listes et mise en forme.");
        } else if (format == ReportFormat.HTML) {
            prompt.append("HTML simple sans CSS.");
        } else {
            // Default/TEXT format
            prompt.append("Texte simple bien structuré.");
        }

        prompt.append("\n\nStructure le rapport avec:\n");
        prompt.append("1. Introduction et contexte\n");
        prompt.append("2. Tendances principales identifiées\n");
        prompt.append("3. Produits les plus prometteurs\n");
        prompt.append("4. Recommandations stratégiques\n");
        prompt.append("5. Perspectives et risques\n");

        return prompt.toString();
    }

    /**
     * Crée un prompt spécifique pour l'analyse de marché.
     */
    private String createMarketAnalysisPrompt(List<PricePrediction> predictions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un analyste de marché spécialisé dans l'agroalimentaire tunisien. ");
        prompt.append("Fournis une analyse de marché basée sur ces prédictions de prix.\n\n");

        prompt.append("Données de prédiction:\n");
        double totalValue = 0;
        for (PricePrediction p : predictions) {
            prompt.append(String.format("- %s: %.2f TND/tonne (confiance: %.1f%%)\n",
                    p.productType().getFrenchName(), p.predictedPrice(), p.confidence() * 100));
            totalValue += p.predictedPrice();
        }

        double avgPrice = totalValue / predictions.size();
        prompt.append(String.format("\nPrix moyen prédit: %.2f TND/tonne\n", avgPrice));

        prompt.append("\nFournis une analyse qui comprend:\n");
        prompt.append("- La situation actuelle du marché\n");
        prompt.append("- Les opportunités d'exportation\n");
        prompt.append("- Les produits les plus compétitifs\n");
        prompt.append("- Des conseils pour les exportateurs\n");
        prompt.append("- Les défis potentiels à anticiper\n");

        return prompt.toString();
    }

    /**
     * Crée un prompt pour un résumé exécutif.
     */
    private String createExecutiveSummaryPrompt(List<PricePrediction> predictions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un consultant économique pour le gouvernement tunisien. ");
        prompt.append("Rédige un résumé exécutif pour des décideurs à partir de ces prédictions.\n\n");

        prompt.append("Données clés:\n");
        for (PricePrediction p : predictions) {
            prompt.append(String.format("• %s devrait se négocier autour de %.2f TND/tonne\n",
                    p.productType().getFrenchName(), p.predictedPrice()));
        }

        prompt.append("\nLe résumé doit être:\n");
        prompt.append("- Très concis (max 10 lignes)\n");
        prompt.append("- Orienté action et décision\n");
        prompt.append("- Avec des points clés en gras\n");
        prompt.append("- Avec une recommandation claire à la fin\n");

        prompt.append("\nStructure:\n");
        prompt.append("1. Contexte en une phrase\n");
        prompt.append("2. 3 points clés principaux\n");
        prompt.append("3. Recommandation stratégique\n");

        return prompt.toString();
    }

    /**
     * Formate la réponse selon le format demandé.
     */
    private String formatResponse(String response, ReportFormat format) {
        if (format == ReportFormat.MARKDOWN) {
            return "## 📋 Rapport d'Analyse Économique\n\n" + response;
        } else if (format == ReportFormat.HTML) {
            return "<html><body><h1>Rapport d'Analyse Économique</h1><p>" +
                    response.replace("\n", "<br>") + "</p></body></html>";
        } else {
            return "RAPPORT D'ANALYSE ÉCONOMIQUE\n" +
                    "=".repeat(40) + "\n\n" + response;
        }
    }

    /**
     * Crée un rapport de secours en cas d'erreur.
     */
    private String createFallbackReport(List<PricePrediction> predictions, ReportFormat format) {
        StringBuilder report = new StringBuilder();

        if (format == ReportFormat.MARKDOWN) {
            report.append("## ⚠️ Rapport Généré Localement\n\n");
            report.append("*(Ollama non disponible)*\n\n");
        } else if (format == ReportFormat.HTML) {
            report.append("<html><body><h1>⚠️ Rapport Généré Localement</h1>");
            report.append("<p><em>(Ollama non disponible)</em></p>");
        } else {
            report.append("⚠️ RAPPORT GÉNÉRÉ LOCALEMENT\n");
            report.append("(Ollama non disponible)\n\n");
        }

        report.append("Analyse basée sur ").append(predictions.size()).append(" prédictions:\n\n");

        for (PricePrediction p : predictions) {
            if (format == ReportFormat.MARKDOWN) {
                report.append(String.format("- **%s**: %.2f TND/tonne (confiance: %.1f%%)\n",
                        p.productType().getFrenchName(), p.predictedPrice(), p.confidence() * 100));
            } else if (format == ReportFormat.HTML) {
                report.append(String.format("<p><strong>%s</strong>: %.2f TND/tonne (confiance: %.1f%%)</p>\n",
                        p.productType().getFrenchName(), p.predictedPrice(), p.confidence() * 100));
            } else {
                report.append(String.format("• %s: %.2f TND/tonne (confiance: %.1f%%)\n",
                        p.productType().getFrenchName(), p.predictedPrice(), p.confidence() * 100));
            }
        }

        if (format == ReportFormat.HTML) {
            report.append("</body></html>");
        }

        return report.toString();
    }

    private String createFallbackMarketReport(List<PricePrediction> predictions) {
        StringBuilder report = new StringBuilder();
        report.append("📊 RAPPORT DE MARCHÉ (LOCAL)\n");
        report.append("=".repeat(40) + "\n\n");
        report.append("Basé sur ").append(predictions.size()).append(" prédictions:\n\n");

        predictions.forEach(p ->
                report.append(String.format("• %s: %.2f TND/tonne\n",
                        p.productType().getFrenchName(), p.predictedPrice()))
        );

        return report.toString();
    }

    private String createFallbackSummary(List<PricePrediction> predictions) {
        return "📈 RÉSUMÉ EXÉCUTIF (LOCAL)\n" +
                "=".repeat(40) + "\n\n" +
                "Les prédictions indiquent des prix stables pour les exportations tunisiennes.\n" +
                "Produits analysés: " + predictions.size() + "\n" +
                "Prix moyen: " + String.format("%.2f",
                predictions.stream().mapToDouble(PricePrediction::predictedPrice).average().orElse(0)) +
                " TND/tonne\n\n" +
                "Recommandation: Maintenir la qualité et explorer de nouveaux marchés.";
    }
}