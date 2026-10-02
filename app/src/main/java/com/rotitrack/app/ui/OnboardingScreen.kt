package com.rotitrack.app.ui

import com.rotitrack.app.i18n.t
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.domain.Nutrition
import com.rotitrack.app.store.AppStore

@Composable
fun OnboardingScreen(store: AppStore, platform: Platform) {
    var started by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(store.profile) }

    if (!started) {
        Column(
            Modifier.fillMaxSize().background(Palette.brand).safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(RotiLogo, contentDescription = t("Roti Track logo"), modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(10.dp))
                Image(WaterDropLogo, contentDescription = null, modifier = Modifier.size(width = 54.dp, height = 72.dp))
            }
            Text("ROTI TRACK", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 12.dp))
            Text(
                t("Indian diet planner, calorie & water tracker"),
                style = Type.body.copy(color = Color.White.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            PillButton(t("Get started"), { started = true }, Modifier.fillMaxWidth(), color = Color.White, textColor = Palette.brand, height = 60.dp)
        }
        return
    }

    val kcal = Nutrition.recommendedCalories(draft)
    val water = Nutrition.recommendedWaterMl(draft.weightKg, draft.gender, draft.activity)
    Box(Modifier.fillMaxSize().background(Palette.background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 100.dp)) {
            ScreenHeader(t("ABOUT YOU"))
            Text(t("We use this to set your calorie and water targets and build your meal plan."), style = Type.body.copy(color = Palette.inkSoft))
            ProfileForm(draft, { draft = it })
            Spacer(Modifier.height(16.dp))
            AppCard(color = Palette.saffronSoft) {
                Text(t("Your starting targets"), style = Type.small.copy(color = Palette.inkSoft))
                Text(t("{0} kcal · {1} water", kcal, liters(water)), style = Type.h2)
                Text(t("You can change these any time on the Profile tab."), style = Type.small)
            }
        }
        PillButton(
            t("Start planning"),
            {
                store.updateProfile { draft.copy(onboarded = true) }
                platform.scheduleReminders(store.profile)
            },
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
