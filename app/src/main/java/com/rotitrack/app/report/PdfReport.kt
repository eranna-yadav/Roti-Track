package com.rotitrack.app.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Report
import com.rotitrack.app.domain.formatServings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Draws a [Report] onto A4 pages with Android's built-in PDF support. */
class PdfReport(private val report: Report) {
    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var y = 0f
    private var pageNo = 0

    private val w = 595f
    private val h = 842f
    private val margin = 40f
    private val brand = Color.rgb(27, 79, 240)
    private val saffron = Color.rgb(255, 138, 31)
    private val ink = Color.rgb(11, 18, 32)
    private val muted = Color.rgb(132, 148, 174)
    private val line = Color.rgb(228, 234, 246)

    private fun paint(size: Float, color: Int = ink, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun newPage() {
        finishPage()
        pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(w.toInt(), h.toInt(), pageNo).create())
        canvas = page!!.canvas
        y = margin
    }

    private fun finishPage() {
        page?.let {
            canvas.drawText("Roti Track · page $pageNo", margin, h - 20f, paint(8f, muted))
            canvas.drawText("Estimates for wellbeing, not medical advice.", w - margin - 170f, h - 20f, paint(8f, muted))
            doc.finishPage(it)
        }
        page = null
    }

    private fun ensure(space: Float) {
        if (y + space > h - 50f) newPage()
    }

    private fun text(s: String, size: Float = 10f, color: Int = ink, bold: Boolean = false, x: Float = margin, gap: Float = 4f) {
        ensure(size + gap)
        y += size
        canvas.drawText(s, x, y, paint(size, color, bold))
        y += gap
    }

    private fun section(title: String) {
        ensure(40f)
        y += 12f
        text(title, 14f, brand, bold = true, gap = 6f)
        canvas.drawLine(margin, y, w - margin, y, Paint().apply { color = line; strokeWidth = 1f })
        y += 8f
    }

    /** A simple table; [widths] are fractions of the content width. */
    private fun table(headers: List<String>, rows: List<List<String>>, widths: List<Float>) {
        val content = w - 2 * margin
        fun row(cells: List<String>, bold: Boolean, color: Int) {
            ensure(16f)
            var x = margin
            val p = paint(9f, color, bold)
            cells.forEachIndexed { i, c ->
                val maxW = widths[i] * content - 4f
                var t = c
                while (t.isNotEmpty() && p.measureText(t) > maxW) t = t.dropLast(1)
                if (t != c && t.length > 1) t = t.dropLast(1) + "…"
                canvas.drawText(t, x, y + 10f, p)
                x += widths[i] * content
            }
            y += 15f
        }
        row(headers, true, muted)
        rows.forEach { row(it, false, ink) }
        if (rows.isEmpty()) text("Nothing logged in this period.", 9f, muted)
    }

    private fun label(day: String): String = Days.parse(day).let { "${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}" }

