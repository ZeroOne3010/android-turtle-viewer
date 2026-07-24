package io.github.zeroone3010.turtleviewer.gpx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GpxMapDataTest {
    @Test fun `every segment becomes its own map polyline`() {
        val tracks = listOf(
            GpxTrack(listOf(GpxSegment(listOf(point(1.0, 2.0))), GpxSegment(listOf(point(3.0, 4.0), point(5.0, 6.0))))),
            GpxTrack(listOf(GpxSegment(listOf(point(7.0, 8.0)))))
        )
        val segments = mapRouteSegments(tracks)
        assertEquals(3, segments.size)
        assertEquals(listOf(MapCoordinate(1.0, 2.0)), segments[0].points)
        assertEquals(listOf(MapCoordinate(3.0, 4.0), MapCoordinate(5.0, 6.0)), segments[1].points)
    }

    @Test fun `bounds include valid points and discard invalid coordinates`() {
        val segments = mapRouteSegments(listOf(GpxTrack(listOf(GpxSegment(listOf(
            point(-10.0, -20.0), point(11.0, 30.0), point(91.0, 0.0), point(Double.NaN, 0.0)
        ))))))
        assertEquals(MapBounds(-10.0, -20.0, 11.0, 30.0), mapBounds(segments))
    }

    @Test fun `empty routes have no bounds and a single point is retained`() {
        assertNull(mapBounds(mapRouteSegments(emptyList())))
        val segment = mapRouteSegments(listOf(GpxTrack(listOf(GpxSegment(listOf(point(1.0, 2.0))))))).single()
        assertEquals(listOf(MapCoordinate(1.0, 2.0)), segment.points)
        assertEquals(MapBounds(1.0, 2.0, 1.0, 2.0), mapBounds(listOf(segment)))
    }

    private fun point(lat: Double, lon: Double) = GpxPoint(lat, lon, null, null)
}
