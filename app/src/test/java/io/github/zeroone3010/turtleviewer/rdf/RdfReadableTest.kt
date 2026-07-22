package io.github.zeroone3010.turtleviewer.rdf

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RdfReadableTest {
    private fun parse(turtle: String) = TurtleRdfParser.parse(ByteArrayInputStream(turtle.toByteArray()), "https://example.test/base/")
    @Test fun parsesPrefixesRepeatedPredicatesBlankNodesAndLiteralMetadata() {
        val document = parse("""@prefix ex: <https://example.test/> . ex:item ex:tag "one", "two"; ex:child [ ex:value 3; ex:word "hello"@en ] .""")
        val item = document.roots.single()
        assertEquals(2, item.properties.single { it.label == "Tag" }.values.size)
        assertTrue(item.properties.single { it.label == "Child" }.values.single() is RdfValueView.ResourceReference)
        assertEquals("en", ((document.resources.values.flatMap { it.properties }.single { it.label == "Word" }.values.single()) as RdfValueView.LiteralValue).language)
    }
    @Test fun cyclesDisplayEverySubjectAsRoot() { assertEquals(2, parse("@prefix e: <https://e/>. e:a e:p e:b. e:b e:p e:a.").roots.size) }
    @Test fun iriAndBlankNodeWithSameLexicalValueHaveDistinctInternalIds() {
        val document = parse("<urn:foo> <urn:p> \"iri\" . _:urn:foo <urn:p> \"blank\" .")
        assertEquals(2, document.resources.size)
        assertEquals(2, document.roots.map { it.id }.toSet().size)
    }
    @Test fun labelsAreGeneric() {
        assertEquals("Start time", RdfDisplayBuilder.humanize("startTime"))
        assertEquals("Measured property", RdfDisplayBuilder.humanize("measured_property"))
        assertEquals("Average speed", RdfDisplayBuilder.humanize("average-speed"))
        assertEquals("Hello world", RdfDisplayBuilder.humanize("hello%20world"))
        assertEquals("C++", RdfDisplayBuilder.humanize("C++"))
        assertEquals("C++", RdfDisplayBuilder.humanize("C%2B%2B"))
    }
}
