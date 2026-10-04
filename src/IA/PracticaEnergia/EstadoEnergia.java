package IA.PracticaEnergia;

import IA.Energia.Central;
import IA.Energia.Centrales;
import IA.Energia.Cliente;
import IA.Energia.Clientes;
import IA.Energia.VEnergia;

import java.util.Random;

/**
 * Representacion del estado para el problema de asignacion de clientes a centrales.
 *
 * <h2>Decisiones de estructura de datos</h2>
 *
 * El espacio de busqueda es inmenso ((nCentrales+1)^nClientes) y Hill Climbing materializa
 * TODOS los sucesores de un nodo antes de elegir el mejor (aima.search.framework.NodeExpander
 * construye un ArrayList de Node). Por lo tanto el tamano de una instancia multiplica
 * directamente la memoria de pico, y el coste de clonar multiplica el tiempo de cada paso.
 * De ahi las dos reglas que sigue esta clase:
 *
 * <ol>
 * <li><b>Todo lo que no cambia es estatico.</b> Las listas de centrales y clientes, las
 *     tarifas, los costes y sobre todo la matriz de Mw necesarios (que depende solo de la
 *     geometria) se calculan UNA vez en {@link #inicializaEscenario} y se comparten entre
 *     todas las instancias. Un nodo nuevo no duplica ni un solo Cliente ni Central.</li>
 * <li><b>Lo que cambia son arrays primitivos.</b> La parte dinamica es un {@code int[]}
 *     indexado por id de cliente, mas dos vectores pequenos por central. Clonar un estado
 *     son tres {@code System.arraycopy} y copiar tres doubles: sin objetos intermedios, sin
 *     HashMap, sin autoboxing y sin recorrer estructuras enlazadas.</li>
 * </ol>
 *
 * Coste por instancia con el escenario base (1000 clientes, 40 centrales):
 * {@code int[1000]} = 4 KB, {@code double[40]} = 336 B, {@code int[40]} = 176 B, ~4,6 KB.
 * Una alternativa "natural" (una List&lt;List&lt;Cliente&gt;&gt; por central, o un
 * HashMap&lt;Cliente,Central&gt;) costaria del orden de 50-100 KB por nodo y obligaria a
 * recorrer objetos para clonar: entre 10x y 20x mas memoria y tiempo.
 *
 * <h2>Beneficio incremental</h2>
 *
 * El beneficio NO se recalcula recorriendo los 1000 clientes en cada sucesor. Se mantiene
 * como un campo que cada operador actualiza en O(1) con las diferencias que provoca. Esto
 * convierte cada generacion de sucesor en O(nClientes) por la copia del array (inevitable)
 * en lugar de O(nClientes + nCentrales) por la copia mas la reevaluacion.
 */
public final class EstadoEnergia {

    /** Valor de {@link #asignacion} para un cliente que no recibe suministro. */
    public static final int SIN_ASIGNAR = -1;

    /** Holgura para comparaciones de capacidad en coma flotante. */
    private static final double EPS = 1e-9;

    // ------------------------------------------------------------------
    // Parte estatica: informacion del problema, compartida por TODOS los nodos.
    // ------------------------------------------------------------------

    private static Centrales centrales;
    private static Clientes clientes;
    private static int nCentrales;
    private static int nClientes;

    /** Produccion maxima de cada central (Mw). */
    private static double[] produccion;
    /** Coste diario de la central si esta en marcha: produccion * costeMw + costeMarcha. */
    private static double[] costeEnMarcha;
    /** Coste diario de la central si esta parada. */
    private static double[] costeEnParada;
    /** costeEnMarcha - costeEnParada: lo que cuesta de mas encender la central. */
    private static double[] sobrecosteEncender;
    /** Suma de costeEnParada de todas las centrales: termino constante del beneficio. */
    private static double costeTodasParadas;

    /**
     * Mw que la central debe PRODUCIR para que al cliente le lleguen los Mw contratados,
     * ya con la perdida de transporte aplicada. Aplanada como [cliente * nCentrales + central]
     * en vez de double[][]: un unico objeto en vez de nClientes arrays, mejor localidad de
     * cache y sin indireccion doble en el bucle mas caliente del programa.
     */
    private static double[] mwNecesarios;

    /** Consumo contratado de cada cliente, cacheado para no ir a la lista en cada consulta. */
    private static double[] consumo;
    /** Ingreso por servir al cliente: tarifa segun tipo y contrato * consumo contratado. */
    private static double[] ingresoServir;
    /** Indemnizacion a pagar si el cliente no recibe suministro. */
    private static double[] penalNoServir;
    /** true si el cliente tiene contrato garantizado. */
    private static boolean[] garantizado;

    private static int[] idsGarantizados;
    private static int[] idsNoGarantizados;

