package IA.PracticaEnergia;

/**
 * El escenario generado no admite ninguna solucion valida: no hay capacidad suficiente para
 * servir a todos los clientes con contrato garantizado.
 *
 * No es un error del programa sino una propiedad de los datos. Ocurre al hacer crecer el
 * numero de clientes manteniendo fijas las 40 centrales del escenario base: la produccion
 * instalada son unos 5.900 Mw y la demanda garantizada crece unos 3.900 Mw por cada 1.000
 * clientes, que ademas hay que multiplicar por la perdida de transporte. A partir de unos
 * 1.250 clientes la demanda garantizada supera lo que el parque puede producir y el problema
 * deja de tener solucion.
 */
public class EscenarioInfactibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EscenarioInfactibleException(String mensaje) {
        super(mensaje);
    }
}
