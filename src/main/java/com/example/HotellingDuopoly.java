package com.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Реализация модели дуополии Хотеллинга. Меню выбора методов:
 * Best Response (тернарный поиск для каждого шага),
 * Exhaustive Search (полный перебор по сетке),
 * Ternary Search (непрерывный поиск для всех фирм одновременно).
 */
public class HotellingDuopoly extends Application {

    private static final int CANVAS_SIZE = 400;
    List<Firm> firms = new ArrayList<>();
    List<double[]> residents = new ArrayList<>();
    private Label equilibriumLabel;
    private String shape = "Circle";
    String metric = "Euclidean";
    private String residentDistribution = "Uniform";
    private String firmDistribution = "Manual";
    private String priceDistribution = "Manual";
    private String method = "Best Response";
    private TextArea residentsInput;
    private TextArea firmsInput;
    private TextArea pricesInput;
    private TextField transportInput;

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        ComboBox<String> shapeBox = new ComboBox<>();
        shapeBox.getItems().addAll("Circle", "Square");
        shapeBox.setValue("Circle");
        shapeBox.setOnAction(e -> shape = shapeBox.getValue());

        ComboBox<String> metricBox = new ComboBox<>();
        metricBox.getItems().addAll("Euclidean", "Manhattan");
        metricBox.setValue("Euclidean");
        metricBox.setOnAction(e -> metric = metricBox.getValue());

        ComboBox<String> residentDistBox = new ComboBox<>();
        residentDistBox.getItems().addAll("Uniform", "Manual");
        residentDistBox.setValue("Uniform");
        residentDistBox.setOnAction(e -> residentDistribution = residentDistBox.getValue());

        ComboBox<String> firmDistBox = new ComboBox<>();
        firmDistBox.getItems().addAll("Random", "Manual");
        firmDistBox.setValue("Manual");
        firmDistBox.setOnAction(e -> firmDistribution = firmDistBox.getValue());

        ComboBox<String> priceDistBox = new ComboBox<>();
        priceDistBox.getItems().addAll("Random", "Manual");
        priceDistBox.setValue("Manual");
        priceDistBox.setOnAction(e -> priceDistribution = priceDistBox.getValue());

        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("Best Response", "Exhaustive Search", "Ternary Search");
        methodBox.setValue("Best Response");
        methodBox.setOnAction(e -> method = methodBox.getValue());

        residentsInput = new TextArea();
        residentsInput.setPromptText("Введите координаты жителей: x1,y1; x2,y2; ...");
        residentsInput.setPrefRowCount(3);
        residentsInput.setVisible(false);

        firmsInput = new TextArea();
        firmsInput.setPromptText("Введите координаты фирм: x1,y1; x2,y2; ...");
        firmsInput.setPrefRowCount(3);
        firmsInput.setVisible(true);

        pricesInput = new TextArea();
        pricesInput.setPromptText("Введите начальные цены: p1; p2; ...");
        pricesInput.setPrefRowCount(3);
        pricesInput.setVisible(true);

        transportInput = new TextField("1.0");
        transportInput.setPromptText("Коэффициент транспортных издержек t");

        residentDistBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            residentsInput.setVisible("Manual".equals(newVal));
        });
        firmDistBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            firmsInput.setVisible("Manual".equals(newVal));
        });
        priceDistBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            pricesInput.setVisible("Manual".equals(newVal));
        });

        Button calculateButton = new Button("Calculate");
        calculateButton.setOnAction(e -> {
            parseInputs();
            drawModel(gc);
            calculateEquilibrium();
        });

        equilibriumLabel = new Label("Равновесие: N/A");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        grid.add(new Label("Форма:"), 0, 0);
        grid.add(shapeBox, 1, 0);
        grid.add(new Label("Метрика:"), 0, 1);
        grid.add(metricBox, 1, 1);
        grid.add(new Label("Распределение жителей:"), 0, 2);
        grid.add(residentDistBox, 1, 2);
        grid.add(new Label("Координаты жителей:"), 0, 3);
        grid.add(residentsInput, 1, 3);
        grid.add(new Label("Распределение фирм:"), 0, 4);
        grid.add(firmDistBox, 1, 4);
        grid.add(new Label("Координаты фирм:"), 0, 5);
        grid.add(firmsInput, 1, 5);
        grid.add(new Label("Распределение цен:"), 0, 6);
        grid.add(priceDistBox, 1, 6);
        grid.add(new Label("Цены:"), 0, 7);
        grid.add(pricesInput, 1, 7);
        grid.add(new Label("Транспортные издержки t:"), 0, 8);
        grid.add(transportInput, 1, 8);
        grid.add(new Label("Метод:"), 0, 9);
        grid.add(methodBox, 1, 9);
        grid.add(calculateButton, 1, 10);

        VBox layout = new VBox(10, grid, canvas, equilibriumLabel);
        layout.setPadding(new javafx.geometry.Insets(10));
        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 800, 600);
        primaryStage.setResizable(true);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Модель Хотеллинга");
        primaryStage.show();
    }

    private void parseInputs() {
        double transportCoef;
        try {
            transportCoef = Double.parseDouble(transportInput.getText().trim());
        } catch (NumberFormatException e) {
            transportCoef = 1.0;
        }

        if (residentDistribution.equals("Manual")) {
            residents = parseResidents(residentsInput.getText());
        } else {
            residents = generateUniformResidents(shape, metric);
        }

        if (firmDistribution.equals("Manual")) {
            firms = parseFirms(firmsInput.getText());
        } else {
            firms = generateRandomFirms(shape);
        }

        List<Double> prices;
        if (priceDistribution.equals("Manual")) {
            prices = parsePrices(pricesInput.getText());
        } else {
            prices = generateRandomPrices(firms.size());
        }

        if (firms.size() != prices.size()) {
            showError("Количество фирм и цен не совпадает");
            return;
        }
        for (int i = 0; i < firms.size(); i++) {
            firms.get(i).price = prices.get(i);
            firms.get(i).transportCoef = transportCoef;
        }
    }

    private List<double[]> parseResidents(String input) {
        List<double[]> residents = new ArrayList<>();
        String[] points = input.split(";");
        for (String point : points) {
            String[] coords = point.trim().split(",");
            if (coords.length == 2) {
                try {
                    double x = Double.parseDouble(coords[0].trim());
                    double y = Double.parseDouble(coords[1].trim());
                    if (isInsideShape(x, y, shape)) {
                        residents.add(new double[]{x, y});
                    }
                } catch (NumberFormatException ex) {
                }
            }
        }
        return residents;
    }

    private List<double[]> generateUniformResidents(String shape, String metric) {
        List<double[]> residents = new ArrayList<>();
        int numResidents = 1000;
        if (metric.equals("Manhattan")) {
            int gridSize = 20;
            Random rand = new Random();
            for (int k = 0; k < numResidents; k++) {
                boolean vertical = rand.nextBoolean();
                if (vertical) {
                    int i = rand.nextInt(gridSize + 1);
                    double x = (double) i / gridSize;
                    if (shape.equals("Circle")) {
                        double dx = Math.abs(x - 0.5);
                        double maxDelta = Math.sqrt(0.25 - dx * dx);
                        double y = 0.5 - maxDelta + rand.nextDouble() * (2 * maxDelta);
                        residents.add(new double[]{x, y});
                    } else {
                        double y = rand.nextDouble();
                        residents.add(new double[]{x, y});
                    }
                } else {
                    int j = rand.nextInt(gridSize + 1);
                    double y = (double) j / gridSize;
                    if (shape.equals("Circle")) {
                        double dy = Math.abs(y - 0.5);
                        double maxDelta = Math.sqrt(0.25 - dy * dy);
                        double x = 0.5 - maxDelta + rand.nextDouble() * (2 * maxDelta);
                        residents.add(new double[]{x, y});
                    } else {
                        double x = rand.nextDouble();
                        residents.add(new double[]{x, y});
                    }
                }
            }
        } else {
            Random rand = new Random();
            if (shape.equals("Circle")) {
                for (int i = 0; i < numResidents; i++) {
                    double r = Math.sqrt(rand.nextDouble()) * 0.5;
                    double theta = rand.nextDouble() * 2 * Math.PI;
                    double x = 0.5 + r * Math.cos(theta);
                    double y = 0.5 + r * Math.sin(theta);
                    residents.add(new double[]{x, y});
                }
            } else {
                for (int i = 0; i < numResidents; i++) {
                    double x = rand.nextDouble();
                    double y = rand.nextDouble();
                    residents.add(new double[]{x, y});
                }
            }
        }
        return residents;
    }

    private List<Firm> parseFirms(String input) {
        List<Firm> firms = new ArrayList<>();
        String[] points = input.split(";");
        for (int i = 0; i < points.length; i++) {
            String[] coords = points[i].trim().split(",");
            if (coords.length == 2) {
                try {
                    double x = Double.parseDouble(coords[0].trim());
                    double y = Double.parseDouble(coords[1].trim());
                    if (isInsideShape(x, y, shape)) {
                        firms.add(new Firm(x, y, i));
                    }
                } catch (NumberFormatException ex) {
                }
            }
        }
        return firms;
    }

    private List<Firm> generateRandomFirms(String shape) {
        List<Firm> firms = new ArrayList<>();
        Random rand = new Random();
        int numFirms = rand.nextInt(5) + 2;
        for (int i = 0; i < numFirms; i++) {
            double x, y;
            if (shape.equals("Circle")) {
                double r = Math.sqrt(rand.nextDouble()) * 0.5;
                double theta = rand.nextDouble() * 2 * Math.PI;
                x = 0.5 + r * Math.cos(theta);
                y = 0.5 + r * Math.sin(theta);
            } else {
                x = rand.nextDouble();
                y = rand.nextDouble();
            }
            firms.add(new Firm(x, y, i));
        }
        return firms;
    }

    private List<Double> parsePrices(String input) {
        List<Double> prices = new ArrayList<>();
        String[] priceStrs = input.split(";");
        for (String p : priceStrs) {
            try {
                double price = Double.parseDouble(p.trim());
                prices.add(price);
            } catch (NumberFormatException ex) {
            }
        }
        return prices;
    }

    private List<Double> generateRandomPrices(int numFirms) {
        List<Double> prices = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < numFirms; i++) {
            double price = 1 + rand.nextDouble() * 9;
            prices.add(price);
        }
        return prices;
    }

    private boolean isInsideShape(double x, double y, String shape) {
        if (shape.equals("Circle")) {
            return Math.hypot(x - 0.5, y - 0.5) <= 0.5;
        } else {
            return x >= 0 && x <= 1 && y >= 0 && y <= 1;
        }
    }

    private double calculateDistance(double x1, double y1, double x2, double y2, String metric) {
        if (metric.equals("Manhattan")) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        } else {
            return Math.hypot(x1 - x2, y1 - y2);
        }
    }

    private void drawModel(GraphicsContext gc) {
        gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        if (shape.equals("Circle")) {
            gc.fillOval(0, 0, CANVAS_SIZE, CANVAS_SIZE);
            gc.strokeOval(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        } else {
            gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
            gc.strokeRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        }

        if (metric.equals("Manhattan")) {
            gc.setStroke(Color.LIGHTGRAY);
            int gridSize = 20;
            if (shape.equals("Square")) {
                for (int i = 0; i <= gridSize; i++) {
                    double pixelX = (double) i / gridSize * CANVAS_SIZE;
                    gc.strokeLine(pixelX, 0, pixelX, CANVAS_SIZE);
                }
                for (int j = 0; j <= gridSize; j++) {
                    double pixelY = (double) j / gridSize * CANVAS_SIZE;
                    gc.strokeLine(0, pixelY, CANVAS_SIZE, pixelY);
                }
            } else {
                double center = CANVAS_SIZE / 2.0;
                double radius = CANVAS_SIZE / 2.0;
                for (int i = 0; i <= gridSize; i++) {
                    double pixelX = (double) i / gridSize * CANVAS_SIZE;
                    double dx = Math.abs(pixelX - center);
                    if (dx <= radius) {
                        double halfH = Math.sqrt(radius * radius - dx * dx);
                        double y0 = center - halfH;
                        double y1 = center + halfH;
                        gc.strokeLine(pixelX, y0, pixelX, y1);
                    }
                }
                for (int j = 0; j <= gridSize; j++) {
                    double pixelY = (double) j / gridSize * CANVAS_SIZE;
                    double dy = Math.abs(pixelY - center);
                    if (dy <= radius) {
                        double halfW = Math.sqrt(radius * radius - dy * dy);
                        double x0 = center - halfW;
                        double x1 = center + halfW;
                        gc.strokeLine(x0, pixelY, x1, pixelY);
                    }
                }
            }
        }

        gc.setFill(Color.GRAY);
        for (double[] resident : residents) {
            int pixelX = (int) (resident[0] * CANVAS_SIZE);
            int pixelY = (int) (resident[1] * CANVAS_SIZE);
            gc.fillRect(pixelX, pixelY, 2, 2);
        }

        for (Firm firm : firms) {
            int pixelX = (int) (firm.x * CANVAS_SIZE) - 5;
            int pixelY = (int) (firm.y * CANVAS_SIZE) - 5;
            gc.setFill(Color.RED);
            gc.fillOval(pixelX, pixelY, 10, 10);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(firm.index + 1), pixelX + 3, pixelY + 8);
        }
    }

    private void calculateEquilibrium() {
        // запустить выбранный метод
        if (method.equals("Best Response")) {
            bestResponseDynamics();
        } else if (method.equals("Exhaustive Search")) {
            exhaustiveGridSearchNash();
        } else {
            ternarySearchNash();
        }
        // формируем строку результатов последней итерации
        StringBuilder result = new StringBuilder("Результаты для последней итерации: ");
        for (Firm firm : firms) {
            double profit = calculateProfit(firm, residents, metric);
            result.append(String.format("Фирма %d: цена %.2f, прибыль %.2f; ",
                    firm.index + 1, firm.price, profit));
        }
        // проверяем Nash
        if (isNashEquilibrium()) {
            result.append("Nash-равновесие найдено.");
        } else {
            result.append("Nash-равновесие не найдено.");
        }
        equilibriumLabel.setText(result.toString());
    }

    /**
     * Best-response dynamics с тернарным поиском для каждого шага.
     */
    private void bestResponseDynamics() {
        double tolPrice = 1e-3;
        int maxIterations = 10000;
        for (int iter = 0; iter < maxIterations; iter++) {
            boolean anyChange = false;
            for (Firm firm : firms) {
                double currentPrice = firm.price;
                double bestPrice = ternarySearchPrice(firm);
                if (Math.abs(bestPrice - currentPrice) > tolPrice) {
                    firm.price = bestPrice;
                    anyChange = true;
                }
            }
            if (!anyChange) break;
        }
    }

    /**
     * Exhaustive Search: перебор сетки цен с шагом 0.01 для каждой фирмы при фиксированных ценах остальных.
     */
    private void exhaustiveGridSearchNash() {
        double tolPrice = 1e-3;
        int maxIterations = 100;
        for (int iter = 0; iter < maxIterations; iter++) {
            boolean anyChange = false;
            for (Firm firm : firms) {
                double currentPrice = firm.price;
                double bestPrice = currentPrice;
                double bestProfit = calculateProfit(firm, residents, metric);
                for (double testPrice = 0.1; testPrice <= 10; testPrice += 0.01) {
                    firm.price = testPrice;
                    double profit = calculateProfit(firm, residents, metric);
                    if (profit > bestProfit) {
                        bestProfit = profit;
                        bestPrice = testPrice;
                    }
                }
                if (Math.abs(bestPrice - currentPrice) > tolPrice) {
                    firm.price = bestPrice;
                    anyChange = true;
                } else {
                    firm.price = currentPrice;
                }
            }
            if (!anyChange) break;
        }
    }

    /**
     * Ternary Search Nash: непрерывный поиск для всех фирм сразу.
     */
    private void ternarySearchNash() {
        double tolPrice = 1e-3;
        int maxIterations = 100;
        for (int iter = 0; iter < maxIterations; iter++) {
            boolean anyChange = false;
            for (Firm firm : firms) {
                double currentPrice = firm.price;
                double bestPrice = ternarySearchPrice(firm);
                if (Math.abs(bestPrice - currentPrice) > tolPrice) {
                    firm.price = bestPrice;
                    anyChange = true;
                }
            }
            if (!anyChange) break;
        }
    }

    /**
     * Тройной (ternary) поиск оптимальной цены для данной фирмы при фиксированных ценах конкурентов.
     */
    private double ternarySearchPrice(Firm firm) {
        double left = 0.1, right = 10.0;
        for (int i = 0; i < 50; i++) {
            double m1 = left + (right - left) / 3;
            double m2 = right - (right - left) / 3;
            firm.price = m1;
            double profit1 = calculateProfit(firm, residents, metric);
            firm.price = m2;
            double profit2 = calculateProfit(firm, residents, metric);
            if (profit1 < profit2) {
                left = m1;
            } else {
                right = m2;
            }
        }
        return (left + right) / 2;
    }

    /**
     * Проверяет, является ли текущий набор цен Nash-равновесием.
     */
    private boolean isNashEquilibrium() {
        double tolImprovement = 1e-4;
        for (Firm firm : firms) {
            double currentProfit = calculateProfit(firm, residents, metric);
            double bestPrice = ternarySearchPrice(firm);
            firm.price = bestPrice;
            double bestProfit = calculateProfit(firm, residents, metric);
            firm.price = bestPrice;
            if (bestProfit > currentProfit + tolImprovement) {
                return false;
            }
        }
        return true;
    }

    /**
     * Вычисление прибыли фирмы: цена * число жителей, для которых фирма минимизирует (price + t*distance).
     */
    private double calculateProfit(Firm firm, List<double[]> residents, String metric) {
        int marketShare = 0;
        for (double[] resident : residents) {
            double minCost = Double.MAX_VALUE;
            for (Firm f : firms) {
                double dist = calculateDistance(resident[0], resident[1], f.x, f.y, metric);
                double cost = f.price + f.transportCoef * dist;
                if (cost < minCost) minCost = cost;
            }
            double firmCost = calculateDistance(resident[0], resident[1], firm.x, firm.y, metric)
                    * firm.transportCoef + firm.price;
            if (Math.abs(firmCost - minCost) < 1e-9 || firmCost < minCost) {
                marketShare++;
            }
        }
        return firm.price * marketShare;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Firm {
        public double x;
        public double y;
        public double price;
        int index;
        public double transportCoef;

        public Firm(double x, double y, int index) {
            this.x = x;
            this.y = y;
            this.index = index;
            this.price = 1.0;
            this.transportCoef = 1.0;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
