package com.rotitrack.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Tab glyphs drawn from SVG path data, tinted by the navigation bar. */
object TabIcons {
    private fun icon(name: String, vararg paths: Pair<String, Boolean>): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for ((d, stroke) in paths) {
            val nodes = PathParser().parsePathString(d).toNodes()
            if (stroke) {
                b.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round)
            } else {
                b.addPath(nodes, fill = SolidColor(Color.Black))
            }
        }
        return b.build()
    }

    val Home = icon("home", "M3.5 10.5 12 3.5l8.5 7V20a1 1 0 0 1-1 1H15v-6H9v6H4.5a1 1 0 0 1-1-1Z" to false)
    val Water = icon("water", "M12 3.2s6 6.4 6 10.1a6 6 0 1 1-12 0C6 9.6 12 3.2 12 3.2Z" to false)
    val Food = icon(
        "food",
        "M3 11.5h18a9 9 0 0 1-18 0Z" to false,
        "M8 8.5c0-1.4 1-1.6 1-3M12 8.5c0-1.4 1-1.6 1-3M16 8.5c0-1.4 1-1.6 1-3" to true,
    )
    val Plan = icon(
        "plan",
        "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" to true,
        "M8 3v4M16 3v4M3.5 10h17M8 15l2.5 2.3L16 13" to true,
    )
    val History = icon("history", "M5 20V11M10 20V5M15 20v-7M20 20V8" to true)
    val Me = icon(
        "me",
        "M12 12.2a3.8 3.8 0 1 0 0-7.6 3.8 3.8 0 0 0 0 7.6Z" to false,
        "M4.5 20.5a7.5 7.5 0 0 1 15 0Z" to false,
    )

    val Bell = icon(
        "bell",
        "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.8h-15Z" to true,
        "M10 20.5a2 2 0 0 0 4 0" to true,
    )
    val Speaker = icon(
        "speaker",
        "M4 9.5h3.5L12 5.5v13l-4.5-4H4Z" to true,
        "M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11" to true,
    )
    val Vibrate = icon("vibrate", "M8.5 4.5h7v15h-7Z" to true, "M5 9v6M2.5 10.5v3M19 9v6M21.5 10.5v3" to true)
    /** A camera viewfinder with a scan line, for the food scanner. */
    val Scan = icon(
        "scan",
        "M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2" to true,
        "M7.5 12h9" to true,
    )
    val Pencil = icon("pencil", "M5 19l1-4L15.5 5.5l3 3L9 18Z" to true)
    val Info = icon("info", "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17Z" to true, "M12 11v5M12 7.8v.4" to true)
}
