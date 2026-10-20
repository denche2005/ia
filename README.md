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
  CentralEnergia.jar   libreria del enunciado (paquete IA.Energia), extraida de CentralEnergia.zip
  AIMA.jar             libreria de la asignatura, extraida de AIMA.zip (dist/AIMA.jar)
  lib/                 dependencias de AIMA.jar (jfreechart, itext, junit...), extraidas de
                       AIMA.zip (dist/lib). Van en lib/lib porque el manifest de AIMA.jar
                       declara "Class-Path: lib/..." relativo a su propia ubicacion, asi que
                       desde ahi se resuelven solas y no hay que anadirlas al classpath.
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
resultados/            salida de los experimentos: csv, figuras png y texto
```

## Procedència del material de suport

`AIMA.src.zip`, `AIMA.zip` i `CentralEnergia.zip` són fitxers proporcionats
amb l'enunciat o el material docent de la pràctica. `docs/aima-src/` és una
còpia extreta de `AIMA.src.zip`, mentre que `docs/javadoc/` és la documentació
HTML de l'API de `CentralEnergia.jar`, distribuïda amb aquest material. No són
implementacions pròpies del grup; només s'han consultat per integrar
correctament les interfícies de la pràctica.

## Requisits

`CentralEnergia.jar` esta compilado con la version 26 del formato de class
(`Created-By: 26.0.2`), asi que **hace falta un JDK 26 o superior**. Con un JDK anterior el
compilador falla con `bad class file ... class file has wrong version 70.0`.

`AIMA.jar` es el que se distribuye en la web de la asignatura dentro de `AIMA.zip`. No hace
falta ninguna dependencia externa: las figuras se generan con el JFreeChart que ya viene entre
sus librerias.

## Compilar y ejecutar (Windows PowerShell)

```powershell
javac -cp "lib\CentralEnergia.jar;lib\AIMA.jar" -d out (Get-ChildItem -Recurse src -Filter *.java | % FullName)

# pruebas de la representacion del estado y los operadores
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.PruebaEstado

# una ejecucion de cada configuracion, para comprobar que todo funciona
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.MainExperimentos smoke

# experimentos: 1..6 (los del enunciado), 7 (heuristicas), 8 (HC frente a SA) o todos
java -Xmx2g -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.MainExperimentos todos

# figuras del informe y contrastes estadisticos (leen los csv de resultados/)
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.Graficas
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.Comparaciones
```

Per generar el report en PDF:

```powershell
quarto render report\report.qmd
```

En Linux o macOS hay que cambiar los `;` del classpath por `:`.

La bateria completa tarda unos dos minutos. Escribe un resumen por pantalla, el detalle de cada
replica en `resultados/expN.csv` y las figuras en `resultados/fig*.png`.

## Notas

**Memoria.** El enunciado sugiere `-Xmx256m`, pero Hill Climbing materializa todos los sucesores
de un nodo antes de elegir, asi que con el vecindario sin restringir (k=40) el pico puede superar
ese limite. Con la configuracion por defecto (k=8) basta con `-Xmx512m`; `-Xmx2g` deja margen para
las variantes con mas centrales.

**Reproducibilidad.** Las semillas de la generacion de escenarios y del generador de sucesores
estan fijadas, asi que los resultados de Hill Climbing se repiten exactamente. Los de Simulated
Annealing no: `SimulatedAnnealingSearch` crea internamente un `java.util.Random` sin semilla. Por
eso todas las conclusiones sobre el recocido se apoyan en medias sobre 10 replicas.

**Avisos de compilació.** Amb `-Xlint:all` apareixen sis avisos, tots aliens
al codi de la pràctica: quatre `bad path element` per jars opcionals que
declara el manifest d'iText i que no venen a `AIMA.zip`, i dos `rawtypes` per
implementar `SuccessorFunction`, que a AIMA no usa genèrics.