    /**
     * Para cada cliente, las {@code k} centrales mas cercanas ordenadas por distancia.
     * Aplanada como [cliente * k + posicion].
     *
     * Sirve para acotar el factor de ramificacion: mover un cliente a cualquiera de las 40
     * centrales genera 40.000 sucesores por paso (mas de 170 MB de pico solo en estados),
     * mientras que restringirlo a las k mas cercanas lo deja en nClientes * k. Las centrales
     * lejanas practicamente nunca forman parte de una buena solucion porque la perdida de
     * transporte llega al 60%, asi que se pierde muy poca calidad.
     */
    private static int[] centralesCercanas;
    /** Para cada cliente, los {@code k} clientes mas cercanos. Usado por el operador swap. */
    private static int[] clientesCercanos;
    /** Tamano del vecindario. Si es >= nCentrales los operadores no estan restringidos. */
    private static int k;

    /**
     * Centrales cercanas que miran las estrategias de solucion inicial, independientemente
     * del vecindario {@link #k} que usen los operadores.
     *
     * Estan desacopladas a proposito por dos motivos. El primero es metodologico: al comparar
     * distintos valores de k en el experimento 1 interesa que todas las ejecuciones partan de
     * la MISMA solucion inicial, para que la diferencia mida solo el efecto de la ramificacion.
     * El segundo es de correccion: con el vecindario sin restringir las estrategias iniciales
     * colocan clientes en centrales lejanas, cada una de ellas consume hasta 2,5 veces su
     * consumo en capacidad, y como la capacidad instalada solo supera la demanda garantizada
     * en un factor ~1,5 el parque se agota antes de colocarlos a todos.
     */
    private static final int VECINOS_INICIAL = 8;

    /**
     * Modo del experimento 5: si es true los clientes garantizados pueden quedar sin servir
     * y su incumplimiento se cobra en la funcion heuristica en vez de prohibirse en los
     * operadores.
     */
    private static boolean modoPenalizacion;
    /** Euros por Mw no servido a un cliente garantizado cuando {@link #modoPenalizacion}. */
    private static double penalizacionGarantizado;

    // ------------------------------------------------------------------
    // Parte dinamica: lo unico que se copia al generar un sucesor.
    // ------------------------------------------------------------------

    /** Indice = id de cliente, valor = id de central asignada o {@link #SIN_ASIGNAR}. */
    private final int[] asignacion;
    /** Mw comprometidos en cada central (ya con perdidas). */
    private final double[] ocupacion;
    /** Numero de clientes asignados a cada central; >0 significa central en marcha. */
    private final int[] clientesPorCentral;

    /** Beneficio del estado, mantenido de forma incremental por los operadores. */
    private double beneficio;
    /** Numero de centrales encendidas, para no recorrer clientesPorCentral. */
    private int centralesEncendidas;
    /** Numero de clientes garantizados sin servir (solo puede ser >0 en modoPenalizacion). */
    private int garantizadosSinServir;

    // Magnitudes energeticas, tambien incrementales. No intervienen en el beneficio pero si en
    // las funciones heuristicas alternativas del apartado experimental, que necesitan poder
    // consultarlas en O(1) igual que el beneficio.

    /** Mw que hay que producir en total, es decir, consumo contratado mas perdidas. */
    private double mwProducidos;
    /** Mw contratados que se estan sirviendo, sin contar perdidas. */
    private double mwContratados;
    /** Suma de la produccion de las centrales en marcha, que es lo que se paga. */
    private double produccionEncendida;

    // ==================================================================
    // Inicializacion del escenario
    // ==================================================================

    /**
     * Genera un escenario aleatorio y precalcula toda la informacion estatica.
     * Debe llamarse antes de construir ningun estado.
     *
     * @param tiposCentrales numero de centrales de tipo A, B y C
     * @param numClientes    numero de clientes
     * @param propTipos      proporcion de clientes XG, MG y G (debe sumar 1)
     * @param propGarantizado proporcion de clientes con contrato garantizado
     * @param semillaCentrales semilla del generador de centrales
     * @param semillaClientes  semilla del generador de clientes
     * @param vecindario     numero de centrales/clientes cercanos que consideran los
     *                       operadores; usar {@link Integer#MAX_VALUE} para no restringir
     */
    public static void inicializaEscenario(int[] tiposCentrales, int numClientes, double[] propTipos,
                                           double propGarantizado, int semillaCentrales,
                                           int semillaClientes, int vecindario) throws Exception {
        centrales = new Centrales(tiposCentrales, semillaCentrales);
        clientes = new Clientes(numClientes, propTipos, propGarantizado, semillaClientes);
        nCentrales = centrales.size();
        nClientes = clientes.size();
        k = Math.min(vecindario, nCentrales);

        modoPenalizacion = false;
        penalizacionGarantizado = 0.0;

        precalculaCentrales();
        precalculaClientes();
        precalculaGeometria();
    }

