package tn.sesame.economics.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Record immuable représentant les données d'une exportation agricole tunisienne.
 * Mise à jour pour inclure la volatilité des prix et le taux de change.
 */
public record ExportData(
        LocalDate date,
        ProductType productType,
        double pricePerTon,
        double volume,
        String destinationCountry,
        MarketIndicator indicator,
        double priceVolatility,      // Nouveau champ
        double exchangeRateTNDUSD    // Nouveau champ
) {

    /**
     * Constructeur compact avec validation des données d'entrée.
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

        // Validation des nouveaux champs
        if (priceVolatility < 0) {
            throw new IllegalArgumentException(
                    String.format("La volatilité des prix ne peut pas être négative: %.4f", priceVolatility)
            );
        }

        if (exchangeRateTNDUSD <= 0) {
            throw new IllegalArgumentException(
                    String.format("Le taux de change doit être positif: %.4f", exchangeRateTNDUSD)
            );
        }

        // Normalisation
        destinationCountry = destinationCountry.trim();
    }

    /**
     * Calcule la valeur totale de l'exportation en TND.
     */
    public double calculateTotalValueTND() {
        return pricePerTon * volume;
    }

    /**
     * Calcule la valeur totale de l'exportation en USD.
     */
    public double calculateTotalValueUSD() {
        return calculateTotalValueTND() / exchangeRateTNDUSD;
    }

    /**
     * Vérifie si cette exportation est valide pour l'analyse.
     */
    public boolean isValidForAnalysis() {
        return pricePerTon > 0 && volume > 0 && exchangeRateTNDUSD > 0;
    }

    /**
     * Crée une nouvelle instance avec les nouveaux champs.
     */
    public ExportData withPriceVolatility(double newVolatility) {
        return new ExportData(
                date,
                productType,
                pricePerTon,
                volume,
                destinationCountry,
                indicator,
                newVolatility,
                exchangeRateTNDUSD
        );
    }

    public ExportData withExchangeRate(double newExchangeRate) {
        return new ExportData(
                date,
                productType,
                pricePerTon,
                volume,
                destinationCountry,
                indicator,
                priceVolatility,
                newExchangeRate
        );
    }

    @Override
    public String toString() {
        return String.format(
                "ExportData[date=%s, produit=%s, prix=%.2f TND, volume=%.2f, " +
                        "destination=%s, indicateur=%s, volatilité=%.4f, taux=%.4f, " +
                        "valeur totale=%.2f TND (%.2f USD)]",
                date,
                productType.getFrenchName(),
                pricePerTon,
                volume,
                destinationCountry,
                indicator,
                priceVolatility,
                exchangeRateTNDUSD,
                calculateTotalValueTND(),
                calculateTotalValueUSD()
        );
    }

    /**
     * Méthode utilitaire pour créer une instance depuis un tableau de chaînes.
     * Mise à jour pour 8 champs.
     */
    public static ExportData fromStringArray(String[] data) {
        if (data.length != 8) {
            throw new IllegalArgumentException(
                    String.format("Le tableau doit contenir 8 éléments, mais contient %d", data.length)
            );
        }

        try {
            LocalDate parsedDate = LocalDate.parse(data[0]);
            ProductType parsedProductType = ProductType.fromFrenchName(data[1]);
            double parsedPrice = Double.parseDouble(data[2]);
            double parsedVolume = Double.parseDouble(data[3]);
            MarketIndicator parsedIndicator = MarketIndicator.valueOf(data[5].toUpperCase());
            double parsedVolatility = Double.parseDouble(data[6]);
            double parsedExchangeRate = Double.parseDouble(data[7]);

            return new ExportData(
                    parsedDate,
                    parsedProductType,
                    parsedPrice,
                    parsedVolume,
                    data[4], // destinationCountry
                    parsedIndicator,
                    parsedVolatility,
                    parsedExchangeRate
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Format de données invalide", e);
        }
    }
}