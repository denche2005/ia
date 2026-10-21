# Pràctica de cerca local: distribució d'energia

Implementació en Java de la primera pràctica d'Intel·ligència Artificial
(FIB-UPC): assignació de clients a centrals elèctriques maximitzant el benefici,
resolta amb Hill Climbing i Simulated Annealing sobre la llibreria AIMA de
l'assignatura.

L'anàlisi detallada dels resultats és a [INFORME.md](INFORME.md). El resum
preparat per lliurar en PDF és a [report/](report/).

## Estructura

```
lib/
  CentralEnergia.jar   libreria del enunciado, extraida de CentralEnergia.zip
  AIMA.jar             libreria de la asignatura, extraida de AIMA.zip (dist/AIMA.jar)
  lib/                 dependencias incluidas en AIMA.zip (dist/lib), como JFreeChart,
                       iText y JUnit. No es una carpeta exclusiva de las demos:
                       Graficas.java usa JFreeChart para generar las figuras del informe.
                       Van en lib/lib porque el manifest de AIMA.jar declara
                       "Class-Path: lib/..." relativo a la ubicacion de AIMA.jar.

src/IA/PracticaEnergia/
  EstadoEnergia.java              representacion del estado, soluciones iniciales y operadores
  ConjuntoOperadores.java         conjuntos de operadores que se comparan
  GeneradorSucesoresHC.java       sucesores para Hill Climbing (todos)
  GeneradorSucesoresSA.java       sucesores para Simulated Annealing (uno al azar)
  FuncionHeuristica.java          las tres heuristicas: beneficio puro, perdidas, capacidad ociosa
  EstadoFinal.java                test de estado final (siempre false)
  Ejecucion.java                  lanza una busqueda via SearchAgent y recoge las medidas
  Estadistica.java                medias, desviaciones y cuantiles de las replicas
  MainExperimentos.java           los seis experimentos del enunciado y dos adicionales
  Graficas.java                   genera las figuras del informe con JFreeChart
  Comparaciones.java              contrastes binomiales por parejas sobre las replicas
  PruebaEstado.java               pruebas de la representacion y los operadores
  EscenarioInfactibleException.java

docs/javadoc/          Javadoc de IA.Energia, inclòs amb el material de la pràctica

docs/aima-src/         codi font d'AIMA, inclòs amb AIMA.src.zip, material de consulta

results/               salida de los experimentos: csv, figuras png y texto
```

## Procedència i organització del material

Els fitxers ZIP de la arrel són el material original proporcionat amb
l'enunciat o amb el material docent. La relació entre l'origen i les còpies
que s'utilitzen en el repositori és:

| Element del repositori | Procedència | Funció |
| --- | --- | --- |
| `lib/AIMA.jar` | `AIMA.zip` (`dist/AIMA.jar`) | Llibreria AIMA que utilitza el codi propi |
| `lib/lib/` | `AIMA.zip` (`dist/lib/`) | Dependències distribuïdes amb AIMA; JFreeChart és necessària per a `Graficas.java` |
| `lib/CentralEnergia.jar` | `CentralEnergia.zip` | Llibreria del problema, amb el paquet `IA.Energia` |
| `docs/aima-src/` | `AIMA.src.zip` | Codi font d'AIMA, conservat només com a material de consulta |
| `docs/javadoc/` | `CentralEnergia.zip` | Javadoc de la llibreria `CentralEnergia.jar`, conservat com a documentació de consulta |

Per tant, `src/` conté el codi desenvolupat per al grup, `lib/` conté les
llibreries binàries necessàries per compilar-lo i executar-lo, i `docs/` conté
còpies de consulta del material docent. Els ZIP originals es mantenen a la
arrel per deixar traçable la procedència. No s'ha modificat el codi de `docs/`.

### Per què existeix `lib/lib/`

`lib/lib/` no s'ha de confondre amb una còpia innecessària ni amb una llibreria
creada pel grup. És la carpeta `dist/lib/` que ja ve dins d'`AIMA.zip`. Conté,
entre altres, els JAR de JFreeChart i JCommon que utilitza
[`Graficas.java`](src/IA/PracticaEnergia/Graficas.java) per crear les figures
de `results/`. També conté altres dependències que formen part de la
distribució original d'AIMA.

No es pot eliminar mantenint la funcionalitat actual. Només es podria treure
si també s'eliminés `Graficas.java` o se substituís la generació de gràfics per
una altra solució i es revisessin les dependències declarades al manifest
de `AIMA.jar`.

## Requisits

`CentralEnergia.jar` esta compilado con la version 26 del formato de class
(`Created-By: 26.0.2`), asi que **hace falta un JDK 26 o superior**. Con un JDK anterior el
compilador falla con `bad class file ... class file has wrong version 70.0`.

`AIMA.jar` es el que se distribuye en la web de la asignatura dentro de `AIMA.zip`. No hace
falta ninguna dependencia externa: las figuras se generan con el JFreeChart que ya viene entre
sus librerias.

## Compilar y ejecutar (Windows PowerShell)

```powershell
javac -Xlint:all,-path -cp "lib\CentralEnergia.jar;lib\AIMA.jar" -d out (Get-ChildItem -Recurse src -Filter *.java | % FullName)

# pruebas de la representacion del estado y los operadores
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.PruebaEstado

# una ejecucion de cada configuracion, para comprobar que todo funciona
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.MainExperimentos smoke

# experimentos: 1..6 (los del enunciado), 7 (heuristicas), 8 (HC frente a SA) o todos
java -Xmx2g -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.MainExperimentos todos

# figuras del informe y contrastes estadisticos (leen los csv de results/)
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.Graficas
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.Comparaciones
```

Per generar el report en PDF:

```powershell
quarto render report\report.qmd
```

En Linux o macOS hay que cambiar los `;` del classpath por `:`.

La bateria completa tarda unos dos minutos. Escribe un resumen por pantalla, el detalle de cada
replica en `results/expN.csv` y las figuras en `results/fig*.png`.

## Notas

**Memoria.** El enunciado sugiere `-Xmx256m`, pero Hill Climbing materializa todos los sucesores
de un nodo antes de elegir, asi que con el vecindario sin restringir (k=40) el pico puede superar
ese limite. Con la configuracion por defecto (k=8) basta con `-Xmx512m`; `-Xmx2g` deja margen para
las variantes con mas centrales.

**Reproducibilidad.** Las semillas de la generacion de escenarios y del generador de sucesores
estan fijadas, asi que los resultados de Hill Climbing se repiten exactamente. Los de Simulated
Annealing no: `SimulatedAnnealingSearch` crea internamente un `java.util.Random` sin semilla. Por
eso todas las conclusiones sobre el recocido se apoyan en medias sobre 10 replicas.

**Avisos de compilación.** La API `SuccessorFunction` de AIMA utiliza `List`
sin genéricos; los dos métodos adaptadores del proyecto lo indican
explícitamente con `@SuppressWarnings("rawtypes")`. El manifiesto de la versión
distribuida de AIMA referencia cuatro JAR opcionales de iText/Bouncy Castle que
no se incluyen en `AIMA.zip`; por eso el comando de compilación usa
`-Xlint:all,-path` para no mostrar esos avisos externos. Las dependencias
necesarias para compilar y generar las gráficas sí están incluidas en `lib/lib/`.
