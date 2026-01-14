package tn.sesame.economics.service;

import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import java.util.List;

/**
 * Interface définissant le contrat pour les services de prédiction de prix.
 * Toute implémentation de service de prédiction doit fournir ces fonctionnalités.
 *
 * @since Java 25
 */
public interface PredictionService {

    /**
     * Prédit le prix futur pour une donnée d'exportation spécifique.
     *
     * @param input Les données d'exportation à analyser
     * @return La prédiction de prix générée par le modèle
     * @throws IllegalArgumentException si les données d'entrée sont invalides
     * @throws tn.sesame.economics.exception.PredictionException en cas d'erreur lors de la prédiction
     */
    PricePrediction predictPrice(ExportData input);

    /**
     * Prédit les prix futurs pour un lot de données d'exportation.
     * Cette méthode optimise les prédictions groupées pour améliorer les performances.
     *
     * @param inputs La liste des données d'exportation à analyser
     * @return La liste des prédictions de prix générées
     * @throws IllegalArgumentException si la liste est null ou vide
     * @throws tn.sesame.economics.exception.PredictionException en cas d'erreur lors de la prédiction
     */
    List<PricePrediction> predictBatch(List<ExportData> inputs);

    /**
     * Retourne la précision courante du modèle de prédiction.
     * Cette métrique est généralement calculée sur un jeu de données de test.
     *
     * @return La précision du modèle sous forme de double entre 0.0 et 1.0
     * @throws tn.sesame.economics.exception.ModelException si le modèle n'est pas chargé
     */
    double getModelAccuracy();
}