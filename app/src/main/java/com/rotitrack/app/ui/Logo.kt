package com.rotitrack.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The Roti Track logo (the roti from the launcher icon), cropped to the roti
 * itself so it can sit on any background. Keep in step with
 * res/drawable/ic_launcher_foreground.xml.
 */
val RotiLogo: ImageVector by lazy {
    val b = ImageVector.Builder("roti", 40.dp, 40.dp, 40f, 40f)
    // The launcher paths are drawn around (54, 54) with radius 20.
    b.addGroup(translationX = -34f, translationY = -34f)
    fun fill(argb: Long, d: String) {
        b.addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color(argb)))
    }
    fill(0xFFE3B267, "M34,54.0A20,20 0,1 1,74,54.0A20,20 0,1 1,34,54.0Z")
    fill(0xFFF7DCA6, "M54,35.5 C64.5,35.2 72.6,43.4 72.4,54.2 C72.2,64.6 64,72.7 53.6,72.5 C43.5,72.3 35.4,64.2 35.6,53.8 C35.8,43.6 43.9,35.8 54,35.5Z")
    fill(0xFFB06A2C, "M44.2,45.6 C45.6,43.9 48.6,44.2 49.3,45.9 C50,47.6 48,49.4 46.2,49.2 C44.3,49 43.2,47 44.2,45.6Z")
    fill(0xFFC98543, "M58.6,44.1 C59.9,43.5 61.6,44.2 61.7,45.4 C61.8,46.6 60.4,47.3 59.2,47 C58.1,46.7 57.6,44.7 58.6,44.1Z")
    fill(0xFFB06A2C, "M62.3,55.2 C64.2,54.5 66.5,55.9 66.1,57.8 C65.7,59.6 63.2,60.2 61.8,59.1 C60.5,58 60.8,55.8 62.3,55.2Z")
    fill(0xFFC98543, "M49.4,60.8 C51.2,59.4 54.9,60 55.4,62.1 C55.9,64.1 53.4,65.6 51.3,65.2 C49.2,64.8 48,62 49.4,60.8Z")
    fill(0xFFB06A2C, "M42.3,55.6 C43.2,55.1 44.4,55.6 44.4,56.5 C44.4,57.4 43.3,57.9 42.5,57.5 C41.8,57.1 41.6,56 42.3,55.6Z")
    fill(0xFFC98543, "M53.3,50.7 C54,50.4 54.9,50.8 54.9,51.5 C54.9,52.2 54,52.5 53.4,52.3 C52.8,52 52.7,51 53.3,50.7Z")
    fill(0xFFB06A2C, "M58.8,66 C59.5,65.7 60.3,66.1 60.3,66.8 C60.3,67.5 59.4,67.8 58.9,67.5 C58.3,67.2 58.2,66.3 58.8,66Z")
    b.clearGroup()
    return@lazy b.build()
}

/** The glossy light-blue water drop shown next to the logo (the classic 💧 look, drawn so every phone matches). */
val WaterDropLogo: ImageVector by lazy {
    val b = ImageVector.Builder("drop", 30.dp, 40.dp, 30f, 40f)
    val drop = "M15,1 C15,1 2,17.5 2,26 A13,13 0 0,0 28,26 C28,17.5 15,1 15,1Z"
    b.addPath(
        PathParser().parsePathString(drop).toNodes(),
        fill = Brush.linearGradient(listOf(Color(0xFFB9ECFF), Color(0xFF55C3F5), Color(0xFF1E88E5)), Offset(8f, 4f), Offset(22f, 39f)),
        stroke = SolidColor(Color(0xFF1976D2)),
        strokeLineWidth = 0.8f,
    )
    // Soft highlight on the left side.
    b.addPath(
        PathParser().parsePathString("M9,20 C9,16.5 11.5,12.5 13,10.5 C12.3,14 11,18 11.2,22.5 C11.3,25 9,24 9,20Z").toNodes(),
        fill = SolidColor(Color(0xCCFFFFFF)),
    )
    b.build()
}

/** Google's "G", for the "Continue with Google" button. */
val GoogleLogo: ImageVector by lazy {
    val b = ImageVector.Builder("google", 20.dp, 20.dp, 48f, 48f)
    fun fill(argb: Long, d: String) {
        b.addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color(argb)))
    }
    fill(0xFFEA4335, "M24,9.5c3.54,0 6.71,1.22 9.21,3.6l6.85,-6.85C35.9,2.38 30.47,0 24,0 14.62,0 6.51,5.38 2.56,13.22l7.98,6.19C12.43,13.72 17.74,9.5 24,9.5z")
    fill(0xFF4285F4, "M46.98,24.55c0,-1.57 -0.15,-3.09 -0.38,-4.55H24v9.02h12.94c-0.58,2.96 -2.26,5.48 -4.78,7.18l7.73,6c4.51,-4.18 7.09,-10.36 7.09,-17.65z")
    fill(0xFFFBBC05, "M10.53,28.59c-0.48,-1.45 -0.76,-2.99 -0.76,-4.59s0.27,-3.14 0.76,-4.59l-7.98,-6.19C0.92,16.46 0,20.12 0,24c0,3.88 0.92,7.54 2.56,10.78l7.97,-6.19z")
    fill(0xFF34A853, "M24,48c6.48,0 11.93,-2.13 15.89,-5.81l-7.73,-6c-2.15,1.45 -4.92,2.3 -8.16,2.3 -6.26,0 -11.57,-4.22 -13.47,-9.91l-7.98,6.19C6.51,42.62 14.62,48 24,48z")
    b.build()
}
