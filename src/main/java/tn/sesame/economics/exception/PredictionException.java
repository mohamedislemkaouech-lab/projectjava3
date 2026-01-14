package tn.sesame.economics.exception;

/**
 * Exception levée lors des erreurs pendant le processus de prédiction
 * ou d'inférence des modèles d'IA.
 *
 * @since Java 25
 */
public class PredictionException extends EconomicIntelligenceException {

    /**
     * Construit une nouvelle exception avec le message spécifié.
     *
     * @param message Le message détaillant l'erreur de prédiction
     */
    public PredictionException(String message) {
        super(message);
    }

    /**
     * Construit une nouvelle exception avec le message et la cause spécifiés.
     *
     * @param message Le message détaillant l'erreur de prédiction
     * @param cause La cause sous-jacente de l'exception
     */
    public PredictionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construit une nouvelle exception avec la cause spécifiée.
     *
     * @param cause La cause sous-jacente de l'exception
     */
    public PredictionException(Throwable cause) {
        super(cause);
    }

    /**
     * Retourne le contexte spécifique aux erreurs de prédiction.
     *
     * @return Le contexte de l'erreur de prédiction
     */
    @Override
    public String getErrorContext() {
        return "Erreur de prédiction: " + getMessage();
    }

    /**
     * Crée une exception pour une prédiction avec des données d'entrée invalides.
     *
     * @param inputType Le type de données d'entrée
     * @param reason La raison de l'invalidité
     * @return Une nouvelle PredictionException
     */
    public static PredictionException invalidInput(String inputType, String reason) {
        return new PredictionException(
                String.format("Données d'entrée invalides pour %s: %s", inputType, reason)
        );
    }

    /**
     * Crée une exception pour un timeout de prédiction.
     *
     * @param timeoutMs Le timeout en millisecondes
     * @return Une nouvelle PredictionException
     */
    public static PredictionException timeout(long timeoutMs) {
        return new PredictionException(
                String.format("La prédiction a dépassé le timeout de %d ms", timeoutMs)
        );
    }

    /**
     * Crée une exception pour une prédiction avec une confiance trop faible.
     *
     * @param actualConfidence La confiance actuelle
     * @param minConfidence La confiance minimum requise
     * @return Une nouvelle PredictionException
     */
    public static PredictionException lowConfidence(double actualConfidence, double minConfidence) {
        return new PredictionException(
                String.format("Confidence trop faible: %.2f (minimum requis: %.2f)", actualConfidence, minConfidence)
        );
    }
}