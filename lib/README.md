# Llibreries

Aquesta carpeta conté les llibreries binàries necessàries per compilar i
executar el codi propi de la pràctica.

## Procedència

- `AIMA.jar` és la llibreria AIMA distribuïda dins de `AIMA.zip`,
  concretament `dist/AIMA.jar`.
- `CentralEnergia.jar` és la llibreria del problema, distribuïda dins de
  `CentralEnergia.zip`. Conté el paquet `IA.Energia`.
- `lib/` és una còpia de `dist/lib/`, també inclosa dins d'`AIMA.zip`.
  Conté les dependències que acompanya la distribució d'AIMA, com JFreeChart,
  JCommon, iText i JUnit.

## Per què existeix `lib/lib/`

`AIMA.jar` declara al seu manifest les dependències amb rutes relatives
`lib/...`. Per això la carpeta `lib/` interior es conserva dins d'aquesta
carpeta i no s'ha de moure ni eliminar.

A més, el codi propi utilitza directament JFreeChart a
[`../src/IA/PracticaEnergia/Graficas.java`](../src/IA/PracticaEnergia/Graficas.java)
per generar les figures dels experiments. Per tant, `lib/lib/` no és només
material de les demos d'AIMA: és necessari per a la funcionalitat actual del
projecte.

## Ús

Des de l'arrel del repositori, el classpath de Windows utilitzat per compilar
i executar és:

```powershell
javac -cp "lib\CentralEnergia.jar;lib\AIMA.jar" -d out (Get-ChildItem -Recurse src -Filter *.java | % FullName)
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar" IA.PracticaEnergia.Graficas
```

El manifest d'AIMA resol les dependències de `lib/lib/` mitjançant aquestes
rutes relatives. Els fitxers ZIP originals (`AIMA.zip` i
`CentralEnergia.zip`) es troben a l'arrel del repositori per conservar la
traçabilitat de la procedència.
