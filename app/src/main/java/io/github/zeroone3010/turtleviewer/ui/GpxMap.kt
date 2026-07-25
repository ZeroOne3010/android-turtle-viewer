package io.github.zeroone3010.turtleviewer.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.zeroone3010.turtleviewer.gpx.GpxDisplayItem
import org.json.JSONArray

/**
 * An interactive GPX map backed by Leaflet. The WebView itself has no scrolling container: all
 * panning and pinch gestures are delivered to Leaflet, while the Compose parent clips it to its
 * assigned frame.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GpxMap(items: List<GpxDisplayItem>, modifier: Modifier = Modifier) {
    val page = remember(items) { mapPage(trackLines(items)) }
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                overScrollMode = WebView.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                tag = page
                loadDataWithBaseURL("https://localhost/", page, "text/html", "UTF-8", null)
            }
        },
        // AndroidView calls update on every Compose recomposition. Reloading here would reset
        // Leaflet's camera during a drag or pinch, so only replace the page for a new GPX track.
        update = { view ->
            if (view.tag != page) {
                view.tag = page
                view.loadDataWithBaseURL("https://localhost/", page, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

private fun trackLines(items: List<GpxDisplayItem>): JSONArray = JSONArray().also { lines ->
    var line = JSONArray()
    items.forEach { item ->
        when (item) {
            is GpxDisplayItem.TrackHeading, is GpxDisplayItem.SegmentHeading -> {
                if (line.length() > 0) lines.put(line)
                line = JSONArray()
            }
            is GpxDisplayItem.Point -> {
                val latitude = item.point.latitude
                val longitude = item.point.longitude
                if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                    line.put(JSONArray().put(latitude).put(longitude))
                }
            }
        }
    }
    if (line.length() > 0) lines.put(line)
}

private fun mapPage(lines: JSONArray): String = """
    <!doctype html>
    <html><head>
      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
      <style>html,body,#map{width:100%;height:100%;margin:0;overflow:hidden;background:#e8edf1}</style>
    </head><body><div id="map"></div>
      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
      <script>
        const map=L.map('map',{zoomControl:true}).setView([0,0],2);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
        const lines=$lines, bounds=[];
        lines.forEach(line=>{if(line.length){L.polyline(line,{color:'#6750A4',weight:4}).addTo(map); line.forEach(point=>bounds.push(point));}});
        if(bounds.length===1){map.setView(bounds[0],15);}else if(bounds.length>1){map.fitBounds(bounds,{padding:[24,24]});}
      </script>
    </body></html>
""".trimIndent()
