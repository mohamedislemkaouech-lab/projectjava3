package tn.sesame.economics.exception;

/**
 * Classe de base pour toutes les exceptions du système d'intelligence économique.
 * Cette classe fournit une hiérarchie cohérente pour la gestion des erreurs
 * dans l'application d'analyse économique.
 *
 * @since Java 25
 */
public class EconomicIntelligenceException extends Exception {

    /**
     * Construit une nouvelle exception avec le message spécifié.
     *
     * @param message Le message détaillant l'erreur
     */
    public EconomicIntelligenceException(String message) {
        super(message);
    }

    /**
     * Construit une nouvelle exception avec le message et la cause spécifiés.
     *
     * @param message Le message détaillant l'erreur
     * @param cause La cause sous-jacente de l'exception
     */
    public EconomicIntelligenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construit une nouvelle exception avec la cause spécifiée.
     * Le message est automatiquement généré à partir de la cause.
     *
     * @param cause La cause sous-jacente de l'exception
     */
    public EconomicIntelligenceException(Throwable cause) {
        super(cause);
    }

    /**
     * Retourne le contexte de l'erreur.
     * Peut être étendu par les sous-classes pour fournir plus de contexte.
     *
     * @return Le contexte de l'erreur ou une chaîne vide
     */
    public String getErrorContext() {
        return "Erreur dans le système d'intelligence économique";
    }
}