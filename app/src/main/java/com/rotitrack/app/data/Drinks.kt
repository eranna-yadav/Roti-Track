package com.rotitrack.app.data

import com.rotitrack.app.i18n.t

/** [hydration] is the share of the volume that counts toward the water goal. */
data class Drink(val id: String, private val nameEn: String, val emoji: String, val hydration: Double, val defaultMl: Int) {
    val name: String get() = t(nameEn)
}

val DRINKS = listOf(
    Drink("water", "Water", "💧", 1.0, 250),
    Drink("nimbu", "Nimbu Pani", "🍋", 1.0, 250),
    Drink("coconut", "Coconut Water", "🥥", 1.0, 240),
    Drink("chaas", "Chaas", "🥛", 0.95, 200),
    Drink("tea", "Chai", "☕", 0.85, 150),
    Drink("coffee", "Coffee", "☕", 0.8, 150),
    Drink("milk", "Milk", "🥛", 0.9, 250),
    Drink("juice", "Juice", "🧃", 0.9, 200),
    Drink("lassi", "Lassi", "🥤", 0.85, 250),
    Drink("soda", "Soft Drink", "🥤", 0.85, 300),
)

fun drinkById(id: String): Drink = DRINKS.firstOrNull { it.id == id } ?: DRINKS.first()
