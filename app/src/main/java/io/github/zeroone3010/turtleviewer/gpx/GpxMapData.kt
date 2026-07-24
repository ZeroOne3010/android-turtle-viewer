package io.github.zeroone3010.turtleviewer.gpx

/** A map-safe coordinate derived from the GPX document already parsed for Readable. */
data class MapCoordinate(val latitude: Double, val longitude: Double)
data class MapRouteSegment(val points: List<MapCoordinate>)
data class MapBounds(val south: Double, val west: Double, val north: Double, val east: Double)

fun mapRouteSegments(tracks: List<GpxTrack>): List<MapRouteSegment> = tracks.flatMap { track ->
    track.segments.map { segment ->
        MapRouteSegment(segment.points.mapNotNull { point -> point.mapCoordinateOrNull() })
    }
}

fun mapBounds(segments: List<MapRouteSegment>): MapBounds? {
    val points = segments.flatMap { it.points }
    if (points.isEmpty()) return null
    return MapBounds(
        south = points.minOf { it.latitude }, west = points.minOf { it.longitude },
        north = points.maxOf { it.latitude }, east = points.maxOf { it.longitude }
    )
}

private fun GpxPoint.mapCoordinateOrNull(): MapCoordinate? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
        MapCoordinate(lat, lon)
    } else null
}
