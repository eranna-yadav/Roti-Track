<p align="center">
  <img src="assets/brand/logo-512.png" width="120" alt="Sipwell logo" />
</p>

<h1 align="center">Sipwell</h1>
<p align="center"><em>Indian diet planner, calorie tracker and water tracker</em></p>

An Android (and iOS) app built with Expo and React Native. Plan home-style
Indian meals, count calories and macros, and log what you drink — all on device.

## Features

**Food** — a daily calorie ring with protein / carbs / fat bars, today's water
alongside, and Breakfast · Lunch · Evening Snack · Dinner sections. Browse or
search 140+ Indian foods (idli, poha, dal tadka, rajma, paneer, biryani, chai,
mithai…) with veg / egg / non-veg marks, pick servings, or add your own recipes.
Step back through previous days and see the last seven at a glance.

**Plan** — a 7-day Indian meal plan scaled to your calorie target. Each meal is
a real thali or plate (Idli Sambar, Rajma Chawal, Bajra Roti & Bharta, Fish
Curry Rice…) filtered by your food preference and cuisine (North, South, West,
East or all India). Swap any meal, or tap "I ate this" to log it in one go.
A daily nutrition tip sits at the bottom.

**Diet Profile** — height, age, activity level, goal (lose / maintain / gain),
veg / egg / non-veg and cuisine. The calorie target comes from Mifflin–St Jeor
× activity, ±500 / +300 kcal for the goal, with ICMR-style macro splits and
Asian-Indian BMI bands. Override it by hand if your dietitian gave you a number.

**Water** — a big running total over an animated water level that rises as you
approach your goal, your target and next reminder at a glance, a one-tap
quick-add for your usual cup, and a full drink sheet for anything else.

**History** — day, week and month views. The day view scatters each individual
drink across the clock and lists every record for editing or deletion; week and
month roll up into bars with a dashed goal line and tap-to-inspect values.

**Insights** — nineteen short articles across six categories, from building a
balanced thali and vegetarian protein to hydration basics. Reached from Me
and the Plan tab.

**Me** — reminders, sound and haptics, daily goal, body data, drink types,
units, week start, day boundary and time format.

Alongside that: an onboarding flow that derives your goal from body weight and
waking hours, seventeen drink types each carrying its own hydration factor (a
coffee counts for 80% of its volume, a beer for 40%), daily streaks, local
notification scheduling in smart / interval / custom modes, and a Pro screen for
the premium drink set.

Everything is stored on device with AsyncStorage. There is no account, no
server and no analytics.

## Running it

```bash
npm install
npx expo start          # then scan the QR code with Expo Go
npx expo start --android
npx expo start --ios
npx expo start --web
```

To build an installable Android APK:

```bash
npm install -g eas-cli
eas build -p android --profile preview   # produces a downloadable .apk
# or, with the Android SDK installed locally:
npx expo run:android --variant release
```

Reminders use `expo-notifications`, which needs a development build or a real
device to deliver on a schedule — they will not fire in a web session.

## Layout

```
app/                        expo-router routes
  (tabs)/                   Water · Food · Plan · History · Me
  onboarding/               first-run setup
  add-food · custom-food    food search and custom recipes
  diet-profile              calorie target and diet preferences
  insights · article/[id]   articles and reader
  reminders · goal · body · drinks · units · day-start · pro · rate · legal
src/
  components/               wave, charts, sheets, icons, shared UI
  store/                    HydrationProvider, DietProvider, defaults, AsyncStorage
  lib/                      units, time, dates, goal & calorie maths, meal planner, notifications
  data/                     Indian foods, meal templates, diet tips, drinks, articles, sounds
assets/
  brand/                    logo source (SVG) and exports
  sounds/                   five water sound effects
```

## Design notes

The daily target is roughly 33–36 ml per kilogram of body weight depending on
gender, clamped to a sane range and rounded to the nearest 10 ml. You can
override it, and once you do it stops tracking your body data.

Volumes are always stored in millilitres and converted only for display, so
switching between metric and imperial never loses precision.

"A Day Starts At" shifts which logical day a drink belongs to, so a glass at
1am can still count toward the night before.

The five sound effects are synthesised, not sampled — see the generator note in
`assets/sounds`.

## Not wired up

The Pro screen has no payment provider behind it; subscribing unlocks the
entitlement locally so the premium drinks can be tried. Language options are a
single entry, and there is no home-screen widget yet.

Food values are typical home-style estimates per serving; restaurant portions
and oil quantities vary a lot.

Sipwell is a wellbeing tool, not a medical device. The goals it suggests are
estimates — check with a doctor or dietitian if you have a medical condition.
