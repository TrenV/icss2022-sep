# ✅ Inleverchecklist – ICSS-20-SEP (Tren Verheijen)

| Nr | Controlepunt | Status | Opmerking                                               |
|----|---------------|--------|---------------------------------------------------------|
| 1  | Project compileert zonder fouten (`mvn clean compile`) | ✅ | Werkt volledig                                          |
| 2  | GUI start correct via `mvn exec:java` | ✅ | AST en output correct zichtbaar                         |
| 3  | Alle **Must**-eisen uit de opdracht zijn geïmplementeerd | ✅ | Parser, Checker, Evaluator, Generator volledig          |
| 4  | Alle **Should**-eisen zijn geïmplementeerd | ✅ | CH01–CH05 aanwezig                                      |
| 5  | Eigen uitbreiding geïmplementeerd | ✅ | “Variabele mag type niet wijzigen (PIXEL → PERCENTAGE)” |
| 6  | Uitbreiding vermeld in PDF-bijlage | ✅ | Beschreven hieronder*                                   |
| 7  | `mvn clean` uitgevoerd (geen `target/` in zip) | ✅ | Map opgeschoond                                         |
| 8  | `.git/` map aanwezig in zip | ✅ | Controleer in `icss2022-sep.zip`                        |
| 9  | Folderstructuur behouden (`icss2022-sep/startcode/...`) | ✅ | Correct volgens eisen                                   |
| 10 | `Toelichting_TrenVerheijen.pdf` toegevoegd aan hoofdmap | ✅ | Inbegrepen in zip                                       |
| 11 | Code goed geformatteerd, nette variabelenamen | ✅ | Consistent                                              |
| 12 | Java 13 + Maven 3.6 of hoger gebruikt | ✅ | Compatibel                                              |
| 13 | Zip-bestand heet `icss2022-sep.zip` | ✅ | Klaar voor upload                                       |
| 14 | Upload in iSAS gecontroleerd (bestand opent correct) | ✅ | Handmatig getest                                        |

---

 **Eindstatus:**  
 **Volledig inleverklaar** – voldoet aan alle Musts, Shoulds en uitbreidingsvereisten.


## 🔧 Eigen uitbreiding: Typeconsistentie van variabelen

### Beschrijving
In de standaard ICSS-specificatie is het toegestaan om variabelen meerdere keren te herdefiniëren.  
In deze uitbreiding wordt een extra **semantische controle** toegevoegd in de `Checker`, die voorkomt dat een variabele tijdens de uitvoering van het programma van type verandert.

### Werking
Wanneer een variabele eenmaal een type heeft gekregen (bijv. `PIXEL`, `PERCENTAGE`, `COLOR`, `BOOLEAN`, `SCALAR`),  
dan moet elke volgende toewijzing van diezelfde variabele hetzelfde type hebben.

Voorbeeld van **ongeldige ICSS**:
```icss
MyWidth := 10px;
MyWidth := 50%; //  Foutmelding: variabele verandert van type (PIXEL → PERCENTAGE)