package tn.sesame.economics.dashboard.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.List;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData;

/**
 * Model for dashboard statistics
 */
@Data
@Builder
public class DashboardModel {
    private List<PricePrediction> currentPredictions;
    private List<ExportData> currentData;
    private DashboardStatistics statistics;
    private Map<String, Object> filters;
    private String selectedView;
}