    fun render(): PdfDocument {
        newPage()
        // Header band
        canvas.drawRect(0f, 0f, w, 90f, Paint().apply { color = brand })
        canvas.drawText("Roti Track", margin, 42f, paint(24f, Color.WHITE, bold = true))
        canvas.drawText("PDF Summary Report", margin, 64f, paint(12f, Color.WHITE))
        val gen = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("${report.name} · ${label(report.from)} – ${label(report.to)} · generated $gen", margin, 80f, paint(9f, Color.WHITE))
        y = 110f

        report.profileLines.forEach { text(it, 10f, ink) }

        section("Calorie & macros breakdown")
        val tiles = listOf(
            "Avg calories" to "${report.avgKcal} kcal",
            "Avg protein" to "${report.avgProtein} g",
            "Avg carbs" to "${report.avgCarbs} g",
            "Avg fat" to "${report.avgFat} g",
            "Days logged" to "${report.daysLogged}",
            "Within goal" to "${report.daysWithinGoal}",
        )
        ensure(50f)
        val tileW = (w - 2 * margin) / tiles.size
        tiles.forEachIndexed { i, (k, v) ->
            val x = margin + i * tileW
            canvas.drawRoundRect(RectF(x + 2, y, x + tileW - 2, y + 44f), 8f, 8f, Paint().apply { color = Color.rgb(241, 244, 251) })
            canvas.drawText(v, x + 8, y + 20f, paint(12f, ink, bold = true))
            canvas.drawText(k, x + 8, y + 36f, paint(8f, muted))
        }
        y += 56f
        chart()
        table(
            listOf("Date", "Eaten", "Goal", "Protein", "Carbs", "Fat", "Burned", "Water"),
            report.days.filter { it.kcal > 0 || it.burned > 0 || it.waterMl > 0 }.map {
                listOf(label(it.day), "${it.kcal}", "${it.goal}", "${it.protein} g", "${it.carbs} g", "${it.fat} g", "${it.burned}", "${it.waterMl} ml")
            },
            listOf(0.14f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.14f),
        )

        section("Weight progress")
        if (report.weeklyWeights.isEmpty()) {
            text("No weight logged in this period. Log it in Profile → Personal details.", 9f, muted)
        } else {
            val first = report.weeklyWeights.first().second
            table(
                listOf("Week of", "Average weight", "Change"),
                report.weeklyWeights.map { (week, kg) ->
                    val d = ((kg - first) * 10).toInt() / 10.0
                    listOf(label(week), "$kg kg", if (d == 0.0) "–" else (if (d > 0) "+" else "") + "$d kg")
                },
                listOf(0.34f, 0.33f, 0.33f),
            )
        }

        section("Exercise history")
        table(
            listOf("Date", "Activity", "Minutes", "Burned"),
            report.exercises.map { listOf(label(it.day), it.name, "${it.minutes}", "${it.kcal} kcal") },
            listOf(0.2f, 0.4f, 0.2f, 0.2f),
        )
        if (report.exercises.isNotEmpty()) text("Total burned: ${report.totalBurned} kcal", 9f, muted)

        section("Meal history")
        table(
            listOf("Date", "Meal", "Food", "Amount", "kcal", "P / C / F"),
            report.meals.map {
                listOf(
                    label(it.day), it.slot.short, it.name, "${formatServings(it.servings)} × ${it.serving}", "${it.kcal}",
                    "${it.protein.toInt()} / ${it.carbs.toInt()} / ${it.fat.toInt()}",
                )
            },
            listOf(0.12f, 0.12f, 0.28f, 0.24f, 0.09f, 0.15f),
        )

        finishPage()
        return doc
    }

    /** Daily calories as bars with the goal as a dashed line. */
    private fun chart() {
        val days = report.days
        if (days.none { it.kcal > 0 }) return
        ensure(150f)
        val top = y
        val chartH = 110f
        val left = margin + 24f
        val right = w - margin
        val max = maxOf(days.maxOf { it.kcal }, days.maxOf { it.goal }) * 1.15f
        val slot = (right - left) / days.size
        val barW = (slot * 0.6f).coerceAtMost(14f)
        days.forEachIndexed { i, d ->
            if (d.kcal <= 0) return@forEachIndexed
            val bh = d.kcal / max * chartH
            val x = left + i * slot + (slot - barW) / 2
            canvas.drawRect(x, top + chartH - bh, x + barW, top + chartH, Paint().apply { color = if (d.kcal > d.goal) saffron else Color.rgb(31, 157, 85) })
        }
        val goalY = top + chartH - days.last().goal / max * chartH
        canvas.drawPath(Path().apply { moveTo(left, goalY); lineTo(right, goalY) }, Paint().apply {
            color = muted; style = Paint.Style.STROKE; strokeWidth = 1f; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        })
        canvas.drawText("${max.toInt()}", margin, top + 8f, paint(7f, muted))
        canvas.drawText("0", margin, top + chartH, paint(7f, muted))
        canvas.drawText(label(days.first().day), left, top + chartH + 12f, paint(7f, muted))
        canvas.drawText(label(days.last().day), right - 30f, top + chartH + 12f, paint(7f, muted))
        y = top + chartH + 24f
    }

    companion object {
        /** Writes the PDF into the app cache and returns the file, ready to share. */
        fun write(context: Context, report: Report): File {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "RotiTrack-report-${report.to}.pdf")
            val doc = PdfReport(report).render()
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
            return file
        }
    }
}
