package com.deepconnect.vpn.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/*
 * DeepConnect icon set — hand-built, thin-line (1.7dp at 24dp), rounded caps/joins.
 *
 * Material's icon artifacts are deprecated/frozen, so every glyph the app needs is drawn
 * here as a vector path in a 24x24 viewport. [androidx.compose.material3.Icon] tints the
 * resulting ImageVector, so the paths are always committed in black and coloured by the
 * caller.
 */
object AppIcons {

    val Shield: ImageVector = lineIcon("Shield") { shield() }

    val ShieldCheck: ImageVector = lineIcon("ShieldCheck") {
        shield()
        moveTo(8.5f, 11.9f)
        lineTo(11.0f, 14.4f)
        lineTo(15.7f, 9.4f)
    }

    val ShieldAlert: ImageVector = icon("ShieldAlert") {
        strokePath {
            shield()
            moveTo(12f, 8.4f)
            lineTo(12f, 13.2f)
        }
        fillPath { dot(12f, 16.1f, 1.05f) }
    }

    val Power: ImageVector = lineIcon("Power") {
        moveTo(12f, 2.9f)
        lineTo(12f, 11.9f)
        moveTo(14.55f, 5.65f)
        arcTo(7.4f, 7.4f, 0f, true, true, 9.45f, 5.65f)
    }

    val Tune: ImageVector = lineIcon("Tune") {
        // Three sliders: a track with a hollow knob on each one.
        moveTo(3.6f, 6.6f)
        lineTo(6.8f, 6.6f)
        moveTo(11.2f, 6.6f)
        lineTo(20.4f, 6.6f)
        circleStroke(9f, 6.6f, 2.2f)
        moveTo(3.6f, 12f)
        lineTo(13.3f, 12f)
        moveTo(17.7f, 12f)
        lineTo(20.4f, 12f)
        circleStroke(15.5f, 12f, 2.2f)
        moveTo(3.6f, 17.4f)
        lineTo(5.3f, 17.4f)
        moveTo(9.7f, 17.4f)
        lineTo(20.4f, 17.4f)
        circleStroke(7.5f, 17.4f, 2.2f)
    }

    val Terminal: ImageVector = lineIcon("Terminal") {
        roundedRectStroke(3.4f, 4.6f, 20.6f, 19.4f, 3.2f)
        moveTo(7.4f, 9.4f)
        lineTo(10.2f, 12.1f)
        lineTo(7.4f, 14.8f)
        moveTo(12.4f, 15.1f)
        lineTo(16.6f, 15.1f)
    }

    val Home: ImageVector = lineIcon("Home") {
        moveTo(3.6f, 10.7f)
        lineTo(12f, 3.6f)
        lineTo(20.4f, 10.7f)
        moveTo(5.8f, 9.1f)
        lineTo(5.8f, 19.6f)
        moveTo(18.2f, 9.1f)
        lineTo(18.2f, 19.6f)
        moveTo(5.8f, 19.6f)
        lineTo(18.2f, 19.6f)
        moveTo(9.8f, 19.6f)
        lineTo(9.8f, 14.9f)
        lineTo(14.2f, 14.9f)
        lineTo(14.2f, 19.6f)
    }

    val Plus: ImageVector = lineIcon("Plus") {
        moveTo(12f, 4.8f)
        lineTo(12f, 19.2f)
        moveTo(4.8f, 12f)
        lineTo(19.2f, 12f)
    }

    /** Filled star, used for the default-profile marker. */
    val Star: ImageVector = icon("Star") { fillPath { starPath() } }

    val StarOutline: ImageVector = lineIcon("StarOutline") { starPath() }

    val MoreVert: ImageVector = icon("MoreVert") {
        fillPath {
            dot(12f, 5.2f, 1.55f)
            dot(12f, 12f, 1.55f)
            dot(12f, 18.8f, 1.55f)
        }
    }

    val Pencil: ImageVector = lineIcon("Pencil") {
        moveTo(4.6f, 19.4f)
        lineTo(5.5f, 15.1f)
        lineTo(15.9f, 4.7f)
        curveTo(16.7f, 3.9f, 17.9f, 3.9f, 18.7f, 4.7f)
        lineTo(19.3f, 5.3f)
        curveTo(20.1f, 6.1f, 20.1f, 7.3f, 19.3f, 8.1f)
        lineTo(8.9f, 18.5f)
        close()
        moveTo(13.4f, 7.2f)
        lineTo(16.8f, 10.6f)
    }

    val Trash: ImageVector = lineIcon("Trash") {
        moveTo(4.4f, 7f)
        lineTo(19.6f, 7f)
        moveTo(9.4f, 7f)
        lineTo(9.4f, 4.8f)
        lineTo(14.6f, 4.8f)
        lineTo(14.6f, 7f)
        moveTo(6.5f, 7.3f)
        lineTo(7.4f, 20.1f)
        lineTo(16.6f, 20.1f)
        lineTo(17.5f, 7.3f)
        moveTo(10.4f, 10.9f)
        lineTo(10.4f, 16.5f)
        moveTo(13.6f, 10.9f)
        lineTo(13.6f, 16.5f)
    }