    private static void precalculaCentrales() throws Exception {
        produccion = new double[nCentrales];
        costeEnMarcha = new double[nCentrales];
        costeEnParada = new double[nCentrales];
        sobrecosteEncender = new double[nCentrales];
        costeTodasParadas = 0.0;

        for (int c = 0; c < nCentrales; ++c) {
            Central central = centrales.get(c);
            int tipo = central.getTipo();
            produccion[c] = central.getProduccion();
            // Una central en marcha cobra toda su capacidad, se use o no: coste fijo + variable
            // sobre la produccion MAXIMA, no sobre la servida.
            costeEnMarcha[c] = produccion[c] * VEnergia.getCosteProduccionMW(tipo) + VEnergia.getCosteMarcha(tipo);
            costeEnParada[c] = VEnergia.getCosteParada(tipo);
            sobrecosteEncender[c] = costeEnMarcha[c] - costeEnParada[c];
            costeTodasParadas += costeEnParada[c];
        }
    }

    private static void precalculaClientes() throws Exception {
        consumo = new double[nClientes];
        ingresoServir = new double[nClientes];
        penalNoServir = new double[nClientes];
        garantizado = new boolean[nClientes];

        int numG = 0;
        for (int i = 0; i < nClientes; ++i) {
            Cliente cliente = clientes.get(i);
            int tipo = cliente.getTipo();
            double cons = cliente.getConsumo();
            boolean esG = cliente.getContrato() == Cliente.GARANTIZADO;
            consumo[i] = cons;
            garantizado[i] = esG;
            if (esG) ++numG;

            // Se factura el consumo CONTRATADO, no los Mw producidos: la perdida de transporte
            // la paga la compania en forma de capacidad consumida, no el cliente.
            ingresoServir[i] = cons * (esG ? VEnergia.getTarifaClienteGarantizada(tipo)
                                           : VEnergia.getTarifaClienteNoGarantizada(tipo));
            // Los garantizados no tienen indemnizacion tabulada: o se sirven o la solucion no
            // es valida. En modoPenalizacion se les asigna un coste artificial (experimento 5).
            penalNoServir[i] = esG ? 0.0 : cons * VEnergia.getTarifaClientePenalizacion(tipo);
        }

        idsGarantizados = new int[numG];
        idsNoGarantizados = new int[nClientes - numG];
        int g = 0, ng = 0;
        for (int i = 0; i < nClientes; ++i) {
            if (garantizado[i]) idsGarantizados[g++] = i;
            else idsNoGarantizados[ng++] = i;
        }
    }

    private static void precalculaGeometria() {
        mwNecesarios = new double[nClientes * nCentrales];
        centralesCercanas = new int[nClientes * k];

        double[] distancias = new double[nCentrales];
        for (int i = 0; i < nClientes; ++i) {
            Cliente cliente = clientes.get(i);
            int cx = cliente.getCoordX();
            int cy = cliente.getCoordY();
            double consumo = cliente.getConsumo();
            int base = i * nCentrales;

            for (int c = 0; c < nCentrales; ++c) {
                Central central = centrales.get(c);
                double dx = central.getCoordX() - cx;
                double dy = central.getCoordY() - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                distancias[c] = dist;
                // Si la perdida es p, para entregar `consumo` hay que producir consumo/(1-p).
                mwNecesarios[base + c] = consumo / (1.0 - VEnergia.getPerdida(dist));
            }
            seleccionaMasCercanos(distancias, i * k, centralesCercanas);
        }

        precalculaClientesCercanos();
    }

    private static void precalculaClientesCercanos() {
        clientesCercanos = new int[nClientes * k];
        double[] distancias = new double[nClientes];
        for (int i = 0; i < nClientes; ++i) {
            Cliente a = clientes.get(i);
            for (int j = 0; j < nClientes; ++j) {
                if (j == i) {
                    distancias[j] = Double.MAX_VALUE;
                    continue;
                }
                Cliente b = clientes.get(j);
                double dx = a.getCoordX() - b.getCoordX();
                double dy = a.getCoordY() - b.getCoordY();
                distancias[j] = dx * dx + dy * dy;
            }
            seleccionaMasCercanos(distancias, i * k, clientesCercanos);
        }
    }

    /** Seleccion parcial de los k indices con menor distancia, escritos en destino[offset..offset+k). */
    private static void seleccionaMasCercanos(double[] distancias, int offset, int[] destino) {
        int n = distancias.length;
        boolean[] usado = new boolean[n];
        for (int pos = 0; pos < k; ++pos) {
            int mejor = -1;
            double mejorDist = Double.MAX_VALUE;
            for (int c = 0; c < n; ++c) {
                if (!usado[c] && distancias[c] < mejorDist) {
                    mejorDist = distancias[c];
                    mejor = c;
                }
            }
            usado[mejor] = true;
            destino[offset + pos] = mejor;
        }
    }

    /**
     * Activa el modo del experimento 5: los clientes garantizados dejan de ser una
     * restriccion dura de los operadores y pasan a penalizarse en el beneficio.
     *
     * @param eurosPorMw penalizacion por Mw contratado y no servido a un garantizado
     */
    public static void activaModoPenalizacion(double eurosPorMw) {
        modoPenalizacion = true;
        penalizacionGarantizado = eurosPorMw;
        for (int i = 0; i < nClientes; ++i) {
            if (garantizado[i]) {
                penalNoServir[i] = clientes.get(i).getConsumo() * eurosPorMw;
            }
        }
    }

