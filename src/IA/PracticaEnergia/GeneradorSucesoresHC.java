package IA.PracticaEnergia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Generador de sucesores para Hill Climbing: devuelve TODOS los estados alcanzables
 * aplicando el conjunto de operadores elegido.
 *
 * El algoritmo de AIMA (HillClimbingSearch) pide la lista completa y se queda con el de
 * mejor heuristica, asi que el tamano de esta lista es a la vez el factor de ramificacion
 * y la memoria de pico de la busqueda. Por eso los operadores solo consideran las k
 * centrales / clientes mas cercanos: las combinaciones lejanas pierden hasta un 60% de la
 * energia en el transporte y casi nunca forman parte de una solucion buena.
 *
 * Las cadenas de accion son constantes compartidas y no se formatean por sucesor: con
 * decenas de miles de sucesores por paso, construir un String descriptivo para cada uno
 * costaria mas que generar el propio estado.
 */
public class GeneradorSucesoresHC implements SuccessorFunction {

    private static final String ACCION_MOVER = "MOVER";
    private static final String ACCION_ASIGNAR = "ASIGNAR";
    private static final String ACCION_DESASIGNAR = "DESASIGNAR";
    private static final String ACCION_INTERCAMBIAR = "INTERCAMBIAR";
    private static final String ACCION_CERRAR = "CERRAR";
    private static final String ACCION_ABRIR = "ABRIR";

    private final ConjuntoOperadores operadores;

    public GeneradorSucesoresHC(ConjuntoOperadores operadores) {
        this.operadores = operadores;
    }

    @Override
    public List getSuccessors(Object estado) {
        EstadoEnergia actual = (EstadoEnergia) estado;
        List<Successor> sucesores = new ArrayList<Successor>();

        int nClientes = EstadoEnergia.getNumClientes();
        int k = EstadoEnergia.getVecindario();

        if (operadores.mover) {
            for (int cli = 0; cli < nClientes; ++cli) {
                if (!actual.estaServido(cli)) continue;
                for (int pos = 0; pos < k; ++pos) {
                    int cen = EstadoEnergia.centralCercana(cli, pos);
                    if (!actual.puedeMover(cli, cen)) continue;
                    EstadoEnergia hijo = new EstadoEnergia(actual);
                    hijo.mover(cli, cen);
                    sucesores.add(new Successor(ACCION_MOVER, hijo));
                }
            }
        }

        if (operadores.altaBaja) {
            for (int cli = 0; cli < nClientes; ++cli) {
                if (actual.estaServido(cli)) {
                    if (!actual.puedeDesasignar(cli)) continue;
                    EstadoEnergia hijo = new EstadoEnergia(actual);
                    hijo.desasignar(cli);
                    sucesores.add(new Successor(ACCION_DESASIGNAR, hijo));
                } else {
                    for (int pos = 0; pos < k; ++pos) {
                        int cen = EstadoEnergia.centralCercana(cli, pos);
                        if (!actual.puedeAsignar(cli, cen)) continue;
                        EstadoEnergia hijo = new EstadoEnergia(actual);
                        hijo.asignar(cli, cen);
                        sucesores.add(new Successor(ACCION_ASIGNAR, hijo));
                    }
                }
            }
        }

        if (operadores.gestionCentrales) {
            for (int cen = 0; cen < EstadoEnergia.getNumCentrales(); ++cen) {
                EstadoEnergia hijo = new EstadoEnergia(actual);
                if (actual.estaEncendida(cen)) {
                    // Si el cierre falla el hijo queda a medias, por eso se descarta entero.
                    if (hijo.cierraCentral(cen)) sucesores.add(new Successor(ACCION_CERRAR, hijo));
                } else {
                    if (hijo.abreCentral(cen)) sucesores.add(new Successor(ACCION_ABRIR, hijo));
                }
            }
        }

        if (operadores.intercambiar) {
            for (int cliA = 0; cliA < nClientes; ++cliA) {
                if (!actual.estaServido(cliA)) continue;
                for (int pos = 0; pos < k; ++pos) {
                    int cliB = EstadoEnergia.clienteCercano(cliA, pos);
                    // cliA < cliB evita generar dos veces el mismo intercambio.
                    if (cliB <= cliA || !actual.estaServido(cliB)) continue;
                    if (!actual.puedeIntercambiar(cliA, cliB)) continue;
                    EstadoEnergia hijo = new EstadoEnergia(actual);
                    hijo.intercambiar(cliA, cliB);
                    sucesores.add(new Successor(ACCION_INTERCAMBIAR, hijo));
                }
            }
        }

        return sucesores;
    }
}
