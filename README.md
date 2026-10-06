# SwitchSort für Android

Android-App im Rahmen einer DHBW-Studienarbeit: Auf einem zufällig gemischten Spielfeld (3×3, 4×4 oder 5×5) muss eine angezeigte Zielzahl möglichst schnell angetippt werden. Nach einem Treffer erscheinen ein neues Feld und eine neue Zielzahl; nach **3 Fehlversuchen** endet die Runde. Gefundene Zahlen und die verstrichene Zeit zählen in den lokalen Highscore.

- Nativ: Kotlin, klassische Android Views, keine Laufzeitabhängigkeiten (kein Compose, kein AndroidX)
- Minimalistisches Design mit Pastellverlauf, Hell-/Dunkelmodus (System, Hell, Dunkel wählbar)

## Schnellstart

Zwei Wege – **Android Studio** (einfachster Weg) oder **Kommandozeile**. Beide brauchen zuerst das Repository:

```bash
git clone https://github.com/ColinVierheller/switch-sort-android.git
cd switch-sort-android
```

## Weg A: Android Studio (empfohlen)

**Voraussetzung:** Android Studio **Quail 2 (2026.1.2) oder neuer** – ältere Versionen unterstützen das verwendete Android Gradle Plugin 9.3.1 nicht. Download: <https://developer.android.com/studio>

1. **Installieren:** Android Studio mit Standardeinstellungen installieren. Der Setup-Assistent lädt Android SDK, Emulator und ein passendes JDK automatisch.
2. **Öffnen:** *File → Open* → Ordner `switch-sort-android` wählen. Der Gradle-Sync startet automatisch (beim ersten Mal werden Gradle 9.7.0 und die Build-Tools geladen, das dauert einige Minuten). Studio legt `local.properties` mit dem SDK-Pfad selbst an.
3. **SDK-Plattform prüfen:** *Tools → SDK Manager* → unter *SDK Platforms* muss **Android 15 (API 35)** installiert sein.
4. **Emulator anlegen (einmalig):** *Device Manager* (rechte Seitenleiste) → **+** → *Create Virtual Device* → **Pixel 7** → System Image **API 35** herunterladen → *Finish*.
5. **Starten:** Oben in der Run-Leiste Konfiguration `app` und den Emulator wählen → **▶ Run**. Die App wird gebaut, installiert und gestartet.

Danach jedes Mal nur noch: **Android Studio öffnen → ▶ drücken.** Ein erneuter Sync ist nur nötig, wenn sich Gradle-Dateien ändern.

**Eigenes Handy statt Emulator:** *Einstellungen → Über das Telefon* → 7× auf *Build-Nummer* tippen → in *Entwickleroptionen* **USB-Debugging** aktivieren → per USB verbinden und Zugriff erlauben. Das Gerät erscheint in der Run-Leiste. Android 6.0 (API 23) oder neuer.

## Weg B: Kommandozeile (ohne Android Studio)

### Voraussetzungen

