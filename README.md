# Turtle Viewer

A small native Android viewer for local Turtle (`.ttl`), GPX (`.gpx`), and JSON (`.json`) files. It opens a file through Android's document picker or from another app's **Open with** / share flow. JSON has a fast, raw view and an asynchronously prepared formatted view.

## Readable RDF outline

Turtle files also have a **Readable** tab, a generic RDF outline beside the unchanged highlighted **Source** tab. It is not an ontology-aware interpretation: there are no Schema.org, FOAF, Dublin Core, activity, fitness, or other vocabulary-specific rules.

Eclipse RDF4J Rio performs standards-compliant Turtle parsing and produces RDF statements. The renderer groups statements by subject and predicate, derives labels from IRI fragments/path components (splitting camel case, underscores, hyphens, and safe percent encodings), and keeps full IRIs in collapsed technical details. Roots are subjects that are not resource-valued objects; when a cycle leaves no roots, all subjects are shown. Disconnected subjects appear under **Other resources**. Blank nodes have a friendly label, local resources can expand inline, and path-aware expansion (maximum depth 20) turns cycles into references rather than unbounded trees. Literals retain their lexical form, datatype, and language; values and units are never combined using vocabulary rules.

Responsibilities are intentionally separate:

* **Custom Turtle lexer**: syntax highlighting and source ranges only.
* **RDF4J Rio**: Turtle parsing and RDF statement creation.
* **Readable renderer**: generic grouping, labels, nesting, and display.

The readable outline is not a graph visualization, query UI, validator, editor, or ontology browser. GPX/XML reading and its XML lexer/view are unchanged.

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
* Choose a `.ttl`, `.gpx`, or `.json` file.
* In another Android app, open or share a supported file and select **Turtle Viewer**.

The document picker deliberately shows all openable documents because many Android document providers use generic MIME types. Turtle Viewer still opens only `.ttl`, `.gpx`, and `.json` files after selection. For **Open with** and share flows, the app declares Turtle, GPX, JSON, and common XML MIME types, plus narrowly scoped supported-extension URI-path fallbacks.

## Build and install

Build locally with Java 17 and Android SDK Platform 35 installed:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

The installable debug APK is `app/build/outputs/apk/debug/app-debug.apk`. From a GitHub Actions run, download the **turtle-viewer-debug-apk** artifact, extract it, transfer `app-debug.apk` to an Android device, enable installation from unknown sources for the installer you use, and install it.

## Current limitations

* Turtle and GPX source is displayed as raw UTF-8 text; there is no editing, saving, or search.
* The GPX Readable tab opens first and samples long segments (up to 2,000 displayed points in total) so a dense track log remains responsive. The **Map** tab renders those samples as an interactive, framed OpenStreetMap track: it can be panned and zoomed without resetting during normal UI updates. The complete, syntax-highlighted source is rendered only when the user selects the Source tab.
* Turtle and GPX files larger than 5 MB, and JSON files larger than 50 MB, are refused to keep memory use bounded.
* Access uses Android `content://` URIs via `ContentResolver`; the app never assumes a filesystem path.

## Design

File opening is separated from rendering. `UriFileReader` obtains metadata and bytes, `FileHandlerRegistry` chooses a `FileHandler`, and the Turtle, GPX, and JSON handlers recognize and load text. JSON formatting is a non-recursive linear pass on a worker thread, and both raw and formatted large documents are split into bounded chunks for lazy rendering. A syntax-format dispatcher chooses Turtle or XML highlighting. New handlers for XML, images, logs, or binary formats can be registered without redesigning the activity or Compose screen.

## Roadmap

* Parsed RDF triple view
* RDF graph visualization
* Search within files
* Additional file handlers
* Release signing

---
✨ Made with vibes ✨
