package IA.PracticaEnergia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contraste por parejas de las configuraciones que compiten entre si.
 *
 * Todas las replicas de todas las configuraciones se ejecutan sobre los mismos escenarios (las
 * semillas estan fijadas en {@link MainExperimentos}), asi que las observaciones estan
 * emparejadas y se pueden comparar replica a replica. Eso permite el contraste que propone el
 * apartado 7.5 del enunciado: bajo la hipotesis nula de que las dos configuraciones son
 * igual de buenas, el numero de replicas en las que gana una de ellas sigue una binomial
 * B(n, 0.5), y el valor p bilateral dice hasta que punto el resultado observado es compatible
 * con esa hipotesis.
 *
 * Este contraste es mucho mas sensible que comparar las medias con sus desviaciones tipicas:
 * la variabilidad entre escenarios es grande (del orden de 45.000 eur) y enmascara diferencias
 * sistematicas mucho menores que, sin embargo, se dan en todas y cada una de las replicas.
 *
 * Uso:
 * <pre>
 *   java -cp out;lib/CentralEnergia.jar;lib/AIMA.jar IA.PracticaEnergia.Comparaciones
 * </pre>
 */
public class Comparaciones {

    private static final String DIR = "resultados";

    public static void main(String[] args) throws Exception {
        java.util.Locale.setDefault(java.util.Locale.US);

        titulo("Experimento 1: conjuntos de operadores (Hill Climbing)");
        List<Map<String, String>> e1 = lee(1);
        if (e1 != null) {
            List<Map<String, String>> base = filtra(e1, "vecindario", "8");
            compara(base, "operadores", "MOVER_ASIGNAR_CENTRALES", "MOVER_ASIGNAR", "beneficio");
            compara(base, "operadores", "MOVER_ASIGNAR", "MOVER", "beneficio");
            compara(base, "operadores", "COMPLETO", "MOVER_ASIGNAR_CENTRALES", "beneficio");
        }

        titulo("Experimento 2: estrategia de solucion inicial");
        List<Map<String, String>> e2 = lee(2);
        if (e2 != null) compara(e2, "estrategia", "VORAZ", "ALEATORIA", "beneficio");

        titulo("Experimento 7: funciones heuristicas");
        List<Map<String, String>> e7 = lee(7);
        if (e7 != null) {
            List<Map<String, String>> simples = filtra(e7, "operadores", "MOVER_ASIGNAR");
            System.out.println("-- operadores simples");
            comparaHeuristicas(simples);
            List<Map<String, String>> compuestos = filtra(e7, "operadores", "MOVER_ASIGNAR_CENTRALES");
            System.out.println("-- operadores compuestos");
            comparaHeuristicas(compuestos);
        }

        titulo("Experimento 8: Hill Climbing frente a Simulated Annealing");
        List<Map<String, String>> e8 = lee(8);
        if (e8 != null) {
            for (String ops : new String[]{"MOVER_ASIGNAR", "MOVER_ASIGNAR_CENTRALES"}) {
                System.out.println("-- operadores " + ops);
                compara(filtra(e8, "operadores", ops), "algoritmo", "SA", "HC", "beneficio");
            }
        }
    }

    private static void comparaHeuristicas(List<Map<String, String>> filas) {
        List<Map<String, String>> h1 = filtra(filas, "criterio", "H1");
        for (String crit : new String[]{"H2", "H3"}) {
            List<Map<String, String>> h = filtra(filtra(filas, "criterio", crit), "peso", "10.0");
            List<Map<String, String>> juntas = new ArrayList<>(h1);
            juntas.addAll(h);
            compara(juntas, "criterio", crit, "H1", "beneficio");
        }
    }

    // ------------------------------------------------------------------

    /**
     * Cuenta en cuantas replicas gana {@code a} a {@code b} y da el valor p bilateral de la
     * binomial. Los empates se reparten a partes iguales, que es lo conservador.
     */
    private static void compara(List<Map<String, String>> filas, String campo,
                                String a, String b, String valor) {
        Map<String, Double> va = porReplica(filas, campo, a, valor);
        Map<String, Double> vb = porReplica(filas, campo, b, valor);

        int ganaA = 0, ganaB = 0, empates = 0;
        double difTotal = 0.0;
        for (Map.Entry<String, Double> e : va.entrySet()) {
            Double otro = vb.get(e.getKey());
            if (otro == null) continue;
            double dif = e.getValue() - otro;
            difTotal += dif;
            if (dif > 1e-6) ++ganaA;
            else if (dif < -1e-6) ++ganaB;
            else ++empates;
        }
        int n = ganaA + ganaB + empates;
        if (n == 0) {
            System.out.printf("   %s vs %s: sin replicas comparables%n", a, b);
            return;
        }

        String veredicto;
        if (empates == n) {
            veredicto = "identicas en todas las replicas";
        } else {
            double p = pValorBilateral(Math.max(ganaA, ganaB), ganaA + ganaB);
            veredicto = String.format("p = %.4f  %s", p,
                    p < 0.05 ? "(diferencia significativa)" : "(no significativa)");
        }

        System.out.printf("   %-24s gana %2d/%d a %-24s  dif. media %+,12.0f   %s%n",
                a, ganaA, n, b, difTotal / n, veredicto);
    }

    /** Valor de {@code valor} en cada replica para las filas con {@code campo == cual}. */
    private static Map<String, Double> porReplica(List<Map<String, String>> filas, String campo,
                                                  String cual, String valor) {
        Map<String, Double> r = new TreeMap<>();
        for (Map<String, String> f : filas) {
            if (!cual.equals(f.get(campo))) continue;
            r.put(f.get("replica"), Double.parseDouble(f.get(valor)));
        }
        return r;
    }

    /** P(X >= exitos) * 2 bajo B(n, 0.5), acotado a 1. */
    private static double pValorBilateral(int exitos, int n) {
        if (n == 0) return 1.0;
        double cola = 0.0;
        for (int i = exitos; i <= n; ++i) cola += combinaciones(n, i);
        cola /= Math.pow(2, n);
        return Math.min(1.0, 2 * cola);
    }

    private static double combinaciones(int n, int k) {
        double r = 1.0;
        for (int i = 0; i < k; ++i) r = r * (n - i) / (i + 1);
        return r;
    }

    // ------------------------------------------------------------------

    private static void titulo(String t) {
        System.out.println();
        System.out.println("== " + t);
    }

    private static List<Map<String, String>> lee(int experimento) throws IOException {
        File f = new File(DIR, "exp" + experimento + ".csv");
        if (!f.exists()) {
            System.out.println("   (falta " + f.getPath() + ")");
            return null;
        }
        List<Map<String, String>> filas = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            String cabecera = in.readLine();
            if (cabecera == null) return filas;
            String[] campos = cabecera.split(",");
            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.isEmpty()) continue;
                String[] v = linea.split(",");
                Map<String, String> fila = new HashMap<>();
                for (int i = 0; i < campos.length && i < v.length; ++i) fila.put(campos[i], v[i]);
                filas.add(fila);
            }
        }
        return filas;
    }

    private static List<Map<String, String>> filtra(List<Map<String, String>> filas,
                                                    String campo, String valor) {
        List<Map<String, String>> r = new ArrayList<>();
        for (Map<String, String> f : filas) {
            if (valor.equals(f.get(campo))) r.add(f);
        }
        return r;
    }
}
