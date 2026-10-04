package IA.PracticaEnergia;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

/**
 * Genera las figuras del informe a partir de los CSV que deja {@link MainExperimentos}.
 *
 * Usa JFreeChart, que viene incluido en las dependencias del AIMA de la asignatura
 * (lib/lib/jfreechart-1.0.13.jar), asi que no hace falta ninguna herramienta externa.
 *
 * Criterios de representacion, siguiendo el capitulo 7 del enunciado:
 * <ul>
 *   <li>Donde hay una distribucion de replicas se usa un diagrama de caja, no la tabla de
 *       valores individuales dibujada como si fuera una serie temporal.</li>
 *   <li>En los diagramas de barras el eje de valores incluye el cero, para no exagerar
 *       visualmente diferencias que son pequenas.</li>
 *   <li>En los diagramas de caja el eje se autoajusta, porque lo que interesa comparar es la
 *       posicion relativa y la dispersion de las distribuciones.</li>
 * </ul>
 *
 * Uso:
 * <pre>
 *   java -cp out;lib/CentralEnergia.jar;lib/AIMA.jar IA.PracticaEnergia.Graficas
 * </pre>
 */
public class Graficas {

    private static final String DIR = "resultados";
    private static final int ANCHO = 780;
    private static final int ALTO = 480;

    public static void main(String[] args) throws Exception {
        java.util.Locale.setDefault(java.util.Locale.US);
        new File(DIR).mkdirs();

        figura1();
        figura2();
        figura3();
        figura4();
        figura5();
        figura6();
        figura7();
        figura8();

        System.out.println("Figuras generadas en " + DIR + "/");
    }

    // ------------------------------------------------------------------
    // Una figura por experimento
    // ------------------------------------------------------------------

    /** Experimento 1: reparto del beneficio segun el conjunto de operadores. */
    private static void figura1() throws IOException {
        List<Map<String, String>> filas = lee(1);
        if (filas == null) return;

        // El CSV mezcla el barrido de operadores con el de vecindario; para la figura de
        // operadores nos quedamos con el vecindario base.
        List<Map<String, String>> base = filtra(filas, "vecindario", "8");

        Map<String, List<Double>> porOperador = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> e : agrupa(base, "operadores", "beneficio").entrySet()) {
            porOperador.put(corto(e.getKey()), e.getValue());
        }
        guarda(cajas(porOperador,
                     "Experimento 1: beneficio segun el conjunto de operadores",
                     "conjunto de operadores", "beneficio (eur)"),
               "fig1a-operadores.png");

