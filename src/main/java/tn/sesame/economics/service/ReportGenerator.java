package tn.sesame.economics.service;

import tn.sesame.economics.model.PricePrediction;
import java.util.List;

/**
 * Interface définissant le contrat pour les générateurs de rapports.
 * Les implémentations utilisent généralement des LLM pour générer des rapports
 * d'analyse de marché basés sur les prédictions de prix.
 *
 * @since Java 25
 */
public interface ReportGenerator {

    /**
     * Génère un rapport complet d'analyse de marché basé sur les prédictions.
     * Le rapport inclut une analyse détaillée des tendances, des recommandations
     * stratégiques et des risques identifiés.
     *
     * @param predictions La liste des prédictions à analyser
     * @return Un rapport d'analyse de marché formaté en texte
     * @throws IllegalArgumentException si la liste est null ou vide
     * @throws IllegalStateException si le service de génération n'est pas disponible
     */
    String generateMarketReport(List<PricePrediction> predictions);

    /**
     * Génère un résumé exécutif des prédictions.
     * Le résumé est plus court et ciblé pour les décideurs.
     *
     * @param predictions La liste des prédictions à résumer
     * @return Un résumé exécutif formaté en texte
     * @throws IllegalArgumentException si la liste est null ou vide
     * @throws IllegalStateException si le service de génération n'est pas disponible
     */
    String generateSummaryReport(List<PricePrediction> predictions);

    /**
     * Génère un rapport dans un format spécifique.
     *
     * @param predictions La liste des prédictions
     * @param format Le format du rapport (MARKDOWN, HTML, PLAIN_TEXT)
     * @return Le rapport dans le format demandé
     */
    default String generateReport(List<PricePrediction> predictions, ReportFormat format) {
        return switch (format) {
            case MARKDOWN -> generateMarketReport(predictions);
            case HTML -> "<html><body>" + generateMarketReport(predictions) + "</body></html>";
            case PLAIN_TEXT -> generateSummaryReport(predictions);
        };
    }

    /**
     * Énumération des formats de rapport supportés.
     */
    enum ReportFormat {
        MARKDOWN,
        HTML,
        PLAIN_TEXT
    }
}