# Turtle Viewer

A small native Android viewer for local Turtle (`.ttl`) and GPX (`.gpx`) files. It opens a file through Android's document picker or from another app's **Open with** / share flow, then displays the raw UTF-8 text without parsing or modifying it.

## Syntax highlighting

Syntax highlighting uses native, hand-written Kotlin lexers selected by file format. `TurtleScanner` is a
deterministic cursor state machine over a CharSequence: it emits immutable token
ranges rather than copying token text, tracks line/column positions, and recovers
with ERROR tokens for incomplete editor input. It supports comments, directives,
IRIs, prefixed names, blank-node labels, quoted and long strings, numeric literals,
booleans, language tags, datatype markers, and Turtle punctuation.

The Turtle lexer is deliberately not a Turtle parser. It does not create triples, resolve
prefixes, validate RDF terms, or construct a graph. Those semantic responsibilities
belong to RDF4J when RDF parsing is added. The lexer has no Android dependency and
the small Compose adapters map their tokens to `AnnotatedString` spans on the file-loading
worker, rather than during composition; no TextMate,
Tree-sitter, parser generator, or third-party grammar definition is bundled.

## Open a file

* Launch Turtle Viewer and select **Open file**.
* Choose either a `.ttl` or `.gpx` file.
* In another Android app, open or share a `.ttl` or `.gpx` file and select **Turtle Viewer**.

The app declares Turtle MIME types (`text/turtle`, `application/x-turtle`, and `application/turtle`) and GPX MIME types (`application/gpx+xml` and `application/gpx`), plus narrowly scoped `.ttl` and `.gpx` filename fallbacks. Providers that expose neither an appropriate MIME type nor a supported display/path name may not offer Turtle Viewer; this is intentional so the app is not offered for unrelated files.

## Build and install

Build locally with Java 17 and Android SDK Platform 35 installed:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

The installable debug APK is `app/build/outputs/apk/debug/app-debug.apk`. From a GitHub Actions run, download the **turtle-viewer-debug-apk** artifact, extract it, transfer `app-debug.apk` to an Android device, enable installation from unknown sources for the installer you use, and install it.

## Current limitations

* Turtle and GPX are displayed as raw UTF-8 text; there is no RDF/GPX semantic parsing, editing, saving, search, or graph view.
* Files larger than 5 MB are refused to keep rendering responsive.
* Access uses Android `content://` URIs via `ContentResolver`; the app never assumes a filesystem path.

## Design

File opening is separated from rendering. `UriFileReader` obtains metadata and bytes, `FileHandlerRegistry` chooses a `FileHandler`, and `TurtleFileHandler` / `GpxFileHandler` recognize and load text. A syntax-format dispatcher chooses Turtle or XML highlighting. New handlers for JSON, XML, images, logs, or binary formats can be registered without redesigning the activity or Compose screen.

## Roadmap

* Parsed RDF triple view
* RDF graph visualization
* Search within files
* Additional file handlers
* Release signing
