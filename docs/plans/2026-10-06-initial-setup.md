# Initial-Setup: SwitchSort für Android

## Ziel und Quellen

Eine einfache, installierbare Android-App erfüllt die Funktionen aus `Dominik-Rietz_SwitchSort.pdf` (Stand 24.05.2022). Spielregeln und Umfang wurden am 06.10.2026 im Gespräch bestätigt. Ausgangslage: leeres Git-Repository ohne Anwendung oder Buildskripte; Branch `initial-setup` ist angelegt.

## Umfang

- **In:** Hauptmenü (Start, Highscore, Optionen, Beenden), Spiel mit 3×3/4×4/5×5, lokal gespeicherter Spielername und Feldgröße, lokaler Highscore, drei Fehlversuche, angezeigte verstrichene Zeit, automatisierte Logiktests, Start-/Buildanleitung und fortlaufendes Entwicklungsprotokoll für die wissenschaftliche Arbeit.
- **Nicht in:** Desktop-Spiel, Onlinefunktionen, Store-Veröffentlichung, Grafiken/Animationen, Zeitlimit, komplexere Sortier- oder Wechselmechanik, PDF-Kopie im Repository.
- **Verwandte Pläne:** keine vorhanden. Bestehende `.idea/`-Dateien sind unversionierte lokale IDE-Daten und werden nicht eingecheckt.

## Architektur und Regeln

- Natives Kotlin-Android-Projekt mit Android-Views; testbare Spiellogik von der Oberfläche trennen. Lokale kleine Datenmengen über private Android-Persistenz; keine API, Datenbank, Cloud oder Berechtigungen.
- Feldgröße `n ∈ {3, 4, 5}`. Jedes neue Feld enthält die eindeutigen Zahlen `1..n²` in zufälliger Reihenfolge; die Zielzahl wird zufällig aus dem sichtbaren Feld gewählt. Nach richtigem Tippen: Score +1, neues gemischtes Feld und neues Ziel. Nach falschem Tippen: Fehlversuche +1, dasselbe Ziel/Feld; nach dem dritten Fehler Spielende.
- Uhr: monotone verstrichene Sekunden seit Spielstart anzeigen; **kein** Zeitlimit. Nach Spielende Trefferzahl, Dauer und Spielername anzeigen und Ergebnis lokal speichern. Highscore: beste Ergebnisse nach Treffern absteigend, bei Gleichstand nach kürzerer Dauer; Name und Feldgröße sichtbar. Einstellung: gültiger nichtleerer Name (maximal 24 Zeichen nach Trimmen), Standard `Gast`; Feldgröße standardmäßig 3×3. Schlechte gespeicherte Werte auf gültige Standards zurücksetzen.
- **Risiken:** fehlendes Android SDK/Gradle vor Ort; Layout auf 5×5 bei kleinen Displays; Activity-Neustart während laufender Runde; Plugin-Transitivabhängigkeiten mit Sicherheitshinweisen. Erforderlich: Builds und tatsächliches Tippen auf Emulator/Gerät separat belegen; ein JVM-Test allein beweist keine lauffähige App.

## Nachweis und Akzeptanzkriterien

| Kriterium | Nachweis |
| --- | --- |
| Menü zeigt vier Aktionen; Beenden schließt die App | Android-Build plus manueller Gerätesmoke-Test |
| Optionen speichern Namen und Feldgröße dauerhaft | Neustart-Smoke-Test, Logiktest für Eingabegrenzen |
| Drei Feldgrößen, einmalige Zahlen und immer vorhandenes Zufallsziel | JVM-Tests mit injizierter deterministischer Zufallsquelle |
| Treffer erzeugt neues Feld/Ziel und erhöht Score; Fehler erhält Feld/Ziel | JVM-Tests für Zustandsübergänge |
| Genau drei Fehler beenden die Runde, danach kein weiterer Score | JVM-Grenzfalltest; Gerätesmoke-Test |
| Zeit wird nur gemessen, Highscore wird gespeichert und sortiert | Tests mit kontrollierter Uhrzeit/Ranking; Neustart-Smoke-Test |
| Auf Android startbar und an Betreuer testbar | `testDebugUnitTest`, `assembleDebug`, `lintDebug`; APK-Pfad und Installation dokumentiert; Emulator/Gerät falls verfügbar |

