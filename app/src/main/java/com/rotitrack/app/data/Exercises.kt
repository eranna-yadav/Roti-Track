package com.rotitrack.app.data

/** [met] is the Compendium of Physical Activities value: kcal ≈ MET × kg × hours. */
data class ExerciseType(val id: String, val name: String, val emoji: String, val met: Double)

val ACTIVITIES = listOf(
    ExerciseType("walk", "Walking", "🚶", 3.5),
    ExerciseType("brisk-walk", "Brisk walking", "🚶", 4.8),
    ExerciseType("run", "Running", "🏃", 9.0),
    ExerciseType("cycle", "Cycling", "🚴", 7.0),
    ExerciseType("yoga", "Yoga", "🧘", 2.8),
    ExerciseType("surya", "Surya Namaskar", "🙏", 5.5),
    ExerciseType("gym", "Gym / weights", "🏋️", 5.0),
    ExerciseType("hiit", "HIIT workout", "🔥", 8.0),
    ExerciseType("dance", "Dancing / Zumba", "💃", 6.5),
    ExerciseType("swim", "Swimming", "🏊", 7.0),
    ExerciseType("cricket", "Cricket", "🏏", 5.0),
    ExerciseType("badminton", "Badminton", "🏸", 5.5),
    ExerciseType("football", "Football", "⚽", 7.0),
    ExerciseType("stairs", "Climbing stairs", "🪜", 8.0),
    ExerciseType("chores", "Household chores", "🧹", 3.3),
    ExerciseType("garden", "Gardening", "🌱", 3.8),
)

fun exerciseById(id: String): ExerciseType = ACTIVITIES.firstOrNull { it.id == id } ?: ACTIVITIES.first()

fun burnedKcal(activity: ExerciseType, weightKg: Double, minutes: Int): Int =
    (activity.met * weightKg * minutes / 60.0).toInt()
