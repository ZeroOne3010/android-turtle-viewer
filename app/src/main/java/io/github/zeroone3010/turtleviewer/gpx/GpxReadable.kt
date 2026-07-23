package io.github.zeroone3010.turtleviewer.gpx

import java.io.InputStream
import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

data class GpxPoint(val latitude: Double?, val longitude: Double?, val elevation: Double?, val time: Instant?)
data class GpxSegment(val points: List<GpxPoint>)
data class GpxTrack(val segments: List<GpxSegment>)
sealed interface GpxDisplayItem {
    data class TrackHeading(val number: Int) : GpxDisplayItem
    data class SegmentHeading(val number: Int, val pointCount: Int, val displayedPointCount: Int = pointCount) : GpxDisplayItem
    data class Point(val point: GpxPoint, val timestamp: String, val coordinates: String, val speed: String?, val bearing: String?, val isStart: Boolean) : GpxDisplayItem
}

/** Streaming GPX parser and lightweight, preformatted list data. Call this on a background dispatcher. */
object GpxReadableParser {
    fun parse(input: InputStream): List<GpxTrack> {
        val tracks = mutableListOf<GpxTrack>()
        var track: MutableList<GpxSegment>? = null
        var segment: MutableList<GpxPoint>? = null
        var point: MutablePoint? = null
        var field: String? = null
        val text = StringBuilder()

        val reader = SAXParserFactory.newInstance().apply { isNamespaceAware = true }.newSAXParser().xmlReader
        // GPX is local document data. Resolving a referenced DTD can block the file-open flow
        // indefinitely when the provider is offline or the URL is unreachable.
        reader.entityResolver = object : EntityResolver {
            override fun resolveEntity(publicId: String?, systemId: String?): InputSource =
                InputSource(StringReader(""))
        }
        reader.contentHandler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                when (elementName(localName, qName)) {
                    "trk" -> track = mutableListOf()
                    "trkseg" -> if (track != null) segment = mutableListOf()
                    "trkpt" -> if (segment != null) point = MutablePoint(attribute(attributes, "lat")?.toDoubleOrNull(), attribute(attributes, "lon")?.toDoubleOrNull())
                    "ele", "time" -> if (point != null) { field = elementName(localName, qName); text.clear() }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) { if (field != null) text.append(ch, start, length) }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (elementName(localName, qName)) {
                    "ele" -> if (field == "ele") { point?.elevation = text.toString().trim().toDoubleOrNull(); field = null }
                    "time" -> if (field == "time") { point?.time = parseTime(text.toString().trim()); field = null }
                    "trkpt" -> point?.let { segment?.add(GpxPoint(it.latitude, it.longitude, it.elevation, it.time)); point = null }
                    "trkseg" -> segment?.let { track?.add(GpxSegment(it)); segment = null }
                    "trk" -> track?.let { tracks += GpxTrack(it); track = null }
                }
            }
        }
        reader.parse(InputSource(input))
        return tracks
    }

    private data class MutablePoint(val latitude: Double?, val longitude: Double?, var elevation: Double? = null, var time: Instant? = null)
    private fun elementName(localName: String?, qName: String?): String = localName?.takeIf { it.isNotEmpty() } ?: qName?.substringAfter(':') ?: ""
    private fun attribute(attributes: Attributes, name: String): String? = (0 until attributes.length).firstOrNull { elementName(attributes.getLocalName(it), attributes.getQName(it)) == name }?.let { attributes.getValue(it) }
    private fun parseTime(value: String): Instant? = try { Instant.parse(value) } catch (_: Exception) { try { OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant() } catch (_: Exception) { null } }
}

/**
 * Builds a bounded list for Compose. GPS devices can record tens of thousands of points in a
 * short file; eagerly formatting every point makes opening the Readable tab unresponsive.
 */
fun gpxDisplayItems(tracks: List<GpxTrack>, maxPoints: Int = MAX_DISPLAY_POINTS): List<GpxDisplayItem> = buildList {
    val segmentBudgets = displayPointBudgets(tracks.flatMap { it.segments }, maxPoints)
    var segmentBudgetIndex = 0
    tracks.forEachIndexed { trackIndex, track ->
        add(GpxDisplayItem.TrackHeading(trackIndex + 1))
        track.segments.forEachIndexed { segmentIndex, segment ->
            val pointCount = segment.points.size
            val displayedIndices = sampledIndices(pointCount, segmentBudgets[segmentBudgetIndex++])
            add(GpxDisplayItem.SegmentHeading(segmentIndex + 1, pointCount, displayedIndices.size))
            displayedIndices.forEach { index ->
                val point = segment.points[index]
                val previous = segment.points.getOrNull(index - 1)
                val first = index == 0
                val distance = if (first) null else geographicDistanceMeters(previous!!, point)
                val seconds = if (first || previous?.time == null || point.time == null) null else point.time.epochSecond - previous.time.epochSecond + (point.time.nano - previous.time.nano) / 1e9
                val speed = if (distance != null && seconds != null && seconds > 0 && distance > 0.01) "${String.format(java.util.Locale.US, "%.1f", distance / seconds * 3.6)} km/h" else null
                add(GpxDisplayItem.Point(point, point.time?.let { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneOffset.UTC).format(it) } ?: "—", formatCoordinates(point.latitude, point.longitude), speed, if (first) null else formatBearing(initialBearing(previous!!, point)), first))
            }
        }
    }
}

