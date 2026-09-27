package com.rotitrack.app.data

import kotlinx.serialization.Serializable

@Serializable enum class Gender { FEMALE, MALE, OTHER }
@Serializable enum class Diet { VEG, EGG, NONVEG }
@Serializable enum class Region(val label: String) {
    ALL("All India"), NORTH("North"), SOUTH("South"), WEST("West"), EAST("East")
}
@Serializable enum class Activity(val label: String, val hint: String, val factor: Double) {
    SEDENTARY("Sedentary", "Desk job, little exercise", 1.2),
    LIGHT("Lightly active", "Walks, exercise 1–3 days a week", 1.375),
    MODERATE("Moderately active", "Exercise 3–5 days a week", 1.55),
    ACTIVE("Very active", "Hard exercise 6–7 days a week", 1.725),
    ATHLETE("Athlete", "Physical job or twice-a-day training", 1.9),
}
@Serializable enum class WeightGoal(val label: String, val emoji: String, val delta: Int) {
    LOSE("Lose weight", "📉", -500), MAINTAIN("Stay fit", "⚖️", 0), GAIN("Gain weight", "💪", 300)
}
@Serializable enum class MealSlot(val label: String, val short: String, val emoji: String, val share: Double, val time: String) {
    BREAKFAST("Breakfast", "Breakfast", "🌅", 0.25, "8:00 AM"),
    LUNCH("Lunch", "Lunch", "🍛", 0.35, "1:00 PM"),
    SNACK("Evening Snack", "Snack", "☕", 0.10, "5:00 PM"),
    DINNER("Dinner", "Dinner", "🌙", 0.30, "8:00 PM"),
}

enum class FoodCategory(val label: String) {
    BREAKFAST("Breakfast"), BREADS("Rotis"), RICE("Rice"), DAL("Dals"), SABZI("Sabzi"),
    NONVEG("Non-veg"), DAIRY("Dairy"), SNACKS("Snacks"), SWEETS("Sweets"),
    BEVERAGES("Drinks"), FRUITS("Fruits"), CUSTOM("My foods"),
}

/** Nutrition is per one serving as described by [serving]. */
@Serializable
data class Food(
    val id: String,
    val name: String,
    val emoji: String,
    val category: FoodCategory,
    val serving: String,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val diet: Diet = Diet.VEG,
    /** Pieces (rotis, idlis) are counted whole; portions (a bowl of dal) can be halved. */
    val piece: Boolean = false,
)

/** A logged food. Nutrition is snapshotted so editing a custom food never rewrites history. */
@Serializable
data class MealEntry(
    val id: String,
    val day: String,
    val ts: Long,
    val slot: MealSlot,
    val foodId: String,
    val name: String,
    val emoji: String,
    val serving: String,
    val servings: Double,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    /** Set when logged from the meal plan, as "day|SLOT". */
    val planKey: String? = null,
)

@Serializable
data class WaterEntry(
    val id: String,
    val day: String,
    val ts: Long,
    val ml: Int,
    val drinkId: String = "water",
)

@Serializable
data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val gender: Gender = Gender.FEMALE,
    val weightKg: Double = 60.0,
    val heightCm: Int = 160,
    val age: Int = 30,
    val activity: Activity = Activity.LIGHT,
    val goal: WeightGoal = WeightGoal.MAINTAIN,
    val diet: Diet = Diet.VEG,
    val region: Region = Region.ALL,
    /** Follows the body data unless [calorieGoalCustom]. */
    val calorieGoal: Int = 1800,
    val calorieGoalCustom: Boolean = false,
    /** Follows body weight unless [waterGoalCustom]. */
    val waterGoalMl: Int = 2000,
    val waterGoalCustom: Boolean = false,
    val cupMl: Int = 250,
    val remindersOn: Boolean = false,
    val reminderEveryMin: Int = 90,
    val wakeHour: Int = 7,
    val sleepHour: Int = 22,
)

@Serializable enum class Appearance(val label: String) { LIGHT("Light"), DARK("Dark"), SYSTEM("System") }

/** A daily nudge to log a meal (or everything at once, for "End of Day"). */
@Serializable
data class MealReminder(val key: String, val label: String, val hour: Int, val minute: Int, val enabled: Boolean = true)

val DEFAULT_MEAL_REMINDERS = listOf(
    MealReminder("breakfast", "Breakfast", 8, 30),
    MealReminder("lunch", "Lunch", 12, 0),
    MealReminder("snack", "Snack", 15, 0),
    MealReminder("dinner", "Dinner", 19, 0),
    MealReminder("end_of_day", "End of Day", 21, 0),
)

@Serializable
data class Prefs(
    val appearance: Appearance = Appearance.LIGHT,
    val badgeCelebrations: Boolean = true,
    /** Calories burned in exercise are added to the day's goal. */
    val addBurnedCalories: Boolean = false,
    /** Up to 200 kcal left over yesterday are added to today's goal. */
    val rolloverCalories: Boolean = false,
    val language: String = "en",
    val mealReminders: List<MealReminder> = DEFAULT_MEAL_REMINDERS,
    /** Where referral earnings are paid. */
    val payoutUpi: String = "",
)

@Serializable
data class ExerciseEntry(
    val id: String,
    val day: String,
    val ts: Long,
    val activityId: String,
    val name: String,
    val emoji: String,
    val minutes: Int,
    val kcal: Int,
)

@Serializable
data class WeightEntry(val day: String, val kg: Double)

@Serializable
data class FastRecord(val start: Long, val end: Long, val targetHours: Int)

@Serializable
data class Fasting(
    /** Hours of fasting in the chosen plan, e.g. 16 for 16:8. */
    val targetHours: Int = 16,
    val activeStart: Long? = null,
    val history: List<FastRecord> = emptyList(),
)

@Serializable
data class AppState(
    val profile: Profile = Profile(),
    val prefs: Prefs = Prefs(),
    val exercises: List<ExerciseEntry> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val fasting: Fasting = Fasting(),
    /** Badge id → when it was unlocked. */
    val badges: Map<String, Long> = emptyMap(),
    val water: List<WaterEntry> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val customFoods: List<Food> = emptyList(),
    /** "day|SLOT" → how many times the user tapped Swap. */
    val planShuffles: Map<String, Int> = emptyMap(),
)
