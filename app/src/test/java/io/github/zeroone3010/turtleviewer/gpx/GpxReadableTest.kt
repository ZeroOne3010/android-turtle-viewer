package io.github.zeroone3010.turtleviewer.gpx

import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class GpxReadableTest {
    @Test fun coordinatesCoverAllHemispheres() {
        assertEquals("60°25.135′ N, 25°14.228′ E", formatCoordinates(60.4189167, 25.2371333))
        assertEquals("12°30.000′ S, 45°15.000′ W", formatCoordinates(-12.5, -45.25))
    }
    @Test fun distanceSpeedBearingAndDirectionAreDerived() {
        val points = listOf(GpxPoint(0.0, 0.0, null, java.time.Instant.parse("2020-01-01T00:00:00Z")), GpxPoint(0.0, 0.001, null, java.time.Instant.parse("2020-01-01T00:00:10Z")))
        val displayed = gpxDisplayItems(listOf(GpxTrack(listOf(GpxSegment(points))))).filterIsInstance<GpxDisplayItem.Point>()
        assertEquals("Start", if (displayed[0].isStart) "Start" else "not start")
        assertTrue(displayed[1].speed!!.startsWith("40.0")); assertEquals("→ 90° E", displayed[1].bearing)
        assertEquals("↗ 44° NE", formatBearing(44.0))
    }
    @Test fun segmentsDoNotCrossBoundariesAndMissingValuesRemain() {
        val gpx = """<gpx><trk><trkseg><trkpt lat="1" lon="2"><time>2020-01-01T00:00:00Z</time></trkpt></trkseg><trkseg><trkpt lat="3" lon="4"><ele>5</ele></trkpt></trkseg></trk><trk><trkseg><trkpt lat="5" lon="6"/></trkseg></trk></gpx>"""
        val tracks = GpxReadableParser.parse(ByteArrayInputStream(gpx.toByteArray()))
        val points = gpxDisplayItems(tracks).filterIsInstance<GpxDisplayItem.Point>()
        assertEquals(2, tracks.size); assertEquals(2, tracks[0].segments.size); assertTrue(points.all { it.isStart }); assertNull(points[1].point.time); assertEquals(5.0, points[1].point.elevation)
    }
    @Test fun namespacePrefixedElementsAreParsed() {
        val gpx = """<gpx:gpx xmlns:gpx="http://www.topografix.com/GPX/1/1"><gpx:trk><gpx:trkseg><gpx:trkpt lat="60" lon="25"><gpx:ele>12</gpx:ele></gpx:trkpt></gpx:trkseg></gpx:trk></gpx:gpx>"""
        val tracks = GpxReadableParser.parse(ByteArrayInputStream(gpx.toByteArray()))
        assertEquals(1, tracks.size)
        assertEquals(1, tracks.single().segments.single().points.size)
        assertEquals(12.0, tracks.single().segments.single().points.single().elevation)
    }
    @Test fun unavailableIntervalsAreSafe() {
        val time = java.time.Instant.parse("2020-01-01T00:00:00Z")
        val a = GpxPoint(1.0, 1.0, null, time)
        val duplicateTime = GpxPoint(1.1, 1.0, null, time)
        val zeroDistance = GpxPoint(1.1, 1.0, null, time.plusSeconds(2))
        assertNull(formatBearing(initialBearing(duplicateTime, zeroDistance)))
        val shown = gpxDisplayItems(listOf(GpxTrack(listOf(GpxSegment(listOf(a, duplicateTime, zeroDistance)))))).filterIsInstance<GpxDisplayItem.Point>()
        assertNull(shown[1].speed); assertNull(shown[2].speed)
    }

    @Test fun `large segments are sampled to keep readable output bounded`() {
        val points = (0..10_000).map { GpxPoint(0.0, it.toDouble() / 100, null, null) }

        val displayed = gpxDisplayItems(listOf(GpxTrack(listOf(GpxSegment(points)))), maxPoints = 100)
        val heading = displayed.filterIsInstance<GpxDisplayItem.SegmentHeading>().single()
        val shownPoints = displayed.filterIsInstance<GpxDisplayItem.Point>()

        assertEquals(10_001, heading.pointCount)
        assertEquals(100, heading.displayedPointCount)
        assertEquals(100, shownPoints.size)
        assertEquals(points.first(), shownPoints.first().point)
        assertEquals(points.last(), shownPoints.last().point)
    }

    @Test fun `point budget is shared across segments`() {
        val segments = List(3) { GpxSegment(List(10) { GpxPoint(0.0, 0.0, null, null) }) }

        val displayed = gpxDisplayItems(listOf(GpxTrack(segments)), maxPoints = 4)

        assertEquals(3, displayed.filterIsInstance<GpxDisplayItem.Point>().size)
    }
    @Test(expected = Exception::class) fun malformedGpxFails() { GpxReadableParser.parse(ByteArrayInputStream("<gpx><trk>".toByteArray())) }

    @Test fun `does not resolve external DTDs while parsing a local GPX file`() {
        val gpx = """<!DOCTYPE gpx SYSTEM "https://unavailable.example/gpx.dtd"><gpx><trk><trkseg><trkpt lat="1" lon="2"/></trkseg></trk></gpx>"""

        val tracks = GpxReadableParser.parse(ByteArrayInputStream(gpx.toByteArray()))

        assertEquals(1, tracks.single().segments.single().points.size)
    }
}