| Komponente | Version |
| --- | --- |
| JDK | **21** (Minimum 17), z. B. [Eclipse Temurin](https://adoptium.net/) |
| Android SDK | `platforms;android-35`, `build-tools;36.0.0`, `platform-tools` |
| Gradle | **nicht** installieren – kommt per Wrapper (`gradlew`, Version 9.7.0, Prüfsumme gepinnt) |
| Shell | Befehle unten für **Git Bash** (Windows) bzw. Terminal (macOS/Linux) |

### 1. Android SDK installieren

1. „Command line tools only“ für dein Betriebssystem laden: <https://developer.android.com/studio#command-line-tools-only>
2. Entpacken, sodass diese Struktur entsteht (der Ordner muss `latest` heißen):

   ```text
   C:\Users\<user>\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat
   ```

3. Lizenzen akzeptieren und Pakete installieren (Git Bash):

   ```bash
   export JAVA_HOME="C:/Program Files/Java/jdk-21"   # Pfad zu deinem JDK
   SDK="C:/Users/<user>/Android/Sdk"
   yes | "$SDK/cmdline-tools/latest/bin/sdkmanager.bat" --sdk_root="$SDK" --licenses
   "$SDK/cmdline-tools/latest/bin/sdkmanager.bat" --sdk_root="$SDK" \
     "platforms;android-35" "build-tools;36.0.0" "platform-tools"
   ```

### 2. SDK-Pfad hinterlegen

Datei `local.properties` im Projektordner anlegen (wird nicht eingecheckt). **Doppelpunkt nach dem Laufwerksbuchstaben mit `\` escapen**, Schrägstriche verwenden:

```properties
sdk.dir=C\:/Users/<user>/Android/Sdk
```

Alternative: Umgebungsvariable `ANDROID_HOME` auf den SDK-Pfad setzen.

### 3. Bauen

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```

Unter Windows-CMD/PowerShell statt `./gradlew` → `gradlew.bat assembleDebug` (vorher `JAVA_HOME` setzen).

### 4. Emulator anlegen und starten (einmalig anlegen)

```bash
"$SDK/cmdline-tools/latest/bin/sdkmanager.bat" --sdk_root="$SDK" \
  "system-images;android-35;google_apis;x86_64" "emulator"
echo no | "$SDK/cmdline-tools/latest/bin/avdmanager.bat" create avd \
  -n switchsort -k "system-images;android-35;google_apis;x86_64" -d pixel_7
"$SDK/emulator/emulator.exe" -avd switchsort &
```

`-d pixel_7` ist wichtig: ohne Geräteprofil entsteht ein Emulator mit sehr kleiner Auflösung (320×640), auf dem die App unscharf wirkt.

### 5. Installieren und öffnen

```bash
"$SDK/platform-tools/adb.exe" devices          # Emulator/Gerät muss als "device" erscheinen
"$SDK/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"$SDK/platform-tools/adb.exe" shell am start -n de.dhbw.switchsort/.MainActivity
```

Alternativ „SwitchSort“ im App-Drawer antippen.

## Tests und Qualität

```bash
./gradlew testDebugUnitTest   # 37 JVM-Unit-Tests (Spiellogik, Einstellungen, Ranking)
./gradlew lintDebug           # Android Lint (0 Errors erwartet)
```

Testberichte: `app/build/reports/tests/testDebugUnitTest/index.html`, Lint: `app/build/reports/lint-results-debug.html`.

## Häufige Probleme

| Symptom | Ursache / Lösung |
| --- | --- |
| Sync-Fehler „requires a newer version of Android Studio“ / AGP nicht unterstützt | Android Studio auf Quail 2 (2026.1.2) oder neuer aktualisieren |
| `SDK location not found` | `local.properties` fehlt oder `ANDROID_HOME` nicht gesetzt |
| Lint-Fehler `PropertyEscape` in `local.properties` | `C:` als `C\:` schreiben, Schrägstriche statt Backslashes |
| `Unsupported class file major version` / JDK-Fehler | JDK 17–21 verwenden; in Studio: *Settings → Build Tools → Gradle → Gradle JDK* |
| `adb devices` zeigt nichts | Emulator noch beim Booten, oder USB-Debugging am Handy nicht erlaubt |
| App wirkt pixelig | Emulator ohne Geräteprofil angelegt → mit `-d pixel_7` neu anlegen |

## Spielablauf

1. **Menü:** „Spielen“, darunter Highscore, Optionen und Beenden.
2. **Optionen:** Spielername (leer → „Gast“, max. 24 Zeichen), Feldgröße (3×3/4×4/5×5) und Darstellung (System/Hell/Dunkel) – bleiben nach Neustart erhalten.
3. **Spiel:** Die große Zahl oben im Feld finden und antippen. Treffer → Punkt, neues Feld, neue Zahl, Hintergrundfarbe verschiebt sich. Falscher Tipp → ein Versuch weniger (Punkte oben rechts). Die Zeit läuft zur Information mit; ✕ bricht die Runde ohne Wertung ab.
4. **Ende:** nach 3 Fehlversuchen; das Ergebnis landet einmalig im lokalen Highscore (Top 10).

## Dokumentation

- Wissensbasis für die wissenschaftliche Arbeit (Architektur, Entscheidungen, Protokoll): [`docs/entwicklung.md`](docs/entwicklung.md)
- Umsetzungsplan (Anforderungen ↔ Akzeptanzkriterien): [`docs/plans/2026-10-06-initial-setup.md`](docs/plans/2026-10-06-initial-setup.md)

## Bekannte Grenzen

- Querformat nutzbar, aber nicht eigens gestaltet.
- Nicht auf Android 6–7.1 (API 23–25) getestet; geprüft auf Emulator Pixel 7, API 35.
- Lint meldet 3 bewusst akzeptierte Warnungen (Details in `docs/entwicklung.md`).
