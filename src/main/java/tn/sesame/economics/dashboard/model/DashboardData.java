package tn.sesame.economics.dashboard.model;

import lombok.Data;
import tn.sesame.economics.model.PricePrediction;
import java.util.List;

/**
 * Main data container for the dashboard
 */
@Data
public class DashboardData {
    private List<PricePrediction> predictions;
    private DashboardStatistics statistics;
    private String lastUpdateTime;
    private int totalProducts;
    private int totalCountries;

    public DashboardData(List<PricePrediction> predictions) {
        this.predictions = predictions;
        this.lastUpdateTime = java.time.LocalDateTime.now().toString();
    }
}