package io.github.zeroone3010.turtleviewer.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.zeroone3010.turtleviewer.gpx.MapBounds
import io.github.zeroone3010.turtleviewer.gpx.MapCoordinate
import io.github.zeroone3010.turtleviewer.gpx.mapBounds
import io.github.zeroone3010.turtleviewer.gpx.mapRouteSegments
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@androidx.compose.runtime.Composable
fun GpxMapView(tracks: List<io.github.zeroone3010.turtleviewer.gpx.GpxTrack>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val segments = remember(tracks) { mapRouteSegments(tracks) }
    val routeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.toArgb()
    val alternateColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary.toArgb()
    val startColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary.toArgb()
    val endColor = androidx.compose.material3.MaterialTheme.colorScheme.error.toArgb()
    val map = remember { MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true) } }

    DisposableEffect(map, lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { map.onResume() }
            override fun onPause(owner: LifecycleOwner) { map.onPause() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); map.onDetach() }
    }
    // This effect is keyed by the parsed document, not normal Compose recompositions.
    LaunchedEffect(map, segments) {
        map.overlays.clear()
        segments.forEachIndexed { index, segment ->
            if (segment.points.isNotEmpty()) {
                map.overlays += Polyline().apply {
                    setPoints(segment.points.map(::geoPoint))
                    outlinePaint.color = if (index % 2 == 0) routeColor else alternateColor
                    outlinePaint.strokeWidth = 8f
                }
                map.overlays += endpointMarker(map, segment.points.first(), "Segment start", startColor)
                map.overlays += endpointMarker(map, segment.points.last(), "Segment end", endColor)
            }
        }
        map.post { fitRoute(map, mapBounds(segments)) }
        map.invalidate()
    }
    AndroidView(factory = { map }, modifier = modifier)
}

private fun geoPoint(point: MapCoordinate) = GeoPoint(point.latitude, point.longitude)

private fun endpointMarker(map: MapView, point: MapCoordinate, title: String, color: Int) = Marker(map).apply {
    position = geoPoint(point); this.title = title
    icon = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color); setSize(28, 28); setStroke(3, Color.WHITE) }
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
}

private fun fitRoute(map: MapView, bounds: MapBounds?) {
    if (bounds == null) return
    if (bounds.south == bounds.north && bounds.west == bounds.east) {
        map.controller.setZoom(16.0); map.controller.setCenter(GeoPoint(bounds.south, bounds.west)); return
    }
    // BoundingBox tolerates routes straddling ±180°. Its fit may be broad, but remains safe.
    map.zoomToBoundingBox(BoundingBox(bounds.north, bounds.east, bounds.south, bounds.west), true, 64)
}
