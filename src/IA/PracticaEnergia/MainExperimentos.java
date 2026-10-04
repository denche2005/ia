package IA.PracticaEnergia;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Bateria de los seis experimentos del enunciado.
 *
 * Cada experimento se repite {@link #REPETICIONES} veces con una semilla distinta pero con
 * el MISMO conjunto de semillas para todas las configuraciones comparadas, de manera que la
 * comparacion sea sobre escenarios identicos y las diferencias se deban a lo que se esta
 * midiendo y no al azar de la generacion de datos.
 *
 * Uso:
 * <pre>
 *   java -Xmx2g -cp out;lib/* IA.PracticaEnergia.MainExperimentos [1|2|3|4|5|6|todos]
 * </pre>
 * Los resultados detallados se escriben ademas en resultados/expN.csv para poder hacer
 * graficas con cualquier herramienta externa.
 */
public class MainExperimentos {

    /** Repeticiones por configuracion. El enunciado exige un minimo de 10. */
    private static final int REPETICIONES = 10;

    // Escenario base del enunciado (apartado 4.6.1).
    private static final int[] CENTRALES_BASE = {5, 10, 25};
    private static final int CLIENTES_BASE = 1000;
    private static final double[] PROPORCIONES_BASE = {0.25, 0.30, 0.45};
    private static final double PROP_GARANTIZADO = 0.75;

    /** Vecindario por defecto: cada cliente considera sus 8 centrales mas cercanas. */
    private static final int VECINDARIO_BASE = 8;

    // Parametros del Simulated Annealing ajustados en el experimento 3, que usan todos los
    // experimentos posteriores. Estan aqui como constantes para que no haya ningun experimento
    // comparando con un recocido configurado de otra manera.
    private static final int SA_PASOS = 100000;
    private static final int SA_ITER_POR_TEMP = 1000;
    private static final int SA_K = 5;
    private static final double SA_LAMBDA = 0.01;

    // Elementos que los experimentos 1 y 2 fijan para los siguientes. Se actualizan alli.
    private static ConjuntoOperadores operadoresElegidos = ConjuntoOperadores.MOVER_ASIGNAR_CENTRALES;
    private static EstadoEnergia.EstrategiaInicial inicialElegida =
            EstadoEnergia.EstrategiaInicial.VORAZ;

    private static PrintWriter csv;

    public static void main(String[] args) throws Exception {
        // Fija el formato numerico para que los CSV usen siempre el punto como separador
        // decimal, independientemente de la configuracion regional de la maquina.
        java.util.Locale.setDefault(java.util.Locale.US);

        String que = args.length > 0 ? args[0].toLowerCase() : "todos";
        new java.io.File("resultados").mkdirs();

        if (que.equals("smoke")) { smoke(); return; }
        if (que.equals("1") || que.equals("todos")) experimento1();
        if (que.equals("2") || que.equals("todos")) experimento2();
        if (que.equals("3") || que.equals("todos")) experimento3();
        if (que.equals("4") || que.equals("todos")) experimento4();
        if (que.equals("5") || que.equals("todos")) experimento5();
        if (que.equals("6") || que.equals("todos")) experimento6();
        if (que.equals("7") || que.equals("heuristicas") || que.equals("todos")) experimento7();
        if (que.equals("8") || que.equals("comparativa") || que.equals("todos")) comparativa();
    }

    // ==================================================================
    // Experimento 7: funciones heuristicas y ponderaciones
    // ==================================================================

    /**
     * Compara la heuristica del objetivo puro (H1) con las dos variantes que anaden un criterio
     * secundario ponderado (H2, perdidas de transporte; H3, capacidad ociosa) para varios pesos.
     *
     * La hipotesis es que un criterio secundario ayuda sobre todo con los operadores simples,
     * porque es el unico que distingue estados que el beneficio ve iguales, y que un peso
     * demasiado alto acaba siendo contraproducente porque deja de optimizarse el objetivo real.
     * Por eso se prueban los dos conjuntos de operadores y un rango amplio de pesos.
     *
     * En todas las filas el beneficio que se reporta es el beneficio REAL del estado final, no
     * el valor de la heuristica: solo asi son comparables entre si.
     */
    private static void experimento7() throws Exception {
        cabecera(7, "Funciones heuristicas y ponderaciones (Hill Climbing, inicial VORAZ)");
        abreCsv(7, "heuristica,criterio,peso,operadores,replica,beneficio,mwPerdidos,"
                + "mwDesperdiciados,tiempoMs,pasos");

        double[] pesos = {1, 10, 50, 200};
        ConjuntoOperadores[] conjuntos = {
                ConjuntoOperadores.MOVER_ASIGNAR,
                ConjuntoOperadores.MOVER_ASIGNAR_CENTRALES
        };

        for (ConjuntoOperadores ops : conjuntos) {
            System.out.println("-- operadores: " + ops);
            System.out.printf("%-20s %18s %12s %14s %10s%n",
                    "heuristica", "beneficio", "Mw perdidos", "tiempo (ms)", "pasos");

            java.util.List<FuncionHeuristica> variantes = new java.util.ArrayList<>();
            variantes.add(FuncionHeuristica.beneficio());
            for (double w : pesos) variantes.add(FuncionHeuristica.conPerdidas(w));
            for (double w : pesos) variantes.add(FuncionHeuristica.conCapacidadOciosa(w));

            for (FuncionHeuristica h : variantes) {
                Estadistica beneficio = new Estadistica();
                Estadistica perdidas = new Estadistica();
                Estadistica tiempo = new Estadistica();
                Estadistica pasos = new Estadistica();

                for (int r = 0; r < REPETICIONES; ++r) {
                    int semilla = semilla(r);
                    escenarioBase(semilla, VECINDARIO_BASE);
                    EstadoEnergia inicial = new EstadoEnergia(
                            EstadoEnergia.EstrategiaInicial.VORAZ, semilla);

                    Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, ops, h);

                    beneficio.anade(res.beneficio);
                    perdidas.anade(res.mwPerdidos);
                    tiempo.anade(res.tiempoMs);
                    pasos.anade(res.nodosExpandidos);
                    csv.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%d,%d%n",
                            h.nombre(), criterioDe(h.nombre()), pesoDe(h.nombre()), ops, r,
                            res.beneficio, res.mwPerdidos, res.mwDesperdiciados,
                            res.tiempoMs, res.nodosExpandidos);
                }

                System.out.printf("%-20s %18s %12.1f %14s %10s%n", h.nombre(),
                        beneficio.resumen(), perdidas.media(), tiempo.resumen(), pasos.resumen());
            }
            System.out.println();
        }
        cierraCsv();
    }

    /** Separa el nombre de la heuristica en criterio y peso para que el CSV sea facil de agrupar. */
    private static String criterioDe(String nombre) {
        return nombre.substring(0, nombre.indexOf(':'));
    }

    private static String pesoDe(String nombre) {
        int i = nombre.indexOf('(');
        return i < 0 ? "0" : nombre.substring(i + 1, nombre.length() - 1);
    }

    // ==================================================================
    // Comparacion final Hill Climbing frente a Simulated Annealing
    // ==================================================================

    /**
     * Enfrenta los dos algoritmos con el conjunto de operadores simples y con el que incluye
     * los operadores compuestos, para poder cuantificar cuanto de la diferencia entre ambos
     * se debe al algoritmo y cuanto al diseno de los operadores.
     */
    private static void comparativa() throws Exception {
        cabecera(8, "Hill Climbing frente a Simulated Annealing (inicial VORAZ)");
        abreCsv(8, "algoritmo,operadores,replica,beneficioInicial,beneficio,tiempoMs");

        ConjuntoOperadores[] conjuntos = {
                ConjuntoOperadores.MOVER_ASIGNAR,
                ConjuntoOperadores.MOVER_ASIGNAR_CENTRALES
        };

        System.out.printf("%-6s %-24s %20s %16s %12s%n",
                "alg", "operadores", "beneficio", "mejora inicial", "tiempo (ms)");

        for (ConjuntoOperadores ops : conjuntos) {
            for (String algoritmo : new String[]{"HC", "SA"}) {
                Estadistica beneficio = new Estadistica();
                Estadistica mejora = new Estadistica();
                Estadistica tiempo = new Estadistica();

                for (int r = 0; r < REPETICIONES; ++r) {
                    int semilla = semilla(r);
                    escenarioBase(semilla, VECINDARIO_BASE);
                    EstadoEnergia inicial = new EstadoEnergia(
                            EstadoEnergia.EstrategiaInicial.VORAZ, semilla);

                    Ejecucion.Resultado res = algoritmo.equals("HC")
                            ? Ejecucion.hillClimbing(inicial, ops)
                            : Ejecucion.simulatedAnnealing(inicial, ops, SA_PASOS, SA_ITER_POR_TEMP,
                                                            SA_K, SA_LAMBDA, semilla);

                    beneficio.anade(res.beneficio);
                    mejora.anade(res.beneficio - res.beneficioInicial);
                    tiempo.anade(res.tiempoMs);
                    csv.printf("%s,%s,%d,%.2f,%.2f,%d%n", algoritmo, ops, r,
                            res.beneficioInicial, res.beneficio, res.tiempoMs);
                }

                System.out.printf("%-6s %-24s %20s %16s %12s%n", algoritmo, ops,
                        beneficio.resumen(), mejora.resumen(), tiempo.resumen());
            }
        }
        cierraCsv();
    }

    /**
     * Una ejecucion de cada algoritmo y cada conjunto de operadores. Sirve para comprobar que
     * todo esta bien enlazado y, sobre todo, para estimar cuanto va a tardar la bateria
     * completa antes de lanzarla.
     */
    private static void smoke() throws Exception {
        System.out.println("Comprobacion rapida: una ejecucion por configuracion");
        System.out.printf("%-4s %-11s %-22s %16s %16s %12s %9s %7s%n",
                "alg", "inicial", "operadores", "inicio", "final", "tiempo (ms)", "pasos", "valido");

        for (EstadoEnergia.EstrategiaInicial est : new EstadoEnergia.EstrategiaInicial[]{
                EstadoEnergia.EstrategiaInicial.ALEATORIA, EstadoEnergia.EstrategiaInicial.VORAZ}) {
            for (ConjuntoOperadores ops : ConjuntoOperadores.values()) {
                escenarioBase(semilla(0), VECINDARIO_BASE);
                EstadoEnergia inicial = new EstadoEnergia(est, semilla(0));
                Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, ops);
                fila("HC", est, ops, res);
            }
        }

        for (ConjuntoOperadores ops : ConjuntoOperadores.values()) {
            escenarioBase(semilla(0), VECINDARIO_BASE);
            EstadoEnergia inicial = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, semilla(0));
            Ejecucion.Resultado res = Ejecucion.simulatedAnnealing(
                    inicial, ops, SA_PASOS, SA_ITER_POR_TEMP, SA_K, SA_LAMBDA, semilla(0));
            fila("SA", EstadoEnergia.EstrategiaInicial.VORAZ, ops, res);
        }
    }

    private static void fila(String alg, EstadoEnergia.EstrategiaInicial est,
                             ConjuntoOperadores ops, Ejecucion.Resultado res) {
        System.out.printf("%-4s %-11s %-22s %,16.0f %,16.0f %12d %9d %7b%n",
                alg, est, ops, res.beneficioInicial, res.beneficio, res.tiempoMs,
                res.nodosExpandidos, res.valido);
    }

    // ==================================================================
    // Experimento 1: que conjunto de operadores da mejores resultados
    // ==================================================================

    private static void experimento1() throws Exception {
        cabecera(1, "Conjunto de operadores (Hill Climbing, inicial VORAZ)");
        abreCsv(1, "operadores,vecindario,replica,beneficio,tiempoMs,pasos,centrales,servidos");

        ConjuntoOperadores[] conjuntos = ConjuntoOperadores.values();

        System.out.printf("%-24s %18s %14s %12s %10s%n",
                "operadores", "beneficio", "tiempo (ms)", "pasos", "centrales");
        ConjuntoOperadores mejor = null;
        double mejorMedia = Double.NEGATIVE_INFINITY;

        for (ConjuntoOperadores ops : conjuntos) {
            Estadistica beneficio = new Estadistica();
            Estadistica tiempo = new Estadistica();
            Estadistica pasos = new Estadistica();
            Estadistica centrales = new Estadistica();

            for (int r = 0; r < REPETICIONES; ++r) {
                int semilla = semilla(r);
                escenarioBase(semilla, VECINDARIO_BASE);
                EstadoEnergia inicial = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, semilla);
                Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, ops);
                acumula(beneficio, tiempo, pasos, centrales, res);
                csv.printf("%s,%d,%d,%.2f,%d,%d,%d,%d%n", ops, VECINDARIO_BASE, r,
                        res.beneficio, res.tiempoMs, res.nodosExpandidos, res.centralesEncendidas,
                        res.clientesServidos);
            }

            fila(ops.toString(), beneficio, tiempo, pasos, centrales);
            if (beneficio.media() > mejorMedia) {
                mejorMedia = beneficio.media();
                mejor = ops;
            }
        }

        // Efecto del tamano del vecindario sobre la ramificacion, con los operadores base.
        System.out.println();
        System.out.println("Influencia del vecindario k (operadores " + mejor + "):");
        System.out.printf("%-24s %18s %14s %12s %10s%n",
                "k", "beneficio", "tiempo (ms)", "pasos", "centrales");
        for (int k : new int[]{2, 4, 8, 16, 40}) {
            Estadistica beneficio = new Estadistica();
            Estadistica tiempo = new Estadistica();
            Estadistica pasos = new Estadistica();
            Estadistica centrales = new Estadistica();
            for (int r = 0; r < REPETICIONES; ++r) {
                int semilla = semilla(r);
                escenarioBase(semilla, k);
                EstadoEnergia inicial = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, semilla);
                Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, mejor);
                acumula(beneficio, tiempo, pasos, centrales, res);
                csv.printf("%s,%d,%d,%.2f,%d,%d,%d,%d%n", mejor, k, r,
                        res.beneficio, res.tiempoMs, res.nodosExpandidos, res.centralesEncendidas,
                        res.clientesServidos);
            }
            fila("k=" + k, beneficio, tiempo, pasos, centrales);
        }

        operadoresElegidos = mejor;
        System.out.println();
        System.out.println(">> Operadores fijados para el resto de experimentos: " + operadoresElegidos);
        cierraCsv();
    }

    // ==================================================================
    // Experimento 2: que estrategia de solucion inicial es mejor
    // ==================================================================

    private static void experimento2() throws Exception {
        cabecera(2, "Estrategia de solucion inicial (Hill Climbing, operadores "
                + operadoresElegidos + ")");
        abreCsv(2, "estrategia,replica,beneficioInicial,beneficio,tiempoMs,pasos,centrales");

        EstadoEnergia.EstrategiaInicial[] estrategias = {
                EstadoEnergia.EstrategiaInicial.ALEATORIA,
                EstadoEnergia.EstrategiaInicial.VORAZ
        };

        System.out.printf("%-22s %18s %18s %14s %12s%n",
                "estrategia", "beneficio inicial", "beneficio final", "tiempo (ms)", "pasos");
        EstadoEnergia.EstrategiaInicial mejor = null;
        double mejorMedia = Double.NEGATIVE_INFINITY;
        int[] gana = new int[estrategias.length];

        Estadistica[] finales = new Estadistica[estrategias.length];
        for (int i = 0; i < estrategias.length; ++i) finales[i] = new Estadistica();

        for (int i = 0; i < estrategias.length; ++i) {
            EstadoEnergia.EstrategiaInicial est = estrategias[i];
            Estadistica inicialStat = new Estadistica();
            Estadistica tiempo = new Estadistica();
            Estadistica pasos = new Estadistica();
            Estadistica centrales = new Estadistica();

            for (int r = 0; r < REPETICIONES; ++r) {
                int semilla = semilla(r);
                escenarioBase(semilla, VECINDARIO_BASE);
                EstadoEnergia inicial = new EstadoEnergia(est, semilla);
                Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, operadoresElegidos);
                inicialStat.anade(res.beneficioInicial);
                finales[i].anade(res.beneficio);
                tiempo.anade(res.tiempoMs);
                pasos.anade(res.nodosExpandidos);
                centrales.anade(res.centralesEncendidas);
                csv.printf("%s,%d,%.2f,%.2f,%d,%d,%d%n", est, r, res.beneficioInicial,
                        res.beneficio, res.tiempoMs, res.nodosExpandidos, res.centralesEncendidas);
            }

            System.out.printf("%-22s %18s %18s %14s %12s%n", est,
                    inicialStat.resumen(), finales[i].resumen(), tiempo.resumen(), pasos.resumen());
            if (finales[i].media() > mejorMedia) {
                mejorMedia = finales[i].media();
                mejor = est;
            }
        }

        // Comparacion replica a replica: cuantas veces gana cada estrategia sobre el MISMO
        // escenario. Es la comparacion que propone el enunciado (test binomial).
        for (int r = 0; r < REPETICIONES; ++r) {
            int ganador = finales[0].getValores().get(r) > finales[1].getValores().get(r) ? 0 : 1;
            ++gana[ganador];
        }
        System.out.println();
        for (int i = 0; i < estrategias.length; ++i) {
            System.out.printf("%s gana en %d de %d replicas%n", estrategias[i], gana[i], REPETICIONES);
        }

        inicialElegida = mejor;
        System.out.println(">> Solucion inicial fijada para el resto de experimentos: " + inicialElegida);
        cierraCsv();
    }

    // ==================================================================
    // Experimento 3: parametros del Simulated Annealing
    // ==================================================================

    private static void experimento3() throws Exception {
        cabecera(3, "Parametros del Simulated Annealing (inicial " + inicialElegida
                + ", operadores " + operadoresElegidos + ")");
        abreCsv(3, "fase,pasos,iterPorTemp,k,lambda,replica,beneficio,tiempoMs");

        // Primero valores extremos de k y lambda, como recomienda el enunciado, y despues se
        // refina alrededor del mejor. La temperatura es F(T) = k * e^(-lambda*T).
        int pasos = 100000;
        int iterPorTemp = 100;
        int[] kes = {1, 5, 25, 125};
        double[] lambdas = {1.0, 0.01, 0.0001};

        System.out.printf("%-10s %-10s %18s %14s%n", "k", "lambda", "beneficio", "tiempo (ms)");
        int mejorK = kes[0];
        double mejorLambda = lambdas[0];
        double mejorMedia = Double.NEGATIVE_INFINITY;

        for (int k : kes) {
            for (double lambda : lambdas) {
                Estadistica beneficio = new Estadistica();
                Estadistica tiempo = new Estadistica();
                for (int r = 0; r < REPETICIONES; ++r) {
                    int semilla = semilla(r);
                    escenarioBase(semilla, VECINDARIO_BASE);
                    EstadoEnergia inicial = new EstadoEnergia(inicialElegida, semilla);
                    Ejecucion.Resultado res = Ejecucion.simulatedAnnealing(
                            inicial, operadoresElegidos, pasos, iterPorTemp, k, lambda, semilla);
                    beneficio.anade(res.beneficio);
                    tiempo.anade(res.tiempoMs);
                    csv.printf("temperatura,%d,%d,%d,%s,%d,%.2f,%d%n", pasos, iterPorTemp, k,
                            lambda, r, res.beneficio, res.tiempoMs);
                }
                System.out.printf("%-10d %-10s %18s %14s%n", k, lambda,
                        beneficio.resumen(), tiempo.resumen());
                if (beneficio.media() > mejorMedia) {
                    mejorMedia = beneficio.media();
                    mejorK = k;
                    mejorLambda = lambda;
                }
            }
        }

        System.out.println();
        System.out.println("Mejor combinacion de este barrido: k=" + mejorK + " lambda=" + mejorLambda
                + " (media " + String.format("%,.0f", mejorMedia) + ")");
        System.out.println("Aviso: el argmax cambia de una ejecucion a otra porque las diferencias");
        System.out.println("entre celdas son menores que el ruido. Se fijan k=" + SA_K + " y lambda="
                + SA_LAMBDA + ", que dan la misma calidad que lambda menores a menos coste.");

        System.out.println();
        System.out.println("Influencia del numero de iteraciones (k=" + SA_K
                + ", lambda=" + SA_LAMBDA + "):");
        System.out.printf("%-12s %-14s %18s %14s%n", "pasos", "iterPorTemp", "beneficio", "tiempo (ms)");
        for (int[] conf : new int[][]{{10000, 100}, {50000, 100}, {100000, 100}, {250000, 100},
                                      {100000, 10}, {100000, 1000}}) {
            Estadistica beneficio = new Estadistica();
            Estadistica tiempo = new Estadistica();
            for (int r = 0; r < REPETICIONES; ++r) {
                int semilla = semilla(r);
                escenarioBase(semilla, VECINDARIO_BASE);
                EstadoEnergia inicial = new EstadoEnergia(inicialElegida, semilla);
                Ejecucion.Resultado res = Ejecucion.simulatedAnnealing(
                        inicial, operadoresElegidos, conf[0], conf[1], SA_K, SA_LAMBDA, semilla);
                beneficio.anade(res.beneficio);
                tiempo.anade(res.tiempoMs);
                csv.printf("iteraciones,%d,%d,%d,%s,%d,%.2f,%d%n", conf[0], conf[1], SA_K,
                        SA_LAMBDA, r, res.beneficio, res.tiempoMs);
            }
            System.out.printf("%-12d %-14d %18s %14s%n", conf[0], conf[1],
                    beneficio.resumen(), tiempo.resumen());
        }

        System.out.println();
        System.out.println(">> Parametros SA para el resto de experimentos: " + SA_PASOS
                + " iteraciones, " + SA_ITER_POR_TEMP + " por temperatura, k=" + SA_K
                + ", lambda=" + SA_LAMBDA);
        cierraCsv();
    }

    // ==================================================================
    // Experimento 4: evolucion del tiempo con el tamano del problema
    // ==================================================================

    private static void experimento4() throws Exception {
        cabecera(4, "Escalado del tiempo de ejecucion (Hill Climbing)");
        abreCsv(4, "variable,valor,replica,beneficio,tiempoMs,pasos");

        System.out.println("Variando el numero de clientes (40 centrales):");
        System.out.printf("%-12s %18s %14s %12s %12s%n",
                "clientes", "beneficio", "tiempo (ms)", "pasos", "demanda/cap");
        for (int clientes = 250; clientes <= 1500; clientes += 250) {
            Estadistica beneficio = new Estadistica();
            Estadistica tiempo = new Estadistica();
            Estadistica pasos = new Estadistica();
            double ocupacionMinima = 0.0;
            boolean infactible = false;

            for (int r = 0; r < REPETICIONES && !infactible; ++r) {
                int semilla = semilla(r);
                EstadoEnergia.inicializaEscenario(CENTRALES_BASE, clientes, PROPORCIONES_BASE,
                        PROP_GARANTIZADO, semilla, semilla, VECINDARIO_BASE);
                ocupacionMinima = EstadoEnergia.cotaMinimaMwGarantizados()
                        / EstadoEnergia.getProduccionInstalada();
                try {
                    EstadoEnergia inicial = new EstadoEnergia(inicialElegida, semilla);
                    Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, operadoresElegidos);
                    beneficio.anade(res.beneficio);
                    tiempo.anade(res.tiempoMs);
                    pasos.anade(res.nodosExpandidos);
                    csv.printf("clientes,%d,%d,%.2f,%d,%d%n", clientes, r, res.beneficio,
                            res.tiempoMs, res.nodosExpandidos);
                } catch (EscenarioInfactibleException ex) {
                    infactible = true;
                }
            }

            if (infactible) {
                // No es un fallo del algoritmo: con 40 centrales fijas la demanda garantizada
                // supera la produccion instalada y el problema deja de tener solucion valida.
                System.out.printf("%-12d %18s %14s %12s %11.2f%n", clientes,
                        "INFACTIBLE", "-", "-", ocupacionMinima);
                break;
            }
            System.out.printf("%-12d %18s %14s %12s %11.2f%n", clientes,
                    beneficio.resumen(), tiempo.resumen(), pasos.resumen(), ocupacionMinima);
        }

        System.out.println();
        System.out.println("Variando el numero de centrales (1000 clientes, proporciones 1:2:5):");
        System.out.printf("%-12s %18s %14s %12s%n", "centrales", "beneficio", "tiempo (ms)", "pasos");
        for (int factor = 1; factor <= 4; ++factor) {
            int[] conf = {5 * factor, 10 * factor, 25 * factor};
            int total = conf[0] + conf[1] + conf[2];
            Estadistica beneficio = new Estadistica();
            Estadistica tiempo = new Estadistica();
            Estadistica pasos = new Estadistica();
            for (int r = 0; r < REPETICIONES; ++r) {
                int semilla = semilla(r);
                EstadoEnergia.inicializaEscenario(conf, CLIENTES_BASE, PROPORCIONES_BASE,
                        PROP_GARANTIZADO, semilla, semilla, VECINDARIO_BASE);
                EstadoEnergia inicial = new EstadoEnergia(inicialElegida, semilla);
                Ejecucion.Resultado res = Ejecucion.hillClimbing(inicial, operadoresElegidos);
                beneficio.anade(res.beneficio);
                tiempo.anade(res.tiempoMs);
                pasos.anade(res.nodosExpandidos);
                csv.printf("centrales,%d,%d,%.2f,%d,%d%n", total, r, res.beneficio,
                        res.tiempoMs, res.nodosExpandidos);
            }
            System.out.printf("%-12d %18s %14s %12s%n", total,
                    beneficio.resumen(), tiempo.resumen(), pasos.resumen());
        }
        cierraCsv();
    }

    // ==================================================================
    // Experimento 5: garantizados por penalizacion en vez de por restriccion
    // ==================================================================

    private static void experimento5() throws Exception {
        cabecera(5, "Penalizacion de garantizados sin servir, partiendo de solucion VACIA");
        abreCsv(5, "algoritmo,penalizacion,replica,beneficio,valido,garantizadosSinServir,tiempoMs");

        // Referencia util: la tarifa garantizada esta entre 400 y 600 euros/Mw, asi que una
        // penalizacion por debajo de ese orden nunca compensa encender centrales para servir
        // a un garantizado lejano.
        double[] penalizaciones = {0, 50, 100, 250, 500, 1000, 2500, 5000, 10000};

        System.out.printf("%-8s %-14s %18s %12s %22s %12s%n",
                "algoritmo", "penal (eur/Mw)", "beneficio", "validas", "garantizados s/servir",
                "tiempo (ms)");

        for (String algoritmo : new String[]{"HC", "SA"}) {
            for (double penal : penalizaciones) {
                Estadistica beneficio = new Estadistica();
                Estadistica sinServir = new Estadistica();
                Estadistica tiempo = new Estadistica();
                int validas = 0;

                for (int r = 0; r < REPETICIONES; ++r) {
                    int semilla = semilla(r);
                    escenarioBase(semilla, VECINDARIO_BASE);
                    // En este modo los garantizados dejan de ser una restriccion dura, asi que
                    // la solucion vacia ya es un punto de partida legal y los operadores de
                    // alta/baja pueden tocar a cualquier cliente.
                    EstadoEnergia.activaModoPenalizacion(penal);
                    EstadoEnergia inicial = new EstadoEnergia(
                            EstadoEnergia.EstrategiaInicial.VACIA, semilla);

                    Ejecucion.Resultado res = algoritmo.equals("HC")
                            ? Ejecucion.hillClimbing(inicial, operadoresElegidos)
                            : Ejecucion.simulatedAnnealing(inicial, operadoresElegidos,
                                    SA_PASOS, SA_ITER_POR_TEMP, SA_K, SA_LAMBDA, semilla);

                    beneficio.anade(res.beneficio);
                    sinServir.anade(res.garantizadosSinServir);
                    tiempo.anade(res.tiempoMs);
                    if (res.garantizadosSinServir == 0) ++validas;
                    csv.printf("%s,%.0f,%d,%.2f,%b,%d,%d%n", algoritmo, penal, r, res.beneficio,
                            res.garantizadosSinServir == 0, res.garantizadosSinServir, res.tiempoMs);

                    EstadoEnergia.desactivaModoPenalizacion();
                }

                System.out.printf("%-8s %-14.0f %18s %12s %22s %12s%n", algoritmo, penal,
                        beneficio.resumen(), validas + "/" + REPETICIONES,
                        sinServir.resumen(), tiempo.resumen());
            }
            System.out.println();
        }
        cierraCsv();
    }

    // ==================================================================
    // Experimento 6: mas centrales de tipo C
    // ==================================================================

    private static void experimento6() throws Exception {
        cabecera(6, "Duplicar y triplicar las centrales de tipo C");
        abreCsv(6, "algoritmo,centralesC,replica,beneficio,encendidasA,encendidasB,encendidasC,"
                + "servidos,mwDesperdiciados,tiempoMs");

        int[][] configuraciones = {{5, 10, 25}, {5, 10, 50}, {5, 10, 75}};

        System.out.printf("%-8s %-8s %18s %10s %10s %10s %14s%n",
                "algoritmo", "nC", "beneficio", "A usadas", "B usadas", "C usadas", "tiempo (ms)");

        for (String algoritmo : new String[]{"HC", "SA"}) {
            for (int[] conf : configuraciones) {
                Estadistica beneficio = new Estadistica();
                Estadistica usadasA = new Estadistica();
                Estadistica usadasB = new Estadistica();
                Estadistica usadasC = new Estadistica();
                Estadistica tiempo = new Estadistica();

                for (int r = 0; r < REPETICIONES; ++r) {
                    int semilla = semilla(r);
                    EstadoEnergia.inicializaEscenario(conf, CLIENTES_BASE, PROPORCIONES_BASE,
                            PROP_GARANTIZADO, semilla, semilla, VECINDARIO_BASE);
                    EstadoEnergia inicial = new EstadoEnergia(inicialElegida, semilla);

                    Ejecucion.Resultado res = algoritmo.equals("HC")
                            ? Ejecucion.hillClimbing(inicial, operadoresElegidos)
                            : Ejecucion.simulatedAnnealing(inicial, operadoresElegidos,
                                    SA_PASOS, SA_ITER_POR_TEMP, SA_K, SA_LAMBDA, semilla);

                    beneficio.anade(res.beneficio);
                    usadasA.anade(res.centralesA);
                    usadasB.anade(res.centralesB);
                    usadasC.anade(res.centralesC);
                    tiempo.anade(res.tiempoMs);
                    csv.printf("%s,%d,%d,%.2f,%d,%d,%d,%d,%.2f,%d%n", algoritmo, conf[2], r,
                            res.beneficio, res.centralesA, res.centralesB, res.centralesC,
                            res.clientesServidos, res.mwDesperdiciados, res.tiempoMs);
                }

                System.out.printf("%-8s %-8d %18s %10.1f %10.1f %10.1f %14s%n",
                        algoritmo, conf[2], beneficio.resumen(), usadasA.media(),
                        usadasB.media(), usadasC.media(), tiempo.resumen());
            }
            System.out.println();
        }
        cierraCsv();
    }

    // ==================================================================
    // Utilidades
    // ==================================================================

    private static void escenarioBase(int semilla, int vecindario) throws Exception {
        EstadoEnergia.inicializaEscenario(CENTRALES_BASE, CLIENTES_BASE, PROPORCIONES_BASE,
                PROP_GARANTIZADO, semilla, semilla, vecindario);
    }

    /** Semillas fijas: todas las configuraciones se comparan sobre los mismos escenarios. */
    private static int semilla(int replica) {
        return 1000 + replica * 77;
    }

    private static void acumula(Estadistica beneficio, Estadistica tiempo, Estadistica pasos,
                                Estadistica centrales, Ejecucion.Resultado res) {
        beneficio.anade(res.beneficio);
        tiempo.anade(res.tiempoMs);
        pasos.anade(res.nodosExpandidos);
        centrales.anade(res.centralesEncendidas);
    }

    private static void fila(String etiqueta, Estadistica beneficio, Estadistica tiempo,
                             Estadistica pasos, Estadistica centrales) {
        System.out.printf("%-24s %18s %14s %12s %10.1f%n", etiqueta, beneficio.resumen(),
                tiempo.resumen(), pasos.resumen(), centrales.media());
    }

    private static void cabecera(int numero, String titulo) {
        System.out.println();
        System.out.println("================================================================");
        System.out.println("EXPERIMENTO " + numero + ": " + titulo);
        System.out.println("(" + REPETICIONES + " repeticiones, media y (desviacion tipica))");
        System.out.println("================================================================");
    }

    private static void abreCsv(int numero, String cabeceraCsv) throws IOException {
        csv = new PrintWriter(new FileWriter("resultados/exp" + numero + ".csv"));
        csv.println(cabeceraCsv);
    }

    private static void cierraCsv() {
        if (csv != null) {
            csv.flush();
            csv.close();
            csv = null;
        }
    }
}
