package com.rotitrack.app.data

/** One feature in the user guide: what it is, how to use it, and why it helps. */
data class GuideFeature(
    val emoji: String,
    val title: String,
    val what: String,
    val how: List<String>,
    val benefit: String,
    val pro: Boolean = false,
)

/** One screen of the app and its features. Text is English; screens pass it through t(). */
data class GuidePage(val id: String, val emoji: String, val title: String, val intro: String, val features: List<GuideFeature>)

val USER_GUIDE: List<GuidePage> = listOf(
    GuidePage(
        "home", "🏠", "Home",
        "Your day at a glance: calories, water, protein, streaks and trends in one place.",
        listOf(
            GuideFeature(
                "📊", "Today card",
                "A calorie ring with bars for calories, water and protein, compared with your goals.",
                listOf("Open the Home tab.", "Read the ring: it fills as you log food.", "Tap the card to open the Food page and log more."),
                "See in five seconds how your day is going, without adding anything up yourself.",
            ),
            GuideFeature(
                "🔥", "Streak, BMI and weight",
                "Three tiles: your logging streak, your BMI with its range, and your current weight with your goal.",
                listOf(
                    "Log any food, water or exercise every day to grow your streak.",
                    "BMI comes from the height and weight in Personal details.",
                    "Log your weight on Profile → Personal details to update the weight tile.",
                ),
                "A streak keeps you motivated, and BMI and weight show whether you're moving towards your goal.",
            ),
            GuideFeature(
                "🍽️", "Up next",
                "The next meal from your diet plan, picked by the time of day, with its foods and calories.",
                listOf("Look at the Up next card.", "Tap it to open the Plan page, where you can log it in one tap."),
                "You never have to wonder what to eat next, and your meals stay within your calorie goal.",
            ),
            GuideFeature(
                "📈", "Trends",
                "Bar charts of your water and calories for the last 7 days (or 30 days with Pro), with daily averages and how many days you met your goal.",
                listOf("Scroll down to Trends.", "Switch between 7 days and 30 days.", "The dashed line is your goal; today's bar is highlighted."),
                "Spot patterns, such as drinking less water at weekends, and fix them early.",
            ),
        ),
    ),
    GuidePage(
        "water", "💧", "Water",
        "Track every glass, get reminders through the day and reach your water goal.",
        listOf(
            GuideFeature(
                "🌊", "Water goal ring",
                "Shows how much you've had today, the percentage of your goal, and how many glasses or bottles are left.",
                listOf("Open the Water tab.", "The wave rises as you drink.", "Change your goal on Profile → Edit Nutrition Goals."),
                "A clear target makes it easy to drink enough, which helps energy, digestion and skin.",
            ),
            GuideFeature(
                "🥤", "Log a drink",
                "Log water, nimbu pani, coconut water, chaas, chai, coffee, milk, juice, lassi or soft drinks. Each drink counts by how much it hydrates you.",
                listOf(
                    "Pick the drink under What are you drinking?",
                    "Tap + 250 ml, + 150 ml or + 500 ml, or Other to type your own amount.",
                    "Remove a wrong entry from Today's log with the bin icon.",
                ),
                "Your count stays honest: a cup of chai doesn't count the same as a glass of water.",
            ),
            GuideFeature(
                "🔔", "Water reminders",
                "Notifications that remind you to drink, in three modes: Standard (around your sleep and meal times), Interval (every few hours while you're awake) and Custom (your own times).",
                listOf(
                    "Tap the bell at the top of the Water page.",
                    "Switch reminders on and choose a mode.",
                    "In Custom mode, tap a time to turn it on or off, and press and hold to delete it.",
                    "Turn on Weekend Mode for no reminders on Saturday and Sunday.",
                    "Tap + 250 ml on a reminder to log a glass without opening the app.",
                ),
                "You drink steadily through the day, and reminders stop by themselves once you reach your goal.",
            ),
            GuideFeature(
                "🔊", "Sounds & Effects",
                "Choose the reminder sound, its volume, and whether the phone vibrates.",
                listOf("Tap the speaker at the top of the Water page.", "Pick one of the water sounds and set the volume.", "Turn vibration on or off."),
                "Reminders you can notice without being annoying.",
            ),
            GuideFeature(
                "🛠️", "Can't receive reminders?",
                "Step-by-step help when reminders don't show, for example on phones that stop apps in the background.",
                listOf(
                    "Open the Water reminders screen and tap Can't receive reminders?",
                    "Allow notifications, set battery use to Unrestricted, and allow Alarms & reminders.",
                    "On Xiaomi, Vivo, Oppo, Realme and OnePlus phones, also turn on Autostart.",
                ),
                "Reminders arrive on time, even when the phone is locked.",
            ),
        ),
    ),
    GuidePage(
        "food", "🍛", "Food",
        "Log what you eat, see calories and macros, and track exercise.",
        listOf(
            GuideFeature(
                "🎯", "Calories and macros",
                "A ring showing calories left, with your goal, calories eaten, burned and water, plus protein, carbs and fat bars.",
                listOf("Open the Food tab.", "Use ‹ and › to look at earlier days."),
                "You know exactly how much you can still eat today, and whether you're getting enough protein.",
            ),
            GuideFeature(
                "➕", "Log food",
                "Search 142 common Indian foods, from idli and dosa to dal, sabzi, biryani and sweets, with nutrition per serving.",
                listOf(
                    "Tap + on Breakfast, Lunch, Evening Snack or Dinner.",
                    "Search by name (in English or your language), or pick from Recent.",
                    "Set the number of servings and tap Add.",
                    "Pro: create your own foods and recipes with Create custom food.",
                ),
                "Logging a meal takes seconds, with values made for Indian home cooking.",
            ),
            GuideFeature(
                "🏃", "Exercise",
                "Log walks, yoga, gym, sports, dancing, housework and more, with the calories burned.",
                listOf(
                    "Tap the orange + on the Exercise card.",
                    "Pick an activity and the minutes.",
                    "To add burned calories to your daily goal, turn on Add Burned Calories in Preferences.",
                ),
                "See how activity balances your food, and stay motivated to move every day.",
            ),
            GuideFeature(
                "📉", "Last 7 days",
                "A chart of your daily calories for the past week, with your goal as a dashed line.",
                listOf("Scroll to the bottom of the Food page."),
                "See at a glance which days went over or under your goal.",
            ),
            GuideFeature(
                "🔔", "Meal reminders and voice",
                "The bell opens your meal reminder times. The speaker switches spoken reminders on or off.",
                listOf(
                    "Tap the bell to set times for breakfast, lunch, snack, dinner and End of Day.",
                    "Tap the speaker to turn voice reminders on; you'll hear a sample.",
                ),
                "Friendly reminders, by name, help you log every meal on time.",
            ),
        ),
    ),
    GuidePage(
        "plan", "📅", "Plan",
        "A daily Indian meal plan that fits your calorie goal, diet and cuisine.",
        listOf(
            GuideFeature(
                "🥗", "Today's meal plan",
                "Breakfast, lunch, evening snack and dinner made from home-style Indian meals, sized to your calorie goal.",
                listOf(
                    "Open the Plan tab to see today's meals and total calories.",
                    "Tap ⚙️ at the top to change your food preference (Veg, Egg or Non-veg) and cuisine (All India, North, South, West or East).",
                    "The plan updates straight away to match.",
                ),
                "Healthy eating without planning: meals you already know, in the right amounts.",
            ),
            GuideFeature(
                "✅", "I ate this",
                "Logs a whole planned meal to your Food page in one tap.",
                listOf("Tap I ate this on a meal. It changes to ✓ Logged."),
                "The quickest way to log when you follow the plan.",
            ),
            GuideFeature(
                "⇄", "Swap a meal",
                "Replace any planned meal with another one of similar calories.",
                listOf("Tap Swap on the meal you want to change.", "Keep tapping until you like the option."),
                "More variety, and you only eat what you enjoy.",
                pro = true,
            ),
            GuideFeature(
                "🗓️", "7-day meal plan",
                "Plan meals for the whole week, not just today.",
                listOf("Tap any day in the row at the top of the Plan page to see its meals."),
                "Shop and cook ahead for the whole week.",
                pro = true,
            ),
            GuideFeature(
                "💡", "Tips and health articles",
                "A new tip every day, and short articles on Indian nutrition, hydration and habits.",
                listOf("Read the Tip of the day on the Plan page.", "Tap Health articles to read more."),
                "Small, practical changes that add up to better health.",
            ),
        ),
    ),
    GuidePage(
        "profile", "👤", "Profile",
        "Your details, goals, reminders, rewards, reports and settings.",
        listOf(
            GuideFeature(
                "🪪", "Personal details and weight",
                "Your name, gender, age, height, weight, activity level and goal (lose weight, stay fit or gain weight), plus your weight history.",
                listOf(
                    "Tap your name at the top of Profile, or Personal details.",
                    "Keep your details up to date.",
                    "Tap Log weight regularly to see your progress chart.",
                ),
                "Your calorie and water targets are worked out from these details, so they stay right for you.",
            ),
            GuideFeature(
                "🎯", "Nutrition goals",
                "Your daily calorie goal, protein, carbs and fat, and your water goal.",
                listOf(
                    "Tap Edit Nutrition Goals.",
                    "Use the suggested target, or turn on Set my own target.",
                    "Set your water goal and your usual glass size.",
                ),
                "Targets based on your body and goal (Mifflin–St Jeor), or your doctor's advice.",
            ),
            GuideFeature(
                "⚙️", "Preferences",
                "Appearance (light, dark or system), badge celebrations, adding burned calories to your goal, and rolling over up to 200 unused calories to the next day.",
                listOf("Tap Preferences and switch the options you want."),
                "The app works the way you like.",
            ),
            GuideFeature(
                "🌐", "Language",
                "Use the app in English, Hindi, Kannada, Telugu, Tamil, Marathi, Bengali or Gujarati.",
                listOf("Tap Language and choose one. Screens and food names change straight away."),
                "Easier for everyone in the family to use.",
            ),
            GuideFeature(
                "⏱️", "Intermittent fasting",
                "Fasting plans from 12:12 to 20:4 and OMAD (one meal a day), with a live timer and your fasting history.",
                listOf(
                    "Tap Intermittent Fasting and choose a plan.",
                    "Tap Start fast. The timer shows how long is left.",
                    "Tap End fast when you eat.",
                ),
                "A simple way to try fasting safely. Check with a doctor first if you are pregnant, diabetic or underweight.",
            ),
            GuideFeature(
                "🔔", "Tracking reminders and voice",
                "Daily reminders for each meal and End of Day. Voice reminders speak a warm message with your name and the meal, in your language.",
                listOf(
                    "Tap Tracking Reminders.",
                    "Set a time for each meal and switch it on.",
                    "Turn on Voice reminders and tap ▶ Try it to hear one.",
                    "If asked, allow Alarms & reminders so reminders come on time when the phone is locked.",
                ),
                "You won't forget to log, and the friendly voice makes it feel personal.",
            ),
            GuideFeature(
                "🏅", "Badges",
                "Awards for milestones like your first meal, first glass of water, streaks, 50 meals, exercise and fasting.",
                listOf("Tap Badges to see what you've unlocked and what's next."),
                "Fun goals that keep you going.",
            ),
            GuideFeature(
                "🎁", "Refer & earn",
                "Share your promo code. When a friend signs up with it and buys Pro, you earn up to ₹500.",
                listOf(
                    "Tap Invite friends and share your code on WhatsApp or anywhere.",
                    "Your friend enters the code when creating their account.",
                    "Add your UPI ID so we can send your earnings.",
                    "Read Referral rules for the details.",
                ),
                "Help friends get healthier and earn money for it.",
            ),
            GuideFeature(
                "👑", "Roti Track Pro",
                "Unlocks the full 7-day meal plan, meal swaps, 30-day trends, custom foods and longer reports.",
                listOf(
                    "Tap Upgrade to Pro.",
                    "Choose Monthly or Yearly, then pay with Google Play or Razorpay (UPI, cards, netbanking, wallets).",
                    "Manage or cancel any time from the same screen.",
                ),
                "More tools to reach your goal faster.",
            ),
            GuideFeature(
                "📄", "PDF summary report",
                "A report of your meals, exercise, weight, calories, macros and water for a period you choose.",
                listOf("Tap Export PDF Summary Report.", "Choose a period and tap Next.", "Save it or share it with your doctor or dietitian."),
                "Share your progress with a professional in one tap.",
            ),
            GuideFeature(
                "✉️", "Support, legal and account",
                "Request a feature, email support, read the Terms and Privacy Policy, log out, or delete your account.",
                listOf("Find these at the bottom of the Profile page."),
                "Help when you need it, and full control over your data.",
            ),
        ),
    ),
)

fun guidePage(id: String): GuidePage? = USER_GUIDE.firstOrNull { it.id == id }
