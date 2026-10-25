# Pràctica de cerca local: distribució d'energia

Implementació en Java de la primera pràctica d'Intel·ligència Artificial
(FIB-UPC). El projecte resol el problema d'assignar clients a centrals
elèctriques maximitzant el benefici mitjançant els algoritmes **Hill
Climbing** i **Simulated Annealing**, sobre la llibreria AIMA de
l'assignatura.

La descripció del model, les decisions d'implementació, els experiments i
l'anàlisi dels resultats es troben a l'[informe del projecte](report/report.pdf).
El seu codi font és a [report/report.qmd](report/report.qmd).

## Estructura

| Directori | Contingut |
| --- | --- |
| [`src/`](src/) | Codi propi de la pràctica: estat, operadors, heurístiques, execució i experiments. |
| [`lib/`](lib/) | Llibreries binàries de l'enunciat i dependències d'AIMA. |
| [`results/`](results/) | CSV, figures i sortides generades pels experiments. |
| [`report/`](report/) | Font i PDF de l'informe. |
| [`docs/`](docs/) | Documentació i codi font de consulta de les llibreries proporcionades. |

Els fitxers ZIP de l'arrel són el material original de la pràctica i del
material docent. No formen part del codi desenvolupat pel grup.

## Requisits

- Windows PowerShell (les ordres següents fan servir el separador `;`).
- JDK 26 o superior, perquè `CentralEnergia.jar` utilitza el format de classes
  corresponent a aquesta versió.
- Quarto i una instal·lació de LaTeX, només per regenerar l'informe PDF.

No cal instal·lar dependències Java addicionals: les llibreries necessàries es
troben dins de [`lib/`](lib/).

## Compilar i executar

Des de l'arrel del repositori:

```powershell
javac -cp "lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName)
```

Comprovar la representació de l'estat i els operadors:

```powershell
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  IA.PracticaEnergia.PruebaEstado
```

Fer una execució ràpida de totes les configuracions:

```powershell
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  IA.PracticaEnergia.MainExperimentos smoke
```

Executar els experiments:

```powershell
java -Xmx2g -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  IA.PracticaEnergia.MainExperimentos todos
```

També es pot executar un experiment concret amb els identificadors `1` a `8`
(`heuristicas` i `comparativa` són àlies dels experiments 7 i 8).

Les figures i les comparacions estadístiques es poden regenerar a partir dels
CSV:

```powershell
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  IA.PracticaEnergia.Graficas
java -cp "out;lib\CentralEnergia.jar;lib\AIMA.jar;lib\lib\*" `
  IA.PracticaEnergia.Comparaciones
```

Per regenerar l'informe:

```powershell
quarto render report\report.qmd
```

Els resultats dels experiments s'escriuen a [`results/`](results/) i el PDF
generat a [`report/report.pdf`](report/report.pdf).
