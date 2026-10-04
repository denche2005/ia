package IA.PracticaEnergia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acumulador de las repeticiones de un experimento.
 *
 * El enunciado pide explicitamente no volcar los resultados individuales sino resumirlos:
 * aqui se guardan las repeticiones y se ofrecen media, desviacion tipica y los cuartiles
 * necesarios para dibujar un diagrama de caja.
 */
public class Estadistica {

    private final List<Double> valores = new ArrayList<Double>();

    public void anade(double v) {
        valores.add(v);
    }

    public int n() {
        return valores.size();
    }

    public double media() {
        if (valores.isEmpty()) return 0.0;
        double s = 0.0;
        for (double v : valores) s += v;
        return s / valores.size();
    }

    /** Desviacion tipica muestral (n-1), que es la adecuada para estimar a partir de replicas. */
    public double desviacion() {
        int n = valores.size();
        if (n < 2) return 0.0;
        double m = media();
        double s = 0.0;
        for (double v : valores) s += (v - m) * (v - m);
        return Math.sqrt(s / (n - 1));
    }

    public double minimo() {
        return Collections.min(valores);
    }

    public double maximo() {
        return Collections.max(valores);
    }

    public double mediana() {
        return cuantil(0.5);
    }

    public double cuantil(double p) {
        List<Double> orden = new ArrayList<Double>(valores);
        Collections.sort(orden);
        double pos = p * (orden.size() - 1);
        int i = (int) Math.floor(pos);
        double frac = pos - i;
        if (i + 1 >= orden.size()) return orden.get(orden.size() - 1);
        return orden.get(i) * (1 - frac) + orden.get(i + 1) * frac;
    }

    /** "media (desviacion)", el formato que usa el enunciado en sus tablas de ejemplo. */
    public String resumen() {
        return String.format("%,.2f (%,.2f)", media(), desviacion());
    }

    public List<Double> getValores() {
        return valores;
    }
}
