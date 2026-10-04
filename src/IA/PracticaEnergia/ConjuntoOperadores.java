package IA.PracticaEnergia;

/**
 * Conjuntos de operadores con los que se explora el espacio de busqueda.
 *
 * El compromiso a evaluar en el experimento 1 es factor de ramificacion frente a
 * conectividad: cuantos mas operadores, mas vecinos alcanzables desde un estado (mejor
 * calidad potencial) pero mas caro es cada paso de Hill Climbing, que genera y evalua
 * TODOS los sucesores antes de elegir uno.
 *
 * Con el escenario base (1000 clientes, 40 centrales) y vecindario k, las ramificaciones
 * aproximadas son:
 * <ul>
 * <li>MOVER: clientes servidos * k (unos 6.000 con k=8)</li>
 * <li>ALTA/BAJA: no garantizados sin servir * k, mas no garantizados servidos</li>
 * <li>INTERCAMBIAR: clientes servidos * k / 2 (unos 3.000 con k=8)</li>
 * <li>ABRIR/CERRAR: una por central (40), es el mas barato con diferencia</li>
 * </ul>
 */
public enum ConjuntoOperadores {

    /**
     * Solo mover clientes entre centrales. Es el conjunto minimo, pero por si solo no sirve:
     * el beneficio depende unicamente de que centrales estan encendidas, asi que casi todos
     * los movimientos individuales lo dejan igual y Hill Climbing, que exige mejora estricta,
     * se detiene enseguida.
     */
    MOVER(true, false, false, false),

    /**
     * Mover mas alta y baja de clientes no garantizados. Este conjunto ya alcanza cualquier
     * asignacion del espacio de soluciones dando de baja y de alta clientes uno a uno.
     */
    MOVER_ASIGNAR(true, true, false, false),

    /**
     * Mover mas intercambio entre clientes cercanos. El intercambio reordena dos clientes
     * cuando ninguno de los dos movimientos por separado cabe en la central destino, una
     * mejora que MOVER no alcanza en un solo paso.
     */
    MOVER_INTERCAMBIAR(true, false, true, false),

    /**
     * Mover, alta/baja y gestion de centrales (abrir llenando y cerrar reubicando). Los dos
     * operadores compuestos son los unicos que cambian el conjunto de centrales encendidas
     * mostrando de golpe toda la mejora que eso supone, que es justo lo que Hill Climbing
     * necesita para no quedarse parado. Ademas son baratisimos en ramificacion.
     */
    MOVER_ASIGNAR_CENTRALES(true, true, false, true),

    /** Los cuatro operadores. Maxima conectividad y maxima ramificacion. */
    COMPLETO(true, true, true, true);

    public final boolean mover;
    public final boolean altaBaja;
    public final boolean intercambiar;
    /** Abrir una central llenandola y cerrar una central reubicando a sus clientes. */
    public final boolean gestionCentrales;

    ConjuntoOperadores(boolean mover, boolean altaBaja, boolean intercambiar,
                       boolean gestionCentrales) {
        this.mover = mover;
        this.altaBaja = altaBaja;
        this.intercambiar = intercambiar;
        this.gestionCentrales = gestionCentrales;
    }
}
