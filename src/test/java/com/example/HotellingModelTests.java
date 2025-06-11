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

    // Генерация 1000 жителей равномерно по квадрату [0,1]^2
    private List<double[]> generateUniformSquareResidents() {
        Random rand = new Random(42);
        return IntStream.range(0, 1000)
                .mapToObj(i -> new double[]{rand.nextDouble(), rand.nextDouble()})
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
}
