package com.rotitrack.app.data

data class Article(val id: String, val title: String, val emoji: String, val minutes: Int, val summary: String, val body: List<String>)

data class ArticleCategory(val title: String, val tint: Long, val fg: Long, val articles: List<Article>)

val ARTICLES: List<ArticleCategory> = listOf(
    ArticleCategory(
        "Indian Diet", 0xFFFFF1DC, 0xFF8A4B00,
        listOf(
            Article(
                "thali", "Building a Balanced Thali", "🍱", 4,
                "The plate method, adapted for dal-roti-sabzi.",
                listOf(
                    "Half the plate: sabzi and salad. Non-starchy vegetables bring fibre, vitamins and volume for very few calories.",
                    "A quarter: protein — dal, chole, rajma, paneer, curd, eggs, fish or chicken. Most Indian meals under-deliver here.",
                    "A quarter: grains — two rotis or one katori of rice, not both in large amounts. Millets like bajra, jowar and ragi count.",
                    "A small katori of curd or a glass of chaas rounds it off and helps digestion.",
                    "Oil is the silent extra: one tablespoon of ghee or oil is about 120 kcal. Measure the tadka for a week and you will see.",
                ),
            ),
            Article(
                "protein-veg", "Protein for Vegetarians", "🫘", 4,
                "Hitting your target without meat.",
                listOf(
                    "Aim for roughly 0.8–1 g of protein per kg of body weight — around 50–60 g for most adults.",
                    "Dal alone is not enough: one katori gives about 7–9 g. Pair it with curd, paneer, sprouts or soya chunks.",
                    "Combining cereal and pulse (dal-chawal, khichdi, idli-sambar) completes the amino-acid profile — the tradition was right.",
                    "Easy upgrades: besan or moong chilla for breakfast, roasted chana as a snack, a bowl of curd with lunch.",
                    "Soya chunks are the densest plant option: 50 g dry is about 26 g protein.",
                ),
            ),
            Article(
                "millets", "Why Millets Are Back", "🌾", 3,
                "Bajra, jowar, ragi and friends.",
                listOf(
                    "Millets have a lower glycaemic response than polished rice or maida, so energy is released more slowly.",
                    "Ragi is one of the richest plant sources of calcium; bajra is high in iron.",
                    "Start by swapping one meal a day: ragi dosa for breakfast, jowar bhakri at lunch, or bajra roti in winter.",
                    "Millets need more water in cooking and pair well with ghee and curd, which also help absorption of their minerals.",
                ),
            ),
            Article(
                "festive", "Eating Well Through Festivals", "🪔", 3,
                "Enjoy the mithai without undoing a month.",
                listOf(
                    "A single gulab jamun is about 150 kcal; a plate of festive snacks can easily pass 800.",
                    "Keep regular meals regular. Skipping lunch to \"save room\" usually leads to overeating in the evening.",
                    "Pick the sweets you truly love and skip the ones you eat out of habit.",
                    "Walk after big meals — even 15 minutes blunts the post-meal blood sugar spike.",
                    "Drink water between snacks. Festive days are often salty, sweet and short on fluids.",
                ),
            ),
        ),
    ),
    ArticleCategory(
        "Beauty & Skin", 0xFFFDE7E9, 0xFFB3261E,
        listOf(
            Article(
                "glow", "Miracle Juices for Glowing Skin", "🥤", 4,
                "Four blends that do more for your skin than any serum.",
                listOf(
                    "Skin is the last organ to receive the water you drink, which is why hydration shows up there last — and fades there first.",
                    "Carrot and orange: beta-carotene converts to vitamin A, which supports cell turnover. Blend two carrots, one orange and a thumb of ginger.",
                    "Cucumber and mint: roughly 95% water, plus silica, which the body uses in collagen production.",
                    "Beetroot and berry: nitrates widen blood vessels slightly, so more oxygen reaches the skin surface.",
                    "Treat juice as a supplement to water, not a replacement. The sugar load of three glasses of juice is not trivial.",
                ),
            ),
            Article(
                "hotcold", "Hot and Cold Water Benefits for Skin", "🌡️", 3,
                "Which temperature actually helps, and when.",
                listOf(
                    "Warm water loosens sebum and makes cleansers work better. Hot water strips the lipid barrier and leaves skin tight.",
                    "Cold water briefly constricts surface vessels, which reduces puffiness — the effect is real but short-lived.",
                    "The practical routine: cleanse with lukewarm, finish with a cool rinse, moisturise while skin is still damp.",
                    "Drinking water at either temperature hydrates identically. The temperature preference is about comfort, not absorption.",
                ),
            ),
            Article(
                "green", "Green Tea for Clearer Skin", "🍵", 3,
                "The polyphenol case, minus the hype.",
                listOf(
                    "Green tea carries EGCG, a polyphenol studied for its anti-inflammatory effect on sebum production.",
                    "Two to three cups a day is the range used in most studies. Beyond that the caffeine cost outweighs the benefit.",
                    "Brew at 80°C for two minutes. Boiling water scalds the leaf and pulls out bitter tannins.",
                    "Counts as hydration: tea is about 95% water, and the mild diuretic effect of its caffeine does not cancel that out.",
                ),
            ),
        ),
    ),
    ArticleCategory(
        "Self-care", 0xFFDCEAFE, 0xFF12417E,
        listOf(
            Article(
                "bedtime", "Bedtime Drinks for Better Sleep", "🌙", 4,
                "What to sip in the last hour of the day.",
                listOf(
                    "Chamomile contains apigenin, which binds to receptors involved in initiating sleep. It is the most evidenced of the bedtime teas.",
                    "Tart cherry juice provides a small dose of natural melatonin — one glass, not more, given the sugar.",
                    "Warm milk works largely through ritual and comfort, and there is nothing wrong with that.",
                    "Stop drinking roughly an hour before bed so your bladder does not undo the work.",
                ),
            ),
            Article(
                "alcohol", "Impact of Alcohol on Your Body", "🍺", 5,
                "Why one night out costs you two days of hydration.",
                listOf(
                    "Alcohol suppresses vasopressin, the hormone that tells your kidneys to hold water. You lose roughly 100 ml of fluid per standard drink beyond what you drank.",
                    "That deficit, not the alcohol itself, drives most of a hangover: headache, fatigue and dry mouth are dehydration signals.",
                    "Alternate each drink with a glass of water, and drink 500 ml before sleeping.",
                    "That is why Roti Track has no alcohol option: it does not count toward your water goal.",
                ),
            ),
            Article(
                "herbal", "Herbal Teas for Better Mental Health", "🌿", 3,
                "Small rituals with measurable effects.",
                listOf(
                    "Lemon balm has been shown in small trials to reduce self-reported anxiety within a few hours.",
                    "Peppermint improves alertness without caffeine — useful in the afternoon dip.",
                    "The ritual matters as much as the plant: a five-minute pause with a warm cup is a genuine intervention.",
                ),
            ),
        ),
    ),
    ArticleCategory(
        "Healthy Lifestyle", 0xFFDDDDF7, 0xFF312E81,
        listOf(
            Article(
                "fatburn", "Top Fat-Burning Drinks for Weight Loss", "🔥", 4,
                "What the evidence supports, and what it does not.",
                listOf(
                    "Plain water before meals reduces intake modestly — around 75 fewer calories per meal in controlled trials.",
                    "Green tea raises energy expenditure by roughly 3-4%. Real, but small: about 60-80 calories a day.",
                    "Black coffee before exercise increases fat oxidation during the session.",
                    "No drink burns fat on its own. These are edges of a few percent on top of a calorie deficit.",
                ),
            ),
            Article(
                "fasting", "What to Drink During Fasting?", "⏰", 4,
                "The line between fasting and breaking it.",
                listOf(
                    "Safe: water, sparkling water, black coffee, plain tea. Effectively zero calories, no insulin response.",
                    "Breaks the fast: anything with sugar, milk, cream, or more than a token amount of calories.",
                    "Grey area: a splash of lemon, a pinch of salt in water. Negligible calories, and both help with electrolytes.",
                    "Fasting increases fluid loss, because you also stop getting the ~20% of daily water that normally comes from food.",
                ),
            ),
            Article(
                "losew", "Drink Water to Lose Weight, Top Tips", "⚖️", 3,
                "Five habits that stack up.",
                listOf(
                    "A glass on waking, before coffee. You are mildly dehydrated after eight hours without fluid.",
                    "A glass 30 minutes before each meal.",
                    "Swap one sweetened drink a day for sparkling water — that alone is often 150 calories.",
                    "Thirst is routinely mistaken for hunger. Drink first, wait ten minutes, then decide.",
                    "Keep a bottle in your line of sight. Visibility beats willpower.",
                ),
            ),
        ),
    ),
    ArticleCategory(
        "Cardiovascular Health", 0xFFD5EEF7, 0xFF0E5A72,
        listOf(
            Article(
                "bloodsugar", "Drink Water for Healthy Blood Sugar", "💉", 4,
                "Hydration and glucose are more linked than most people expect.",
                listOf(
                    "Low fluid volume concentrates blood glucose and raises vasopressin, which pushes the liver to release more glucose.",
                    "Large cohort studies associate drinking under 500 ml a day with a meaningfully higher risk of developing high blood sugar.",
                    "Water is the only drink with no glycaemic effect at all — the free baseline.",
                    "If you take diabetes medication, changes in fluid intake can affect it. Talk to your doctor before making a big shift.",
                ),
            ),
            Article(
                "diabetic", "Best and Worst Drinks for Diabetics", "🥤", 4,
                "A short list worth memorising.",
                listOf(
                    "Best: water, sparkling water, unsweetened tea, black coffee, and milk in measured amounts.",
                    "Worst: soda, sweetened juice, energy drinks, and sweetened coffee drinks — fast sugar with no fibre to slow it.",
                    "Fruit juice is not a health food here. A glass of orange juice raises blood glucose about as fast as a soft drink.",
                    "Alcohol can cause delayed low blood sugar hours later. Never drink it on an empty stomach.",
                ),
            ),
            Article(
                "heart", "Best Times to Prevent Heart Disease", "❤️", 3,
                "Timing your intake around cardiac load.",
                listOf(
                    "Blood is thickest in the early morning, which is also when cardiac events cluster. A glass on waking is sensible.",
                    "Drink before, not only during, exercise — arriving hydrated lowers cardiac strain.",
                    "Two hours before bed, so overnight blood viscosity stays lower without wrecking your sleep.",
                    "Steady sipping beats large boluses. Your kidneys can only process around 800 ml an hour.",
                ),
            ),
        ),
    ),
    ArticleCategory(
        "Hydration Basics", 0xFFDFF3E6, 0xFF14532D,
        listOf(
            Article(
                "howmuch", "How Much Water Do You Actually Need?", "💧", 4,
                "Where the \"eight glasses\" rule came from, and what to use instead.",
                listOf(
                    "The eight-glasses rule has no strong evidence behind it. It survives because it is easy to remember.",
                    "A better estimate is 30-40 ml per kilogram of body weight, which is what Roti Track uses for your daily target.",
                    "Add roughly 500-700 ml for each hour of hard exercise, and more in heat.",
                    "About a fifth of your daily water comes from food. Your drinking target already accounts for that.",
                    "Urine colour is the simplest check: pale straw is right, dark amber means drink more, fully clear means you can ease off.",
                ),
            ),
            Article(
                "signs", "Seven Signs You Are Dehydrated", "🚨", 3,
                "Most of them are not thirst.",
                listOf(
                    "Thirst is a late signal — you are already down about 2% of body water by the time you feel it.",
                    "Afternoon headache, difficulty concentrating, and irritability usually arrive first.",
                    "Dark urine, infrequent bathroom trips, dry lips, and skin that is slow to spring back when pinched.",
                    "Fatigue that coffee does not fix is often a fluid problem, not a sleep problem.",
                ),
            ),
            Article(
                "morning", "Why Morning Water Matters Most", "🌅", 3,
                "The one habit with the best return.",
                listOf(
                    "You lose 300-500 ml overnight through breathing and perspiration, with nothing coming in to replace it.",
                    "A 400-500 ml glass on waking restores that before your first coffee, which is mildly diuretic.",
                    "It also front-loads your daily total, so you are not trying to catch up at 10pm.",
                    "Set it next to your bed the night before. The habit forms around the cue, not the intention.",
                ),
            ),
        ),
    ),
)

fun articleById(id: String): Article? = ARTICLES.flatMap { it.articles }.firstOrNull { it.id == id }
