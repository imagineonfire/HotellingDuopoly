package com.example;

import com.example.HotellingDuopoly.Firm;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class AlgorithmConvergenceTests {

    private List<double[]> generateLineResidents() {
        return IntStream.range(0, 1000)
                .mapToObj(i -> new double[]{i/999.0, 0.5})
                .toList();
    }

    private boolean checkNash(List<Firm> firms, List<double[]> residents, String metric) {
        // используем тот же критерий, что и в коде
        for (Firm firm : firms) {
            double current = calculateProfit(firm, firms, residents, metric);
            double best = ternarySearchPrice(firm, firms, residents, metric);
            if (best > current + 1e-4) return false;
        }
        return true;
    }

    private double calculateProfit(Firm firm, List<Firm> all, List<double[]> res, String metric) {
        int cnt = 0;
        for (double[] r : res) {
            double minC = Double.MAX_VALUE;
            for (Firm f : all) {
                double d = metric.equals("Manhattan")
                        ? Math.abs(r[0]-f.x)+Math.abs(r[1]-f.y)
                        : Math.hypot(r[0]-f.x, r[1]-f.y);
                minC = Math.min(minC, f.price + f.transportCoef * d);
            }
            double selfD = metric.equals("Manhattan")
                    ? Math.abs(r[0]-firm.x)+Math.abs(r[1]-firm.y)
                    : Math.hypot(r[0]-firm.x, r[1]-firm.y);
            double cost = firm.price + firm.transportCoef * selfD;
            if (Math.abs(cost - minC) < 1e-6) cnt++;
        }
        return cnt * firm.price;
    }

    private double ternarySearchPrice(Firm firm, List<Firm> all, List<double[]> res, String metric) {
        double l=0.1, r=10;
        for(int i=0;i<40;i++){
            double m1=l+(r-l)/3, m2=r-(r-l)/3;
            firm.price=m1; double p1=calculateProfit(firm, all,res,metric);
            firm.price=m2; double p2=calculateProfit(firm, all,res,metric);
            if(p1<p2) l=m1; else r=m2;
        }
        return (l+r)/2;
    }

    private void setupTwoFirms(List<Firm> firms) {
        for (Firm f : firms) {
            f.price = 1.0;
            f.transportCoef = 1.0;
        }
    }

    @Test
    public void testBestResponseConvergence() throws Exception {
        // две фирмы на концах отрезка
        List<Firm> firms = Arrays.asList(
                new Firm(0.0, 0.5, 0),
                new Firm(1.0, 0.5, 1)
        );
        setupTwoFirms(firms);
        List<double[]> residents = generateLineResidents();

        // вызываем private void bestResponseDynamics() через Reflection
        Method m = HotellingDuopoly.class.getDeclaredMethod("bestResponseDynamics");
        m.setAccessible(true);
        HotellingDuopoly app = new HotellingDuopoly();
        // подставляем наши фирмы и жителей (напрямую меняем поля)
        app.firms = firms; app.residents = residents; app.metric = "Euclidean";
        m.invoke(app);

        Assert.assertTrue(checkNash(firms, residents, "Euclidean"), "BestResponse did not reach Nash");
    }

    @Test
    public void testExhaustiveSearchConvergence() throws Exception {
        List<Firm> firms = Arrays.asList(
                new Firm(0.0, 0.5, 0),
                new Firm(1.0, 0.5, 1)
        );
        setupTwoFirms(firms);
        List<double[]> residents = generateLineResidents();

        Method m = HotellingDuopoly.class.getDeclaredMethod("exhaustiveGridSearchNash");
        m.setAccessible(true);
        HotellingDuopoly app = new HotellingDuopoly();
        app.firms = firms; app.residents = residents; app.metric = "Euclidean";
        m.invoke(app);

        Assert.assertTrue(checkNash(firms, residents, "Euclidean"), "ExhaustiveSearch did not reach Nash");
    }

    @Test
    public void testTernarySearchConvergence() throws Exception {
        List<Firm> firms = Arrays.asList(
                new Firm(0.0, 0.5, 0),
                new Firm(1.0, 0.5, 1)
        );
        setupTwoFirms(firms);
        List<double[]> residents = generateLineResidents();

        Method m = HotellingDuopoly.class.getDeclaredMethod("ternarySearchNash");
        m.setAccessible(true);
        HotellingDuopoly app = new HotellingDuopoly();
        app.firms = firms; app.residents = residents; app.metric = "Euclidean";
        m.invoke(app);

        Assert.assertTrue(checkNash(firms, residents, "Euclidean"), "TernarySearch did not reach Nash");
    }
}
