# AGENTS.md — SwitchSort für Android

## Pflicht: Dokumentation für die Studienarbeit

- `docs/entwicklung.md` ist die Wissensbasis für die wissenschaftliche Arbeit.
- Bei JEDER Änderung an Architektur, Technologie, Implementierung, Design,
  Tests, Build oder Workflow:
  - Teil A (aktueller Stand) aktualisieren: Entscheidung, Alternativen,
    Begründung, Umsetzung, Grenzen.
  - Teil B (chronologisches Protokoll) um einen datierten Eintrag ergänzen;
    alte Einträge nicht umschreiben, sondern korrigieren.
- Nur verifizierte Aussagen (Messwerte, Testzahlen, Screenshots) dokumentieren.
- Doku-Update gehört in denselben Commit wie die Änderung.

## Git

- Keine KI-/Tool-Attribution in Commits, PRs oder Dateien
  (kein `Co-Authored-By`, kein "Generated with").
- `main` ist geschützt: Änderungen nur per Pull Request von Feature-Branches.
- Push und PR nur auf ausdrücklichen Wunsch.

## Build und Verifikation (Git Bash)

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew testDebugUnitTest lintDebug assembleDebug
```

- Erwartung: BUILD SUCCESSFUL, alle Tests grün, Lint 0 Errors.
- Emulator: AVD `switchsort` (Pixel 7, API 35);
  ADB: `C:/Users/vierhec/Android/Sdk/platform-tools/adb.exe`.
- UI-Änderungen per Emulator-Screenshot hell und dunkel prüfen;
  keine Zeilenumbrüche in Labels/Werten/Buttons.

## Konventionen

- Native Kotlin, klassische Android Views, keine AndroidX-/Drittbibliotheken.
- Spiellogik (`game/`, `score/`) bleibt Android-frei und JVM-getestet.
- Alle Texte in `strings.xml` (Deutsch), Farben nur über Ressourcen/`ui/Palette`.
