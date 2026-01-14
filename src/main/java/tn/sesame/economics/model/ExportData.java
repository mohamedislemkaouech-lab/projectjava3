package tn.sesame.economics.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Record immuable représentant les données d'une exportation agricole tunisienne.
 * Ce record sert de DTO (Data Transfer Object) pour les opérations de prédiction et d'analyse.
 *
 * @param date Date de l'exportation
 * @param productType Type de produit agricole exporté
 * @param pricePerTon Prix par tonne en devise locale (doit être ≥ 0)
 * @param volume Volume exporté en tonnes (doit être ≥ 0)
 * @param destinationCountry Pays de destination de l'exportation
 * @param indicator Indicateur de marché au moment de l'exportation
 *
 * @since Java 25
 */
public record ExportData(
        LocalDate date,
        ProductType productType,
        double pricePerTon,
        double volume,
        String destinationCountry,
        MarketIndicator indicator
) {

    /**
     * Constructeur compact avec validation des données d'entrée.
     * Effectue une validation stricte pour garantir l'intégrité des données.
     *
     * @throws IllegalArgumentException si pricePerTon < 0 ou volume < 0
     * @throws NullPointerException si un paramètre non-nullable est null
     */
    public ExportData {
        // Validation des champs non-nullables
        Objects.requireNonNull(date, "La date ne peut pas être null");
        Objects.requireNonNull(productType, "Le type de produit ne peut pas être null");
        Objects.requireNonNull(destinationCountry, "Le pays de destination ne peut pas être null");
        Objects.requireNonNull(indicator, "L'indicateur de marché ne peut pas être null");

        // Validation de la chaîne destinationCountry
        if (destinationCountry.isBlank()) {
            throw new IllegalArgumentException("Le pays de destination ne peut pas être vide");
        }

        // Validation des valeurs numériques
        if (pricePerTon < 0) {
            throw new IllegalArgumentException(
                    String.format("Le prix par tonne ne peut pas être négatif: %.2f", pricePerTon)
            );
        }

        if (volume < 0) {
            throw new IllegalArgumentException(
                    String.format("Le volume ne peut pas être négatif: %.2f", volume)
            );
        }

        // Normalisation: trim et capitalisation du pays de destination
        destinationCountry = destinationCountry.trim();
    }

    /**
     * Calcule la valeur totale de l'exportation.
     *
     * @return La valeur totale (prix × volume)
     */
    public double calculateTotalValue() {
        return pricePerTon * volume;
    }

    /**
     * Vérifie si cette exportation est valide pour l'analyse.
     * Une exportation est valide si les valeurs sont positives.
     *
     * @return true si l'exportation est valide pour l'analyse
     */
    public boolean isValidForAnalysis() {
        return pricePerTon > 0 && volume > 0;
    }

    /**
     * Crée une nouvelle instance avec une date mise à jour.
     * Utile pour les simulations et projections.
     *
     * @param newDate La nouvelle date
     * @return Une nouvelle instance avec la date mise à jour
     */
    public ExportData withDate(LocalDate newDate) {
        return new ExportData(
                newDate,
                productType,
                pricePerTon,
                volume,
                destinationCountry,
                indicator
        );
    }

    /**
     * Crée une nouvelle instance avec un prix ajusté.
     *
     * @param newPrice Le nouveau prix par tonne
     * @return Une nouvelle instance avec le prix ajusté
     * @throws IllegalArgumentException si le nouveau prix est négatif
     */
    public ExportData withPrice(double newPrice) {
        return new ExportData(
                date,
                productType,
                newPrice,
                volume,
                destinationCountry,
                indicator
        );
    }

    /**
     * Retourne une représentation formatée des données d'exportation.
     *
     * @return Chaîne formatée avec toutes les informations
     */
    @Override
    public String toString() {
        return String.format(
                "ExportData[date=%s, produit=%s, prix=%.2f, volume=%.2f, " +
                        "destination=%s, indicateur=%s, valeur totale=%.2f]",
                date,
                productType.getFrenchName(),
                pricePerTon,
                volume,
                destinationCountry,
                indicator,
                calculateTotalValue()
        );
    }

    /**
     * Méthode utilitaire pour créer une instance depuis un tableau de chaînes.
     * Utile pour le chargement depuis des fichiers CSV.
     *
     * @param data Tableau de chaînes contenant les données dans l'ordre :
     *             0: date (format: yyyy-MM-dd)
     *             1: productType (nom français)
     *             2: pricePerTon (double)
     *             3: volume (double)
     *             4: destinationCountry
     *             5: indicator (nom de l'enum)
     * @return Une nouvelle instance de ExportData
     * @throws IllegalArgumentException si le format des données est incorrect
     */
    public static ExportData fromStringArray(String[] data) {
        if (data.length != 6) {
            throw new IllegalArgumentException(
                    String.format("Le tableau doit contenir 6 éléments, mais contient %d", data.length)
            );
        }

        try {
            LocalDate parsedDate = LocalDate.parse(data[0]);
            ProductType parsedProductType = ProductType.fromFrenchName(data[1]);
            double parsedPrice = Double.parseDouble(data[2]);
            double parsedVolume = Double.parseDouble(data[3]);
            MarketIndicator parsedIndicator = MarketIndicator.valueOf(data[5].toUpperCase());

            return new ExportData(
                    parsedDate,
                    parsedProductType,
                    parsedPrice,
                    parsedVolume,
                    data[4],
                    parsedIndicator
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Format de données invalide", e);
        }
    }
}