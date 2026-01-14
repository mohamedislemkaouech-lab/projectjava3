package tn.sesame.economics.model;

public enum MarketIndicator {
    STABLE("Marché stable avec peu de variations"),
    VOLATILE("Marché volatile avec fortes variations"),
    RISING("Marché en hausse"),
    FALLING("Marché en baisse"),
    UNPREDICTABLE("Marché imprévisible");

    private final String description;

    MarketIndicator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}