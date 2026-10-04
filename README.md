# Practica de Busqueda Local - Distribucion de energia

Implementacion en Java de la primera practica de Inteligencia Artificial (FIB-UPC): asignacion
de clientes a centrales electricas maximizando el beneficio, resuelta con los algoritmos Hill
Climbing y Simulated Annealing de la libreria AIMA de la asignatura.

El analisis de los resultados esta en [INFORME.md](INFORME.md).

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
docs/javadoc/          documentacion de IA.Energia (extraida de CentralEnergia.zip)
docs/aima-src/         codigo fuente de AIMA (extraido de AIMA.src.zip), material de consulta
resultados/            salida de los experimentos: csv, figuras png y texto
```

## Requisitos

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

**Avisos de compilacion.** Con `-Xlint:all` aparecen seis avisos, todos ajenos al codigo de la
practica: cuatro `bad path element` por jars opcionales que declara el manifest de iText y que no
vienen en `AIMA.zip`, y dos `rawtypes` por implementar `SuccessorFunction`, que en AIMA no usa
genericos.
