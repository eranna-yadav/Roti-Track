package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A pushed screen: back arrow, title, and a scrolling body. */
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Palette.background)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Palette.chip).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, color = Palette.ink) }
            Spacer(Modifier.width(12.dp))
            Text(title, style = Type.h2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String) =
    Text(text, style = Type.title.copy(color = Palette.muted, fontSize = 18.sp), modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp))

/** White rounded group of rows, as on the settings screens. */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.card).padding(horizontal = 16.dp),
        content = content,
    )
}

@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    last: Boolean = false,
    titleColor: Color = Palette.ink,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 20.sp, modifier = Modifier.width(34.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Type.title.copy(color = titleColor))
                subtitle?.let { Text(it, style = Type.small) }
            }
            value?.let { Text(it, style = Type.body.copy(color = Palette.inkSoft), modifier = Modifier.padding(start = 8.dp)) }
            when {
                trailing != null -> trailing()
                onClick != null -> Text("›", fontSize = 24.sp, color = Palette.muted, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (!last) Divider()
    }
}

@Composable
fun Banner(text: String, action: String, color: Color = Palette.card, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(color)
            .clickable(onClick = onAction).padding(16.dp),
    ) {
        Text(text, style = Type.body)
        Text("$action ›", style = Type.title.copy(fontSize = 16.sp), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun Heading(text: String, sub: String? = null) {
    Text(text, style = Type.screenTitle, modifier = Modifier.padding(top = 4.dp))
    sub?.let { Text(it, style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
    Spacer(Modifier.height(8.dp))
}