    val ArrowDown: ImageVector = lineIcon("ArrowDown") {
        moveTo(12f, 4.4f)
        lineTo(12f, 19.2f)
        moveTo(6.6f, 13.8f)
        lineTo(12f, 19.2f)
        lineTo(17.4f, 13.8f)
    }

    val ArrowUp: ImageVector = lineIcon("ArrowUp") {
        moveTo(12f, 19.6f)
        lineTo(12f, 4.8f)
        moveTo(6.6f, 10.2f)
        lineTo(12f, 4.8f)
        lineTo(17.4f, 10.2f)
    }

    val Download: ImageVector = lineIcon("Download") {
        moveTo(12f, 3.8f)
        lineTo(12f, 15.2f)
        moveTo(7.2f, 10.4f)
        lineTo(12f, 15.2f)
        lineTo(16.8f, 10.4f)
        moveTo(4.5f, 19.6f)
        lineTo(19.5f, 19.6f)
    }

    val Upload: ImageVector = lineIcon("Upload") {
        moveTo(12f, 20.2f)
        lineTo(12f, 8.8f)
        moveTo(7.2f, 13.6f)
        lineTo(12f, 8.8f)
        lineTo(16.8f, 13.6f)
        moveTo(4.5f, 4.4f)
        lineTo(19.5f, 4.4f)
    }

    val Check: ImageVector = lineIcon("Check") {
        moveTo(4.8f, 12.6f)
        lineTo(9.6f, 17.4f)
        lineTo(19.2f, 7.8f)
    }

    val Close: ImageVector = lineIcon("Close") {
        moveTo(5.6f, 5.6f)
        lineTo(18.4f, 18.4f)
        moveTo(18.4f, 5.6f)
        lineTo(5.6f, 18.4f)
    }

    val ChevronLeft: ImageVector = lineIcon("ChevronLeft") {
        moveTo(14.6f, 5.4f)
        lineTo(8.2f, 12f)
        lineTo(14.6f, 18.6f)
    }

    val ChevronRight: ImageVector = lineIcon("ChevronRight") {
        moveTo(9.4f, 5.4f)
        lineTo(15.8f, 12f)
        lineTo(9.4f, 18.6f)
    }

    val ChevronDown: ImageVector = lineIcon("ChevronDown") {
        moveTo(6.2f, 9.4f)
        lineTo(12f, 15.2f)
        lineTo(17.8f, 9.4f)
    }

    val ChevronUp: ImageVector = lineIcon("ChevronUp") {
        moveTo(6.2f, 14.6f)
        lineTo(12f, 8.8f)
        lineTo(17.8f, 14.6f)
    }

    val Eye: ImageVector = icon("Eye") {
        strokePath {
            eye()
            circleStroke(12f, 12f, 3f)
        }
    }

    val EyeOff: ImageVector = lineIcon("EyeOff") {
        eye()
        moveTo(4f, 3.8f)
        lineTo(20f, 20.2f)
    }

    val Copy: ImageVector = lineIcon("Copy") {
        moveTo(9.6f, 4.4f)
        lineTo(19.6f, 4.4f)
        lineTo(19.6f, 14.4f)
        lineTo(9.6f, 14.4f)
        close()
        moveTo(9.6f, 9.6f)
        lineTo(4.4f, 9.6f)
        lineTo(4.4f, 19.6f)
        lineTo(14.4f, 19.6f)
        lineTo(14.4f, 14.4f)
    }

    val Share: ImageVector = lineIcon("Share") {
        moveTo(12f, 15.4f)
        lineTo(12f, 3.6f)
        moveTo(7.4f, 7.8f)
        lineTo(12f, 3.6f)
        lineTo(16.6f, 7.8f)
        moveTo(5.6f, 12.4f)
        lineTo(5.6f, 19.6f)
        lineTo(18.4f, 19.6f)
        lineTo(18.4f, 12.4f)
    }

    val Search: ImageVector = lineIcon("Search") {
        circleStroke(10.8f, 10.8f, 5.9f)
        moveTo(15.2f, 15.2f)
        lineTo(20.2f, 20.2f)
    }

