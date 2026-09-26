package com.rotitrack.app.data

/** One item of a plate. [fixed] items (a cup of chai) are never scaled. */
data class PlanItem(val foodId: String, val servings: Double, val fixed: Boolean = false)

/** [region] is null for dishes eaten all over India. */
data class MealTemplate(val name: String, val region: Region?, val items: List<PlanItem>)

private fun t(name: String, region: Region?, vararg items: PlanItem) = MealTemplate(name, region, items.toList())
private fun i(id: String, servings: Double, fixed: Boolean = false) = PlanItem(id, servings, fixed)

/**
 * Home-style thalis and plates. The planner picks one per slot and scales the
 * servings to hit that slot's share of the daily calorie target.
 */
val TEMPLATES: Map<MealSlot, List<MealTemplate>> = mapOf(
    MealSlot.BREAKFAST to listOf(
        t("Kanda Poha & Chai", Region.WEST, i("poha", 1.0), i("chai", 1.0, fixed = true)),
        t("Idli Sambar", Region.SOUTH, i("idli", 3.0), i("sambar", 1.0), i("coconut-chutney", 1.0, fixed = true)),
        t("Dosa & Sambar", Region.SOUTH, i("dosa", 2.0), i("sambar", 1.0), i("coconut-chutney", 1.0, fixed = true)),
        t("Rava Upma & Coffee", Region.SOUTH, i("upma", 1.0), i("filter-coffee", 1.0, fixed = true)),
        t("Pesarattu", Region.SOUTH, i("pesarattu", 2.0), i("coconut-chutney", 1.0, fixed = true)),
        t("Ragi Dosa & Sambar", Region.SOUTH, i("ragi-dosa", 2.0), i("sambar", 1.0)),
        t("Aloo Paratha & Dahi", Region.NORTH, i("aloo-paratha", 1.0), i("curd", 1.0), i("chai-nosugar", 1.0, fixed = true)),
        t("Paneer Paratha", Region.NORTH, i("paneer-paratha", 1.0), i("curd", 0.5)),
        t("Besan Chilla & Curd", Region.NORTH, i("besan-chilla", 2.0), i("curd", 1.0)),
        t("Moong Dal Chilla", null, i("moong-chilla", 2.0), i("curd", 1.0)),
        t("Methi Thepla & Dahi", Region.WEST, i("thepla", 2.0), i("curd", 1.0), i("chai", 1.0, fixed = true)),
        t("Khaman Dhokla", Region.WEST, i("dhokla", 1.5), i("chai-nosugar", 1.0, fixed = true)),
        t("Oats Porridge & Banana", null, i("oats-milk", 1.0), i("banana", 1.0)),
        t("Masala Oats & Fruit", null, i("oats", 1.0), i("papaya", 1.0)),
        t("Sprouts & Milk", null, i("sprouts", 1.0), i("milk", 1.0), i("apple", 1.0)),
        t("Poha & Sprouts", Region.EAST, i("poha", 1.0), i("sprouts", 0.5), i("chai-nosugar", 1.0, fixed = true)),
        t("Egg Omelette & Toast", null, i("omelette", 1.0), i("bread", 2.0), i("chai-nosugar", 1.0, fixed = true)),
        t("Boiled Eggs & Toast", null, i("boiled-egg", 2.0), i("bread", 2.0), i("orange", 1.0)),
        t("Egg Bhurji & Roti", Region.NORTH, i("egg-bhurji", 1.0), i("roti", 1.0)),
        t("Appam & Egg Curry", Region.SOUTH, i("appam", 2.0), i("egg-curry", 0.5)),
    ),
    MealSlot.LUNCH to listOf(
        t("Dal Roti Sabzi", Region.NORTH, i("roti", 2.0), i("dal-tadka", 1.0), i("aloo-gobi", 1.0), i("salad", 1.0, fixed = true), i("curd", 0.5)),
        t("Rajma Chawal", Region.NORTH, i("rice", 1.0), i("rajma", 1.0), i("salad", 1.0, fixed = true)),
        t("Chole & Roti", Region.NORTH, i("roti", 2.0), i("chole", 1.0), i("raita", 0.5), i("salad", 1.0, fixed = true)),
        t("Palak Paneer Thali", Region.NORTH, i("roti", 2.0), i("palak-paneer", 1.0), i("moong-dal", 0.5), i("salad", 1.0, fixed = true)),
        t("Sambar Rice Meal", Region.SOUTH, i("rice", 1.0), i("sambar", 1.0), i("poriyal", 1.0), i("curd", 0.5)),
        t("Rasam Rice & Avial", Region.SOUTH, i("rice", 1.0), i("rasam", 1.0), i("avial", 1.0), i("curd", 0.5)),
        t("Bisi Bele Bath", Region.SOUTH, i("bisi-bele-bath", 1.0), i("raita", 0.5), i("salad", 1.0, fixed = true)),
        t("Gujarati Khichdi Kadhi", Region.WEST, i("khichdi", 1.0), i("kadhi", 1.0), i("salad", 1.0, fixed = true)),
        t("Bajra Roti & Bharta", Region.WEST, i("bajra-roti", 2.0), i("baingan-bharta", 1.0), i("chaas", 1.0, fixed = true)),
        t("Usal & Bhakri", Region.WEST, i("jowar-roti", 2.0), i("usal", 1.0), i("salad", 1.0, fixed = true)),
        t("Dal Bhaat & Bhindi", Region.EAST, i("rice", 1.0), i("moong-dal", 1.0), i("bhindi", 1.0), i("salad", 1.0, fixed = true)),
        t("Veg Pulao & Raita", null, i("veg-pulao", 1.0), i("raita", 1.0), i("moong-dal", 0.5)),
        t("Brown Rice Power Bowl", null, i("brown-rice", 1.0), i("chana-dal", 1.0), i("mixed-veg", 1.0), i("curd", 0.5)),
        t("Soya Curry & Roti", null, i("roti", 2.0), i("soya-curry", 1.0), i("salad", 1.0, fixed = true)),
        t("Egg Curry Rice", Region.EAST, i("rice", 1.0), i("egg-curry", 1.0), i("salad", 1.0, fixed = true)),
        t("Fish Curry Rice", Region.EAST, i("rice", 1.0), i("fish-curry", 1.0), i("poriyal", 0.5)),
        t("Kerala Fish Meal", Region.SOUTH, i("rice", 1.0), i("fish-curry", 1.0), i("beans-poriyal", 1.0)),
        t("Chicken Curry & Roti", Region.NORTH, i("roti", 2.0), i("chicken-curry", 1.0), i("salad", 1.0, fixed = true), i("curd", 0.5)),
        t("Chicken Biryani & Raita", null, i("chicken-biryani", 1.0), i("raita", 0.5)),
    ),
    MealSlot.SNACK to listOf(
        t("Roasted Chana & Chai", null, i("roasted-chana", 1.0), i("chai-nosugar", 1.0, fixed = true)),
        t("Makhana & Green Tea", null, i("makhana", 1.0), i("green-tea", 1.0, fixed = true)),
        t("Fruit & Nuts", null, i("apple", 1.0), i("nuts", 0.5)),
        t("Chaas & Peanuts", Region.WEST, i("chaas", 1.0, fixed = true), i("peanuts", 0.5)),
        t("Sprouts Chaat", Region.NORTH, i("sprouts-chaat", 1.0)),
        t("Sundal", Region.SOUTH, i("sundal", 1.0), i("filter-coffee", 0.5, fixed = true)),
        t("Khakhra & Chai", Region.WEST, i("khakhra", 1.0), i("chai-nosugar", 1.0, fixed = true)),
        t("Coconut Water & Guava", Region.SOUTH, i("coconut-water", 1.0, fixed = true), i("guava", 1.0)),
        t("Masala Murmura", Region.EAST, i("murmura", 1.0), i("chai-nosugar", 1.0, fixed = true)),
        t("Banana & Milk", null, i("banana", 1.0), i("milk", 0.5)),
        t("Boiled Eggs", null, i("boiled-egg", 2.0), i("green-tea", 1.0, fixed = true)),
    ),
    MealSlot.DINNER to listOf(
        t("Moong Dal & Lauki", Region.NORTH, i("roti", 2.0), i("moong-dal", 1.0), i("lauki", 1.0), i("salad", 1.0, fixed = true)),
        t("Roti & Mixed Veg", null, i("roti", 2.0), i("mixed-veg", 1.0), i("dal-tadka", 0.5), i("curd", 0.5)),
        t("Phulka & Palak Paneer", Region.NORTH, i("phulka", 3.0), i("palak-paneer", 1.0), i("salad", 1.0, fixed = true)),
        t("Matar Paneer & Bhakri", Region.WEST, i("jowar-roti", 2.0), i("matar-paneer", 1.0), i("salad", 1.0, fixed = true)),
        t("Light Khichdi", null, i("khichdi", 1.5), i("curd", 1.0), i("salad", 1.0, fixed = true)),
        t("Dosa Night", Region.SOUTH, i("dosa", 2.0), i("sambar", 1.0), i("coconut-chutney", 1.0, fixed = true)),
        t("Uttapam & Sambar", Region.SOUTH, i("uttapam", 2.0), i("sambar", 1.0)),
        t("Curd Rice & Poriyal", Region.SOUTH, i("curd-rice", 1.0), i("beans-poriyal", 1.0)),
        t("Roti & Aloo Methi", Region.NORTH, i("roti", 2.0), i("methi-aloo", 1.0), i("moong-dal", 0.5)),
        t("Soya & Mushroom", null, i("phulka", 3.0), i("mushroom-masala", 1.0), i("soya-curry", 0.5)),
        t("Dal Bhaat Light", Region.EAST, i("rice", 1.0), i("moong-dal", 1.0), i("baingan-bharta", 0.5)),
        t("Egg Bhurji & Dal", null, i("roti", 2.0), i("egg-bhurji", 1.0), i("moong-dal", 0.5)),
        t("Tandoori Chicken Plate", Region.NORTH, i("roti", 2.0), i("tandoori-chicken", 1.0), i("salad", 1.0, fixed = true)),
        t("Grilled Chicken Bowl", null, i("brown-rice", 1.0), i("grilled-chicken", 1.5), i("mixed-veg", 1.0)),
        t("Fish Curry & Roti", Region.EAST, i("roti", 2.0), i("fish-curry", 1.0), i("salad", 1.0, fixed = true)),
    ),
)
