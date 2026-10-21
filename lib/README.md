# Llibreries

Aquesta carpeta conté les biblioteques binàries necessàries per compilar i
executar el codi propi de la pràctica.

## Contingut

- `CentralEnergia.jar`: biblioteca del problema energètic, proporcionada amb
  l'enunciat. Conté el paquet `IA.Energia`, amb les classes `Central`,
  `Centrales`, `Cliente`, `Clientes` i `VEnergia`.
- `AIMA.jar`: biblioteca AIMA proporcionada per l'assignatura. Conté la
  infraestructura i els algoritmes de cerca que utilitza el projecte.
- `lib/`: dependències distribuïdes amb AIMA, com JFreeChart, JCommon, iText i
  JUnit.

La carpeta interior `lib/` prové de `dist/lib/`, inclosa en `AIMA.zip`. No és
una còpia creada pel grup ni una carpeta exclusiva de les demostracions
d'AIMA: `Graficas.java` utilitza JFreeChart per generar les figures dels
experiments.

## Procedència i ubicació

- `AIMA.jar` i `lib/` provenen de `AIMA.zip`.
- `CentralEnergia.jar` prové de `CentralEnergia.zip`.

No s'ha de moure ni eliminar `lib/`: el manifest d'AIMA declara algunes
dependències amb rutes relatives `lib/...`, i la generació de gràfiques depèn
de JFreeChart i JCommon.

Des de l'arrel del repositori, les biblioteques principals s'afegeixen al
*classpath* amb:

```powershell
javac -cp "lib\CentralEnergia.jar;lib\AIMA.jar" ...
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" ...
```

La documentació del paquet `IA.Energia` es troba a
[`../docs/javadoc/`](../docs/javadoc/).