    /** Vuelve al modo por defecto: garantizados obligatorios, sin penalizacion artificial. */
    public static void desactivaModoPenalizacion() throws Exception {
        modoPenalizacion = false;
        penalizacionGarantizado = 0.0;
        for (int i = 0; i < nClientes; ++i) {
            if (garantizado[i]) penalNoServir[i] = 0.0;
        }
    }

    // ==================================================================
    // Constructores
    // ==================================================================

    /** Estrategias de generacion de la solucion inicial. */
    public enum EstrategiaInicial {
        /**
         * Aleatoria valida. Recorre los clientes garantizados en orden aleatorio y asigna
         * cada uno a una central elegida al azar entre sus {@link #VECINOS_INICIAL} mas
         * cercanas que aun tengan hueco. Los no garantizados quedan sin servir.
         *
         * Elegir la central uniformemente entre las 40 no funciona: la distancia media entre
         * dos puntos de un cuadrado de 100x100 km es ~52 km, lo que da una perdida esperada
         * del orden del 50%, y los ~3.940 Mw contratados por los garantizados se convertirian
         * en ~5.900 Mw a producir, justo la capacidad total instalada. Restringir el azar a
         * las centrales cercanas mantiene la solucion factible sin dejar de ser aleatoria.
         */
        ALEATORIA,
        /**
         * Voraz. Sirve primero los garantizados grandes y reutiliza centrales ya encendidas
         * siempre que pueda, porque el coste marginal de anadir un cliente a una central en
         * marcha es CERO (ya se paga toda su produccion). Solo enciende una central nueva
         * cuando ninguna encendida tiene hueco, y entonces elige la de menor coste por Mw de
         * capacidad. Despues anade los no garantizados que quepan en lo ya encendido, que
         * son beneficio puro.
         */
        VORAZ,
        /**
         * Solucion vacia: ningun cliente servido, todas las centrales paradas. Solo es una
         * solucion valida en {@link #activaModoPenalizacion} (experimento 5).
         */
        VACIA
    }

    /**
     * Construye una solucion inicial.
     *
     * @param estrategia estrategia de generacion
     * @param semilla    semilla para las decisiones aleatorias (reproducibilidad)
     */
    public EstadoEnergia(EstrategiaInicial estrategia, int semilla) {
        asignacion = new int[nClientes];
        ocupacion = new double[nCentrales];
        clientesPorCentral = new int[nCentrales];
        java.util.Arrays.fill(asignacion, SIN_ASIGNAR);

        // Punto de partida: nada servido, todo parado. A partir de aqui los metodos de
        // asignacion van corrigiendo el beneficio de forma incremental.
        beneficio = -costeTodasParadas;
        for (int i = 0; i < nClientes; ++i) beneficio -= penalNoServir[i];
        centralesEncendidas = 0;
        garantizadosSinServir = idsGarantizados.length;

        switch (estrategia) {
            case ALEATORIA:
                inicialAleatoria(new Random(semilla));
                break;
            case VORAZ:
                inicialVoraz();
                break;
            case VACIA:
                break;
        }
    }

    /** Constructor de copia: la operacion mas frecuente del programa. */
    public EstadoEnergia(EstadoEnergia otro) {
        asignacion = otro.asignacion.clone();
        ocupacion = otro.ocupacion.clone();
        clientesPorCentral = otro.clientesPorCentral.clone();
        beneficio = otro.beneficio;
        centralesEncendidas = otro.centralesEncendidas;
        garantizadosSinServir = otro.garantizadosSinServir;
        mwProducidos = otro.mwProducidos;
        mwContratados = otro.mwContratados;
        produccionEncendida = otro.produccionEncendida;
    }

    private void inicialAleatoria(Random rnd) {
        int[] orden = idsGarantizados.clone();
        barajar(orden, rnd);

        int vecinos = Math.min(k, VECINOS_INICIAL);
        int[] candidatas = new int[vecinos];
        for (int cli : orden) {
            int base = cli * k;
            int n = 0;
            for (int pos = 0; pos < vecinos; ++pos) {
                int cen = centralesCercanas[base + pos];
                if (cabe(cli, cen)) candidatas[n++] = cen;
            }
            if (n > 0) {
                asigna(cli, candidatas[rnd.nextInt(n)]);
            } else {
                // Ninguna cercana tiene hueco: se recurre a cualquier central con capacidad
                // para no romper la restriccion de los garantizados.
                int cen = centralConMenorGasto(cli, SIN_ASIGNAR);
                if (cen == SIN_ASIGNAR) {
                    throw new EscenarioInfactibleException(
                            "No hay capacidad para servir al cliente garantizado " + cli);
                }
                asigna(cli, cen);
            }
        }
    }

