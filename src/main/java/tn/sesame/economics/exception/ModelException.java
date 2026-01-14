package tn.sesame.economics.exception;

/**
 * Exception levée lors des erreurs de chargement, de configuration
 * ou d'initialisation des modèles d'IA.
 *
 * @since Java 25
 */
public class ModelException extends EconomicIntelligenceException {

    /**
     * Construit une nouvelle exception avec le message spécifié.
     *
     * @param message Le message détaillant l'erreur du modèle
     */
    public ModelException(String message) {
        super(message);
    }

    /**
     * Construit une nouvelle exception avec le message et la cause spécifiés.
     *
     * @param message Le message détaillant l'erreur du modèle
     * @param cause La cause sous-jacente de l'exception
     */
    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construit une nouvelle exception avec la cause spécifiée.
     *
     * @param cause La cause sous-jacente de l'exception
     */
    public ModelException(Throwable cause) {
        super(cause);
    }

    /**
     * Retourne le contexte spécifique aux erreurs de modèle.
     *
     * @return Le contexte de l'erreur du modèle
     */
    @Override
    public String getErrorContext() {
        return "Erreur de modèle d'IA: " + getMessage();
    }

    /**
     * Crée une exception pour un modèle qui n'a pas pu être chargé.
     *
     * @param modelName Le nom du modèle qui a échoué au chargement
     * @return Une nouvelle ModelException
     */
    public static ModelException modelLoadingFailed(String modelName) {
        return new ModelException(
                String.format("Le modèle '%s' n'a pas pu être chargé. Vérifiez le chemin et les dépendances.", modelName)
        );
    }

    /**
     * Crée une exception pour un modèle avec une version incompatible.
     *
     * @param expectedVersion La version attendue
     * @param actualVersion La version actuelle
     * @return Une nouvelle ModelException
     */
    public static ModelException incompatibleVersion(String expectedVersion, String actualVersion) {
        return new ModelException(
                String.format("Version incompatible: attendue %s, obtenue %s", expectedVersion, actualVersion)
        );
    }
}