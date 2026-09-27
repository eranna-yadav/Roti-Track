package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.data.ARTICLES
import com.rotitrack.app.data.articleById

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 34.sp, color = Palette.ink, modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp))
        Text(title, style = Type.h2)
    }
}

@Composable
fun ArticlesScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackBar("Health articles", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(ARTICLES) { cat ->
                Text(cat.title, style = Type.h2, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cat.articles) { a ->
                        Column(
                            Modifier.width(160.dp).height(220.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color(cat.tint)).clickable { onOpen(a.id) },
                        ) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(a.emoji, fontSize = 56.sp)
                            }
                            Column(Modifier.fillMaxWidth().background(Palette.card.copy(alpha = 0.6f)).padding(12.dp)) {
                                Text(a.title, color = Color(cat.fg), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 3)
                                Text("${a.minutes} min read", style = Type.tiny.copy(color = Color(cat.fg)), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(id: String, onBack: () -> Unit) {
    val a = articleById(id) ?: return onBack()
    Column(Modifier.fillMaxSize()) {
        BackBar("", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(a.emoji, fontSize = 56.sp)
            Text(a.title, style = Type.h2.copy(fontSize = 26.sp), modifier = Modifier.padding(top = 12.dp))
            Text(a.summary, style = Type.body.copy(color = Palette.muted), modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
            a.body.forEach { para ->
                Text(para, style = Type.body.copy(color = Palette.inkSoft, lineHeight = 24.sp, fontSize = 16.sp), modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
