package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.GuideFeature
import com.rotitrack.app.data.USER_GUIDE
import com.rotitrack.app.data.guidePage
import com.rotitrack.app.i18n.t

/** The user guide's contents: one card per page of the app. */
@Composable
fun GuideScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    SubScreen(t("User guide"), onBack) {
        Text(
            t("Every feature of Roti Track, page by page: what it does, how to use it, and how it helps you."),
            style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(bottom = 12.dp),
        )
        USER_GUIDE.forEach { page ->
            AppCard(onClick = { onOpen(page.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(page.emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t(page.title), style = Type.h2)
                        Text(t(page.intro), style = Type.small)
                        Text(
                            t("{0} features", page.features.size),
                            style = Type.tiny.copy(color = Palette.brand), modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text("›", fontSize = 28.sp, color = Palette.muted)
                }
            }
        }
    }
}

/** One page of the app, with each of its features explained. */
@Composable
fun GuidePageScreen(id: String, onBack: () -> Unit) {
    val page = guidePage(id) ?: return onBack()
    SubScreen("${page.emoji} ${t(page.title)}", onBack) {
        Text(t(page.intro), style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(bottom = 12.dp))
        page.features.forEach { FeatureCard(it) }
    }
}

@Composable
private fun FeatureCard(f: GuideFeature) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Text(t(f.title), style = Type.h2.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
            if (f.pro) Badge("PRO", Palette.saffron)
        }
        Text(t(f.what), style = Type.body, modifier = Modifier.padding(top = 8.dp))

        Heading(t("How to use it"))
        f.how.forEachIndexed { i, step ->
            Row(Modifier.padding(bottom = 6.dp)) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(Palette.brand),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Text(t(step), style = Type.body, modifier = Modifier.weight(1f))
            }
        }

        Heading(t("Why it helps"))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.leaf.copy(alpha = 0.1f)).padding(12.dp),
        ) { Text("✨ " + t(f.benefit), style = Type.body.copy(color = Palette.ink)) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Heading(text: String) =
    Text(text.uppercase(), style = Type.tiny.copy(color = Palette.muted, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