## Verifizierte technische Fakten und Abhängigkeiten

- Android-Gradle-Plugin 8.13.2: stabiles Release, Android API bis 36.1, Gradle mindestens 8.13, JDK mindestens 17 ([Android](https://developer.android.com/build/releases/agp-8-13-0-release-notes)); vorgesehen: API 35, JDK 21. Gradle 8.14.4 behebt CVE-2026-22816 und CVE-2026-22865 ([Gradle](https://github.com/gradle/gradle/releases/tag/v8.14.4)); Wrapper-Binärdistribution SHA-256 `f1771298a70f6db5a29daf62378c4e18a17fc33c9ba6b14362e0cdf40610380d` ([Checksummen](https://gradle.org/release-checksums/)).
- Kotlin-Android-Plugin `org.jetbrains.kotlin.android` 2.3.21 ist laut [Kotlin-Kompatibilitätstabelle](https://kotlinlang.org/docs/gradle-configure-project.html) mit AGP 8.13 und Gradle 8.14 kompatibel. Testbibliothek `junit:junit:4.13.2` nur im Test-Scope; keine weiteren App-Bibliotheken geplant. Versionen exakt festlegen. Vor Einbau transitive Build-Abhängigkeiten auf gemeldete kritische/hohe Schwachstellen prüfen; ungeklärte Funde sperren den Build-Setup-Schritt statt Sicherheitskontrollen zu lockern.
- Android `SharedPreferences.getString(key, default)`/`getInt(key, default)` und `Editor.putString`/`putInt`/`apply` speichern kleine lokale Werte ([API](https://developer.android.com/reference/android/content/SharedPreferences)). Android `SystemClock.elapsedRealtime()` misst monotone Intervalle inklusive Ruhezustand ([API](https://developer.android.com/reference/android/os/SystemClock)). Kotlin `Random.nextInt(until)` und `MutableList.shuffle(random)` erzeugen testbar zufällige Felder ([API](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.random/-random/next-int.html), [API](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/shuffle.html)).
- Lokale Ausgangslage: `java` und `python` vorhanden; `gradle`, `adb` und `kotlin` nicht im PATH, kein `ANDROID_HOME`/`ANDROID_SDK_ROOT` gesetzt. SDK/Wrapper-Verfügbarkeit muss beim Setup geprüft werden. Zielbefehle unter Git Bash: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew lintDebug`.

## Ausführungsschritte

- [ ] **1. Build-Grundlage und Sicherheitsfreigabe:** Android-/Gradle-Werkzeuge samt SDK prüfen, feste und überprüfte Abhängigkeiten einrichten; lokale `.idea/`, SDK-Dateien, Build-Artefakte und Schlüssel ignorieren. Gradle-Wrapper mit offizieller SHA-256-Prüfung erstellen; Synchronisierung und Debug-Build nachweisen.
- [ ] **2. Spielregel-Tests und Spiellogik:** Erst fehlschlagende JVM-Tests je Verhalten (Feld, Ziel, Treffer, Fehlversuche, Dauer), dann minimalen Spielzustand implementieren; Tests erneut ausführen.
- [ ] **3. Einstellungen und Highscore:** Erst Tests für Namen/Feldgröße, Ranking und ungültige Daten; dann lokale Persistenz und Highscore-Ansicht umsetzen; Neustartverhalten prüfen.
- [ ] **4. Android-Bedienung:** Hauptmenü, Optionen, Brett, Zielanzeige, Zeit, Ergebnis und Beenden anbinden; 3×3/4×4/5×5 und Activity-Neustart/Zurück-Navigation auf Gerät oder Emulator prüfen.
- [ ] **5. Wissenschaftliches Protokoll und Übergabe:** `docs/entwicklung.md` während jedes Schritts fortschreiben (Anforderung → Entscheidung → Alternativen/Begründung → konkrete Umsetzung → Test/Evidenz → Grenzen), verwendete Literatur/Quellen kennzeichnen, keine erfundenen Messungen; Build-/Installationsanleitung, APK-Pfad und offene Testlücken festhalten. Abschließend Tests, Lint, Debug-Build, Abhängigkeitscheck und Diff-Hygiene prüfen. Kein Push ohne gesonderten Auftrag.
