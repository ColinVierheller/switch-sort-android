# Entwicklungsprotokoll

## 2026-10-06 — Setup- und Sicherheitsprüfung

- **Anforderung:** native Kotlin-/Android-App mit Views, lokaler Persistenz, deterministisch testbarer Spiellogik; Umfang in [Plan](plans/2026-10-06-initial-setup.md).
- **Vorläufiger Befund:** AGP 8.13.2 deklariert `bcpkix-jdk18on:1.79`; Gradle 8.14.4 enthält `bcprov-jdk18on:1.78.1`. Beide `bcprov`-Versionen fallen in den Bereich von CVE-2025-14813 / GHSA-574f-3g2m-x479 (Critical, CVSS 9.3). Die erste Prüfung stoppte vorsorglich vor App-Code.
- **Nachtrag – Reachability:** CVE-2025-14813 betrifft spezifisch `G3413CTRBlockCipher` (GOST R 34.13 CTR): mehr als 255 Datenblöcke verschlüsselt/entschlüsselt mit wiederverwendetem Schlüssel und IV. Die bloße JAR-Präsenz belegt keine Ausführung. Für den repo-definierten Android-Paketierungs- und APK-Signierpfad fanden sich keine Aufrufe des GOST-Pfads:
  - AGP-`PackageAndroidArtifact` führt über `IncrementalPackagerBuilder.withSigning` in die APK-Paketierung.
  - APK-Signer 8.13.2 verwendet laut `javap` RSA, ECDSA und DSA. `jdeps` fand keine Bouncy-Castle-, GOST- oder G3413-Referenzen in den lokalen AGP-JARs `apksig`, `builder`, `apkzlib`, `signflinger` und `bundletool`.
  - Gradles PGP-`signing`-Plugin ist im Projekt nicht angewandt; Gradles Konfigurationscache verwendet AES-Cipher-APIs, nicht GOST CTR.
  - **Grenze:** Der Android-Build wurde wegen fehlendem SDK nicht ausgeführt. Externe Gradle-Init-Skripte wurden nicht außerhalb des Repos untersucht; falls sie Plugins oder eigene Signierung ergänzen, ist deren Pfad nicht beurteilt.
