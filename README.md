# Roti Track

_Indian diet planner, calorie tracker and water tracker for Android._

A native Android app written in Kotlin with Jetpack Compose. Accounts and the
admin dashboard use Firebase, and Pro subscriptions use Google Play Billing.
Food and water logs stay on the phone.

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

**Home (user dashboard)**: a greeting, today's calories, water and protein, your
streak, BMI and goal, the next meal from your plan, and 7- or 30-day trends for
water and calories.

**Account**: sign up, sign in, forgot password and sign out, with email and a
password. Each account keeps its own logs on the phone.

**Pro**: ₹259/month or ₹990/year (68% cheaper), sold through Google Play.
Pro unlocks the full 7-day plan, meal swaps, 30-day trends and your own recipes.
Everything else is free.

**Admin dashboard** (admins only, from the Me tab):
- Totals: users, Pro users, monthly and yearly subscribers, estimated monthly
  revenue in ₹, new sign-ups (7 days), active users (24 h), and a 14-day sign-up
  chart.
- A searchable, filterable user list. For each user you can give free Pro or
  block the account.

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

## Accounts, Pro and admin: setup

The app runs in one of two modes:

| | Without setup | With Firebase + Play Console |
|---|---|---|
| Accounts | Stored on this phone. The first account is the admin | Firebase Auth (email/password) |
| Admin dashboard | Shows accounts on this phone | Shows every user |
| Pro (debug build) | Demo purchase with no charge | Demo purchase with no charge |
| Pro (release build) | Google Play Billing | Google Play Billing |

### 1. Firebase (accounts + admin)

1. At <https://console.firebase.google.com>, create a project and add an Android
   app with package `com.rotitrack.app`.
2. Download `google-services.json` into the `app/` folder, then sync Gradle.
3. Go to **Authentication → Sign-in method** and enable **Email/Password**.
4. Go to **Firestore Database** and create a database. Under **Rules**, paste
   the contents of `firestore.rules` and publish.
5. Sign up in the app. Then, in Firestore → `users` → your document, add a
   field `role` with the value `admin`. Reopen the app and the Admin dashboard
   appears under Me.

### 2. Google Play (Pro subscriptions)

1. Upload a release build (`./gradlew bundleRelease`) to a testing track in
   Play Console. Billing only works for apps installed from Play.
2. Go to **Monetize → Subscriptions** and create two subscriptions, each with an
   auto-renewing base plan:
   - `rotitrack_pro_monthly`: ₹259, renews every month
   - `rotitrack_pro_yearly`: ₹990, renews every year
3. Add your Google account as a license tester so test purchases are free.

Pro status is checked on the device. Before a large launch, verify purchase
tokens on a server with the Google Play Developer API (for example, a Firebase
Cloud Function). The plan shown to admins is the one each device reports.

## Layout

```
app/src/main/java/com/rotitrack/app/
  MainActivity.kt, RotiTrackApp.kt   entry points; chooses Firebase or on-device accounts
  account/     accounts, plans, admin stats, on-device fallbacks
  cloud/       Firebase Auth + Firestore, Google Play Billing
  data/        foods, meal templates, drinks, tips, articles, models
  domain/      calorie & macro maths, meal planner, dates
  store/       AppStore (state + persistence), AndroidStorage (JSON file)
  reminders/   WorkManager reminder, "+ glass" notification action
  ui/          Compose screens: Login, Home, Water, Food, Add food, Plan, Me,
               Pro, Admin, onboarding, articles, shared components and theme
firestore.rules   who may read and write which user fields
app/src/test/  unit tests
```

## Notes

Food values are typical home-style estimates per serving, based on IFCT 2017 and
standard recipes. Restaurant portions and the amount of oil vary a lot.

Roti Track is a wellbeing tool, not a medical device. Its targets are estimates. If
you have diabetes, a thyroid, kidney or heart condition, or are pregnant, check
with a doctor or dietitian.
