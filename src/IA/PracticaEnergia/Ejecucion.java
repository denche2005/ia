package IA.PracticaEnergia;

import java.util.Properties;

import IA.Energia.Central;
import aima.search.framework.Problem;
import aima.search.framework.Search;
import aima.search.framework.SearchAgent;
import aima.search.informed.HillClimbingSearch;
import aima.search.informed.SimulatedAnnealingSearch;

/**
 * Lanza una busqueda sobre un estado inicial y recoge las medidas de cada ejecucion.
 *
 * Centraliza el montaje del Problem de AIMA para que todos los experimentos usen exactamente
 * los mismos elementos (test de estado final, funcion heuristica) y solo varie lo que se
 * esta midiendo.
 *
 * La busqueda se lanza a traves de {@link SearchAgent}, que es la forma en que AIMA espera que
 * se use: construye el Problem, ejecuta el algoritmo y expone las medidas internas en
 * {@code getInstrumentation()}. De ahi se lee {@code nodesExpanded}, que es el contador que
 * mantiene {@code NodeExpander} y que cuenta llamadas a la funcion generadora de sucesores.
 */
public class Ejecucion {

    /** Medidas de una ejecucion, todo lo que despues se agrega y se grafica. */
    public static class Resultado {
        public double beneficioInicial;
        public double beneficio;
        public long tiempoMs;
        public int nodosExpandidos;
        public boolean valido;
        public int clientesServidos;
        public int garantizadosSinServir;
        public int centralesEncendidas;
        public int centralesA;
        public int centralesB;
        public int centralesC;
        public double mwServidos;
        public double mwPerdidos;
        public double mwDesperdiciados;
    }

    public static Resultado hillClimbing(EstadoEnergia inicial, ConjuntoOperadores operadores)
            throws Exception {
        return hillClimbing(inicial, operadores, FuncionHeuristica.beneficio());
    }

    public static Resultado hillClimbing(EstadoEnergia inicial, ConjuntoOperadores operadores,
                                         FuncionHeuristica heuristica) throws Exception {
        HillClimbingSearch busqueda = new HillClimbingSearch();
        return ejecuta(inicial, new GeneradorSucesoresHC(operadores), busqueda, heuristica);
    }

    /**
     * @param pasos       iteraciones totales del recocido
     * @param iterPorTemp iteraciones que se mantienen a la misma temperatura
     * @param k           factor de escala de la temperatura, F(T) = k * e^(-lambda*T)
     * @param lambda      velocidad de enfriamiento
     * @param semilla     semilla del generador de sucesores aleatorios
     */
    public static Resultado simulatedAnnealing(EstadoEnergia inicial, ConjuntoOperadores operadores,
                                               int pasos, int iterPorTemp, int k, double lambda,
                                               int semilla) throws Exception {
        return simulatedAnnealing(inicial, operadores, pasos, iterPorTemp, k, lambda, semilla,
                                  FuncionHeuristica.beneficio());
    }

    public static Resultado simulatedAnnealing(EstadoEnergia inicial, ConjuntoOperadores operadores,
                                               int pasos, int iterPorTemp, int k, double lambda,
                                               int semilla, FuncionHeuristica heuristica)
            throws Exception {
        SimulatedAnnealingSearch busqueda = new SimulatedAnnealingSearch(pasos, iterPorTemp, k, lambda);
        return ejecuta(inicial, new GeneradorSucesoresSA(operadores, semilla), busqueda, heuristica);
    }

    private static Resultado ejecuta(EstadoEnergia inicial,
                                     aima.search.framework.SuccessorFunction sucesores,
                                     Search busqueda,
                                     FuncionHeuristica heuristica) throws Exception {
        Problem problema = new Problem(inicial, sucesores, new EstadoFinal(), heuristica);

        long t0 = System.nanoTime();
        SearchAgent agente = new SearchAgent(problema, busqueda);
        long t1 = System.nanoTime();

        // SearchAgent publica las medidas del algoritmo como Properties; nodesExpanded es el
        // numero de veces que se ha llamado al generador de sucesores.
        Properties medidas = agente.getInstrumentation();
        int nodos = Integer.parseInt(medidas.getProperty("nodesExpanded", "0"));

        EstadoEnergia fin = (EstadoEnergia) busqueda.getGoalState();
        return recoge(inicial, fin, (t1 - t0) / 1000000L, nodos);
    }

    private static Resultado recoge(EstadoEnergia inicial, EstadoEnergia fin, long tiempoMs,
                                    int nodos) {
        Resultado r = new Resultado();
        r.beneficioInicial = inicial.getBeneficio();
        r.beneficio = fin.getBeneficio();
        r.tiempoMs = tiempoMs;
        r.nodosExpandidos = nodos;
        r.valido = fin.esValido();
        r.clientesServidos = fin.getClientesServidos();
        r.garantizadosSinServir = fin.getGarantizadosSinServir();
        r.centralesEncendidas = fin.getCentralesEncendidas();
        r.centralesA = fin.getCentralesEncendidasDeTipo(Central.CENTRALA);
        r.centralesB = fin.getCentralesEncendidasDeTipo(Central.CENTRALB);
        r.centralesC = fin.getCentralesEncendidasDeTipo(Central.CENTRALC);
        r.mwServidos = fin.getMwServidos();
        r.mwPerdidos = fin.getMwPerdidos();
        r.mwDesperdiciados = fin.getMwDesperdiciados();
        return r;
    }
}
