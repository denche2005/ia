package IA.PracticaEnergia;

import aima.search.framework.HeuristicFunction;

/**
 * Funcion heuristica del problema. Los algoritmos de AIMA MINIMIZAN la heuristica
 * ({@code HillClimbingSearch} y {@code SimulatedAnnealingSearch} trabajan internamente con
 * {@code valor = -heuristica} y suben por ese valor), asi que todas las variantes de esta
 * clase devuelven el criterio de calidad del enunciado (4.3.6, maximizar la ganancia)
 * cambiado de signo.
 *
 * <p>El termino principal es siempre el mismo:
 *
 * <pre>
 * beneficio = ingresos por clientes servidos
 *           - coste de las centrales (marcha si tienen algun cliente, parada si no)
 *           - indemnizaciones a los no garantizados sin servir
 * </pre>
 *
 * <p>Las perdidas de transporte no aparecen como un termino de coste en euros: se traducen en
 * Mw adicionales que hay que producir, y por tanto consumen capacidad. Su efecto economico es
 * indirecto (obligan a encender mas centrales) y ya queda recogido en el coste de marcha.
 *
 * <p>El resto de criterios (4.3.1 a 4.3.5) no se penalizan porque los operadores no pueden
 * violarlos: la capacidad se comprueba antes de cada asignacion, cada cliente tiene una unica
 * central por construccion de la representacion, y los garantizados se sirven en la solucion
 * inicial y nunca se dan de baja. La excepcion es el experimento 5, donde la restriccion de
 * los garantizados se traslada a la heuristica via
 * {@link EstadoEnergia#activaModoPenalizacion(double)}.
 *
 * <h2>Por que hay mas de una variante</h2>
 *
 * El beneficio a secas es el objetivo real, pero como funcion guia tiene un defecto: es ciego
 * a los operadores que no encienden ni apagan ninguna central. Mover un cliente de una central
 * en marcha a otra tambien en marcha no cambia ni los ingresos ni los costes, luego el
 * beneficio no varia y el Hill Climbing ve una meseta. Las variantes H2 y H3 anaden un termino
 * secundario, ponderado, que si distingue esos estados y convierte parte de la meseta en
 * pendiente. Son criterios de desempate, no objetivos: el beneficio que se reporta en los
 * experimentos es siempre el real, nunca el valor de la heuristica.
 *
 * <p>Todas las variantes se evaluan en O(1) porque el estado mantiene el beneficio y las
 * magnitudes energeticas de forma incremental.
 */
public class FuncionHeuristica implements HeuristicFunction {

    /** Criterio secundario que se suma al beneficio, ya ponderado. */
    public enum Criterio {
        /** H1: solo el beneficio. Es el objetivo puro del enunciado. */
        NINGUNO,
        /**
         * H2: penaliza los Mw que se pierden en el transporte. Es el unico termino que cambia
         * al mover un cliente entre dos centrales ya encendidas, asi que es el que da gradiente
         * a los operadores simples.
         */
        PERDIDAS,
        /**
         * H3: penaliza la capacidad de las centrales en marcha que queda sin usar. Empuja a
         * concentrar los clientes en pocas centrales bien llenas, que es la forma de poder
         * apagar alguna mas adelante.
         */
        CAPACIDAD_OCIOSA
    }

    private final Criterio criterio;
    /** Euros equivalentes por cada Mw del criterio secundario. */
    private final double peso;

    private FuncionHeuristica(Criterio criterio, double peso) {
        this.criterio = criterio;
        this.peso = peso;
    }

    /** H1: heuristica del objetivo puro, sin criterio secundario. */
    public static FuncionHeuristica beneficio() {
        return new FuncionHeuristica(Criterio.NINGUNO, 0.0);
    }

    /** H2: beneficio penalizando las perdidas de transporte a {@code peso} eur/Mw. */
    public static FuncionHeuristica conPerdidas(double peso) {
        return new FuncionHeuristica(Criterio.PERDIDAS, peso);
    }

    /** H3: beneficio penalizando la capacidad ociosa a {@code peso} eur/Mw. */
    public static FuncionHeuristica conCapacidadOciosa(double peso) {
        return new FuncionHeuristica(Criterio.CAPACIDAD_OCIOSA, peso);
    }

    @Override
    public double getHeuristicValue(Object estado) {
        EstadoEnergia e = (EstadoEnergia) estado;
        double valor = e.getBeneficio();
        switch (criterio) {
            case PERDIDAS:          valor -= peso * e.getMwPerdidos();      break;
            case CAPACIDAD_OCIOSA:  valor -= peso * e.getCapacidadOciosa(); break;
            default:                                                        break;
        }
        return -valor;
    }

    /** Nombre corto para las tablas y los graficos de los experimentos. */
    public String nombre() {
        switch (criterio) {
            case PERDIDAS:         return "H2:perdidas(" + peso + ")";
            case CAPACIDAD_OCIOSA: return "H3:ociosa(" + peso + ")";
            default:               return "H1:beneficio";
        }
    }

    @Override
    public String toString() {
        return nombre();
    }
}