    val Refresh: ImageVector = lineIcon("Refresh") {
        // Two curved arrows chasing each other around a circle.
        moveTo(4.2f, 12.6f)
        curveTo(4.6f, 8.3f, 8.2f, 5.2f, 12.4f, 5.2f)
        curveTo(15.1f, 5.2f, 17.5f, 6.5f, 19.1f, 8.6f)
        moveTo(19.1f, 4.9f)
        lineTo(19.1f, 8.9f)
        lineTo(15.1f, 8.9f)
        moveTo(19.8f, 11.4f)
        curveTo(19.4f, 15.7f, 15.8f, 18.8f, 11.6f, 18.8f)
        curveTo(8.9f, 18.8f, 6.5f, 17.5f, 4.9f, 15.4f)
        moveTo(4.9f, 19.1f)
        lineTo(4.9f, 15.1f)
        lineTo(8.9f, 15.1f)
    }

    val Fingerprint: ImageVector = lineIcon("Fingerprint") {
        moveTo(6.0f, 18.2f)
        curveTo(4.7f, 16.6f, 4.0f, 14.6f, 4.0f, 12.2f)
        curveTo(4.0f, 7.8f, 7.6f, 4.2f, 12.0f, 4.2f)
        curveTo(16.4f, 4.2f, 20.0f, 7.8f, 20.0f, 12.2f)
        moveTo(17.2f, 18.9f)
        curveTo(18.3f, 16.9f, 18.9f, 14.6f, 18.9f, 12.2f)
        curveTo(18.9f, 8.4f, 15.8f, 5.3f, 12.0f, 5.3f)
        moveTo(9.0f, 18.5f)
        curveTo(7.6f, 17.1f, 7.1f, 15.3f, 7.1f, 12.2f)
        curveTo(7.1f, 9.5f, 9.3f, 7.3f, 12.0f, 7.3f)
        curveTo(14.7f, 7.3f, 16.9f, 9.5f, 16.9f, 12.2f)
        curveTo(16.9f, 14.4f, 16.4f, 16.5f, 15.4f, 18.3f)
    }

    val Lock: ImageVector = lineIcon("Lock") {
        moveTo(6.2f, 11.2f)
        lineTo(17.8f, 11.2f)
        quadTo(19.2f, 11.2f, 19.2f, 12.6f)
        lineTo(19.2f, 18.6f)
        quadTo(19.2f, 20f, 17.8f, 20f)
        lineTo(6.2f, 20f)
        quadTo(4.8f, 20f, 4.8f, 18.6f)
        lineTo(4.8f, 12.6f)
        quadTo(4.8f, 11.2f, 6.2f, 11.2f)
        close()
        moveTo(8.4f, 11.0f)
        lineTo(8.4f, 7.9f)
        curveTo(8.4f, 5.6f, 10.0f, 3.8f, 12f, 3.8f)
        curveTo(14.0f, 3.8f, 15.6f, 5.6f, 15.6f, 7.9f)
        lineTo(15.6f, 11.0f)
        moveTo(12f, 14.4f)
        lineTo(12f, 16.7f)
    }

    val Key: ImageVector = lineIcon("Key") {
        circleStroke(8.6f, 8.6f, 3.4f)
        moveTo(11.0f, 11.0f)
        lineTo(19.4f, 19.4f)
        moveTo(16.2f, 16.2f)
        lineTo(18.0f, 14.4f)
        moveTo(14.2f, 14.2f)
        lineTo(16.0f, 12.4f)
    }

    val Info: ImageVector = icon("Info") {
        strokePath {
            circleStroke(12f, 12f, 8.4f)
            moveTo(12f, 11.2f)
            lineTo(12f, 16.5f)
        }
        fillPath { dot(12f, 7.9f, 1.15f) }
    }

    val AlertTriangle: ImageVector = icon("AlertTriangle") {
        strokePath {
            moveTo(12f, 3.4f)
            lineTo(21.1f, 19.6f)
            lineTo(2.9f, 19.6f)
            close()
            moveTo(12f, 9.4f)
            lineTo(12f, 13.9f)
        }
        fillPath { dot(12f, 16.8f, 1.05f) }
    }

    /** Half-filled circle; the appearance/theme marker. */
    val Contrast: ImageVector = icon("Contrast") {
        strokePath { circleStroke(12f, 12f, 8.4f) }
        fillPath {
            moveTo(12f, 3.6f)
            arcTo(8.4f, 8.4f, 0f, true, true, 12f, 20.4f)
            close()
        }
    }

    val Bolt: ImageVector = icon("Bolt") {
        fillPath {
            moveTo(13.2f, 2.6f)
            lineTo(5.2f, 13.6f)
            lineTo(10.9f, 13.6f)
            lineTo(10.4f, 21.4f)
            lineTo(18.8f, 10.2f)
            lineTo(12.9f, 10.2f)
            close()
        }
    }

    val Clock: ImageVector = lineIcon("Clock") {
        circleStroke(12f, 12f, 8.4f)
        moveTo(12f, 7.2f)
        lineTo(12f, 12.2f)
        lineTo(15.6f, 14.1f)
    }

