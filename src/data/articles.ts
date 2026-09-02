export type Article = {
  id: string;
  title: string;
  emoji: string;
  minutes: number;
  summary: string;
  body: string[];
};

export type ArticleCategory = {
  id: string;
  title: string;
  tint: string;
  fg: string;
  articles: Article[];
};

const a = (
  id: string, title: string, emoji: string, minutes: number, summary: string, body: string[]
): Article => ({ id, title, emoji, minutes, summary, body });

export const CATEGORIES: ArticleCategory[] = [
  {
    id: 'beauty',
    title: 'Beauty & Skin',
    tint: '#FDE7E9',
    fg: '#B3261E',
    articles: [
      a('glow', 'Miracle Juices for Glowing Skin', '🥤', 4,
        'Four blends that do more for your skin than any serum.',
        [
          'Skin is the last organ to receive the water you drink, which is why hydration shows up there last — and fades there first.',
          'Carrot and orange: beta-carotene converts to vitamin A, which supports cell turnover. Blend two carrots, one orange and a thumb of ginger.',
          'Cucumber and mint: roughly 95% water, plus silica, which the body uses in collagen production.',
          'Beetroot and berry: nitrates widen blood vessels slightly, so more oxygen reaches the skin surface.',
          'Treat juice as a supplement to water, not a replacement. The sugar load of three glasses of juice is not trivial.',
        ]),
      a('hotcold', 'Hot and Cold Water Benefits for Skin', '🌡️', 3,
        'Which temperature actually helps, and when.',
        [
          'Warm water loosens sebum and makes cleansers work better. Hot water strips the lipid barrier and leaves skin tight.',
          'Cold water briefly constricts surface vessels, which reduces puffiness — the effect is real but short-lived.',
          'The practical routine: cleanse with lukewarm, finish with a cool rinse, moisturise while skin is still damp.',
          'Drinking water at either temperature hydrates identically. The temperature preference is about comfort, not absorption.',
        ]),
      a('green', 'Green Tea for Clearer Skin', '🍵', 3,
        'The polyphenol case, minus the hype.',
        [
          'Green tea carries EGCG, a polyphenol studied for its anti-inflammatory effect on sebum production.',
          'Two to three cups a day is the range used in most studies. Beyond that the caffeine cost outweighs the benefit.',
          'Brew at 80°C for two minutes. Boiling water scalds the leaf and pulls out bitter tannins.',
          'Counts as hydration: tea is about 95% water, and the mild diuretic effect of its caffeine does not cancel that out.',
        ]),
    ],
  },
  {
    id: 'selfcare',
    title: 'Self-care',
    tint: '#DCEAFE',
    fg: '#12417E',
    articles: [
      a('bedtime', 'Bedtime Drinks for Better Sleep', '🌙', 4,
        'What to sip in the last hour of the day.',
        [
          'Chamomile contains apigenin, which binds to receptors involved in initiating sleep. It is the most evidenced of the bedtime teas.',
          'Tart cherry juice provides a small dose of natural melatonin — one glass, not more, given the sugar.',
          'Warm milk works largely through ritual and comfort, and there is nothing wrong with that.',
          'Stop drinking roughly an hour before bed so your bladder does not undo the work.',
        ]),
      a('alcohol', 'Impact of Alcohol on Your Body', '🍺', 5,
        'Why one night out costs you two days of hydration.',
        [
          'Alcohol suppresses vasopressin, the hormone that tells your kidneys to hold water. You lose roughly 100 ml of fluid per standard drink beyond what you drank.',
          'That deficit, not the alcohol itself, drives most of a hangover: headache, fatigue and dry mouth are dehydration signals.',
          'Alternate each drink with a glass of water, and drink 500 ml before sleeping.',
          'In Sipwell, alcoholic drinks are logged with a low or negative hydration factor, so your daily total reflects the real balance.',
        ]),
      a('herbal', 'Herbal Teas for Better Mental Health', '🌿', 3,
        'Small rituals with measurable effects.',
        [
          'Lemon balm has been shown in small trials to reduce self-reported anxiety within a few hours.',
          'Peppermint improves alertness without caffeine — useful in the afternoon dip.',
          'The ritual matters as much as the plant: a five-minute pause with a warm cup is a genuine intervention.',
        ]),
    ],
  },
  {
    id: 'lifestyle',
    title: 'Healthy Lifestyle',
    tint: '#DDDDF7',
    fg: '#312E81',
    articles: [
      a('fatburn', 'Top Fat-Burning Drinks for Weight Loss', '🔥', 4,
        'What the evidence supports, and what it does not.',
        [
          'Plain water before meals reduces intake modestly — around 75 fewer calories per meal in controlled trials.',
          'Green tea raises energy expenditure by roughly 3-4%. Real, but small: about 60-80 calories a day.',
          'Black coffee before exercise increases fat oxidation during the session.',
          'No drink burns fat on its own. These are edges of a few percent on top of a calorie deficit.',
        ]),
      a('fasting', 'What to Drink During Fasting?', '⏰', 4,
        'The line between fasting and breaking it.',
        [
          'Safe: water, sparkling water, black coffee, plain tea. Effectively zero calories, no insulin response.',
          'Breaks the fast: anything with sugar, milk, cream, or more than a token amount of calories.',
          'Grey area: a splash of lemon, a pinch of salt in water. Negligible calories, and both help with electrolytes.',
          'Fasting increases fluid loss, because you also stop getting the ~20% of daily water that normally comes from food.',
        ]),
      a('losew', 'Drink Water to Lose Weight, Top Tips', '⚖️', 3,
        'Five habits that stack up.',
        [
          'A glass on waking, before coffee. You are mildly dehydrated after eight hours without fluid.',
          'A glass 30 minutes before each meal.',
          'Swap one sweetened drink a day for sparkling water — that alone is often 150 calories.',
          'Thirst is routinely mistaken for hunger. Drink first, wait ten minutes, then decide.',
          'Keep a bottle in your line of sight. Visibility beats willpower.',
        ]),
    ],
  },
  {
    id: 'cardio',
    title: 'Cardiovascular Health',
    tint: '#D5EEF7',
    fg: '#0E5A72',
    articles: [
      a('bloodsugar', 'Drink Water for Healthy Blood Sugar', '💉', 4,
        'Hydration and glucose are more linked than most people expect.',
        [
          'Low fluid volume concentrates blood glucose and raises vasopressin, which pushes the liver to release more glucose.',
          'Large cohort studies associate drinking under 500 ml a day with a meaningfully higher risk of developing high blood sugar.',
          'Water is the only drink with no glycaemic effect at all — the free baseline.',
          'If you take diabetes medication, changes in fluid intake can affect it. Talk to your doctor before making a big shift.',
        ]),
      a('diabetic', 'Best and Worst Drinks for Diabetics', '🥤', 4,
        'A short list worth memorising.',
        [
          'Best: water, sparkling water, unsweetened tea, black coffee, and milk in measured amounts.',
          'Worst: soda, sweetened juice, energy drinks, and sweetened coffee drinks — fast sugar with no fibre to slow it.',
          'Fruit juice is not a health food here. A glass of orange juice raises blood glucose about as fast as a soft drink.',
          'Alcohol can cause delayed low blood sugar hours later. Never drink it on an empty stomach.',
        ]),
      a('heart', 'Best Times to Prevent Heart Disease', '❤️', 3,
        'Timing your intake around cardiac load.',
        [
          'Blood is thickest in the early morning, which is also when cardiac events cluster. A glass on waking is sensible.',
          'Drink before, not only during, exercise — arriving hydrated lowers cardiac strain.',
          'Two hours before bed, so overnight blood viscosity stays lower without wrecking your sleep.',
          'Steady sipping beats large boluses. Your kidneys can only process around 800 ml an hour.',
        ]),
    ],
  },
  {
    id: 'basics',
    title: 'Hydration Basics',
    tint: '#DFF3E6',
    fg: '#14532D',
    articles: [
      a('howmuch', 'How Much Water Do You Actually Need?', '💧', 4,
        'Where the "eight glasses" rule came from, and what to use instead.',
        [
          'The eight-glasses rule has no strong evidence behind it. It survives because it is easy to remember.',
          'A better estimate is 30-40 ml per kilogram of body weight, which is what Sipwell uses for your daily target.',
          'Add roughly 500-700 ml for each hour of hard exercise, and more in heat.',
          'About a fifth of your daily water comes from food. Your drinking target already accounts for that.',
          'Urine colour is the simplest check: pale straw is right, dark amber means drink more, fully clear means you can ease off.',
        ]),
      a('signs', 'Seven Signs You Are Dehydrated', '🚨', 3,
        'Most of them are not thirst.',
        [
          'Thirst is a late signal — you are already down about 2% of body water by the time you feel it.',
          'Afternoon headache, difficulty concentrating, and irritability usually arrive first.',
          'Dark urine, infrequent bathroom trips, dry lips, and skin that is slow to spring back when pinched.',
          'Fatigue that coffee does not fix is often a fluid problem, not a sleep problem.',
        ]),
      a('morning', 'Why Morning Water Matters Most', '🌅', 3,
        'The one habit with the best return.',
        [
          'You lose 300-500 ml overnight through breathing and perspiration, with nothing coming in to replace it.',
          'A 400-500 ml glass on waking restores that before your first coffee, which is mildly diuretic.',
          'It also front-loads your daily total, so you are not trying to catch up at 10pm.',
          'Set it next to your bed the night before. The habit forms around the cue, not the intention.',
        ]),
    ],
  },
];

export const ALL_ARTICLES: Article[] = CATEGORIES.flatMap((c) => c.articles);
export const articleById = (id: string) => ALL_ARTICLES.find((x) => x.id === id);
export const categoryOf = (id: string) => CATEGORIES.find((c) => c.articles.some((x) => x.id === id));
