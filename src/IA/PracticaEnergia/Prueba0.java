package IA.PracticaEnergia;

import IA.Energia.*;

public class Prueba0 {
    public static void main(String[] args) throws Exception {
        int seed = 1234;
        Centrales cs = new Centrales(new int[]{5, 10, 25}, seed);
        Clientes cl = new Clientes(1000, new double[]{0.25, 0.30, 0.45}, 0.75, seed);

        System.out.println("Centrals: " + cs.size() + " | Clients: " + cl.size());
        System.out.println("Constants central: A=" + Central.CENTRALA
            + " B=" + Central.CENTRALB + " C=" + Central.CENTRALC);
        System.out.println("Constants client: XG=" + Cliente.CLIENTEXG
            + " MG=" + Cliente.CLIENTEMG + " G=" + Cliente.CLIENTEG
            + " GARANT=" + Cliente.GARANTIZADO + " NOGARANT=" + Cliente.NOGARANTIZADO);

        double prod = 0;
        for (int i = 0; i < cs.size(); i++) {
            Central c = cs.get(i);
            prod += c.getProduccion();
            if (i < 3) System.out.println("Central " + i + ": tipus=" + c.getTipo()
                + " prod=" + c.getProduccion() + " (" + c.getCoordX() + "," + c.getCoordY() + ")");
        }

        double dem = 0; int garant = 0;
        for (int i = 0; i < cl.size(); i++) {
            Cliente c = cl.get(i);
            dem += c.getConsumo();
            if (c.getContrato() == Cliente.GARANTIZADO) garant++;
            if (i < 3) System.out.println("Client " + i + ": tipus=" + c.getTipo()
                + " consum=" + c.getConsumo() + " contracte=" + c.getContrato()
                + " (" + c.getCoordX() + "," + c.getCoordY() + ")");
        }
        System.out.println("Producció total: " + prod + " MW | Demanda total: " + dem
            + " MW | Garantits: " + garant);

        System.out.println("Cost marxa A=" + VEnergia.getCosteMarcha(Central.CENTRALA)
            + " | Pèrdua a 30 km=" + VEnergia.getPerdida(30));

        double[] mn = {1e9,1e9,1e9}, mx = {0,0,0};
        double demGar = 0;
        for (int i = 0; i < cl.size(); i++) {
            Cliente c = cl.get(i);
            int t = c.getTipo();
            mn[t] = Math.min(mn[t], c.getConsumo());
            mx[t] = Math.max(mx[t], c.getConsumo());
            if (c.getContrato() == Cliente.GARANTIZADO) demGar += c.getConsumo();
        }
        for (int t = 0; t < 3; t++)
            System.out.println("Tipus " + t + ": consum min=" + mn[t] + " max=" + mx[t]);
        System.out.println("Demanda garantits (sense pèrdues): " + demGar);
    }
}