    val Globe: ImageVector = lineIcon("Globe") {
        circleStroke(12f, 12f, 8.4f)
        moveTo(3.6f, 12f)
        lineTo(20.4f, 12f)
        moveTo(12f, 3.6f)
        arcTo(4.1f, 8.4f, 0f, true, true, 12f, 20.4f)
        arcTo(4.1f, 8.4f, 0f, true, true, 12f, 3.6f)
    }

    val Server: ImageVector = icon("Server") {
        strokePath {
            roundedRectStroke(4.2f, 4.4f, 19.8f, 10.2f, 2f)
            roundedRectStroke(4.2f, 13.8f, 19.8f, 19.6f, 2f)
        }
        fillPath {
            dot(7.6f, 7.3f, 1.05f)
            dot(7.6f, 16.7f, 1.05f)
        }
    }

    val ExternalLink: ImageVector = lineIcon("ExternalLink") {
        moveTo(13.6f, 3.8f)
        lineTo(20.2f, 3.8f)
        lineTo(20.2f, 10.4f)
        moveTo(20.2f, 3.8f)
        lineTo(11.6f, 12.4f)
        moveTo(9.6f, 4.6f)
        lineTo(5.4f, 4.6f)
        quadTo(4.2f, 4.6f, 4.2f, 5.8f)
        lineTo(4.2f, 18.6f)
        quadTo(4.2f, 19.8f, 5.4f, 19.8f)
        lineTo(18.2f, 19.8f)
        quadTo(19.4f, 19.8f, 19.4f, 18.6f)
        lineTo(19.4f, 14.4f)
    }

    /** Full disk used by dot indicators drawn through the icon API. */
    val Dot: ImageVector = icon("Dot") { fillPath { dot(12f, 12f, 8.6f) } }
}

/* -------------------------------------------------------------------------- */
/* builders                                                                    */
/* -------------------------------------------------------------------------- */

private const val STROKE = 1.7f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun lineIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    icon(name) { strokePath(block = block) }

private fun ImageVector.Builder.strokePath(
    width: Float = STROKE,
    block: PathBuilder.() -> Unit,
) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = block,
    )
}

private fun ImageVector.Builder.fillPath(block: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero,
        pathBuilder = block,
    )
}

/* -------------------------------------------------------------------------- */
/* reusable path fragments                                                    */
/* -------------------------------------------------------------------------- */

private fun PathBuilder.shield() {
    moveTo(12f, 2.7f)
    lineTo(19.6f, 5.7f)
    lineTo(19.6f, 11.4f)
    curveTo(19.6f, 16.4f, 16.3f, 20.4f, 12f, 21.7f)
    curveTo(7.7f, 20.4f, 4.4f, 16.4f, 4.4f, 11.4f)
    lineTo(4.4f, 5.7f)
    close()
}

/** Five-point star (filled or outlined depending on the path style used). */
private fun PathBuilder.starPath() {
    val outer = 8.5f
    val inner = 3.7f
    val cx = 12f
    val cy = 12.2f
    val angles = floatArrayOf(
        -90f, -54f, -18f, 18f, 54f, 90f, 126f, 162f, 198f, 234f,
    )
    angles.forEachIndexed { index, degrees ->
        val radius = if (index % 2 == 0) outer else inner
        val radians = Math.toRadians(degrees.toDouble())
        val x = cx + (radius * Math.cos(radians)).toFloat()
        val y = cy + (radius * Math.sin(radians)).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Hollow circle via two elliptical arcs. */
private fun PathBuilder.circleStroke(cx: Float, cy: Float, r: Float) {
    moveTo(cx, cy - r)
    arcTo(r, r, 0f, true, true, cx, cy + r)
    arcTo(r, r, 0f, true, true, cx, cy - r)
}

/** Rounded rectangle outline built from lines and quadratic corners. */
private fun PathBuilder.roundedRectStroke(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    quadTo(right, top, right, top + radius)
    lineTo(right, bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    lineTo(left + radius, bottom)
    quadTo(left, bottom, left, bottom - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}

/** Filled dot (used for punctuation inside icons). Only visible in filled paths. */
private fun PathBuilder.dot(cx: Float, cy: Float, r: Float) {
    moveTo(cx, cy - r)
    arcTo(r, r, 0f, true, true, cx, cy + r)
    arcTo(r, r, 0f, true, true, cx, cy - r)
    close()
}

private fun PathBuilder.eye() {
    moveTo(2.8f, 12f)
    curveTo(5.2f, 7.6f, 8.4f, 5.2f, 12f, 5.2f)
    curveTo(15.6f, 5.2f, 18.8f, 7.6f, 21.2f, 12f)
    curveTo(18.8f, 16.4f, 15.6f, 18.8f, 12f, 18.8f)
    curveTo(8.4f, 18.8f, 5.2f, 16.4f, 2.8f, 12f)
    close()
}
