package tn.sesame.economics.dashboard.view.chart;

import tn.sesame.economics.dashboard.util.chart.ChartStrategy;
import tn.sesame.economics.dashboard.util.chart.ChartFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Interactive chart panel with zoom, pan, and export capabilities
 */
public class InteractiveChartPanel extends VBox {

    private Canvas chartCanvas;
    private GraphicsContext gc;
    private ComboBox<String> chartTypeCombo;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button resetZoomButton;
    private Button exportImageButton;
    private Label chartTitleLabel;
    private Label statusLabel;

    private ChartStrategy currentChartStrategy;
    private Map<String, Double> currentData;
    private String currentTitle;

    private double zoomLevel = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private boolean isPanning = false;
    private double lastMouseX, lastMouseY;

    public InteractiveChartPanel() {
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        setPadding(new Insets(15));
        setSpacing(10);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 5;");

        // Title
        chartTitleLabel = new Label("📈 CHART VISUALIZATION");
        chartTitleLabel.setFont(Font.font("Arial", 16));
        chartTitleLabel.setTextFill(Color.DARKBLUE);

        // Control panel
        HBox controlPanel = createControlPanel();

        // Canvas for drawing
        chartCanvas = new Canvas(600, 400);
        gc = chartCanvas.getGraphicsContext2D();

        // Status label
        statusLabel = new Label("Select chart type and data will appear here");
        statusLabel.setFont(Font.font("Arial", 11));
        statusLabel.setTextFill(Color.DARKSLATEGRAY);

        // Add tooltips
        setupTooltips();

        getChildren().addAll(chartTitleLabel, controlPanel, chartCanvas, statusLabel);
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10, 0, 10, 0));

        // Chart type selection
        Label typeLabel = new Label("Chart Type:");
        chartTypeCombo = new ComboBox<>();
        chartTypeCombo.getItems().addAll(ChartFactory.getAvailableChartTypes());
        chartTypeCombo.setValue("BAR");
        chartTypeCombo.setPrefWidth(120);

        // Zoom controls
        zoomInButton = createControlButton("➕", "Zoom In");
        zoomOutButton = createControlButton("➖", "Zoom Out");
        resetZoomButton = createControlButton("🔄", "Reset View");

        // Export button
        exportImageButton = createControlButton("💾", "Export as Image");

        controlPanel.getChildren().addAll(
                typeLabel, chartTypeCombo,
                zoomInButton, zoomOutButton, resetZoomButton,
                exportImageButton
        );

        return controlPanel;
    }

    private Button createControlButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 14px; -fx-padding: 5px 10px;");
        Tooltip.install(button, new Tooltip(tooltipText));
        return button;
    }

    private void setupTooltips() {
        Tooltip.install(chartTypeCombo, new Tooltip("Select chart type (Bar, Line, or Pie)"));
        Tooltip.install(zoomInButton, new Tooltip("Zoom in for detailed view"));
        Tooltip.install(zoomOutButton, new Tooltip("Zoom out for overview"));
        Tooltip.install(resetZoomButton, new Tooltip("Reset zoom and pan"));
        Tooltip.install(exportImageButton, new Tooltip("Save chart as PNG image"));
    }

    private void setupEventHandlers() {
        // Chart type change
        chartTypeCombo.setOnAction(event -> updateChart());

        // Zoom controls
        zoomInButton.setOnAction(event -> {
            zoomLevel *= 1.2;
            updateChart();
            statusLabel.setText("Zoom: " + String.format("%.1fx", zoomLevel));
        });

        zoomOutButton.setOnAction(event -> {
            zoomLevel /= 1.2;
            if (zoomLevel < 0.1) zoomLevel = 0.1;
            updateChart();
            statusLabel.setText("Zoom: " + String.format("%.1fx", zoomLevel));
        });

        resetZoomButton.setOnAction(event -> {
            zoomLevel = 1.0;
            translateX = 0;
            translateY = 0;
            updateChart();
            statusLabel.setText("View reset");
        });

        // Export button
        exportImageButton.setOnAction(event -> exportChartAsImage());

        // Mouse events for panning
        chartCanvas.setOnMousePressed(event -> {
            isPanning = true;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });

        chartCanvas.setOnMouseDragged(event -> {
            if (isPanning) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                translateX += deltaX;
                translateY += deltaY;

                lastMouseX = event.getX();
                lastMouseY = event.getY();

                updateChart();
                statusLabel.setText("Panning... (drag to move chart)");
            }
        });

        chartCanvas.setOnMouseReleased(event -> {
            isPanning = false;
            statusLabel.setText("Chart updated with pan");
        });

        // Mouse wheel for zoom
        chartCanvas.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                zoomLevel *= 1.1;
            } else {
                zoomLevel /= 1.1;
                if (zoomLevel < 0.1) zoomLevel = 0.1;
            }
            updateChart();
            statusLabel.setText("Zoom: " + String.format("%.1fx", zoomLevel));
            event.consume();
        });
    }

    /**
     * Update the chart with current data and settings
     */
    public void updateChart() {
        if (currentChartStrategy == null || currentData == null || currentData.isEmpty()) {
            drawPlaceholder();
            return;
        }

        // Clear canvas
        gc.clearRect(0, 0, chartCanvas.getWidth(), chartCanvas.getHeight());

        // Save current transformation
        gc.save();

        // Apply zoom and pan transformations
        gc.translate(translateX, translateY);
        gc.scale(zoomLevel, zoomLevel);

        // Draw chart
        currentChartStrategy.drawChart(gc, currentData,
                chartCanvas.getWidth() / zoomLevel,
                chartCanvas.getHeight() / zoomLevel,
                currentTitle);

        // Restore transformation
        gc.restore();

        // Draw zoom/pan info
        drawTransformationInfo();
    }

    /**
     * Set data and update chart
     */
    public void setChartData(Map<String, Double> data, String title) {
        this.currentData = data;
        this.currentTitle = title;

        if (data != null && !data.isEmpty()) {
            // Create chart strategy based on current selection
            String chartType = chartTypeCombo.getValue();
            if (chartType != null) {
                currentChartStrategy = ChartFactory.createChart(chartType);
                updateChart();
                statusLabel.setText(String.format("Showing %d data points", data.size()));
            }
        } else {
            drawPlaceholder();
            statusLabel.setText("No data available for chart");
        }
    }

    /**
     * Change chart type
     */
    public void setChartType(String chartType) {
        chartTypeCombo.setValue(chartType);
        if (currentData != null && !currentData.isEmpty()) {
            currentChartStrategy = ChartFactory.createChart(chartType);
            updateChart();
        }
    }

    /**
     * Export chart as PNG image
     */
    private void exportChartAsImage() {
        try {
            // Create exports directory if it doesn't exist
            File exportDir = new File("exports/charts");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Generate filename with timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String chartType = chartTypeCombo.getValue().toLowerCase();
            String fileName = String.format("chart_%s_%s.png", chartType, timestamp);
            File file = new File(exportDir, fileName);

            // Capture canvas as image and save
            javafx.scene.image.WritableImage image = chartCanvas.snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

            statusLabel.setText("✅ Chart exported to: " + file.getAbsolutePath());
            System.out.println("✅ Chart exported to: " + file.getAbsolutePath());

        } catch (Exception e) {
            statusLabel.setText("❌ Export failed: " + e.getMessage());
            System.err.println("❌ Chart export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawPlaceholder() {
        gc.clearRect(0, 0, chartCanvas.getWidth(), chartCanvas.getHeight());
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, chartCanvas.getWidth(), chartCanvas.getHeight());

        gc.setFill(Color.DARKGRAY);
        gc.setFont(Font.font("Arial", 14));
        String message = "Select data to visualize chart";
        gc.fillText(message,
                chartCanvas.getWidth() / 2 - (message.length() * 3.5),
                chartCanvas.getHeight() / 2);
    }

    private void drawTransformationInfo() {
        if (zoomLevel != 1.0 || translateX != 0 || translateY != 0) {
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(5, 5, 150, 40);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.format("Zoom: %.1fx", zoomLevel), 10, 20);
            gc.fillText(String.format("Pan: %.0f, %.0f", translateX, translateY), 10, 35);
        }
    }

    // Getters for testing
    public ComboBox<String> getChartTypeCombo() { return chartTypeCombo; }
    public Button getZoomInButton() { return zoomInButton; }
    public Button getZoomOutButton() { return zoomOutButton; }
    public Button getResetZoomButton() { return resetZoomButton; }
    public Button getExportImageButton() { return exportImageButton; }
    public Canvas getChartCanvas() { return chartCanvas; }
}