- **Offizielle Versionen:** Gradle 9.7.0 enthält gemäß Gradle-Release-Katalog Bouncy Castle 1.84; Gradle 9.8.0 enthält 1.85. Beide liegen oberhalb des Fixstands 1.84 für CVE-2025-14813, aber nicht bei 1.86. AGP 9.3.1 deklariert `bcpkix`/`bcprov` 1.79; AGP 9.4.1 und 9.5.0-alpha08 deklarierten 1.80.2. Kein geprüftes AGP-POM deklariert 1.86. Kotlin dokumentiert KGP 2.4.20 als voll kompatibel bis Gradle 9.7.0 und AGP 9.3.1; Kotlin 2.4 benötigt R8 mindestens 9.1.29.
- **Experimenteller Kandidat:** Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.20 + BC BOM 1.86. Der root `buildscript`-Classpath verwendete eine BOM-Constraint statt des unwirksamen zusätzlichen `settings.pluginManagement.buildscript`. `buildEnvironment` löste AGP-POM-Versionen `bcpkix`/`bcprov` 1.79 tatsächlich zu 1.86 auf. Der App-Gradle-Skript konfigurierte mit AGP-Built-in-Kotlin. Die R8-Laufzeitklasse wurde aus dem Builder-Classloader geladen: `9.3.16` aus `builder-9.3.1.jar`; das übertrifft Kotlin 2.4s R8-Mindestversion. AGP- und Kotlin-Plugins wurden konfiguriert, aber nicht kompiliert.
- **Versions-/Lizenzgrenze:** BC 1.86 ist am 11.09.2026 veröffentlicht und weist laut abgefragten SCA-Seiten keine direkten bekannten CVEs auf. Die Bouncy-Castle-Lizenz ist eine kundenspezifische Lizenz und bedarf rechtlicher Prüfung. Ein BOM-Upgrade auf 1.86 ist nur ein getesteter Resolution-Versuch, keine freigegebene API-Kompatibilität.
- **Nachweise:** Wrapper 9.7.0 JAR SHA-256 stimmt mit Gradles offizieller Prüfsumme überein: `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`. Gradle lud die Distribution mit konfigurierter SHA-256 `84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae` und startete. `buildEnvironment`, `:app:buildEnvironment` und der temporäre R8-Origin-Task liefen erfolgreich. Keine `testDebugUnitTest`, `assembleDebug`, `lintDebug` oder Gerätetests.
- **Nächste Entscheidung:** Der GOST-CVE-Pfad ist für normale Android-APK-Signierung nicht belegt; der experimentelle Versionssatz ist kompatibilitätsplausibel und die Plugin-Auflösung nachgewiesen. Vor Freigabe bleibt `assembleDebug`/Kotlin-Kompilierung mit einem genehmigten SDK erforderlich. BC-1.86-BOM und AGP/KGP-API-Kompatibilität sind bis zu diesem Gate experimentell.
- **Quellen:** [BC-CVE-2025-14813](https://github.com/bcgit/bc-java/wiki/CVE%E2%80%902025%E2%80%9014813), [AGP 9.3.1 builder-POM](https://dl.google.com/dl/android/maven2/com/android/tools/build/builder/9.3.1/builder-9.3.1.pom), [AGP 9.4.1 builder-POM](https://dl.google.com/dl/android/maven2/com/android/tools/build/builder/9.4.1/builder-9.4.1.pom), [Gradle 9.7.0 release-Katalog](https://github.com/gradle/gradle/blob/v9.7.0/gradle/dependency-management/distribution.versions.toml), [Gradle 9.8.0 release-Katalog](https://github.com/gradle/gradle/blob/v9.8.0/gradle/dependency-management/distribution.versions.toml), [AGP-Paketierungspfad](https://android.googlesource.com/platform/tools/base/+/studio-master-dev/build-system/gradle-core/src/main/java/com/android/build/gradle/tasks/PackageAndroidArtifact.java), [APK-Signaturalgorithmen](https://android.googlesource.com/platform/tools/apksig/+/master/src/main/java/com/android/apksig/internal/apk/SignatureAlgorithm.java), [AGP/R8-Kotlin-Matrix](https://developer.android.com/build/kotlin-support), [KGP-Kompatibilität](https://kotlinlang.org/docs/gradle-configure-project.html), [Gradle-Prüfsummen](https://gradle.org/release-checksums/).

## 2026-10-06 — Umsetzung Mindestspiel (Logik, Persistenz, Views)

### Blockerbefund vorab (ehrlich, ohne Beschönigung)

- **Das freigegebene SDK existiert nicht auf diesem Rechner.** `C:\Users\vierhec\AppData\Local\Android\Sdk` fehlt vollständig (kein Verzeichnis `Android` unter `%LOCALAPPDATA%`; geprüft via `ls`, Datei-Listing des gesamten `%LOCALAPPDATA%`, `which adb`/`sdkmanager` ohne Treffer, `ANDROID_HOME`/`ANDROID_SDK_ROOT` leer). Es gibt auch keine Plattform `android-35` und kein `platform-tools`. Damit ist jeder AGP-Build (`assembleDebug`, `lintDebug`, `testDebugUnitTest`) in dieser Session von vornherein nicht lauffähig.
- **Ausführungsrechte der Agenten-Session:** In dieser Hintergrund-Session wurden wiederholt `./gradlew --version`, `bash gradlew`, `gradle`, `java -version` (PATH und Voll-Pfad) ausprobiert; alle Java/JVM-Startkommandos wurden von der Sandbox ohne Nutzerabfrage abgelehnt („running in the background … automatically denied"). Builds/Tests konnten daher **nicht ausgeführt** werden; es gibt keine künstliche Grün-Evidenz.
- Konsequenz: Alle Schritte dieses Abschnitts sind als *umgesetzt, Ausführungsnachweis ausstehend* dokumentiert. Die unten stehenden Befehle müssen in einer Session mit Ausführungsrechten und installiertem SDK nachgeholt werden (Anleitung am Abschnittsende). Das entspricht dem Vorgehen „maximal 3 Selbstreparaturversuche, dann sperren und berichten" – versucht wurden u. a. direkter Wrapper-Aufruf, `bash`-/`sh`-Aufruf, direkter `java`-Aufruf und SDK-Standortsuche.

### Plugin-Konfiguration (Entscheidung mit ausstehender Build-Verifikation)

- **Anforderung:** App-Modul soll mit `id("org.jetbrains.kotlin.android")` explizit kompilieren, damit Kotlin-Quellen sicher kompiliert werden und das Tooling konsistent ist (AGP Built-in-Kotlin war anfangs genutzt worden).
- **Entscheidung:** Das App-Modul wendet `com.android.application` **und** `org.jetbrains.kotlin.android` an; beide Plugins liegen bereits mit fester Version (AGP 9.3.1, KGP 2.4.20) auf dem `buildscript`-Classpath des Root-Projekts. Gradle-Regel: Plugins, die bereits auf dem Buildskript-Classpath liegen, dürfen **nicht** mit Versionsangabe angefragt werden („plugin already on the classpath must not include a version"). Die vom Lead skizzierte Alternative „Root-Plugins-Block mit `version "2.4.20" apply false` zusätzlich zum Classpath" kollidiert mit dieser Regel und würde die Plugin-Auflösung auf den plugins-DSL-Pfad verlagern, auf dem die BouncyCastle-1.86-BOM-Constraint **nicht** greift (die Constraint wirkt nur auf dem buildscript-Classpath – genau deshalb wurde sie dort platziert).
- **Konkret:** `app/build.gradle.kts` enthält nun beide Plugin-IDs ohne Version; `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` auf Projektebene (KGP-Erweiterung). Der Root bleibt: `buildscript { dependencies { classpath AGP 9.3.1 + kotlin-gradle-plugin 2.4.20 + platform(bc-jdk18on-bom:1.86) } }`. Build-Nachweis ausstehend (siehe Blocker).
- **`android.useAndroidX=true` entfernt** aus `gradle.properties`: Die App verwendet ausschließlich Plattform-APIs (`android.app.Activity`, `android.widget.*`, `android.content.SharedPreferences`); keine AndroidX-Abhängigkeit vorhanden, daher ist der AndroidX-/Jetifier-Schalter ungenutzt und entfallen.

### TDD-Scheiben (Tests zuerst, dann Implementierung)

Pro Verhalten wurden zuerst Tests in `app/src/test/java/de/dhbw/switchsort/` geschrieben; Red/Green-Ausführung pro Klasse ist wegen obiger Blocker **noch nicht gelaufen**:

1. **SettingsNormalizer** (`score/SettingsNormalizerTest.kt`): normaler Name, Trimming, leer/blank→„Gast", exakt 24 Zeichen gültig, 25 Zeichen ungültig (`TooLong`), Längenprüfung nach Trimmen; gespeicherte Alt-Werte (leer/zu lang/null→„Gast", gültig getrimmt); Feldgrößen 3/4/5 gültig, 2/6/0/null→3. Implementierung `score/SettingsNormalizer.kt` mit Ergebnis-Typ `NameValidation (Valid|TooLong)` – zu lange UI-Eingaben werden nicht still gespeichert, gespeicherte Alt-Werte fallen auf „Gast" zurück.
2. **HighScoreRanking** (`score/HighScoreRankingTest.kt`): Sortierung Treffer absteigend, Gleichstand→kürzere Dauer, voller Gleichstand→früherer Zeitstempel, Top-10-Schnitt (Schwächster fällt, Verdrängung Platz 10), Eingabeliste unverändert (neue Liste), leere Liste. Implementierung `score/HighScoreRanking.kt` (`insert`: sort+take(10)) plus `score/HighScoreEntry.kt` als `Serializable`-Data-Class mit `serialVersionUID = 1L` und `RANK_COMPARATOR`.
3. **BoardGenerator** (`game/BoardGeneratorTest.kt`): für n∈{3,4,5} genau 1..n² eindeutig, Ziel im Feld, gleicher Seed→gleiches Ergebnis, n∈{2,6,0}→`IllegalArgumentException`. Implementierung `game/BoardGenerator.kt` mit injizierbarem `kotlin.random.Random`: `(1..n*n).toMutableList().apply { shuffle(random) }`, Ziel via `numbers[random.nextInt(numbers.size)]` (garantiert im Feld).
4. **GameSession** (`game/GameSessionTest.kt`): Start→elapsed 0, Score/Misses 0, Feld gültig; negative Restzeit→0; monotones Wachstum; Treffer→+1 & frisches gültiges Feld (Ziel im neuen Feld); Fehlversuch→Zähler+1 bei gleichem Feld/Ziel; 2 Fehler→nicht beendet; 3. Fehler→`finished` + eingefrorene Zeit (auch viel später identisch abfragbar); Taps nach Ende (falsch **und** „Treffer") wirkungslos; `restore(...)` setzt laufende Runde exakt fort (Score, Fehler, Feld, Ziel, Zeit fortsetzbar, danach zählt weiter) und beendete Runde bleibt beendet und eingefroren. Implementierung `game/GameSession.kt`: zeitlos modelliert (alle Zeitpunkte als Parameter), `finalElapsedMillis` friert Zeit erst bei Miss 3 ein, Treffer generiert sofort neues Feld ohne Rundenende, `restore(snapshot, startMillis)` für Rotationswiederherstellung (`startMillis = jetzt − gespeicherteElapsed`).

Keine Mock-Bibliotheken, alle Zufallstests mit festem Seed; keine trivialen Getter-/Data-Class-Tests.

### Android-Persistenz

- `score/SettingsStorage.kt`: kleines Interface (getString/putString/getInt/putInt), Trennung pure Logik ↔ Android.
- `score/AppSettingsRepository.kt` (implementiert `SettingsStorage`): private Named-Preferences „switchsort_settings", Schlüssel `player_name` (String) und `board_size` (Int), Schreiben mit `apply()`. `savePlayerName` liefert `NameValidation` zurück und schreibt bei `TooLong` nichts.
- `score/HighScoreRepository.kt`: private Named-Preferences „switchsort_highscores", ein Schlüssel `entries`; Java-Serialisierung (`ObjectOutputStream` einer `ArrayList<HighScoreEntry>`, Base64 via `android.util.Base64`). Beim Laden führen `ClassCastException`, `ClassNotFoundException`, `IOException`, `SecurityException` und `IllegalArgumentException` (kaputtes Base64) zu leerer Liste plus entferntem Schlüssel (defekte Alt-Daten werden bereinigt statt abzustürzen). Die Deserialisierung liest ausschließlich selbstgeschriebene, app-private Daten – keine externe Quelle. Ranking/Trimming in `HighScoreRanking` (JVM-testbar), Serialisierung nur hier; keine doppelten Utils.
- Begründung Java-Serialisierung statt JSON: keine zusätzliche Bibliothek (Vorgabe: minimale Dependencies), Datenmenge ≤10 kleine Einträge, Klasse hat feste `serialVersionUID`.

### UI (eine Activity, vier Panels, klassische Views)

- `MainActivity` (Paket `de.dhbw.switchsort`, extends `android.app.Activity`, kein AppCompat) mit Panels `menu`/`options`/`game`/`scores` in `res/layout/activity_main.xml` (Sichtbarkeitssteuerung). Hauptmenü: Spiel starten, Highscore, Optionen, Beenden (`finish()`).
- Optionen: EditText Name, RadioButtons 3×3/4×4/5×5, **„Speichern"** (validiert; zu langer Name→Inline-Fehltext, es wird nichts gespeichert) und **„Zurück"** ohne Speichern (bewusste Trennung statt unzuverlässigem Speichern bei Fokusverlust).
- Highscore: formatierte Top 10 als TextView-Liste in ScrollView („1. Name · 3×3 · 7 Treffer · 01:24"), kein RecyclerView (keine AndroidX-Dependenz).
- Spielpanel: Zielzahl, Treffer, Fehlversuche X/3, verstrichene Zeit mm:ss (1-s-`Handler`-Loop nur solange die Runde aktiv ist; stoppt bei Ende/Abbruch/`onStop`), GridLayout (Plattform-Widget, API≥14; Spalten per Gewicht gleichmäßig – so bleibt 5×5 auf kleinen Displays nutzbar, Panel liegt zusätzlich in ScrollView).
- Ende: Brett deaktiviert, „SPIELENDE – X Treffer in mm:ss" + Buttons „Nochmal spielen" (gleiche Feldgröße aus den Einstellungen) und „Zurück zum Menü". Ergebnis wird **genau einmal** gespeichert (Flag `savedThisRound`, rotiert mit).
- **Abbruch (Zurück-Navigation aus dem Spiel) speichert nichts** – begründet: Highscore erfasst abgeschlossene Runden; Abbrüche würden die Liste mit Ergebnissen ohne natürliches Rundenende verzerren.
- Rotation: `onSaveInstanceState` speichert Brett, Ziel, Score, Misses, Feldgröße, gespielte Zeit, Finished-Flag und Saved-Flag; `GameSession.restore(snapshot, startMillis = SystemClock.elapsedRealtime() − elapsed)` rekonstruiert die Runde; eine beendete Runde wird **ohne erneutes Speichern** weiter angezeigt.
- Alle sichtbaren Texte in `res/values/strings.xml` (Deutsch), Manifest-Label auf `@string/app_name`. Keine Berechtigungen im Manifest, `allowBackup="false"` unverändert.
- Zeitmessung in der App: `SystemClock.elapsedRealtime()` (monoton) → Parameter der JVM-testbaren `GameSession`; `finishedAtMillis` eines Highscore-Eintrags ist bewusst Wall-Clock (`System.currentTimeMillis()`) für Anzeige/Sortier-Stabilität.

### Bekannte Grenzen / ausstehende Nachweise

- **Build & Tests nicht ausgeführt** (Blocker oben): `./gradlew testDebugUnitTest`, `lintDebug`, `assembleDebug` sowie Red/Green-Protokolle, Lint-Ergebnis, APK-Pfad und Geräte-/Emulator-Smoke-Test fehlen noch vollständig. Ein Geräte-Smoke-Test (manuelles Tippen, Rotation, App-Neustart mit gespeicherten Einstellungen/Highscore) ist Voraussetzung für die Akzeptanzkriterien des Plans; ein JVM-Test allein beweist keine lauffähige App.
- Java-Serialisierung ist versionsfragiler als JSON; akzeptiert wegen fester `serialVersionUID` und Selbstheilung (defekte Daten → Reset). Fremddaten werden nie deserialisiert.
- Zu plugin-Konfiguration: falls der Build zeigt, dass AGP 9.3.1 das explizite `org.jetbrains.kotlin.android` anders erwartet (z. B. Built-in-Kotlin-Konflikt), ist die Root-Plugins-Variante ohne buildscript-Classpath zu prüfen – dann muss die BC-BOM-Constraint neu verankert werden, sonst fällt bcprov auf 1.79 zurück.
- Richter (2021) wird nur als Literatur nachgewiesen (siehe unten); es wurden keine Buchinhalte referenziert oder Details daraus behauptet.

### Build-/Testanleitung (Befehle, sobald SDK und Ausführungsrechte vorliegen)

Voraussetzungen: JDK 21 (vorhanden: `C:\Program Files\Java\jdk-21` bzw. Eclipse Adoptium 21.0.9 im PATH) und ein Android SDK mit Plattform `android-35` und Build-Tools.

```bash
# Im Projektverzeichnis, Git Bash:
# 1) SDK-Pfad setzen (eine der beiden Varianten):
printf 'sdk.dir=C:\\\\Users\\\\vierhec\\\\AppData\\\\Local\\\\Android\\\\Sdk\n' > local.properties   # (gitignored)
#    oder: export ANDROID_HOME="C:/Users/vierhec/AppData/Local/Android/Sdk"
# 2) Ausführen:
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew testDebugUnitTest
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew lintDebug
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew assembleDebug
# APK danach: app/build/outputs/apk/debug/app-debug.apk
```

Hinweis: In den Vorexperimenten wurde `GRADLE_USER_HOME=<projekt>/.gradle-home` genutzt (gitignored, Wrapper-Cache); ohne diese Variable wird standardmäßig `%USERPROFILE%\.gradle` verwendet – beides funktioniert, die Versionen sind per `distributionSha256Sum` gepinnt.

### Quellen und Lizenznotizen (Abschnitt)

- Richter, E. (2021): *Android-Apps programmieren – Professionelle App-Entwicklung mit Android Studio 4.* (nur Literaturverweis für den Views-Aktivitätsansatz, keine Detailzitate).
- Bouncy Castle: offizielle Lizenz der Legion of the Bouncy Castle ist an eine MIT-/X11-Lizenz angelehnt (MIT-äquivalent); Freigabe des BOM 1.86 durch Lead erfolgt.
- `junit:junit:4.13.2` (nur Test-Scope) zieht transitiv `org.hamcrest:hamcrest-core:1.3`; `org.opentest4j` ist **keine** Abhängigkeit von JUnit 4 und wird hier nicht eingebunden (kein Fund, nichts zu entscheiden).
- Android-API-Referenzen siehe Plan-Abschnitt „Verifizierte technische Fakten" (SharedPreferences, SystemClock, kotlin.random).

## 2026-10-06 — SDK-Einrichtung und Verifikation (nachgeholt)

- **SDK:** Auf Freigabe des Nutzers wurden die Android Command Line Tools (11076708) nach `C:\Users\vierhec\Android\Sdk` installiert; per `sdkmanager` Pakete `platforms;android-35`, `build-tools;36.0.0`, `platform-tools` sowie Lizenzen. `local.properties` (gitignored) zeigt mit escaped Pfad (`sdk.dir=C\:/Users/vierhec/Android/Sdk`) darauf.
- **Plugin-Entscheidung, korrigiert durch Build-Evidenz:** Das explizite `org.jetbrains.kotlin.android`-Plugin schlug mit AGP 9.3.1 fehl („no longer required … since AGP 9.0", Fehler beim Apply). Entfernt; Kotlin-Kompilierung läuft über AGP-9-Built-in-Kotlin mit `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. Die Kotlin-Gradle-Plugin-Version verbleibt als 2.4.20 auf dem Buildscript-Classpath; die BC-BOM-1.86-Constraint greift weiterhin genau dort.
- **Nachweise (tatsächlich ausgeführt):**
  - `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**, 35 Tests, 0 Failures, 0 Errors (`app/build/test-results/testDebugUnitTest/`).
  - `./gradlew assembleDebug` → **BUILD SUCCESSFUL**; APK: `app/build/outputs/apk/debug/app-debug.apk` (≈0,96 MB).
  - `./gradlew lintDebug` → **BUILD SUCCESSFUL**; 0 Fehler, 8 Warnungen, alle als für den MVP akzeptiert bewertet: `OldTargetApi` (targetSdk 35 bewusst), `GradleDependency` (compileSdk 35 bewusst), 2× `PluralsCandidate` (deutsche Einzahl identisch), `DataExtractionRules` (komplett kein Backup gewünscht, `allowBackup=false` genügt), `MergeRootFrame`, `MissingApplicationIcon` (Icon kommt mit der Verschönerungsphase), `SetTextI18n` (reine Zahlenbuttons).
- **Ausstehend:** Geräte-/Emulator-Smoke-Test (Tippen, Rotation, Highscore-/Einstellungs-Persistenz über App-Neustart). Der Nutzer muss selbst ein Emulator-Image oder Gerät bereitstellen; kein Emulator-Image installiert.
- **Mehrwert für die wissenschaftliche Arbeit:** Der Red/Green-Prozess wurde nachträglich als *geschriebenes* TDD-Protokoll dokumentiert (Abschnitt „TDD-Scheiben"), die tatsächliche Ausführung erfolgte erst jetzt. Vorgehen muss in der Arbeit ehrlich als „tests zuerst geschrieben, Ausführung erst nach SDK-Bereitstellung" beschrieben werden.
