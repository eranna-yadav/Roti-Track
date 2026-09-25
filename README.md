# Sipwell

_Indian diet planner, calorie tracker and water tracker for Android._

A native Android app written in Kotlin with Jetpack Compose. It has no backend and
needs no account. Everything stays on the phone.

## Features

**Water**: an animated glass that fills toward your daily goal, quick-add buttons
for your usual glass, and ten drinks (water, nimbu pani, coconut water, chaas,
chai, coffee…). Each drink has its own hydration factor, so a cup of chai counts
for 85% of its volume. You also get a daily log and a streak.

**Food**: a calorie ring and protein / carbs / fat bars, shown next to today's
water. Meals are split into Breakfast · Lunch · Evening Snack · Dinner. You can
search 142 Indian foods (idli, poha, dal tadka, rajma, paneer, biryani, chai,
mithai…), filter by category, and see a veg / egg / non-veg mark on each one.
You can log half portions and add your own recipes. Earlier days are one tap
away, with a chart of the last seven days.

**Plan**: a 7-day plan built from 65 real thalis and plates (Idli Sambar, Rajma
Chawal, Bajra Roti & Bharta, Fish Curry Rice…). Portions are scaled to your
calorie target, and the plan follows your food preference and cuisine (North,
South, West, East or all India). **Swap** any meal, or tap **I ate this** to log
the whole meal. There's also a daily nutrition tip.

**History**: water and calorie charts for 7 or 30 days, with averages, goal hits
and your streak.

**Me**: gender, weight, height, age, activity, goal (lose / maintain / gain),
veg / egg / non-veg and cuisine. The calorie target uses Mifflin–St Jeor ×
activity level, −500 kcal to lose weight or +300 kcal to gain. Macros use an
ICMR-style split. BMI uses the Asian-Indian bands. You can override the calorie
and water targets. Water reminders can be set every 30 min – 4 h during your
waking hours. Each reminder has a **+250 ml** button, and reminders stop once
you reach your goal.

**Articles**: 19 short reads on Indian nutrition and hydration.

## Build in Android Studio

1. **File → Open** and pick this folder.
2. Let Gradle sync. Android Studio downloads the SDK 35 platform if it's missing.
3. Pick a device or emulator (Android 8.0 / API 26 or newer) and press **Run ▶**.

To get an APK you can install on a phone, use **Build → Build App Bundle(s) / APK(s)
→ Build APK(s)**. You can also run this from the terminal:

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # minified; signed with the debug key for now
./gradlew test               # unit tests for the planner, calorie maths and storage
```

Before you publish to the Play Store, replace the debug signing config in
`app/build.gradle.kts` with your own keystore.

Toolchain: AGP 8.7, Kotlin 2.1, Compose BOM 2024.12, JDK 17+. When a newer
Android Studio offers to upgrade these, accepting is safe.

## Layout

```
app/src/main/java/com/sipwell/app/
  MainActivity.kt, SipwellApp.kt   entry points, notification permission
  data/        foods, meal templates, drinks, tips, articles, models
  domain/      calorie & macro maths, meal planner, dates
  store/       AppStore (state + persistence), AndroidStorage (JSON file)
  reminders/   WorkManager reminder, "+ glass" notification action
  ui/          Compose screens: Water, Food, Add food, Plan, History, Me,
               onboarding, articles, shared components and theme
app/src/test/  unit tests
```

## Notes

Food values are typical home-style estimates per serving, based on IFCT 2017 and
standard recipes. Restaurant portions and the amount of oil vary a lot.

Sipwell is a wellbeing tool, not a medical device. Its targets are estimates. If
you have diabetes, a thyroid, kidney or heart condition, or are pregnant, check
with a doctor or dietitian.
