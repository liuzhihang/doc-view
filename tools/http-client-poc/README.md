# IDEA HTTP Client bridge PoC

This is an isolated Java-only probe for the IntelliJ IDEA 2026.1 HTTP Client bridge. It compiles against the selected IDE distribution, packages a small plugin, and can run the cached Plugin Verifier. It does not modify the Doc View production plugin or start an IDE automatically.

## Build and verify

Use a fresh output directory outside this repository. On macOS, select the exact IDEA distribution and Java 21 explicitly:

```sh
python3 tools/http-client-poc/run.py --help
python3 tools/http-client-poc/run.py \
  --ide-home "$DOC_VIEW_HTTP_POC_IDE_HOME" \
  --java-home /opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home \
  --output-dir "$DOC_VIEW_HTTP_POC_OUTPUT" \
  --verify
```

The output contains `doc-view-http-client-poc.zip`, the compile and verifier logs, `build-report.json`, and a new isolated `profile/` with separate config, system, log, and plugin directories. The script refuses to reuse an existing output directory or place output under the repository. A verifier result counts as accepted only when at least one scheduled verification completed, the plugin is compatible, and there are no invalid-descriptor, deprecated, internal, or override-only API findings. Build/verification does not install the plugin or launch the IDE.

## Install and launch separately

Only use the isolated profile created in the output directory. Never point `IDEA_PROPERTIES` at a regular IDEA profile or another PoC profile. Install the ZIP into the PoC profile's plugin directory. For launch, `DOC_VIEW_HTTP_POC_IDE_HOME` must refer to the official target `.app/Contents` distribution, not only Gradle's transformed SDK cache; the latter was suitable for compile/verification but its direct launcher exited 137 without producing `idea.log` in this run. Do not change quarantine or signing to force it to launch. With an official target app bundle available, launch it with the generated properties file:

```sh
mkdir -p "$DOC_VIEW_HTTP_POC_OUTPUT/profile/plugins"
unzip -q "$DOC_VIEW_HTTP_POC_OUTPUT/doc-view-http-client-poc.zip" \
  -d "$DOC_VIEW_HTTP_POC_OUTPUT/profile/plugins"
IDEA_PROPERTIES="$DOC_VIEW_HTTP_POC_OUTPUT/profile/idea.properties" \
  "$DOC_VIEW_HTTP_POC_IDE_HOME/MacOS/idea"
```

Build, install, launch, and UI acceptance are separate steps. After launch, inspect `profile/log/idea.log` for plugin load errors. In the verified IU 261.22158.277 isolated run, IDEA started but disabled `com.intellij.modules.ultimate`; Spring Web and this probe were consequently disabled, while HTTP Client, Endpoints, Java, and Markdown were enabled. The probe did not load, and no trial, license, or disabled-plugin setting was changed. The Tools menu action is named **Doc View HTTP Client PoC: Inspect selected method context** when its dependencies are enabled. It only reports Spring URL-context discovery and whether the native request builder can be created. It deliberately does not publish or execute a request.

## Probe boundary

The plugin has a compile-time reference to `HttpRequestGenerationManager.generateRequestsInHttpEditor`, but the method is never called. SDK bytecode shows that this method launches a coroutine and discards the `Job`, leaving the caller without a completion or cancellation handle. Therefore native generation was rejected at the lifecycle gate; A01–A12 generation, cancellation, and content-preservation scenarios are not run. The fictional Spring and Feign fixtures are local PSI inputs only and contain no real endpoint or credentials.

Do not use this PoC to send network requests. It has no execute action and the fixture host uses the reserved `.invalid` domain.
