# SwitchSort für Android

Android-App im Rahmen einer DHBW-Studienarbeit: Auf einem zufällig gemischten Spielfeld (3×3, 4×4 oder 5×5) muss eine angezeigte Zielzahl möglichst schnell angetippt werden. Nach einem Treffer erscheinen ein neues Feld und eine neue Zielzahl; nach **3 Fehlversuchen** endet die Runde. Gefundene Zahlen und die verstrichene Zeit zählen in den lokalen Highscore.

- Nativ: Kotlin, klassische Android Views (kein Compose, keine AndroidX-Abhängigkeiten)
- Status: Mindestanforderungen umgesetzt, getestet und lauffähig (Branch `initial-setup`)

## Voraussetzungen

| Komponente | Version / Quelle |
| --- | --- |
| JDK | 21 (z. B. `C:\Program Files\Java\jdk-21`; JDK 17 Minimum) |
| Android SDK | Plattform `android-35`, `build-tools;36.0.0`, `platform-tools` |
| Gradle | 9.7.0 – **kein** separates Installieren nötig, kommt per Wrapper |
| Gerät | Android Emulator oder Gerät mit Android 6.0+ (API 23) |

### Android SDK installieren (Windows)

```bash
# Command Line Tools nach C:\Users\<user>\Android\Sdk entpacken, dann:
yes | sdkmanager --sdk_root=C:\Users\<user>\Android\Sdk --licenses
sdkmanager --sdk_root=C:\Users\<user>\Android\Sdk \
  "platforms;android-35" "build-tools;36.0.0" "platform-tools"
```

### SDK-Pfad verknüpfen

`local.properties` im Projektroot anlegen (wird von Git ignoriert):

```properties
sdk.dir=C\:/Users/<user>/Android/Sdk
```

Alternative: Umgebungsvariable `ANDROID_HOME` auf den SDK-Pfad setzen.

## Bauen und Starten

Alle Befehle im Projektroot, **Git Bash** unter Windows:

```bash
# Debug-APK bauen
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```

### Auf Emulator/Gerät starten

```bash
# Gerät/Emulator prüfen (platform-tools muss im PATH oder im SDK liegen)
"C:/Users/<user>/Android/Sdk/platform-tools/adb.exe" devices

# Installieren und starten
"C:/Users/<user>/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Danach „SwitchSort" im App-Drawer öffnen. In Android Studio alternativ: Projekt öffnen → Sync → Run.

Einen Emulator anlegen (falls keiner existiert):

```bash
sdkmanager "system-images;android-35;google_apis;x86_64" "emulator"
avdmanager create avd -n switchsort -k "system-images;android-35;google_apis;x86_64"
emulator -avd switchsort
```

## In Android Studio starten (Ablauf)

*Hinweis: benötigt Android Studio Quail/2025.1+ (AGP 9.3.1). Das SDK unter `C:\Users\<user>\Android\Sdk` wird über `local.properties` gefunden.*

1. **Öffnen:** File → Open → Projektordner wählen. Gradle-Sync läuft automatisch (beim ersten Mal lädt der Wrapper gradle-9.7.0 herunter).
2. **Emulator haben:** Device Manager (rechte Seitenleiste) → + → Create Virtual Device → Pixel-Gerät wählen → System Image **API 35** herunterladen → Finish. *(Einmalig nötig.)*
3. **Starten:** Oben in der Run-Leiste Emulator/Gerät + `app` auswählen → **Play-Button ▶**. Fertig – App baut sich und startet selbst.

Ab dann bei jedem Mal nur noch: **Android Studio aufmachen → ▶ drücken.** Ein Re-Sync ist nur nötig, wenn sich Gradle-Dateien ändern; der Emulator muss nicht vorher gestartet sein.

## Tests und Qualität

```bash
./gradlew testDebugUnitTest   # 35 JVM-Unit-Tests (Spiellogik, Einstellungen, Ranking)
./gradlew lintDebug           # Android Lint (0 Errors erwartet)
```

## Spielablauf

1. **Menü:** Spiel starten, Highscore, Optionen, Beenden.
2. **Optionen:** Spielername (leer → „Gast", max. 24 Zeichen) und Feldgröße (3×3/4×4/5×5) – beides bleibt nach Neustart erhalten.
3. **Spiel:** angezeigte Zielzahl auf dem Feld finden und antippen. Treffer → neues Feld + neues Ziel, Score +1. Falscher Tipp → Fehlversuch (Feld bleibt). Zeit läuft nur zur Information mit.
4. **Ende:** nach 3 Fehlversuchen; Ergebnis wird einmalig im lokalen Highscore gespeichert (Top 10, Name + Feldgröße + Treffer + Zeit).

## Dokumentation

- Entscheidungs-/Entwicklungsprotokoll für die wissenschaftliche Arbeit: [`docs/entwicklung.md`](docs/entwicklung.md)
- Umsetzungsplan (Anforderungen ↔ Akzeptanzkriterien): [`docs/plans/2026-10-06-initial-setup.md`](docs/plans/2026-10-06-initial-setup.md)

## Bekannte Grenzen (MVP)

- Geräte-/Emulator-Smoke-Test und App-Icon folgen in der Verschönerungsphase.
- Lint meldet 8 akzeptierte Warnungen (Details in `docs/entwicklung.md`).
