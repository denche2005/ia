package IA.PracticaEnergia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generador de sucesores para Simulated Annealing: devuelve UN solo sucesor, obtenido
 * eligiendo al azar un operador del conjunto y al azar sus parametros.
 *
 * El motivo es el propio algoritmo: SimulatedAnnealingSearch llama a expandNode en cada
 * iteracion y despues se queda con un elemento al azar de la lista
 * (aima.basic.Util.selectRandomlyFromList). Generar la lista completa como en Hill
 * Climbing significaria construir decenas de miles de estados para tirar todos menos uno,
 * y el algoritmo hace decenas de miles de iteraciones. Con un unico sucesor por llamada,
 * cada iteracion es O(nClientes) en vez de O(nClientes * ramificacion).
 *
 * El generador nunca devuelve la lista vacia: si tras varios intentos no encuentra ningun
 * movimiento legal, devuelve una copia del estado actual. Asi SA ve un salto de energia
 * nulo y continua en lugar de fallar al seleccionar de una lista vacia.
 */
public class GeneradorSucesoresSA implements SuccessorFunction {

    private static final String ACCION = "SA";
    /** Intentos de generar un movimiento legal antes de devolver el estado sin cambios. */
    private static final int MAX_INTENTOS = 30;

    private final ConjuntoOperadores operadores;
    private final Random rnd;
    /** Operadores activos, precalculados para sortear uno en O(1). */
    private final int[] disponibles;

    public GeneradorSucesoresSA(ConjuntoOperadores operadores, int semilla) {
        this.operadores = operadores;
        this.rnd = new Random(semilla);

        int n = 0;
        int[] tmp = new int[4];
        if (operadores.mover) tmp[n++] = 0;
        if (operadores.altaBaja) tmp[n++] = 1;
        if (operadores.intercambiar) tmp[n++] = 2;
        if (operadores.gestionCentrales) tmp[n++] = 3;
        this.disponibles = new int[n];
        System.arraycopy(tmp, 0, this.disponibles, 0, n);
    }

    @Override
    public List getSuccessors(Object estado) {
        EstadoEnergia actual = (EstadoEnergia) estado;
        List<Successor> sucesores = new ArrayList<Successor>(1);

        for (int intento = 0; intento < MAX_INTENTOS; ++intento) {
            EstadoEnergia hijo = aplicaOperadorAlAzar(actual);
            if (hijo != null) {
                sucesores.add(new Successor(ACCION, hijo));
                return sucesores;
            }
        }

        sucesores.add(new Successor(ACCION, new EstadoEnergia(actual)));
        return sucesores;
    }

    /** Devuelve un sucesor aplicando un operador al azar, o null si la tirada no es legal. */
    private EstadoEnergia aplicaOperadorAlAzar(EstadoEnergia actual) {
        int nClientes = EstadoEnergia.getNumClientes();
        int k = EstadoEnergia.getVecindario();

        switch (disponibles[rnd.nextInt(disponibles.length)]) {
            case 0: { // mover
                int cli = rnd.nextInt(nClientes);
                if (!actual.estaServido(cli)) return null;
                int cen = EstadoEnergia.centralCercana(cli, rnd.nextInt(k));
                if (!actual.puedeMover(cli, cen)) return null;
                EstadoEnergia hijo = new EstadoEnergia(actual);
                hijo.mover(cli, cen);
                return hijo;
            }
            case 1: { // alta o baja
                int cli = rnd.nextInt(nClientes);
                if (actual.estaServido(cli)) {
                    if (!actual.puedeDesasignar(cli)) return null;
                    EstadoEnergia hijo = new EstadoEnergia(actual);
                    hijo.desasignar(cli);
                    return hijo;
                }
                int cen = EstadoEnergia.centralCercana(cli, rnd.nextInt(k));
                if (!actual.puedeAsignar(cli, cen)) return null;
                EstadoEnergia hijo = new EstadoEnergia(actual);
                hijo.asignar(cli, cen);
                return hijo;
            }
            case 2: { // intercambiar
                int cliA = rnd.nextInt(nClientes);
                int cliB = EstadoEnergia.clienteCercano(cliA, rnd.nextInt(k));
                if (!actual.puedeIntercambiar(cliA, cliB)) return null;
                EstadoEnergia hijo = new EstadoEnergia(actual);
                hijo.intercambiar(cliA, cliB);
                return hijo;
            }
            default: { // abrir o cerrar una central al azar
                int cen = rnd.nextInt(EstadoEnergia.getNumCentrales());
                EstadoEnergia hijo = new EstadoEnergia(actual);
                boolean ok = actual.estaEncendida(cen) ? hijo.cierraCentral(cen)
                                                       : hijo.abreCentral(cen);
                return ok ? hijo : null;
            }
        }
    }
}
