package IA.PracticaEnergia;

import IA.Energia.Central;

import java.util.Random;

/**
 * Pruebas de la representacion del estado. Comprueba que el escenario se genera bien, que
 * las dos soluciones iniciales son validas, que clonar no comparte estructura con el padre
 * y que el beneficio incremental de los operadores coincide con el recalculado desde cero.
 *
 * Ejecutar con: java -cp out;lib/* IA.PracticaEnergia.PruebaEstado
 */
public class PruebaEstado {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        EstadoEnergia.inicializaEscenario(new int[]{5, 10, 25}, 1000,
                new double[]{0.25, 0.30, 0.45}, 0.75, 1234, 1234, 8);

        System.out.println("=== Escenario ===");
        System.out.println("centrales = " + EstadoEnergia.getNumCentrales()
                + "  clientes = " + EstadoEnergia.getNumClientes()
                + "  garantizados = " + EstadoEnergia.getIdsGarantizados().length
                + "  vecindario k = " + EstadoEnergia.getVecindario());

        EstadoEnergia aleatorio = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.ALEATORIA, 42);
        EstadoEnergia voraz = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, 42);

        System.out.println();
        System.out.println("=== Soluciones iniciales ===");
        informa("ALEATORIA", aleatorio);
        informa("VORAZ    ", voraz);

        System.out.println();
        System.out.println("=== Comprobaciones ===");
        compruebaEstado("inicial aleatoria", aleatorio);
        compruebaEstado("inicial voraz", voraz);
        afirma("aleatoria sirve a todos los garantizados", aleatorio.esValido());
        afirma("voraz sirve a todos los garantizados", voraz.esValido());
        afirma("voraz es mejor que aleatoria", voraz.getBeneficio() > aleatorio.getBeneficio());

        compruebaCopiaIndependiente(voraz);
        compruebaOperadores(voraz);
        compruebaAbrirCerrar(voraz);
        compruebaCapacidad(voraz);
        compruebaDeterminismo();
        mideCosteDeClonar(voraz);

        System.out.println();
        if (fallos == 0) {
            System.out.println("TODAS LAS PRUEBAS CORRECTAS");
        } else {
            System.out.println(fallos + " PRUEBAS FALLIDAS");
            System.exit(1);
        }
    }

    private static void informa(String nombre, EstadoEnergia e) {
        System.out.printf("%s beneficio=%,14.2f  centrales=%2d (A=%d B=%d C=%d)  clientes=%4d/%d  Mw servidos=%.1f%n",
                nombre, e.getBeneficio(), e.getCentralesEncendidas(),
                e.getCentralesEncendidasDeTipo(Central.CENTRALA),
                e.getCentralesEncendidasDeTipo(Central.CENTRALB),
                e.getCentralesEncendidasDeTipo(Central.CENTRALC),
                e.getClientesServidos(), EstadoEnergia.getNumClientes(), e.getMwServidos());
    }

    private static void compruebaEstado(String nombre, EstadoEnergia e) {
        String error = e.comprueba();
        afirma(nombre + " es coherente" + (error == null ? "" : " -> " + error), error == null);
    }

    /** Clonar debe copiar los arrays, no compartirlos: si no, HC corromperia el padre. */
    private static void compruebaCopiaIndependiente(EstadoEnergia original) {
        EstadoEnergia copia = new EstadoEnergia(original);
        afirma("la copia parte del mismo beneficio",
                copia.getBeneficio() == original.getBeneficio());

        int cli = primerClienteServido(original);
        int destino = buscaDestinoValido(copia, cli);
        afirma("hay un movimiento aplicable para la prueba de copia", destino != -1);
        if (destino == -1) return;

        double beneficioOriginal = original.getBeneficio();
        int centralOriginal = original.getCentralAsignada(cli);
        copia.mover(cli, destino);

        afirma("mutar la copia no cambia el beneficio del padre",
                original.getBeneficio() == beneficioOriginal);
        afirma("mutar la copia no cambia la asignacion del padre",
                original.getCentralAsignada(cli) == centralOriginal);
        compruebaEstado("copia tras mover", copia);
    }

    /**
     * Aplica muchas operaciones al azar y verifica que el beneficio mantenido de forma
     * incremental no se separa del recalculado desde cero.
     */
    private static void compruebaOperadores(EstadoEnergia base) {
        Random rnd = new Random(7);
        EstadoEnergia e = new EstadoEnergia(base);
        int nClientes = EstadoEnergia.getNumClientes();
        int k = EstadoEnergia.getVecindario();
        int aplicadas = 0;

        for (int paso = 0; paso < 20000; ++paso) {
            int cli = rnd.nextInt(nClientes);
            switch (rnd.nextInt(4)) {
                case 0: {
                    int cen = EstadoEnergia.centralCercana(cli, rnd.nextInt(k));
                    if (e.puedeMover(cli, cen)) { e.mover(cli, cen); ++aplicadas; }
                    break;
                }
                case 1: {
                    int cen = EstadoEnergia.centralCercana(cli, rnd.nextInt(k));
                    if (e.puedeAsignar(cli, cen)) { e.asignar(cli, cen); ++aplicadas; }
                    break;
                }
                case 2: {
                    if (e.puedeDesasignar(cli)) { e.desasignar(cli); ++aplicadas; }
                    break;
                }
                default: {
                    int otro = EstadoEnergia.clienteCercano(cli, rnd.nextInt(k));
                    if (e.puedeIntercambiar(cli, otro)) { e.intercambiar(cli, otro); ++aplicadas; }
                    break;
                }
            }
        }

        System.out.println("  (" + aplicadas + " operaciones aleatorias aplicadas)");
        compruebaEstado("estado tras operaciones aleatorias", e);
        afirma("los garantizados siguen servidos tras las operaciones", e.esValido());
        afirma("el beneficio incremental coincide con el recalculado",
                Math.abs(e.getBeneficio() - e.beneficioRecalculado()) < 1e-6);
    }

    /**
     * Los operadores compuestos se aplican sobre una copia y esta puede quedar a medias si
     * fallan, asi que se comprueba la coherencia solo cuando devuelven true.
     */
    private static void compruebaAbrirCerrar(EstadoEnergia base) {
        Random rnd = new Random(11);
        EstadoEnergia e = new EstadoEnergia(base);
        int nCentrales = EstadoEnergia.getNumCentrales();
        int cierres = 0, aperturas = 0, fallos = 0;

        boolean efectoCorrecto = true;
        String incoherencia = null;

        for (int paso = 0; paso < 500 && incoherencia == null; ++paso) {
            int cen = rnd.nextInt(nCentrales);
            EstadoEnergia copia = new EstadoEnergia(e);
            boolean encendida = e.estaEncendida(cen);
            boolean ok = encendida ? copia.cierraCentral(cen) : copia.abreCentral(cen);
            if (!ok) { ++fallos; continue; }

            if (encendida) {
                ++cierres;
                if (copia.estaEncendida(cen)) efectoCorrecto = false;
            } else {
                ++aperturas;
                if (!copia.estaEncendida(cen)) efectoCorrecto = false;
            }
            incoherencia = copia.comprueba();
            e = copia;
        }

        System.out.println("  (" + aperturas + " aperturas, " + cierres + " cierres, "
                + fallos + " intentos no aplicables)");
        afirma("se han podido abrir y cerrar centrales", aperturas > 0 && cierres > 0);
        afirma("abrir enciende y cerrar apaga la central", efectoCorrecto);
        afirma("estado coherente tras abrir/cerrar"
                + (incoherencia == null ? "" : " -> " + incoherencia), incoherencia == null);
        afirma("los garantizados siguen servidos tras abrir/cerrar", e.esValido());
    }

    /** Ningun operador debe poder sobrepasar la produccion de una central. */
    private static void compruebaCapacidad(EstadoEnergia base) {
        EstadoEnergia e = new EstadoEnergia(base);
        int nCentrales = EstadoEnergia.getNumCentrales();
        boolean ok = true;
        for (int cen = 0; cen < nCentrales; ++cen) {
            if (e.getOcupacion(cen) > EstadoEnergia.getCentrales().get(cen).getProduccion() + 1e-6) ok = false;
        }
        afirma("ninguna central supera su produccion", ok);
    }

    /** Con la misma semilla, el escenario y las soluciones iniciales deben repetirse. */
    private static void compruebaDeterminismo() throws Exception {
        EstadoEnergia.inicializaEscenario(new int[]{5, 10, 25}, 1000,
                new double[]{0.25, 0.30, 0.45}, 0.75, 99, 99, 8);
        double a = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.ALEATORIA, 5).getBeneficio();
        double v = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, 5).getBeneficio();

        EstadoEnergia.inicializaEscenario(new int[]{5, 10, 25}, 1000,
                new double[]{0.25, 0.30, 0.45}, 0.75, 99, 99, 8);
        double a2 = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.ALEATORIA, 5).getBeneficio();
        double v2 = new EstadoEnergia(EstadoEnergia.EstrategiaInicial.VORAZ, 5).getBeneficio();

        afirma("la solucion aleatoria es reproducible con la misma semilla", a == a2);
        afirma("la solucion voraz es reproducible", v == v2);
    }

    /** El clonado es la operacion mas repetida: conviene saber cuanto cuesta. */
    private static void mideCosteDeClonar(EstadoEnergia base) {
        int repeticiones = 200000;
        EstadoEnergia ultimo = null;
        long t0 = System.nanoTime();
        for (int i = 0; i < repeticiones; ++i) ultimo = new EstadoEnergia(base);
        long t1 = System.nanoTime();
        System.out.printf("  clonar un estado: %.0f ns (%d copias, ultimo beneficio %.0f)%n",
                (t1 - t0) / (double) repeticiones, repeticiones, ultimo.getBeneficio());
    }

    private static int primerClienteServido(EstadoEnergia e) {
        for (int i = 0; i < EstadoEnergia.getNumClientes(); ++i) {
            if (e.estaServido(i)) return i;
        }
        return -1;
    }

    private static int buscaDestinoValido(EstadoEnergia e, int cli) {
        for (int pos = 0; pos < EstadoEnergia.getVecindario(); ++pos) {
            int cen = EstadoEnergia.centralCercana(cli, pos);
            if (e.puedeMover(cli, cen)) return cen;
        }
        return -1;
    }

    private static void afirma(String descripcion, boolean condicion) {
        System.out.println((condicion ? "  OK   " : "  FALLO ") + descripcion);
        if (!condicion) ++fallos;
    }
}
