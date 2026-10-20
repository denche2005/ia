# Material de suport i procedència

Aquesta carpeta conté material de consulta que no forma part del codi
desenvolupat pel grup:

- [`aima-src/`](aima-src/) és el codi font d'AIMA inclòs en el material de
  l'assignatura (`AIMA.src.zip`). S'ha conservat per consultar el comportament
  real de `HillClimbingSearch` i `SimulatedAnnealingSearch`.
- [`javadoc/`](javadoc/) és la documentació HTML de l'API de
  `CentralEnergia.jar`, inclosa a `CentralEnergia.zip`. Serveix per consultar
  les classes del paquet `IA.Energia` sense disposar del seu codi font.

No s'han modificat aquests fitxers. El codi propi de la pràctica es troba a
[`../src/`](../src/).

Les llibreries binàries corresponents es troben a [`../lib/`](../lib/):

- `../lib/AIMA.jar` prové de `AIMA.zip` (`dist/AIMA.jar`).
- `../lib/lib/` prové de `AIMA.zip` (`dist/lib/`) i conté les dependències
  distribuïdes amb AIMA. No és una carpeta exclusiva de les demos: els JAR de
  JFreeChart que hi ha dins són necessaris per a `src/IA/PracticaEnergia/Graficas.java`,
  que genera les figures dels experiments.

No s'ha d'eliminar `../lib/lib/` mentre es mantingui la generació actual de
gràfics. A més, `AIMA.jar` declara aquestes dependències al seu manifest amb
rutes relatives `lib/...`, que és el motiu de conservar aquesta carpeta dins
de `lib/`.
- `../lib/CentralEnergia.jar` prové de `CentralEnergia.zip`.
