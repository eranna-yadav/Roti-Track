# Roti Track

_Indian diet planner, calorie tracker and water tracker for Android._

A native Android app written in Kotlin with Jetpack Compose. Accounts and the
admin dashboard use Firebase. Pro subscriptions are paid through Razorpay (UPI,
cards, netbanking, wallets) or Google Play. Food and water logs stay on the
phone.

## Features

**Water**: an animated glass that fills toward your daily goal, quick-add buttons
for your usual glass, and ten drinks (water, nimbu pani, coconut water, chaas,
chai, coffee…). Each drink has its own hydration factor, so a cup of chai counts
for 85% of its volume. You also get a daily log and a streak.

**Water reminders** (bell on the Water tab):
- **Standard** mode: eight reminders around your day (After Wake-up, Before/After
  Breakfast, Lunch and Dinner, Before Sleep). Each has its own time and switch.
- **Interval** mode: every 30 min – 4 h between your wake-up time and bedtime.
- **Custom** mode: your own list of times. Tap a time to turn it on or off,
  press and hold to delete it, and tap **+** to add one.
- **Weekend Mode** skips Saturday and Sunday. Reminders stop once you reach
  your goal. **Can't receive reminders?** explains notification, battery and
  autostart settings.

**Sounds & Effects** (speaker on the Water tab): five water sounds (Water drop
1–2, Water flowing 1–3), a volume slider and vibration. The sound plays with each
reminder and when you log a drink, and is muted when the phone is on silent.

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

**Pro**: ₹359/month or ₹1,099/year (74% cheaper). Users who sign up with a
friend's referral code pay ₹990 for their first year. Pro unlocks the full 7-day
plan, meal swaps, 30-day trends and your own recipes; everything else is free.
Users pay through Razorpay or Google Play, and can turn off Razorpay auto-renew
from the Pro screen.

**Admin dashboard** (admins only, from the Profile tab):
- Totals: users, Pro users, monthly and yearly subscribers, estimated monthly
  revenue in ₹, new sign-ups (7 days), active users (24 h), and a 14-day sign-up
  chart.
- A searchable, filterable user list. For each user you can give free Pro,
  block the account, and see referral earnings owed with a "mark paid" button.

**Profile**:
- **Invite friends**: each user gets a promo code. When a friend signs up with
  it and then buys Pro, the referrer earns up to ₹500: all at once for a yearly
  plan, or ₹250 after the friend's 1st monthly payment and ₹250 after the 2nd.
  The full rules are under Profile → Referral rules. Users add a
  UPI ID for payouts.
- **Personal details**: name, age, gender, height, a weight log with history,
  activity level, diet preference and cuisine.
- **Preferences**: Light, Dark or System appearance; badge celebrations; add
  calories burned to the day's goal; roll over up to 200 unused calories from
  yesterday.
- **Language**: English. Indian languages are listed as "coming soon".
- **Edit Nutrition Goals**, **Intermittent Fasting** (12:12 to OMAD, with a live
  timer and history), **Tracking Reminders** (Breakfast, Lunch, Snack, Dinner and
  End of Day at your own times, plus water reminders), and **Badges**.
- **Export PDF Summary Report**: meal history, exercise history, weekly weight
  trend, and daily calories and macros, shared as a PDF. The 7-day report is
  free; 30 and 90 days need Pro.
- **Support & Legal**: request a feature, support email, terms, privacy.
- **Logout** and **Delete Account**.

**Exercise** (Food tab): log 16 activities, from walking and yoga to Surya
Namaskar and cricket, with calories estimated as MET × weight × time.

**Body and targets**: gender, weight, height, age, activity, goal (lose / maintain / gain),
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

| | Without setup | With Firebase (+ Razorpay, Play Console) |
|---|---|---|
| Accounts | Stored on this phone. The first account is the admin | Firebase Auth (email/password) |
| Admin dashboard | Shows accounts on this phone | Shows every user |
| Pro, debug build | Demo purchase with no charge | Razorpay (use test keys) |
| Pro, installed from Play | Google Play Billing | Play's choice screen: Razorpay or Google Play* |
| Pro, APK installed directly | Demo purchase with no charge | Razorpay |

\* Only once you enrol in Google's user choice billing. Until then, Play installs
use Google Play Billing only.

### 1. Firebase (accounts + admin)

1. At <https://console.firebase.google.com>, create a project and add an Android
   app with package `com.rotitrack.app`.
