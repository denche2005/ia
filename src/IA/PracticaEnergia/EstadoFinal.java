package IA.PracticaEnergia;

import aima.search.framework.GoalTest;

/**
 * Test de estado final.
 *
 * En busqueda local no existe una condicion que permita reconocer la solucion: no se busca
 * un estado concreto sino el mejor valor de una funcion objetivo, y no hay forma de saber
 * si el optimo local alcanzado es el global. El criterio de parada lo pone el propio
 * algoritmo (Hill Climbing se detiene cuando ningun sucesor mejora, Simulated Annealing
 * cuando agota las iteraciones), asi que esta funcion devuelve siempre false.
 */
public class EstadoFinal implements GoalTest {

    @Override
    public boolean isGoalState(Object estado) {
        return false;
    }
}
