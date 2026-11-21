package com.rotitrack.app.data

import com.rotitrack.app.i18n.t
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** A section of the weight loss recipe book. */
enum class RecipeCategory(val key: String, private val labelEn: String, val emoji: String) {
    VEG("veg", "Vegetarian", "🥗"),
    NONVEG("nonveg", "Non-veg", "🍗"),
    EGG("egg", "Egg", "🥚"),
    DRINK("drink", "Drinks", "🥤"),
    SWEET("sweet", "Sweets", "🍮"),
    DIP("dip", "Dips & chutneys", "🥣");

    val label: String get() = t(labelEn)

    companion object {
        fun of(key: String): RecipeCategory = entries.firstOrNull { it.key == key } ?: VEG
    }
}

/** One recipe from the book. Calories and protein are per serving. */
@Serializable
data class Recipe(
    val id: String,
    val cat: String,
    val title: String,
    val intro: String,
    val kcal: Int,
    val protein: Int,
    val minutes: Int,
    val ingredients: List<String>,
    val steps: List<String>,
    val benefits: List<String>,
) {
    val category: RecipeCategory get() = RecipeCategory.of(cat)

    /** A food picture from the recipe's name. */
    val emoji: String get() = Recipes.emojiFor(title, category)

    /**
     * The recipe as a food to log, one serving. The book gives calories and protein; carbs and fat
     * are an estimate from the remaining calories (about 55% carbs, 45% fat).
     */
    fun toFood(): Food {
        val rest = (kcal - protein * 4).coerceAtLeast(0)
        return Food(
            id = "recipe-$id", name = title, emoji = emoji, category = FoodCategory.CUSTOM, serving = "1 serving",
            kcal = kcal, protein = protein.toDouble(),
            carbs = (rest * 0.55 / 4 * 10).roundToInt() / 10.0, fat = (rest * 0.45 / 9 * 10).roundToInt() / 10.0,
            diet = when (category) { RecipeCategory.NONVEG -> Diet.NONVEG; RecipeCategory.EGG -> Diet.EGG; else -> Diet.VEG },
        )
    }
}

object Recipes {
    private val json = Json { ignoreUnknownKeys = true }

    val all: List<Recipe> by lazy {
        RECIPE_JSON_PARTS.flatMap { json.decodeFromString(ListSerializer(Recipe.serializer()), it) }
    }

    fun byId(id: String): Recipe? = all.firstOrNull { it.id == id }

    /** Matches the name, an ingredient or the description, ignoring case. */
    fun search(query: String, category: RecipeCategory?): List<Recipe> {
        val q = query.trim().lowercase()
        return all.filter { r ->
            (category == null || r.category == category) &&
                (q.isEmpty() || r.title.lowercase().contains(q) || r.ingredients.any { it.lowercase().contains(q) } || r.intro.lowercase().contains(q))
        }
    }

    private val PICTURES = listOf(
        "prawn" to "🦐", "fish" to "🐟", "chicken" to "🍗", "mutton" to "🍖", "keema" to "🍖", "egg" to "🥚", "omelette" to "🍳",
        "soup" to "🍲", "salad" to "🥗", "raita" to "🥣", "dosa" to "🥞", "chilla" to "🥞", "cheela" to "🥞", "idli" to "🍚",
        "roti" to "🫓", "thepla" to "🫓", "thalipeeth" to "🫓", "paratha" to "🫓", "sandwich" to "🥪", "tikka" to "🍢", "kabab" to "🍢",
        "kebab" to "🍢", "cutlet" to "🍘", "paneer" to "🧀", "tofu" to "🧀", "khichdi" to "🍚", "pulao" to "🍚", "rice" to "🍚",
        "dal" to "🥣", "sambar" to "🥣", "kadhi" to "🥣", "oats" to "🥣", "dalia" to "🥣", "upma" to "🥣", "poha" to "🍛",
        "chaat" to "🥗", "sprouts" to "🌱", "rajma" to "🫘", "chana" to "🫘", "chickpea" to "🫘", "lobia" to "🫘", "moong" to "🫘",
        "soya" to "🫘", "baingan" to "🍆", "bhindi" to "🌿", "karela" to "🥒", "cucumber" to "🥒", "lauki" to "🥒", "tinda" to "🥒",
        "capsicum" to "🫑", "cabbage" to "🥬", "palak" to "🥬", "spinach" to "🥬", "methi" to "🌿", "broccoli" to "🥦",
        "mushroom" to "🍄", "pumpkin" to "🎃", "corn" to "🌽", "quinoa" to "🌾", "ragi" to "🌾", "bajra" to "🌾", "jowar" to "🌾",
        "lassi" to "🥛", "chaas" to "🥛", "milk" to "🥛", "tea" to "🍵", "chai" to "🍵", "water" to "💧", "lemon" to "🍋",
        "smoothie" to "🥤", "kheer" to "🍮", "ladoo" to "🍡", "laddu" to "🍡", "halwa" to "🍮", "chutney" to "🥣", "dip" to "🥣",
        "hummus" to "🥣", "curry" to "🍛", "masala" to "🍛",
    )

    fun emojiFor(title: String, category: RecipeCategory): String {
        val name = title.lowercase()
        // Whole-word start, so "veggies" isn't an egg.
        return PICTURES.firstOrNull { (word, _) -> Regex("\\b$word").containsMatchIn(name) }?.second ?: category.emoji
    }
}
