package io.github.zeroone3010.turtleviewer.rdf

import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import java.io.InputStream
import java.net.URLDecoder

/** App-owned RDF view data. Compose never receives RDF4J values directly. */
data class RdfDocumentView(val roots: List<RdfResourceView>, val otherResources: List<RdfResourceView>, val prefixes: Map<String, String>, val resources: Map<String, RdfResourceView> = emptyMap())
data class RdfResourceView(val id: String, val displayLabel: String, val compactId: String, val kind: ResourceKind, val properties: List<RdfPropertyView>)
data class RdfPropertyView(val iri: String, val label: String, val compactIri: String, val values: List<RdfValueView>)
sealed interface RdfValueView {
    data class LiteralValue(val lexicalValue: String, val displayValue: String, val datatypeIri: String?, val language: String?) : RdfValueView
    data class ResourceReference(val resourceId: String, val displayLabel: String, val kind: ResourceKind, val isExpandable: Boolean) : RdfValueView
}
enum class ResourceKind { IRI, BLANK_NODE }
sealed interface ReadableRdfState { data object Loading : ReadableRdfState; data object Empty : ReadableRdfState; data class Ready(val document: RdfDocumentView) : ReadableRdfState; data class Error(val message: String) : ReadableRdfState }

object TurtleRdfParser {
    fun parse(input: InputStream, baseIri: String): RdfDocumentView {
        val model: Model = LinkedHashModel()
        val prefixes = linkedMapOf<String, String>()
        val collector = object : StatementCollector(model) {
            override fun handleNamespace(prefix: String, uri: String) { prefixes[prefix] = uri }
        }
        Rio.createParser(RDFFormat.TURTLE).apply { rdfHandler = collector }.parse(input, baseIri)
        return RdfDisplayBuilder.build(model, prefixes)
    }
}

object RdfDisplayBuilder {
    fun build(model: Model, prefixes: Map<String, String>): RdfDocumentView {
        val subjects = model.map { it.subject }.distinct()
        if (subjects.isEmpty()) return RdfDocumentView(emptyList(), emptyList(), prefixes)
        val subjectIds = subjects.map { id(it) }.toSet()
        val referenced = model.mapNotNull { (it.`object` as? Resource)?.let(::id) }.toSet()
        val roots = subjects.filter { id(it) !in referenced }.ifEmpty { subjects }
        val reachable = mutableSetOf<String>()
        fun visit(resource: Resource) {
            if (!reachable.add(id(resource))) return
            model.filter(resource, null, null).forEach { statement -> (statement.`object` as? Resource)?.let(::visit) }
        }
        roots.forEach(::visit)
        fun resource(resource: Resource) = RdfResourceView(
            id(resource), label(resource, prefixes), compact(resource, prefixes), if (resource is BNode) ResourceKind.BLANK_NODE else ResourceKind.IRI,
            model.filter(resource, null, null).groupBy { it.predicate }.map { (predicate, statements) ->
                RdfPropertyView(predicate.stringValue(), label(predicate, prefixes), compact(predicate, prefixes), statements.map { value(it.`object`, subjectIds, prefixes) })
            }
        )
        val all = subjects.associate { id(it) to resource(it) }
        return RdfDocumentView(roots.map { all.getValue(id(it)) }, subjects.filter { id(it) !in reachable }.map { all.getValue(id(it)) }, prefixes, all)
    }
    private fun value(value: Value, subjects: Set<String>, prefixes: Map<String, String>): RdfValueView = when (value) {
        is Literal -> RdfValueView.LiteralValue(value.label, value.label, value.datatype?.stringValue(), value.language.orElse(null))
        is Resource -> RdfValueView.ResourceReference(id(value), label(value, prefixes), if (value is BNode) ResourceKind.BLANK_NODE else ResourceKind.IRI, id(value) in subjects)
        else -> error("Unsupported RDF value")
    }
    private fun id(resource: Resource) = resource.stringValue()
    fun compact(iri: Resource, prefixes: Map<String, String>): String = if (iri is BNode) "_:${iri.id}" else prefixes.entries.firstOrNull { iri.stringValue().startsWith(it.value) }?.let { "${it.key}:${iri.stringValue().removePrefix(it.value)}" } ?: iri.stringValue()
    fun label(resource: Resource, prefixes: Map<String, String>): String {
        if (resource is BNode) return "Blank node"
        val value = resource.stringValue(); val local = value.substringAfterLast('#', "").ifEmpty { value.trimEnd('/').substringAfterLast('/', "") }.ifEmpty { compact(resource, prefixes) }
        return humanize(local)
    }
    fun humanize(value: String): String {
        val decoded = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
        return decoded.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace(Regex("[_-]+"), " ").replace(Regex("\\s+"), " ").trim().replaceFirstChar { it.uppercase() }.ifBlank { value }
    }
}