    private void inicialVoraz() {
        Integer[] orden = ordenaPorConsumoDescendente(idsGarantizados);
        for (int idx = 0; idx < orden.length; ++idx) {
            int cli = orden[idx];
            int cen = mejorCentralVoraz(cli, true, SIN_ASIGNAR);
            if (cen == SIN_ASIGNAR) {
                throw new EscenarioInfactibleException(
                        "No hay capacidad para servir al cliente garantizado " + cli);
            }
            asigna(cli, cen);
        }

        // Los no garantizados se colocan solo en centrales YA encendidas: no cuestan nada
        // (la produccion ya esta pagada) y evitan la indemnizacion, asi que siempre suman.
        Integer[] ordenNG = ordenaPorConsumoDescendente(idsNoGarantizados);
        for (int idx = 0; idx < ordenNG.length; ++idx) {
            int cli = ordenNG[idx];
            int cen = mejorCentralVoraz(cli, false, SIN_ASIGNAR);
            if (cen != SIN_ASIGNAR) asigna(cli, cen);
        }
    }

    /**
     * Central preferida para un cliente, buscando solo entre sus {@link #VECINOS_INICIAL}
     * centrales mas cercanas. Primero las ya encendidas con hueco (su coste marginal es cero
     * porque su produccion ya esta pagada) y de esas la que menos capacidad consuma; si
     * ninguna encendida cabe y se permite encender, la apagada con hueco de menor coste por
     * Mw de capacidad instalada.
     */
    private int mejorCentralVoraz(int cli, boolean permiteEncender, int excluida) {
        int mejorEncendida = SIN_ASIGNAR;
        double mejorMw = Double.MAX_VALUE;
        int mejorApagada = SIN_ASIGNAR;
        double mejorRatio = Double.MAX_VALUE;

        int base = cli * nCentrales;
        int baseK = cli * k;
        int vecinos = Math.min(k, VECINOS_INICIAL);
        for (int pos = 0; pos < vecinos; ++pos) {
            int cen = centralesCercanas[baseK + pos];
            if (cen == excluida || !cabe(cli, cen)) continue;
            if (clientesPorCentral[cen] > 0) {
                double mw = mwNecesarios[base + cen];
                if (mw < mejorMw) {
                    mejorMw = mw;
                    mejorEncendida = cen;
                }
            } else if (permiteEncender) {
                double ratio = sobrecosteEncender[cen] / produccion[cen];
                if (ratio < mejorRatio) {
                    mejorRatio = ratio;
                    mejorApagada = cen;
                }
            }
        }
        if (mejorEncendida != SIN_ASIGNAR) return mejorEncendida;
        if (mejorApagada != SIN_ASIGNAR) return mejorApagada;
        // Ultimo recurso, solo para no dejar a un garantizado sin servir: cualquier central
        // con hueco, la que menos capacidad gaste.
        return permiteEncender ? centralConMenorGasto(cli, excluida) : SIN_ASIGNAR;
    }

    /** Central con hueco que menos capacidad consume para este cliente, mirando todas. */
    private int centralConMenorGasto(int cli, int excluida) {
        int mejor = SIN_ASIGNAR;
        double mejorMw = Double.MAX_VALUE;
        int base = cli * nCentrales;
        for (int cen = 0; cen < nCentrales; ++cen) {
            if (cen == excluida || !cabe(cli, cen)) continue;
            if (mwNecesarios[base + cen] < mejorMw) {
                mejorMw = mwNecesarios[base + cen];
                mejor = cen;
            }
        }
        return mejor;
    }

    private Integer[] ordenaPorConsumoDescendente(int[] ids) {
        Integer[] orden = new Integer[ids.length];
        for (int i = 0; i < ids.length; ++i) orden[i] = ids[i];
        java.util.Arrays.sort(orden, (a, b) ->
                Double.compare(clientes.get(b).getConsumo(), clientes.get(a).getConsumo()));
        return orden;
    }

    private static void barajar(int[] v, Random rnd) {
        for (int i = v.length - 1; i > 0; --i) {
            int j = rnd.nextInt(i + 1);
            int t = v[i];
            v[i] = v[j];
            v[j] = t;
        }
    }

    // ==================================================================
    // Operadores
    // ==================================================================

    /** true si la central {@code cen} tiene capacidad libre para el cliente {@code cli}. */
    public boolean cabe(int cli, int cen) {
        return ocupacion[cen] + mwNecesarios[cli * nCentrales + cen] <= produccion[cen] + EPS;
    }

    /**
     * true si se puede mover el cliente a otra central. Requiere que la central destino sea
     * distinta de la actual y que tenga hueco descontando lo que el cliente libera si ya
     * estaba en ella (nunca es el caso, pero mantiene el chequeo homogeneo).
     */
    public boolean puedeMover(int cli, int cen) {
        int actual = asignacion[cli];
        return actual != cen && actual != SIN_ASIGNAR && cabe(cli, cen);
    }

    /** Mueve un cliente ya servido a otra central. */
    public void mover(int cli, int cen) {
        desasigna(cli);
        asigna(cli, cen);
    }

    /**
     * true si se puede dar servicio a un cliente que ahora no lo tiene. En el modo por
     * defecto los garantizados ya estan siempre servidos, asi que esto solo afecta a los
     * no garantizados.
     */
    public boolean puedeAsignar(int cli, int cen) {
        return asignacion[cli] == SIN_ASIGNAR && cabe(cli, cen);
    }