2. Download `google-services.json` into the `app/` folder, then sync Gradle.
3. Go to **Authentication → Sign-in method** and enable **Email/Password**.
4. Go to **Firestore Database** and create a database. Under **Rules**, paste
   the contents of `firestore.rules` and publish.
5. Sign up in the app. Then, in Firestore → `users` → your document, add a
   field `role` with the value `admin`. Reopen the app and the Admin dashboard
   appears under Profile.

### 2. Razorpay (Pro payments)

The Razorpay key secret must never go into the app. It lives in Firebase Cloud
Functions (`functions/`), which create each subscription, verify the payment
signature, and track renewals through webhooks. Cloud Functions need the
Firebase **Blaze** (pay-as-you-go) plan.

1. In the [Razorpay Dashboard](https://dashboard.razorpay.com), go to
   **Subscriptions → Plans** and create two plans. Start in Test mode.
   - ₹359, every 1 month
   - ₹1,099, every 1 year (referred users pay ₹990 up front for the first year;
     the server adds that as an add-on and starts this plan a year later)
2. Copy `functions/.env.example` to `functions/.env` and paste in the two
   `plan_…` IDs.
3. Install the [Firebase CLI](https://firebase.google.com/docs/cli), then run
   these from the repo folder:
   ```bash
   firebase use <your-project-id>
   firebase functions:secrets:set RAZORPAY_KEY_ID        # rzp_test_… or rzp_live_…
   firebase functions:secrets:set RAZORPAY_KEY_SECRET
   firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET # any strong random string
   firebase deploy --only functions,firestore:rules
   ```
4. In the Razorpay Dashboard, go to **Settings → Webhooks** and add
   `https://asia-south1-<project-id>.cloudfunctions.net/razorpayWebhook`. Use the
   same webhook secret, and tick the `subscription.*` events.
5. Try it with a debug build and Razorpay's test UPI ID or test cards. When it
   works, switch to live keys and live plan IDs.

Payment records are saved in the Firestore `payments` collection, which only
admins can read.

### 3. Google Play (Pro subscriptions)

1. Upload a release build (`./gradlew bundleRelease`) to a testing track in
   Play Console. Billing only works for apps installed from Play.
2. Go to **Monetize → Subscriptions** and create two subscriptions, each with an
   auto-renewing base plan:
   - `rotitrack_pro_monthly`: ₹359, renews every month
   - `rotitrack_pro_yearly`: ₹1,099, renews every year. Add a **developer-determined
     offer** with the tag `referral`: ₹990 for the first year (1 billing period).
     The app picks it only for users who signed up with a referral code.
3. Add your Google account as a license tester so test purchases are free.
4. **To offer Razorpay on Play:** enrol in **Play Console → Monetization setup
   → Alternative billing (user choice billing, India)**. Then:
   - set `user_choice_billing` to `true` in `app/src/main/res/values/config.xml`;
   - enable the Google Play Android Developer API in Google Cloud;
   - in Play Console → Users & permissions, invite the Cloud Functions service
     account (`<project-id>@appspot.gserviceaccount.com`) with financial access.

   The functions then report each Razorpay transaction to Google, as the
   programme requires. Google charges a reduced service fee on these
   transactions.

Razorpay payments are verified on the server. Google Play purchases are still
checked on the device only. For Play, add server-side purchase verification
with the Google Play Developer API before a large launch.

## Layout

```
app/src/main/java/com/rotitrack/app/
  MainActivity.kt, RotiTrackApp.kt   entry points; chooses Firebase or on-device accounts
  account/     accounts, plans, admin stats, on-device fallbacks
  cloud/       Firebase Auth + Firestore, Razorpay Checkout, Google Play Billing
  data/        foods, meal templates, drinks, tips, articles, models
  domain/      calorie & macro maths, meal planner, dates
  store/       AppStore (state + persistence), AndroidStorage (JSON file)
  reminders/   WorkManager reminder, "+ glass" notification action
  ui/          Compose screens: Login, Home, Water, Food, Add food, Plan, Me,
               Pro, Admin, onboarding, articles, shared components and theme
firestore.rules   who may read and write which user fields
functions/        Cloud Functions: Razorpay subscriptions, verification, webhooks
app/src/test/  unit tests
```

## Notes

Food values are typical home-style estimates per serving, based on IFCT 2017 and
standard recipes. Restaurant portions and the amount of oil vary a lot.

Roti Track is a wellbeing tool, not a medical device. Its targets are estimates. If
you have diabetes, a thyroid, kidney or heart condition, or are pregnant, check
with a doctor or dietitian.