private const val MAX_DISPLAY_POINTS = 2_000

/** Gives every short segment all of its points, then shares the remaining budget among dense ones. */
private fun displayPointBudgets(segments: List<GpxSegment>, maxPoints: Int): List<Int> {
    val budgets = IntArray(segments.size)
    var remainingBudget = minOf(maxPoints.coerceAtLeast(0).toLong(), segments.sumOf { it.points.size.toLong() })
    val active = segments.indices.toMutableList()
    while (active.isNotEmpty() && remainingBudget > 0) {
        val equalShare = remainingBudget / active.size
        val shortSegments = active.filter { segments[it].points.size.toLong() <= equalShare }
        if (shortSegments.isNotEmpty()) {
            shortSegments.forEach { index ->
                budgets[index] = segments[index].points.size
                remainingBudget -= budgets[index]
            }
            active.removeAll(shortSegments.toSet())
        } else {
            var remainingSourcePoints = active.sumOf { segments[it].points.size.toLong() }
            active.forEach { index ->
                val share = ((remainingBudget * segments[index].points.size + remainingSourcePoints - 1) / remainingSourcePoints).toInt()
                budgets[index] = share
                remainingBudget -= share
                remainingSourcePoints -= segments[index].points.size
            }
            break
        }
    }
    return budgets.toList()
}

/** Returns evenly spaced indices, always retaining the first and last point when sampling. */
private fun sampledIndices(pointCount: Int, maxPoints: Int): List<Int> {
    if (maxPoints <= 0) return emptyList()
    if (pointCount <= maxPoints) return (0 until pointCount).toList()
    if (maxPoints == 1) return listOf(0)
    return List(maxPoints) { index -> index * (pointCount - 1) / (maxPoints - 1) }
}

fun formatCoordinates(latitude: Double?, longitude: Double?): String = "${formatCoordinate(latitude, true)}, ${formatCoordinate(longitude, false)}"
private fun formatCoordinate(value: Double?, latitude: Boolean): String {
    if (value == null || !value.isFinite() || (latitude && abs(value) > 90) || (!latitude && abs(value) > 180)) return "—"
    val degrees = floor(abs(value)).toInt(); val minutes = (abs(value) - degrees) * 60
    val hemisphere = if (latitude) if (value < 0) "S" else "N" else if (value < 0) "W" else "E"
    return String.format(java.util.Locale.US, "%d°%.3f′ %s", degrees, minutes, hemisphere)
}
fun geographicDistanceMeters(a: GpxPoint, b: GpxPoint): Double? {
    val la1 = a.latitude ?: return null; val lo1 = a.longitude ?: return null; val la2 = b.latitude ?: return null; val lo2 = b.longitude ?: return null
    if (!valid(la1, lo1) || !valid(la2, lo2)) return null
    val dLat = Math.toRadians(la2 - la1); val dLon = Math.toRadians(lo2 - lo1)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(la1)) * cos(Math.toRadians(la2)) * sin(dLon / 2).pow(2)
    return 6_371_000.0 * 2 * atan2(sqrt(h), sqrt(1 - h))
}
fun initialBearing(a: GpxPoint, b: GpxPoint): Double? {
    val la1 = a.latitude ?: return null; val lo1 = a.longitude ?: return null; val la2 = b.latitude ?: return null; val lo2 = b.longitude ?: return null
    if (!valid(la1, lo1) || !valid(la2, lo2) || geographicDistanceMeters(a, b)!! <= 0.01) return null
    val dLon = Math.toRadians(lo2 - lo1); val p1 = Math.toRadians(la1); val p2 = Math.toRadians(la2)
    return (Math.toDegrees(atan2(sin(dLon) * cos(p2), cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLon))) + 360) % 360
}
fun formatBearing(angle: Double?): String? = angle?.let { angleValue ->
    val rounded = angleValue.roundToInt() % 360
    val labels = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val arrows = arrayOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
    val index = ((rounded + 22) / 45) % 8
    "${arrows[index]} $rounded° ${labels[index]}"
}
private fun valid(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() && abs(lat) <= 90 && abs(lon) <= 180