    /** Da servicio a un cliente que no lo tenia. */
    public void asignar(int cli, int cen) {
        asigna(cli, cen);
    }

    /**
     * true si se puede retirar el suministro a un cliente. Un garantizado solo se puede
     * dejar sin servir en el modo del experimento 5; en el modo normal los operadores
     * mantienen la restriccion por construccion.
     */
    public boolean puedeDesasignar(int cli) {
        if (asignacion[cli] == SIN_ASIGNAR) return false;
        return modoPenalizacion || !garantizado[cli];
    }

    /** Retira el suministro a un cliente. */
    public void desasignar(int cli) {
        desasigna(cli);
    }

    /**
     * true si se pueden intercambiar las centrales de dos clientes servidos. Hay que
     * comprobar la capacidad con ambos clientes fuera de sus centrales, porque el
     * intercambio es simultaneo.
     */
    public boolean puedeIntercambiar(int cliA, int cliB) {
        int cenA = asignacion[cliA];
        int cenB = asignacion[cliB];
        if (cenA == SIN_ASIGNAR || cenB == SIN_ASIGNAR || cenA == cenB) return false;

        double libreA = produccion[cenA] - ocupacion[cenA] + mwNecesarios[cliA * nCentrales + cenA];
        double libreB = produccion[cenB] - ocupacion[cenB] + mwNecesarios[cliB * nCentrales + cenB];
        return mwNecesarios[cliB * nCentrales + cenA] <= libreA + EPS
                && mwNecesarios[cliA * nCentrales + cenB] <= libreB + EPS;
    }

    /**
     * Apaga una central reubicando a TODOS sus clientes en otras centrales.
     *
     * Este operador compuesto existe por una propiedad del problema que se ve en cuanto se
     * ejecuta Hill Climbing: el beneficio no depende de COMO se reparten los clientes sino
     * solo de que centrales estan encendidas, porque una central en marcha cuesta toda su
     * produccion la use o no. Mover un cliente suelto deja el beneficio exactamente igual,
     * asi que el espacio de busqueda es una meseta para el operador de mover y Hill Climbing,
     * que exige mejora estricta, se para en el primer paso. Vaciar una central entera si
     * cambia el beneficio, en {@link #sobrecosteEncender}, y es lo que permite que la
     * busqueda avance.
     *
     * Si algun cliente no cabe en ninguna otra central el cierre no es posible; en ese caso
     * el estado queda a medio modificar y el llamador debe descartarlo (los generadores de
     * sucesores siempre lo aplican sobre una copia recien hecha).
     *
     * @return true si la central ha quedado apagada
     */
    public boolean cierraCentral(int cen) {
        if (clientesPorCentral[cen] == 0) return false;
        for (int cli = 0; cli < nClientes; ++cli) {
            if (asignacion[cli] != cen) continue;
            desasigna(cli);
            int destino = mejorCentralVoraz(cli, true, cen);
            if (destino == SIN_ASIGNAR) return false;
            asigna(cli, destino);
        }
        return true;
    }

    /**
     * Enciende una central parada y la llena con los clientes sin servir que menos capacidad
     * consuman desde ella.
     *
     * Es el operador simetrico de {@link #cierraCentral(int)} y responde al mismo problema.
     * Encender una central para UN solo cliente nunca mejora el beneficio: el sobrecoste de
     * arranque mas barato es de unos 11.000 euros y el cliente mas rentable aporta del orden
     * de 8.000. Hill Climbing, que solo acepta mejoras estrictas, nunca da ese primer paso y
     * por eso se queda muy por debajo de Simulated Annealing, que si acepta empeorar un rato.
     * Abriendo y llenando la central en un unico movimiento, la mejora se ve entera de golpe
     * y Hill Climbing puede aprovecharla.
     *
     * @return true si la central ha quedado encendida con al menos un cliente
     */
    public boolean abreCentral(int cen) {
        if (clientesPorCentral[cen] > 0) return false;

        // Se llena la central como una mochila: por beneficio aportado dividido entre la
        // capacidad que consume. Este criterio prioriza a los clientes cercanos y rentables,
        // y ademas se adapta solo al modo del experimento 5, donde la penalizacion por dejar
        // sin servir a un garantizado infla su beneficio aportado y hace que sea el primero
        // en entrar.
        int n = 0;
        long[] candidatos = new long[nClientes];
        for (int cli = 0; cli < nClientes; ++cli) {
            if (asignacion[cli] != SIN_ASIGNAR) continue;
            double densidad = (ingresoServir[cli] + penalNoServir[cli])
                    / mwNecesarios[cli * nCentrales + cen];
            // Los bits de un double positivo, leidos como long, conservan el orden. Se dejan
            // libres los 20 bits bajos para guardar el identificador del cliente; perder esa
            // precision del mantisa no altera la ordenacion. Asi se ordena con un array
            // primitivo, sin objetos ni comparadores.
            candidatos[n++] = (Double.doubleToRawLongBits(densidad) & ~0xFFFFFL) | cli;
        }
        java.util.Arrays.sort(candidatos, 0, n);

        // De mayor a menor densidad.
        for (int i = n - 1; i >= 0; --i) {
            int cli = (int) (candidatos[i] & 0xFFFFF);
            if (cabe(cli, cen)) asigna(cli, cen);
        }
        return clientesPorCentral[cen] > 0;
    }

