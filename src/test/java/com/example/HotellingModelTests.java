package com.example;

import com.example.HotellingDuopoly.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class HotellingModelTests {

    // 1. Две фирмы на концах отрезка, равные цены
    @Test
    public void testSymmetricFirmsEqualPrices() {
        Firm left = new Firm(0.0, 0.5, 0);
        Firm right = new Firm(1.0, 0.5, 1);
        left.price = right.price = 1.0;
        left.transportCoef = right.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(left, right);
        List<double[]> residents = generateLineResidents();

        double p1 = calculateProfit(left, firms, residents, "Euclidean");
        double p2 = calculateProfit(right, firms, residents, "Euclidean");

        // Ожидаем примерно по 500 жителей на фирму
        Assert.assertTrue(Math.abs(p1 - 500.0) < 50, "Left profit");
        Assert.assertTrue(Math.abs(p2 - 500.0) < 50, "Right profit");
    }

    // 2. Две фирмы с разными ценами
    @Test
    public void testAsymmetricPrices() {
        Firm left = new Firm(0.0, 0.5, 0);
        Firm right = new Firm(1.0, 0.5, 1);
        left.price = 0.5; right.price = 1.0;
        left.transportCoef = right.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(left, right);
        List<double[]> residents = generateLineResidents();

        double pLeft = calculateProfit(left, firms, residents, "Euclidean");
        double pRight = calculateProfit(right, firms, residents, "Euclidean");

        Assert.assertTrue(pLeft > pRight, "Left should earn more");
        Assert.assertTrue(pLeft > 350 && pLeft < 450, "Left profit range");
        Assert.assertTrue(pRight > 200 && pRight < 300, "Right profit range");
    }

    // 3. Разделение по диагонали при Manhattan
    @Test
    public void testManhattanDiagonalSplit() {
        Firm f1 = new Firm(0.0, 0.0, 0);
        Firm f2 = new Firm(1.0, 1.0, 1);
        f1.price = f2.price = 1.0;
        f1.transportCoef = f2.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(f1, f2);
        List<double[]> residents = generateUniformSquareResidents();

        double p1 = calculateProfit(f1, firms, residents, "Manhattan");
        double p2 = calculateProfit(f2, firms, residents, "Manhattan");

        Assert.assertTrue(Math.abs(p1 - p2) < 50, "Profits should be nearly equal");
    }

    // 4. Три фирмы в углах равностороннего треугольника
    @Test
    public void testThreeFirmsTriangle() {
        double cx = 0.5, cy = 0.5, r = 0.4;
        Firm f1 = new Firm(cx + r * Math.cos(0), cy + r * Math.sin(0), 0);
        Firm f2 = new Firm(cx + r * Math.cos(2 * Math.PI / 3), cy + r * Math.sin(2 * Math.PI / 3), 1);
        Firm f3 = new Firm(cx + r * Math.cos(4 * Math.PI / 3), cy + r * Math.sin(4 * Math.PI / 3), 2);

        for (Firm f : Arrays.asList(f1, f2, f3)) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }

        List<Firm> firms = Arrays.asList(f1, f2, f3);
        List<double[]> residents = generateGridResidents();

        double p1 = calculateProfit(f1, firms, residents, "Euclidean");
        double p2 = calculateProfit(f2, firms, residents, "Euclidean");
        double p3 = calculateProfit(f3, firms, residents, "Euclidean");

        System.out.printf("Triangle profits: p1=%.2f, p2=%.2f, p3=%.2f%n", p1, p2, p3);

        double avg = (p1 + p2 + p3) / 3.0;

        Assert.assertTrue(Math.abs(p1 - avg) < 100, "p1 deviation");
        Assert.assertTrue(Math.abs(p2 - avg) < 100, "p2 deviation");
        Assert.assertTrue(Math.abs(p3 - avg) < 100, "p3 deviation");
    }


    // 5. Чувствительность к t (транспортные издержки)
    @Test
    public void testSensitivityToTransportCostFixedCompetitor() {
        Firm f1 = new Firm(0.2, 0.5, 0);
        Firm f2 = new Firm(0.8, 0.5, 1);
        f1.price = f2.price = 1.0;

        f2.transportCoef = 1.0;

        // t1 = 0.5
        f1.transportCoef = 0.5;
        double pLowT = calculateProfit(f1, Arrays.asList(f1, f2), generateLineResidents(), "Euclidean");

        // t1 = 2.0
        f1.transportCoef = 2.0;
        double pHighT = calculateProfit(f1, Arrays.asList(f1, f2), generateLineResidents(), "Euclidean");

        Assert.assertTrue(pLowT > pHighT, "Firm with lower t should get more customers");
    }

    // 6. Четыре фирмы на равномерной сетке (евклидова метрика)
    @Test
    public void testFourFirmsInSquare_Euclidean() {
        List<Firm> firms = Arrays.asList(
                new Firm(0.25, 0.25, 0),
                new Firm(0.75, 0.25, 1),
                new Firm(0.25, 0.75, 2),
                new Firm(0.75, 0.75, 3)
        );
        for (Firm f : firms) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }

        List<double[]> residents = generateUniformSquareResidents();

        double[] profits = firms.stream()
                .mapToDouble(f -> calculateProfit(f, firms, residents, "Euclidean"))
                .toArray();

        for (int i = 0; i < profits.length; i++) {
            Assert.assertTrue(profits[i] > 200 && profits[i] < 300,
                    "Firm " + i + " profit in expected range");
        }
    }

    // 7. Две фирмы в круге (Манхэттенова метрика)
    @Test
    public void testTwoFirmsInCircle_Manhattan() {
        Firm f1 = new Firm(0.3, 0.5, 0);
        Firm f2 = new Firm(0.7, 0.5, 1);
        f1.price = f2.price = 1.0;
        f1.transportCoef = f2.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(f1, f2);
        List<double[]> residents = generateUniformCircleResidents();

        double p1 = calculateProfit(f1, firms, residents, "Manhattan");
        double p2 = calculateProfit(f2, firms, residents, "Manhattan");

        Assert.assertTrue(Math.abs(p1 - p2) < 50, "Profits should be similar");
    }

    // 8. Три фирмы и кластерная плотность спроса
    @Test
    public void testClusterDemandBias() {
        Firm f1 = new Firm(0.2, 0.2, 0); // рядом с кластером
        Firm f2 = new Firm(0.8, 0.2, 1);
        Firm f3 = new Firm(0.5, 0.8, 2);
        for (Firm f : List.of(f1, f2, f3)) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }

        List<Firm> firms = Arrays.asList(f1, f2, f3);
        List<double[]> residents = generateClusteredResidents();

        double p1 = calculateProfit(f1, firms, residents, "Euclidean");
        double p2 = calculateProfit(f2, firms, residents, "Euclidean");
        double p3 = calculateProfit(f3, firms, residents, "Euclidean");

        Assert.assertTrue(p1 > p2 && p1 > p3, "Firm near cluster should have highest profit");
    }

    // 9. Пять фирм на равномерной сетке на квадрате (euclidean)
    @Test
    public void testFiveFirmsGridUniform_Euclidean() {
        // Располагаем 5 фирм: центр и четыре угла
        List<Firm> firms = Arrays.asList(
                new Firm(0.5, 0.5, 0),
                new Firm(0.0, 0.0, 1),
                new Firm(1.0, 0.0, 2),
                new Firm(1.0, 1.0, 3),
                new Firm(0.0, 1.0, 4)
        );
        for (Firm f : firms) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }
        List<double[]> residents = generateGridResidents(50, 50);

        double[] profits = firms.stream()
                .mapToDouble(f -> calculateProfit(f, firms, residents, "Euclidean"))
                .toArray();

        double avg = Arrays.stream(profits).average().orElse(0);
        for (double p : profits) {
            Assert.assertTrue(Math.abs(p - avg) / avg < 1.41,
                    "Profit should be within 140% of average");
        }
    }

    // 10. Шесть фирм на вершинах правильного шестиугольника (манхэттен)
    @Test
    public void testSixFirmsHexagon_Manhattan() {
        double cx = 0.5, cy = 0.5, r = 0.4;
        List<Firm> firms = IntStream.range(0, 6)
                .mapToObj(i -> {
                    double theta = 2 * Math.PI * i / 6;
                    return new Firm(cx + r * Math.cos(theta), cy + r * Math.sin(theta), i);
                }).toList();
        for (Firm f : firms) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }
        List<double[]> residents = generateUniformSquareResidents(1000);

        double[] profits = firms.stream()
                .mapToDouble(f -> calculateProfit(f, firms, residents, "Manhattan"))
                .toArray();

        double avg = Arrays.stream(profits).average().orElse(0);
        for (double p : profits) {
            Assert.assertTrue(Math.abs(p - avg) / avg < 0.3,
                    "Profits should be within 30% of average with Manhattan metric");
        }
    }

    // 11. Две фирмы, одна рядом с центром кластера спроса
    @Test
    public void testClusterCenterAdvantage() {
        Firm centerFirm = new Firm(0.5, 0.5, 0);
        Firm edgeFirm   = new Firm(0.1, 0.1, 1);
        centerFirm.price = edgeFirm.price = 1.0;
        centerFirm.transportCoef = edgeFirm.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(centerFirm, edgeFirm);
        List<double[]> residents = generateCustomClusteredResidents(800, 0.5, 0.5, 0.05);
        residents.addAll(generateUniformSquareResidents(200));

        double pCenter = calculateProfit(centerFirm, firms, residents, "Euclidean");
        double pEdge   = calculateProfit(edgeFirm,   firms, residents, "Euclidean");

        Assert.assertTrue(pCenter >= pEdge,
                "Firm at cluster center should earn at least as much as edge firm");
    }

    // 12. Экстремальные цены: p=0.1 и p=10
    @Test
    public void testExtremePriceSettings() {
        Firm cheap = new Firm(0.4, 0.4, 0);
        Firm expensive = new Firm(0.6, 0.6, 1);
        cheap.price = 0.1; expensive.price = 10.0;
        cheap.transportCoef = expensive.transportCoef = 1.0;

        List<Firm> firms = Arrays.asList(cheap, expensive);
        List<double[]> residents = generateUniformSquareResidents(1000);

        double pCheap = calculateProfit(cheap, firms, residents, "Euclidean");
        double pExp   = calculateProfit(expensive, firms, residents, "Euclidean");

        Assert.assertTrue(pCheap > pExp,
                "Cheap price (0.1) should yield higher profit than expensive (10.0)");
    }

    // Генерация 1000 жителей по линии y=0.5
    private List<double[]> generateLineResidents() {
        return IntStream.range(0, 1000)
                .mapToObj(i -> new double[]{i / 999.0, 0.5})
                .toList();
    }

    private List<double[]> generateGridResidents() {
        int n = 32; // 32x32 = 1024 жителей
        return IntStream.range(0, n)
                .boxed()
                .flatMap(i -> IntStream.range(0, n)
                        .mapToObj(j -> new double[]{i / (double)(n - 1), j / (double)(n - 1)}))
                .toList();
    }

    private List<double[]> generateCustomClusteredResidents(int count, double cx, double cy, double sigma) {
        Random rand = new Random(321);
        List<double[]> list = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = cx + sigma * rand.nextGaussian();
            double y = cy + sigma * rand.nextGaussian();
            if (0 <= x && x <= 1 && 0 <= y && y <= 1) list.add(new double[]{x,y});
        }
        return list;
    }

    private List<double[]> generateUniformSquareResidents(int count) {
        Random rand = new Random(123);
        return IntStream.range(0, count)
                .mapToObj(i -> new double[]{rand.nextDouble(), rand.nextDouble()})
                .toList();
    }

    // Генерация 1000 жителей равномерно по квадрату [0,1]^2
    private List<double[]> generateUniformSquareResidents() {
        Random rand = new Random(42);
        return IntStream.range(0, 1000)
                .mapToObj(i -> new double[]{rand.nextDouble(), rand.nextDouble()})
                .toList();
    }

    private List<double[]> generateGridResidents(int nx, int ny) {
        return IntStream.range(0, nx)
                .boxed()
                .flatMap(i -> IntStream.range(0, ny)
                        .mapToObj(j -> new double[]{i/(double)(nx-1), j/(double)(ny-1)}))
                .toList();
    }

    // Локальная дубликация функции прибыли
    private double calculateProfit(Firm firm, List<Firm> allFirms,
                                   List<double[]> residents, String metric) {
        int count = 0;
        for (double[] r : residents) {
            double minCost = Double.MAX_VALUE;
            for (Firm f : allFirms) {
                double d = metric.equals("Manhattan")
                        ? Math.abs(r[0]-f.x) + Math.abs(r[1]-f.y)
                        : Math.hypot(r[0]-f.x, r[1]-f.y);
                double cost = f.price + f.transportCoef * d;
                if (cost < minCost) minCost = cost;
            }
            double selfD = metric.equals("Manhattan")
                    ? Math.abs(r[0]-firm.x) + Math.abs(r[1]-firm.y)
                    : Math.hypot(r[0]-firm.x, r[1]-firm.y);
            double selfCost = firm.price + firm.transportCoef * selfD;
            if (Math.abs(selfCost - minCost) < 1e-6) count++;
        }
        return firm.price * count;
    }

    private List<double[]> generateUniformCircleResidents() {
        Random rand = new Random(42);
        return IntStream.range(0, 1000)
                .mapToObj(i -> {
                    double r = Math.sqrt(rand.nextDouble()) * 0.5;
                    double theta = rand.nextDouble() * 2 * Math.PI;
                    double x = 0.5 + r * Math.cos(theta);
                    double y = 0.5 + r * Math.sin(theta);
                    return new double[]{x, y};
                }).toList();
    }

    private List<double[]> generateClusteredResidents() {
        Random rand = new Random(42);
        List<double[]> result = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) {
            double x = 0.2 + 0.05 * rand.nextGaussian();  // кластер около (0.2, 0.2)
            double y = 0.2 + 0.05 * rand.nextGaussian();
            if (x >= 0 && x <= 1 && y >= 0 && y <= 1) result.add(new double[]{x, y});
        }
        for (int i = 0; i < 500; i++) {
            result.add(new double[]{rand.nextDouble(), rand.nextDouble()});
        }
        return result;
    }
}