        // El barrido de k se hizo solo con el conjunto de operadores ya elegido, asi que se
        // filtra para no dibujar puntos sueltos de los demas conjuntos.
        List<Map<String, String>> barridoK =
                filtra(filas, "operadores", "MOVER_ASIGNAR_CENTRALES");
        guarda(lineas(medias(barridoK, "vecindario", "operadores", "tiempoMs"),
                      "Experimento 1: coste temporal segun el tamano del vecindario",
                      "vecindario k (centrales candidatas por cliente)", "tiempo medio (ms)", true),
               "fig1b-vecindario.png");
    }

    /** Experimento 2: influencia de la solucion inicial. */
    private static void figura2() throws IOException {
        List<Map<String, String>> filas = lee(2);
        if (filas == null) return;

        guarda(cajas(agrupa(filas, "estrategia", "beneficio"),
                     "Experimento 2: beneficio final segun la solucion inicial",
                     "estrategia inicial", "beneficio (eur)"),
               "fig2a-inicial-beneficio.png");

        // Barras: de donde se parte y a donde se llega. Con el cero incluido para que la
        // mejora se vea en su proporcion real respecto al beneficio total.
        DefaultCategoryDataset d = new DefaultCategoryDataset();
        for (Map.Entry<String, List<Double>> e
                : agrupa(filas, "estrategia", "beneficioInicial").entrySet()) {
            d.addValue(media(e.getValue()), "beneficio inicial", e.getKey());
        }
        for (Map.Entry<String, List<Double>> e : agrupa(filas, "estrategia", "beneficio").entrySet()) {
            d.addValue(media(e.getValue()), "beneficio final", e.getKey());
        }
        guarda(barras(d, "Experimento 2: cuanto aporta la busqueda a cada solucion inicial",
                      "estrategia inicial", "beneficio medio (eur)"),
               "fig2b-inicial-mejora.png");
    }

    /** Experimento 3: ajuste de los parametros del Simulated Annealing. */
    private static void figura3() throws IOException {
        List<Map<String, String>> filas = lee(3);
        if (filas == null) return;

        List<Map<String, String>> temp = filtra(filas, "fase", "temperatura");
        List<Map<String, String>> iter = filtra(filas, "fase", "iteraciones");

        // El beneficio apenas depende de k y lambda. Para no caer en el error del apartado 7.4
        // del enunciado (exagerar diferencias recortando el eje), el eje se abre como minimo a
        // una desviacion tipica de las replicas a cada lado de la media: asi se ve que la
        // variacion entre configuraciones es pequena comparada con la que hay entre escenarios.
        JFreeChart c = lineas(medias(temp, "lambda", "k", "beneficio"),
                "Experimento 3: beneficio segun la temperatura inicial k y el enfriamiento lambda",
                "lambda (velocidad de enfriamiento)", "beneficio medio (eur)", false);
        escalaConDispersion(c, columna(temp, "beneficio"));
        guarda(c, "fig3a-temperatura-beneficio.png");

        // Donde si hay un efecto grande es en el tiempo: con lambda=1 la temperatura se anula
        // enseguida y el recocido se detiene casi al empezar.
        guarda(lineas(medias(temp, "lambda", "k", "tiempoMs"),
                      "Experimento 3: coste temporal segun la temperatura inicial k y el enfriamiento lambda",
                      "lambda (velocidad de enfriamiento)", "tiempo medio (ms)", true),
               "fig3b-temperatura-tiempo.png");

        JFreeChart ci = lineas(medias(iter, "pasos", "iterPorTemp", "beneficio"),
                "Experimento 3: beneficio segun el numero de iteraciones",
                "iteraciones del recocido", "beneficio medio (eur)", false);
        escalaConDispersion(ci, columna(iter, "beneficio"));
        guarda(ci, "fig3c-iteraciones.png");
    }

    /** Experimento 4: escalado del problema. */
    private static void figura4() throws IOException {
        List<Map<String, String>> filas = lee(4);
        if (filas == null) return;

        // El CSV apila los dos barridos (numero de clientes y numero de centrales) en las
        // columnas variable/valor, asi que se dibuja una figura por barrido.
        guarda(lineas(medias(filtra(filas, "variable", "clientes"), "valor", "variable", "tiempoMs"),
                      "Experimento 4: coste temporal al aumentar los clientes (40 centrales)",
                      "numero de clientes", "tiempo medio (ms)", true),
               "fig4a-escalado-clientes.png");

        guarda(lineas(medias(filtra(filas, "variable", "centrales"), "valor", "variable", "tiempoMs"),
                      "Experimento 4: coste temporal al aumentar las centrales (1000 clientes)",
                      "numero de centrales", "tiempo medio (ms)", true),
               "fig4b-escalado-centrales.png");
    }

    /** Experimento 5: los garantizados como penalizacion en vez de como restriccion. */
    private static void figura5() throws IOException {
        List<Map<String, String>> filas = lee(5);
        if (filas == null) return;

        // Proporcion de ejecuciones que acaban respetando a todos los garantizados.
        Map<String, Map<String, List<Double>>> porAlg = new LinkedHashMap<>();
        for (Map<String, String> f : filas) {
            porAlg.computeIfAbsent(f.get("algoritmo"), x -> new LinkedHashMap<>())
                  .computeIfAbsent(f.get("penalizacion"), x -> new ArrayList<>())
                  .add(Boolean.parseBoolean(f.get("valido")) ? 100.0 : 0.0);
        }
        DefaultCategoryDataset d = new DefaultCategoryDataset();
        for (Map.Entry<String, Map<String, List<Double>>> alg : porAlg.entrySet()) {
            for (Map.Entry<String, List<Double>> p : alg.getValue().entrySet()) {
                d.addValue(media(p.getValue()), alg.getKey(), p.getKey());
            }
        }
        JFreeChart c = lineas(d, "Experimento 5: soluciones que respetan a todos los garantizados",
                              "penalizacion por Mw garantizado sin servir (eur)",
                              "ejecuciones validas (%)", false);
        ((CategoryPlot) c.getPlot()).getRangeAxis().setRange(-5, 105);
        guarda(c, "fig5-penalizacion.png");
    }

    /** Experimento 6: que centrales se usan al aumentar las de tipo C. */
    private static void figura6() throws IOException {
        List<Map<String, String>> filas = lee(6);
        if (filas == null) return;

        List<Map<String, String>> hc = filtra(filas, "algoritmo", "HC");
        DefaultCategoryDataset d = new DefaultCategoryDataset();
        for (String tipo : new String[]{"A", "B", "C"}) {
            for (Map.Entry<String, List<Double>> e
                    : agrupa(hc, "centralesC", "encendidas" + tipo).entrySet()) {
                d.addValue(media(e.getValue()), "tipo " + tipo, e.getKey());
            }
        }
        guarda(barras(d, "Experimento 6: centrales encendidas al ampliar el parque de tipo C",
                      "centrales de tipo C disponibles", "centrales encendidas (media)"),
               "fig6-tipos-centrales.png");
    }

    /** Experimento 7: funciones heuristicas y ponderaciones. */
    private static void figura7() throws IOException {
        List<Map<String, String>> filas = lee(7);
        if (filas == null) return;

        String[] pesos = {"1.0", "10.0", "50.0", "200.0"};

        // Figura principal: distribucion del beneficio de las tres heuristicas (con el peso
        // intermedio) en cada conjunto de operadores. Al ser diagramas de caja se ve a la vez
        // la diferencia entre medias y si esa diferencia es grande frente a la dispersion.
        DefaultBoxAndWhiskerCategoryDataset caja = new DefaultBoxAndWhiskerCategoryDataset();
        for (String ops : new String[]{"MOVER_ASIGNAR", "MOVER_ASIGNAR_CENTRALES"}) {
            String etiqueta = ops.equals("MOVER_ASIGNAR")
                    ? "operadores simples" : "operadores compuestos";
            List<Map<String, String>> sub = filtra(filas, "operadores", ops);
            caja.add(columna(filtra(sub, "criterio", "H1"), "beneficio"), "H1 beneficio", etiqueta);
            for (String crit : new String[]{"H2", "H3"}) {
                List<Double> v = columna(filtra(filtra(sub, "criterio", crit), "peso", "10.0"),
                                         "beneficio");
                if (!v.isEmpty()) {
                    caja.add(v, crit + (crit.equals("H2") ? " perdidas" : " cap. ociosa") + " (w=10)",
                             etiqueta);
                }
            }
        }
        guarda(cajasDe(caja, "Experimento 7: beneficio real segun la funcion heuristica",
                       "conjunto de operadores", "beneficio (eur)"),
               "fig7a-heuristicas.png");

        // Efecto del peso. Se dibuja una figura por conjunto de operadores porque los dos
        // niveles de beneficio estan muy separados y juntos aplastarian las dos curvas.
        for (String ops : new String[]{"MOVER_ASIGNAR", "MOVER_ASIGNAR_CENTRALES"}) {
            boolean simples = ops.equals("MOVER_ASIGNAR");
            List<Map<String, String>> sub = filtra(filas, "operadores", ops);

            DefaultCategoryDataset d = new DefaultCategoryDataset();
            double h1 = media(columna(filtra(sub, "criterio", "H1"), "beneficio"));
            for (String p : pesos) d.addValue(h1, "H1 (sin criterio secundario)", p);
            for (String crit : new String[]{"H2", "H3"}) {
                Map<String, List<Double>> porPeso =
                        agrupa(filtra(sub, "criterio", crit), "peso", "beneficio");
                for (String p : pesos) {
                    if (porPeso.containsKey(p)) d.addValue(media(porPeso.get(p)), crit, p);
                }
            }
            JFreeChart c = lineas(d, "Experimento 7: efecto del peso con operadores "
                            + (simples ? "simples" : "compuestos"),
                    "peso del criterio secundario (eur/Mw)", "beneficio medio (eur)", false);
            escalaConDispersion(c, columna(sub, "beneficio"));
            guarda(c, simples ? "fig7b-pesos-simples.png" : "fig7c-pesos-compuestos.png");
        }

        // Pasos de busqueda: es lo que mejor explica por que una heuristica ayuda o no.
        DefaultCategoryDataset p = new DefaultCategoryDataset();
        for (String ops : new String[]{"MOVER_ASIGNAR", "MOVER_ASIGNAR_CENTRALES"}) {
            String etiqueta = ops.equals("MOVER_ASIGNAR")
                    ? "operadores simples" : "operadores compuestos";
            List<Map<String, String>> sub = filtra(filas, "operadores", ops);
            for (String crit : new String[]{"H1", "H2", "H3"}) {
                List<Double> v = columna(filtra(sub, "criterio", crit), "pasos");
                if (!v.isEmpty()) p.addValue(media(v), etiqueta, crit);
            }
        }
        guarda(barras(p, "Experimento 7: pasos que consigue dar el Hill Climbing",
                      "funcion heuristica", "pasos (media sobre todos los pesos)"),
               "fig7d-heuristicas-pasos.png");
    }

    /** Experimento 8: Hill Climbing frente a Simulated Annealing. */
    private static void figura8() throws IOException {
        List<Map<String, String>> filas = lee(8);
        if (filas == null) return;

        DefaultBoxAndWhiskerCategoryDataset d = new DefaultBoxAndWhiskerCategoryDataset();
        for (String ops : new String[]{"MOVER_ASIGNAR", "MOVER_ASIGNAR_CENTRALES"}) {
            String corto = ops.equals("MOVER_ASIGNAR") ? "simples" : "compuestos";
            for (String alg : new String[]{"HC", "SA"}) {
                List<Double> v = new ArrayList<>();
                for (Map<String, String> f : filas) {
                    if (f.get("operadores").equals(ops) && f.get("algoritmo").equals(alg)) {
                        v.add(Double.parseDouble(f.get("beneficio")));
                    }
                }
                if (!v.isEmpty()) d.add(v, alg, "operadores " + corto);
            }
        }
        guarda(cajasDe(d, "Experimento 8: Hill Climbing frente a Simulated Annealing",
                       "conjunto de operadores", "beneficio (eur)"),
               "fig8-hc-vs-sa.png");
    }

    // ------------------------------------------------------------------
    // Construccion de graficos
    // ------------------------------------------------------------------

    private static JFreeChart cajas(Map<String, List<Double>> datos, String titulo,
                                    String ejeX, String ejeY) {
        DefaultBoxAndWhiskerCategoryDataset d = new DefaultBoxAndWhiskerCategoryDataset();
        for (Map.Entry<String, List<Double>> e : datos.entrySet()) {
            d.add(e.getValue(), "replicas", e.getKey());
        }
        return cajasDe(d, titulo, ejeX, ejeY);
    }

    private static JFreeChart cajasDe(DefaultBoxAndWhiskerCategoryDataset d, String titulo,
                                      String ejeX, String ejeY) {
        JFreeChart c = ChartFactory.createBoxAndWhiskerChart(titulo, ejeX, ejeY, d, true);
        CategoryPlot plot = (CategoryPlot) c.getPlot();
        BoxAndWhiskerRenderer r = (BoxAndWhiskerRenderer) plot.getRenderer();
        r.setFillBox(true);
        r.setMaximumBarWidth(0.12);
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
        return estiliza(c);
    }

    private static JFreeChart lineas(DefaultCategoryDataset d, String titulo, String ejeX,
                                     String ejeY, boolean desdeCero) {
        JFreeChart c = ChartFactory.createLineChart(titulo, ejeX, ejeY, d,
                PlotOrientation.VERTICAL, true, false, false);
        NumberAxis eje = (NumberAxis) ((CategoryPlot) c.getPlot()).getRangeAxis();
        eje.setAutoRangeIncludesZero(desdeCero);
        return estiliza(c);
    }

    /** Los diagramas de barras siempre arrancan en cero: comparan magnitudes, no tendencias. */
    private static JFreeChart barras(DefaultCategoryDataset d, String titulo, String ejeX, String ejeY) {
        JFreeChart c = ChartFactory.createBarChart(titulo, ejeX, ejeY, d,
                PlotOrientation.VERTICAL, true, false, false);
        ((NumberAxis) ((CategoryPlot) c.getPlot()).getRangeAxis()).setAutoRangeIncludesZero(true);
        return estiliza(c);
    }

    /**
     * Abre el eje de valores hasta cubrir al menos una desviacion tipica de los datos brutos a
     * cada lado de su media.
     *
     * Es la salvaguarda contra el error que describe el apartado 7.4 del enunciado: si las
     * medias de las configuraciones se diferencian en mucho menos de lo que varian las replicas
     * entre si, el autoajuste del eje las separaria visualmente como si la diferencia fuera
     * enorme. Con esta escala la figura muestra las diferencias en su proporcion real.
     */
    private static void escalaConDispersion(JFreeChart c, List<Double> crudos) {
        if (crudos.isEmpty()) return;
        double m = media(crudos);
        double s = 0.0;
        for (double x : crudos) s += (x - m) * (x - m);
        s = Math.sqrt(s / crudos.size());

        NumberAxis eje = (NumberAxis) ((CategoryPlot) c.getPlot()).getRangeAxis();
        Range actual = eje.getRange();
        eje.setRange(new Range(Math.min(actual.getLowerBound(), m - s),
                               Math.max(actual.getUpperBound(), m + s)));
    }

    /** Nombre compacto de un conjunto de operadores, para que quepa en el eje de categorias. */
    private static String corto(String operadores) {
        switch (operadores) {
            case "MOVER":                   return "MOV";
            case "MOVER_ASIGNAR":           return "MOV+ASIG";
            case "MOVER_INTERCAMBIAR":      return "MOV+INTER";
            case "MOVER_ASIGNAR_CENTRALES": return "MOV+ASIG+CEN";
            default:                        return operadores;
        }
    }

    private static JFreeChart estiliza(JFreeChart c) {
        c.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));
        c.setBackgroundPaint(java.awt.Color.WHITE);
        CategoryPlot plot = (CategoryPlot) c.getPlot();
        plot.setBackgroundPaint(new java.awt.Color(0xF2, 0xF2, 0xF2));
        plot.setRangeGridlinePaint(java.awt.Color.GRAY);
        return c;
    }

    private static void guarda(JFreeChart c, String nombre) throws IOException {
        ChartUtilities.saveChartAsPNG(new File(DIR, nombre), c, ANCHO, ALTO);
        System.out.println("  " + DIR + "/" + nombre);
    }

    // ------------------------------------------------------------------
    // Lectura y agregacion de los CSV
    // ------------------------------------------------------------------

    /** Devuelve null si el CSV no existe, para poder generar solo las figuras disponibles. */
    private static List<Map<String, String>> lee(int experimento) throws IOException {
        File f = new File(DIR, "exp" + experimento + ".csv");
        if (!f.exists()) {
            System.out.println("  (falta " + f.getPath() + ", se omiten sus figuras)");
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

    /** Todos los valores numericos de una columna, en el orden en que aparecen. */
    private static List<Double> columna(List<Map<String, String>> filas, String campo) {
        List<Double> r = new ArrayList<>();
        for (Map<String, String> f : filas) {
            String v = f.get(campo);
            if (v != null) r.add(Double.parseDouble(v));
        }
        return r;
    }

    /** Agrupa los valores de {@code valor} por el campo {@code clave}, conservando el orden. */
    private static Map<String, List<Double>> agrupa(List<Map<String, String>> filas,
                                                    String clave, String valor) {
        Map<String, List<Double>> r = new LinkedHashMap<>();
        for (Map<String, String> f : filas) {
            String k = f.get(clave);
            String v = f.get(valor);
            if (k == null || v == null) continue;
            r.computeIfAbsent(k, x -> new ArrayList<>()).add(Double.parseDouble(v));
        }
        return r;
    }

    /** Medias de {@code valor} con {@code x} en el eje de categorias y {@code serie} como leyenda. */
    private static DefaultCategoryDataset medias(List<Map<String, String>> filas, String x,
                                                 String serie, String valor) {
        Map<String, Map<String, List<Double>>> acum = new LinkedHashMap<>();
        for (Map<String, String> f : filas) {
            String s = f.get(serie);
            String cx = f.get(x);
            String v = f.get(valor);
            if (s == null || cx == null || v == null) continue;
            acum.computeIfAbsent(s, k -> new LinkedHashMap<>())
                .computeIfAbsent(cx, k -> new ArrayList<>())
                .add(Double.parseDouble(v));
        }
        DefaultCategoryDataset d = new DefaultCategoryDataset();
        for (Map.Entry<String, Map<String, List<Double>>> s : acum.entrySet()) {
            for (Map.Entry<String, List<Double>> c : s.getValue().entrySet()) {
                d.addValue(media(c.getValue()), s.getKey(), c.getKey());
            }
        }
        return d;
    }

    private static double media(List<Double> v) {
        if (v.isEmpty()) return 0.0;
        double s = 0.0;
        for (double x : v) s += x;
        return s / v.size();
    }
}