    /** Intercambia las centrales de dos clientes servidos. */
    public void intercambiar(int cliA, int cliB) {
        int cenA = asignacion[cliA];
        int cenB = asignacion[cliB];
        desasigna(cliA);
        desasigna(cliB);
        asigna(cliA, cenB);
        asigna(cliB, cenA);
    }

    // ------------------------------------------------------------------
    // Primitivas internas: unicos puntos donde se toca el beneficio.
    // ------------------------------------------------------------------

    private void asigna(int cli, int cen) {
        if (clientesPorCentral[cen] == 0) {
            // Encender la central: pasa de pagar parada a pagar marcha completa.
            beneficio -= sobrecosteEncender[cen];
            produccionEncendida += produccion[cen];
            ++centralesEncendidas;
        }
        double mw = mwNecesarios[cli * nCentrales + cen];
        ocupacion[cen] += mw;
        ++clientesPorCentral[cen];
        asignacion[cli] = cen;

        mwProducidos += mw;
        mwContratados += consumo[cli];
        beneficio += ingresoServir[cli] + penalNoServir[cli];
        if (garantizado[cli]) --garantizadosSinServir;
    }

    private void desasigna(int cli) {
        int cen = asignacion[cli];
        double mw = mwNecesarios[cli * nCentrales + cen];
        ocupacion[cen] -= mw;
        --clientesPorCentral[cen];
        if (clientesPorCentral[cen] == 0) {
            ocupacion[cen] = 0.0; // evita residuos de coma flotante al vaciar la central
            beneficio += sobrecosteEncender[cen];
            produccionEncendida -= produccion[cen];
            --centralesEncendidas;
        }
        asignacion[cli] = SIN_ASIGNAR;

        mwProducidos -= mw;
        mwContratados -= consumo[cli];
        beneficio -= ingresoServir[cli] + penalNoServir[cli];
        if (garantizado[cli]) ++garantizadosSinServir;
    }

    // ==================================================================
    // Consultas
    // ==================================================================

    /** Beneficio del estado en euros. La heuristica de AIMA usa su negativo (minimiza). */
    public double getBeneficio() {
        return beneficio;
    }

    public int getCentralAsignada(int cli) {
        return asignacion[cli];
    }

    public boolean estaServido(int cli) {
        return asignacion[cli] != SIN_ASIGNAR;
    }

    public double getOcupacion(int cen) {
        return ocupacion[cen];
    }

    public boolean estaEncendida(int cen) {
        return clientesPorCentral[cen] > 0;
    }

    public int getCentralesEncendidas() {
        return centralesEncendidas;
    }

    public int getGarantizadosSinServir() {
        return garantizadosSinServir;
    }

    /** Numero de centrales encendidas de un tipo dado (Central.CENTRALA/B/C). */
    public int getCentralesEncendidasDeTipo(int tipo) {
        int n = 0;
        for (int cen = 0; cen < nCentrales; ++cen) {
            if (clientesPorCentral[cen] > 0 && centrales.get(cen).getTipo() == tipo) ++n;
        }
        return n;
    }

    /** Mw contratados que se estan sirviendo (sin contar perdidas). */
    public double getMwServidos() {
        return mwContratados;
    }

    /** Mw que hay que producir para servirlos, es decir, contratados mas perdidas. */
    public double getMwProducidos() {
        return mwProducidos;
    }

    /**
     * Mw que se pierden en el transporte. Es la unica de estas magnitudes que cambia al mover
     * un cliente de central sin encender ni apagar ninguna, y por eso es la que puede dar un
     * gradiente a los operadores simples (ver la heuristica H2).
     */
    public double getMwPerdidos() {
        return mwProducidos - mwContratados;
    }

    /** Capacidad de las centrales en marcha que no se esta usando para nada. */
    public double getCapacidadOciosa() {
        return produccionEncendida - mwProducidos;
    }

    /** Mw que se pagan y no llegan a ningun cliente: perdidas mas capacidad ociosa. */
    public double getMwDesperdiciados() {
        return produccionEncendida - mwContratados;
    }

    public int getClientesServidos() {
        int n = 0;
        for (int i = 0; i < nClientes; ++i) {
            if (asignacion[i] != SIN_ASIGNAR) ++n;
        }
        return n;
    }

    /** Una solucion es valida si todos los garantizados reciben suministro. */
    public boolean esValido() {
        return garantizadosSinServir == 0;
    }

    // ==================================================================
    // Acceso a la parte estatica
    // ==================================================================

    public static int getNumClientes() {
        return nClientes;
    }

