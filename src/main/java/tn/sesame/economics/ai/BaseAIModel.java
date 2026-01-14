package tn.sesame.economics.ai;

import tn.sesame.economics.annotation.AIService;
import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.service.PredictionService;
import tn.sesame.economics.exception.ModelException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Classe abstraite de base pour tous les modèles d'IA de prédiction.
 * Définit le cycle de vie commun et les fonctionnalités de base
 * pour les services de prédiction utilisant différentes bibliothèques d'IA.
 *
 * @since Java 25
 */
@AIService(provider = "Base", version = "1.0")
public abstract class BaseAIModel implements PredictionService {

    /**
     * Logger partagé pour toutes les sous-classes.
     */
    protected static final Logger LOGGER = Logger.getLogger(BaseAIModel.class.getName());

    /**
     * Nom du modèle d'IA.
     */
    protected String modelName;

    /**
     * Indicateur de chargement du modèle.
     */
    protected boolean isLoaded = false;

    /**
     * Constructeur de la classe de base.
     *
     * @param modelName Le nom du modèle d'IA
     * @throws IllegalArgumentException si modelName est null ou vide
     */
    public BaseAIModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Le nom du modèle ne peut pas être null ou vide");
        }
        this.modelName = modelName;
        LOGGER.info(String.format("Modèle %s initialisé (non chargé)", modelName));
    }

    /**
     * Charge le modèle d'IA en mémoire.
     * Cette méthode doit être implémentée par les sous-classes spécifiques.
     *
     * @throws ModelException si le chargement échoue
     */
    public abstract void loadModel() throws ModelException;

    /**
     * Décharge le modèle d'IA de la mémoire.
     * Cette méthode doit libérer toutes les ressources associées au modèle.
     *
     * @throws ModelException si le déchargement échoue
     */
    public abstract void unloadModel() throws ModelException;

    /**
     * Valide les données d'entrée avant la prédiction.
     *
     * @param input Les données d'exportation à valider
     * @throws IllegalArgumentException si l'entrée est null
     * @throws IllegalStateException si le modèle n'est pas chargé
     */
    protected void validateInput(ExportData input) {
        if (input == null) {
            throw new IllegalArgumentException("Les données d'entrée ne peuvent pas être null");
        }
        if (!isLoaded) {
            throw new IllegalStateException(
                    String.format("Le modèle %s n'est pas chargé. Appelez loadModel() d'abord.", modelName)
            );
        }
        LOGGER.fine(String.format("Validation réussie pour l'entrée: %s", input));
    }

    /**
     * Retourne le nom du modèle.
     *
     * @return Le nom du modèle d'IA
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Vérifie si le modèle est actuellement chargé.
     *
     * @return true si le modèle est chargé et prêt à l'emploi
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Retourne la précision par défaut du modèle.
     * Les sous-classes peuvent surcharger cette méthode pour fournir
     * une précision plus précise basée sur des métriques réelles.
     *
     * @return La précision par défaut (0.75)
     */
    @Override
    public double getModelAccuracy() {
        return 0.75;
    }

    /**
     * Prédit les prix pour un lot de données d'exportation.
     * Implémentation par défaut utilisant le Stream API.
     *
     * @param inputs La liste des données d'exportation
     * @return La liste des prédictions générées
     * @throws IllegalArgumentException si la liste est null ou vide
     */
    @Override
    public List<PricePrediction> predictBatch(List<ExportData> inputs) {
        if (inputs == null) {
            throw new IllegalArgumentException("La liste d'entrées ne peut pas être null");
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("La liste d'entrées ne peut pas être vide");
        }

        LOGGER.info(String.format("Prédiction par lot pour %d entrées", inputs.size()));

        return inputs.stream()
                .map(this::predictPrice)
                .toList();
    }

    /**
     * Retourne une représentation textuelle du modèle.
     *
     * @return Description du modèle
     */
    @Override
    public String toString() {
        return String.format(
                "BaseAIModel{nom='%s', chargé=%s, précision=%.2f}",
                modelName, isLoaded, getModelAccuracy()
        );
    }
}