package tn.sesame.economics.dashboard.view;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight; // ADD THIS IMPORT
import java.time.LocalDate; // ADD THIS IMPORT

/**
 * Panel for filter controls
 */
public class FilterPanel extends VBox {

    private ComboBox<String> productFilter;
    private ComboBox<String> countryFilter;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Button applyFilterButton;
    private Button resetFilterButton;
    private Button exportCsvButton;
    private Button exportJsonButton;
    private Button savePresetButton;
    private Button loadPresetButton;
    private Label resultsLabel;
    private Label filterStatusLabel;

    public FilterPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(15));
        setSpacing(10);
        setStyle("-fx-background-color: #e8f4f8; -fx-border-color: #87ceeb; -fx-border-radius: 5;");

        // Title
        Label title = new Label("🔍 FILTER CONTROLS");
        title.setFont(Font.font("Arial", 16));
        title.setTextFill(Color.DARKBLUE);

        // Product filter
        Label productLabel = new Label("Product Type:");
        productFilter = new ComboBox<>();
        productFilter.getItems().addAll(
                "All Products",
                "Huile d'olive",
                "Dattes",
                "Agrumes",
                "Blé",
                "Tomates",
                "Piments"
        );
        productFilter.setValue("All Products");
        productFilter.setPrefWidth(200);

        // Country filter
        Label countryLabel = new Label("Destination Country:");
        countryFilter = new ComboBox<>();
        countryFilter.getItems().addAll(
                "All Countries",
                "France",
                "Germany",
                "Italy",
                "Spain",
                "UK",
                "USA",
                "Canada",
                "Japan",
                "China",
                "Algeria",
                "Libya",
                "Morocco"
        );
        countryFilter.setValue("All Countries");
        countryFilter.setPrefWidth(200);

        // Date range filter
        Label dateLabel = new Label("Date Range:");
        HBox dateBox = new HBox(10);
        startDatePicker = new DatePicker(LocalDate.now().minusMonths(3));
        endDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setPrefWidth(120);
        endDatePicker.setPrefWidth(120);
        dateBox.getChildren().addAll(
                new Label("From:"), startDatePicker,
                new Label("To:"), endDatePicker
        );

        // Action buttons row 1
        HBox actionButtons1 = new HBox(10);
        applyFilterButton = createStyledButton("✅ Apply Filters", "#4CAF50");
        resetFilterButton = createStyledButton("🔄 Reset All", "#f44336");

        actionButtons1.getChildren().addAll(applyFilterButton, resetFilterButton);

        // Action buttons row 2
        HBox actionButtons2 = new HBox(10);
        exportCsvButton = createStyledButton("📊 Export CSV", "#FF9800");
        exportJsonButton = createStyledButton("📋 Export JSON", "#9C27B0");

        actionButtons2.getChildren().addAll(exportCsvButton, exportJsonButton);

        // Action buttons row 3 (Presets)
        HBox actionButtons3 = new HBox(10);
        savePresetButton = createStyledButton("💾 Save Preset", "#607D8B");
        loadPresetButton = createStyledButton("📂 Load Preset", "#795548");

        actionButtons3.getChildren().addAll(savePresetButton, loadPresetButton);

        // Results label
        resultsLabel = new Label("Showing all records - No filters applied");
        resultsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12)); // FIXED
        resultsLabel.setTextFill(Color.DARKSLATEBLUE);

        // Filter status label
        filterStatusLabel = new Label("Active filters: None");
        filterStatusLabel.setFont(Font.font("Arial", 11));
        filterStatusLabel.setTextFill(Color.DARKSLATEGRAY);

        // Add everything to panel
        getChildren().addAll(
                title,
                createSeparator(),
                productLabel, productFilter,
                countryLabel, countryFilter,
                dateLabel, dateBox,
                createSeparator(),
                actionButtons1,
                actionButtons2,
                actionButtons3,
                createSeparator(),
                resultsLabel,
                filterStatusLabel
        );
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 12px;",
                color
        ));
        button.setPrefWidth(150);
        return button;
    }

    private Label createSeparator() {
        Label separator = new Label("────────────────────────────");
        separator.setTextFill(Color.LIGHTGRAY);
        separator.setPadding(new Insets(5, 0, 5, 0));
        return separator;
    }

    // Getters for controller to access filter values
    public ComboBox<String> getProductFilter() { return productFilter; }
    public ComboBox<String> getCountryFilter() { return countryFilter; }
    public DatePicker getStartDatePicker() { return startDatePicker; }
    public DatePicker getEndDatePicker() { return endDatePicker; }
    public Button getApplyFilterButton() { return applyFilterButton; }
    public Button getResetFilterButton() { return resetFilterButton; }
    public Button getExportCsvButton() { return exportCsvButton; }
    public Button getExportJsonButton() { return exportJsonButton; }
    public Button getSavePresetButton() { return savePresetButton; }
    public Button getLoadPresetButton() { return loadPresetButton; }
    public Label getResultsLabel() { return resultsLabel; }
    public Label getFilterStatusLabel() { return filterStatusLabel; }

    public void updateResultsLabel(int filteredCount, int totalCount) {
        if (filteredCount == totalCount) {
            resultsLabel.setText(String.format("Showing all %d records", totalCount));
            resultsLabel.setTextFill(Color.DARKSLATEBLUE);
        } else {
            resultsLabel.setText(String.format("Showing %d of %d records (Filtered)", filteredCount, totalCount));
            resultsLabel.setTextFill(Color.DARKGREEN);
        }
    }

    public void updateFilterStatus(String status) {
        filterStatusLabel.setText("Active filters: " + status);
    }

    public void resetFilters() {
        productFilter.setValue("All Products");
        countryFilter.setValue("All Countries");
        startDatePicker.setValue(LocalDate.now().minusMonths(3));
        endDatePicker.setValue(LocalDate.now());
        updateFilterStatus("None");
    }
}