    /** Suma de la produccion de todas las centrales del escenario. */
    public static double getProduccionInstalada() {
        double total = 0.0;
        for (int cen = 0; cen < nCentrales; ++cen) total += produccion[cen];
        return total;
    }

    /**
     * Cota inferior de los Mw que hay que producir para servir a todos los garantizados,
     * suponiendo que cada uno se sirviese desde su central mas cercana. Si supera la
     * produccion instalada el escenario es seguro que no tiene solucion valida; si no la
     * supera puede seguir siendo infactible, porque la cota ignora los limites por central.
     */
    public static double cotaMinimaMwGarantizados() {
        double total = 0.0;
        for (int cli : idsGarantizados) {
            double mejor = Double.MAX_VALUE;
            int base = cli * nCentrales;
            for (int cen = 0; cen < nCentrales; ++cen) {
                if (mwNecesarios[base + cen] < mejor) mejor = mwNecesarios[base + cen];
            }
            total += mejor;
        }
        return total;
    }

    public static int getNumCentrales() {
        return nCentrales;
    }

    public static int getVecindario() {
        return k;
    }

    public static boolean esGarantizado(int cli) {
        return garantizado[cli];
    }

    public static int[] getIdsGarantizados() {
        return idsGarantizados;
    }

    public static int[] getIdsNoGarantizados() {
        return idsNoGarantizados;
    }

    public static boolean isModoPenalizacion() {
        return modoPenalizacion;
    }

    /** Central en la posicion {@code pos} del ranking de cercania del cliente {@code cli}. */
    public static int centralCercana(int cli, int pos) {
        return centralesCercanas[cli * k + pos];
    }

    /** Cliente en la posicion {@code pos} del ranking de cercania del cliente {@code cli}. */
    public static int clienteCercano(int cli, int pos) {
        return clientesCercanos[cli * k + pos];
    }

    public static Centrales getCentrales() {
        return centrales;
    }

    public static Clientes getClientes() {
        return clientes;
    }

    // ==================================================================
    // Verificacion y depuracion
    // ==================================================================

    /**
     * Recalcula el beneficio desde cero y comprueba las invariantes de capacidad. Solo se usa
     * en las pruebas: si el calculo incremental de los operadores se desviase del real, esto
     * lo detecta.
     */
    public double beneficioRecalculado() {
        double total = 0.0;
        for (int cen = 0; cen < nCentrales; ++cen) {
            total -= clientesPorCentral[cen] > 0 ? costeEnMarcha[cen] : costeEnParada[cen];
        }
        for (int i = 0; i < nClientes; ++i) {
            total += asignacion[i] != SIN_ASIGNAR ? ingresoServir[i] : -penalNoServir[i];
        }
        return total;
    }

    /** Comprueba capacidad, coherencia de contadores y beneficio. Devuelve null si todo esta bien. */
    public String comprueba() {
        double[] ocup = new double[nCentrales];
        int[] cuenta = new int[nCentrales];
        for (int i = 0; i < nClientes; ++i) {
            int cen = asignacion[i];
            if (cen == SIN_ASIGNAR) continue;
            ocup[cen] += mwNecesarios[i * nCentrales + cen];
            ++cuenta[cen];
        }
        for (int cen = 0; cen < nCentrales; ++cen) {
            if (ocup[cen] > produccion[cen] + 1e-6) {
                return "central " + cen + " sobrecargada: " + ocup[cen] + " > " + produccion[cen];
            }
            if (cuenta[cen] != clientesPorCentral[cen]) {
                return "contador de clientes incoherente en central " + cen;
            }
            if (Math.abs(ocup[cen] - ocupacion[cen]) > 1e-6) {
                return "ocupacion incoherente en central " + cen;
            }
        }
        if (Math.abs(beneficio - beneficioRecalculado()) > 1e-6) {
            return "beneficio incremental " + beneficio + " != recalculado " + beneficioRecalculado();
        }

        double mwProd = 0.0, mwContr = 0.0, prodOn = 0.0;
        for (int i = 0; i < nClientes; ++i) {
            int cen = asignacion[i];
            if (cen == SIN_ASIGNAR) continue;
            mwProd += mwNecesarios[i * nCentrales + cen];
            mwContr += consumo[i];
        }
        for (int cen = 0; cen < nCentrales; ++cen) {
            if (clientesPorCentral[cen] > 0) prodOn += produccion[cen];
        }
        if (Math.abs(mwProd - mwProducidos) > 1e-6) return "mwProducidos incoherente";
        if (Math.abs(mwContr - mwContratados) > 1e-6) return "mwContratados incoherente";
        if (Math.abs(prodOn - produccionEncendida) > 1e-6) return "produccionEncendida incoherente";
        if (!modoPenalizacion && garantizadosSinServir != 0) {
            return garantizadosSinServir + " clientes garantizados sin servir";
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("beneficio=%.2f centrales=%d/%d clientes=%d/%d garantizadosSinServir=%d",
                beneficio, centralesEncendidas, nCentrales, getClientesServidos(), nClientes,
                garantizadosSinServir);
    }
}
