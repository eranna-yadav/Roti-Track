import type { Food, FoodCategory } from '@/lib/diet-types';

/**
 * Common Indian foods with per-serving nutrition. Values are typical home-style
 * estimates collated from IFCT 2017 and standard recipe analyses — good enough
 * for day-to-day tracking, not for clinical use.
 *
 * `unit: 'piece'` servings are counted whole (rotis, idlis); `'portion'` ones
 * can be halved (a bowl of dal).
 */
type Row = [
  id: string, name: string, emoji: string, category: FoodCategory, serving: string,
  kcal: number, protein: number, carbs: number, fat: number,
  diet?: Food['diet'], unit?: Food['unit'],
];

const rows: Row[] = [
  // breakfast
  ['poha', 'Poha', '🍛', 'breakfast', '1 plate (150 g)', 250, 5, 45, 6],
  ['upma', 'Upma', '🥣', 'breakfast', '1 bowl (200 g)', 290, 7, 42, 10],
  ['idli', 'Idli', '⚪', 'breakfast', '1 piece', 58, 2, 12, 0.2, 'veg', 'piece'],
  ['dosa', 'Plain Dosa', '🫓', 'breakfast', '1 dosa', 170, 4, 28, 5, 'veg', 'piece'],
  ['masala-dosa', 'Masala Dosa', '🫓', 'breakfast', '1 dosa', 390, 8, 52, 16, 'veg', 'piece'],
  ['rava-dosa', 'Rava Dosa', '🫓', 'breakfast', '1 dosa', 220, 4, 30, 9, 'veg', 'piece'],
  ['medu-vada', 'Medu Vada', '🍩', 'breakfast', '1 piece', 140, 4, 13, 8, 'veg', 'piece'],
  ['uttapam', 'Uttapam', '🥞', 'breakfast', '1 uttapam', 210, 5, 34, 6, 'veg', 'piece'],
  ['pesarattu', 'Pesarattu', '🥞', 'breakfast', '1 piece', 180, 9, 26, 5, 'veg', 'piece'],
  ['ragi-dosa', 'Ragi Dosa', '🫓', 'breakfast', '1 dosa', 140, 3, 24, 4, 'veg', 'piece'],
  ['appam', 'Appam', '🥞', 'breakfast', '1 appam', 120, 2, 23, 2, 'veg', 'piece'],
  ['pongal', 'Ven Pongal', '🍚', 'breakfast', '1 bowl (200 g)', 300, 8, 42, 11],
  ['aloo-paratha', 'Aloo Paratha', '🫓', 'breakfast', '1 paratha', 290, 6, 40, 12, 'veg', 'piece'],
  ['paneer-paratha', 'Paneer Paratha', '🫓', 'breakfast', '1 paratha', 320, 12, 34, 15, 'veg', 'piece'],
  ['thepla', 'Methi Thepla', '🫓', 'breakfast', '1 thepla', 120, 3, 16, 5, 'veg', 'piece'],
  ['besan-chilla', 'Besan Chilla', '🥞', 'breakfast', '1 chilla', 150, 7, 18, 5, 'veg', 'piece'],
  ['moong-chilla', 'Moong Dal Chilla', '🥞', 'breakfast', '1 chilla', 130, 8, 16, 4, 'veg', 'piece'],
  ['dhokla', 'Khaman Dhokla', '🧽', 'breakfast', '4 pieces (100 g)', 160, 6, 22, 5],
  ['oats', 'Masala Oats', '🥣', 'breakfast', '1 bowl', 220, 7, 34, 6],
  ['oats-milk', 'Oats Porridge with Milk', '🥣', 'breakfast', '1 bowl', 230, 9, 34, 6],
  ['semiya-upma', 'Semiya Upma', '🍝', 'breakfast', '1 bowl', 250, 6, 42, 7],
  ['sabudana-khichdi', 'Sabudana Khichdi', '🍚', 'breakfast', '1 bowl (150 g)', 330, 4, 52, 12],
  ['puri', 'Puri', '🫓', 'breakfast', '1 puri', 100, 2, 12, 5, 'veg', 'piece'],
  ['bhatura', 'Bhatura', '🫓', 'breakfast', '1 bhatura', 250, 5, 30, 12, 'veg', 'piece'],
  ['bread', 'Brown Bread', '🍞', 'breakfast', '1 slice', 75, 3, 13, 1, 'veg', 'piece'],
  ['boiled-egg', 'Boiled Egg', '🥚', 'breakfast', '1 egg', 78, 6.3, 0.6, 5.3, 'egg', 'piece'],
  ['omelette', 'Masala Omelette', '🍳', 'breakfast', '2 eggs', 190, 13, 2, 15, 'egg'],
  ['egg-bhurji', 'Egg Bhurji', '🍳', 'breakfast', '1 bowl (2 eggs)', 220, 14, 4, 16, 'egg'],
  ['coconut-chutney', 'Coconut Chutney', '🥥', 'breakfast', '2 tbsp (30 g)', 70, 1, 3, 6],
  ['sprouts', 'Moong Sprouts Salad', '🥗', 'breakfast', '1 bowl', 110, 8, 18, 1],

  // breads
  ['roti', 'Roti / Chapati', '🫓', 'breads', '1 roti (40 g)', 110, 3, 18, 3, 'veg', 'piece'],
  ['phulka', 'Phulka (no oil)', '🫓', 'breads', '1 phulka', 70, 2.5, 14, 0.5, 'veg', 'piece'],
  ['bajra-roti', 'Bajra Roti', '🫓', 'breads', '1 roti', 120, 3.5, 21, 2.5, 'veg', 'piece'],
  ['jowar-roti', 'Jowar Bhakri', '🫓', 'breads', '1 roti', 110, 3.5, 22, 1, 'veg', 'piece'],
  ['makki-roti', 'Makki di Roti', '🫓', 'breads', '1 roti', 150, 3, 26, 4.5, 'veg', 'piece'],
  ['missi-roti', 'Missi Roti', '🫓', 'breads', '1 roti', 140, 5, 20, 4.5, 'veg', 'piece'],
  ['paratha', 'Plain Paratha', '🫓', 'breads', '1 paratha', 200, 4, 26, 9, 'veg', 'piece'],
  ['naan', 'Naan', '🫓', 'breads', '1 naan', 260, 8, 45, 5, 'veg', 'piece'],
  ['butter-naan', 'Butter Naan', '🫓', 'breads', '1 naan', 320, 8, 45, 11, 'veg', 'piece'],
  ['pav', 'Pav', '🍞', 'breads', '1 pav', 110, 3, 20, 2, 'veg', 'piece'],

  // rice
  ['rice', 'Steamed Rice', '🍚', 'rice', '1 bowl (150 g cooked)', 195, 4, 43, 0.4],
  ['brown-rice', 'Brown Rice', '🍚', 'rice', '1 bowl (150 g cooked)', 170, 4, 36, 1.4],
  ['jeera-rice', 'Jeera Rice', '🍚', 'rice', '1 bowl', 240, 4, 44, 5],
  ['veg-pulao', 'Veg Pulao', '🍚', 'rice', '1 bowl', 270, 5, 45, 8],
  ['veg-biryani', 'Veg Biryani', '🍛', 'rice', '1 plate (250 g)', 400, 9, 60, 13],
  ['curd-rice', 'Curd Rice', '🍚', 'rice', '1 bowl', 230, 6, 36, 7],
  ['lemon-rice', 'Lemon Rice', '🍋', 'rice', '1 bowl', 260, 4, 45, 7],
  ['khichdi', 'Moong Dal Khichdi', '🍲', 'rice', '1 bowl', 220, 8, 36, 5],
  ['bisi-bele-bath', 'Bisi Bele Bath', '🍲', 'rice', '1 bowl', 300, 9, 45, 9],
  ['chicken-biryani', 'Chicken Biryani', '🍛', 'rice', '1 plate (250 g)', 500, 25, 55, 18, 'nonveg'],
  ['egg-biryani', 'Egg Biryani', '🍛', 'rice', '1 plate (250 g)', 440, 16, 58, 15, 'egg'],

  // dals & legumes
  ['dal-tadka', 'Dal Tadka', '🥣', 'dal', '1 bowl (150 g)', 180, 9, 22, 6],
  ['moong-dal', 'Yellow Moong Dal', '🥣', 'dal', '1 bowl (150 g)', 150, 9, 20, 4],
  ['chana-dal', 'Chana Dal', '🥣', 'dal', '1 bowl (150 g)', 190, 10, 26, 5],
  ['dal-makhani', 'Dal Makhani', '🥣', 'dal', '1 bowl (150 g)', 280, 10, 26, 15],
  ['sambar', 'Sambar', '🍲', 'dal', '1 bowl (150 ml)', 130, 6, 18, 4],
  ['rasam', 'Rasam', '🍵', 'dal', '1 bowl (150 ml)', 60, 2, 9, 2],
  ['rajma', 'Rajma Masala', '🫘', 'dal', '1 bowl (150 g)', 210, 10, 30, 6],
  ['chole', 'Chole / Chana Masala', '🫘', 'dal', '1 bowl (150 g)', 240, 10, 32, 8],
  ['kadhi', 'Kadhi', '🥣', 'dal', '1 bowl (150 g)', 160, 6, 14, 9],
  ['usal', 'Matki Usal', '🫘', 'dal', '1 bowl (150 g)', 200, 11, 28, 5],

  // sabzi & veg curries
  ['aloo-gobi', 'Aloo Gobi', '🥦', 'sabzi', '1 bowl (150 g)', 170, 4, 18, 9],
  ['bhindi', 'Bhindi Masala', '🌿', 'sabzi', '1 bowl (150 g)', 150, 3, 12, 10],
  ['palak-paneer', 'Palak Paneer', '🥬', 'sabzi', '1 bowl (150 g)', 270, 13, 9, 20],
  ['paneer-butter', 'Paneer Butter Masala', '🧀', 'sabzi', '1 bowl (150 g)', 350, 14, 12, 28],
  ['matar-paneer', 'Matar Paneer', '🧀', 'sabzi', '1 bowl (150 g)', 260, 12, 14, 17],
  ['paneer-bhurji', 'Paneer Bhurji', '🧀', 'sabzi', '1 bowl (100 g)', 250, 15, 6, 19],
  ['mixed-veg', 'Mixed Veg Curry', '🥕', 'sabzi', '1 bowl (150 g)', 150, 4, 15, 8],
  ['baingan-bharta', 'Baingan Bharta', '🍆', 'sabzi', '1 bowl (150 g)', 140, 3, 13, 9],
  ['aloo-matar', 'Aloo Matar', '🥔', 'sabzi', '1 bowl (150 g)', 180, 5, 24, 7],
  ['lauki', 'Lauki Sabzi', '🥒', 'sabzi', '1 bowl (150 g)', 90, 2, 10, 5],
  ['poriyal', 'Cabbage Poriyal', '🥬', 'sabzi', '1 bowl (150 g)', 110, 3, 10, 7],
  ['beans-poriyal', 'Beans Poriyal', '🫛', 'sabzi', '1 bowl (150 g)', 120, 4, 11, 7],
  ['avial', 'Avial', '🥥', 'sabzi', '1 bowl (150 g)', 180, 4, 14, 12],
  ['soya-curry', 'Soya Chunks Curry', '🍲', 'sabzi', '1 bowl (150 g)', 200, 18, 14, 8],
  ['mushroom-masala', 'Mushroom Masala', '🍄', 'sabzi', '1 bowl (150 g)', 150, 5, 10, 10],
  ['methi-aloo', 'Aloo Methi', '🥔', 'sabzi', '1 bowl (150 g)', 160, 4, 20, 8],
  ['pav-bhaji-bhaji', 'Pav Bhaji (bhaji only)', '🍲', 'sabzi', '1 bowl (150 g)', 230, 5, 26, 12],
  ['salad', 'Kachumber Salad', '🥗', 'sabzi', '1 bowl', 40, 1.5, 8, 0.3],

  // non-veg
  ['chicken-curry', 'Chicken Curry', '🍗', 'nonveg', '1 bowl (150 g)', 260, 24, 8, 15, 'nonveg'],
  ['butter-chicken', 'Butter Chicken', '🍗', 'nonveg', '1 bowl (150 g)', 380, 26, 10, 26, 'nonveg'],
  ['tandoori-chicken', 'Tandoori Chicken', '🍗', 'nonveg', '2 pieces (200 g)', 260, 36, 4, 11, 'nonveg'],
  ['chicken-tikka', 'Chicken Tikka', '🍢', 'nonveg', '6 pieces (150 g)', 220, 30, 5, 9, 'nonveg'],
  ['grilled-chicken', 'Grilled Chicken Breast', '🍗', 'nonveg', '100 g', 165, 31, 0, 3.6, 'nonveg'],
  ['keema', 'Chicken Keema', '🍖', 'nonveg', '1 bowl (150 g)', 300, 22, 8, 20, 'nonveg'],
  ['mutton-curry', 'Mutton Curry', '🍖', 'nonveg', '1 bowl (150 g)', 340, 26, 6, 24, 'nonveg'],
  ['fish-curry', 'Fish Curry', '🐟', 'nonveg', '1 bowl (150 g)', 220, 22, 6, 12, 'nonveg'],
  ['fish-fry', 'Fish Fry', '🐟', 'nonveg', '1 piece (100 g)', 200, 20, 6, 11, 'nonveg', 'piece'],
  ['prawn-masala', 'Prawn Masala', '🦐', 'nonveg', '1 bowl (150 g)', 200, 22, 7, 10, 'nonveg'],
  ['egg-curry', 'Egg Curry', '🥚', 'nonveg', '1 bowl (2 eggs)', 240, 14, 8, 17, 'egg'],

  // dairy
  ['curd', 'Curd / Dahi', '🥛', 'dairy', '1 bowl (150 g)', 90, 5, 7, 5],
  ['raita', 'Boondi / Veg Raita', '🥣', 'dairy', '1 bowl (150 g)', 100, 4, 8, 5],
  ['paneer', 'Paneer (raw)', '🧀', 'dairy', '100 g', 265, 18, 1.2, 21],
  ['milk', 'Toned Milk', '🥛', 'dairy', '1 glass (250 ml)', 145, 8, 12, 7.5],
  ['haldi-doodh', 'Haldi Doodh', '🥛', 'dairy', '1 glass (250 ml)', 160, 8, 14, 8],
  ['chaas', 'Chaas / Buttermilk', '🥛', 'dairy', '1 glass (250 ml)', 40, 2, 4, 1.5],
  ['lassi', 'Sweet Lassi', '🥤', 'dairy', '1 glass (250 ml)', 220, 7, 34, 6],
  ['ghee', 'Ghee', '🧈', 'dairy', '1 tsp (5 g)', 45, 0, 0, 5],

  // snacks
  ['samosa', 'Samosa', '🥟', 'snacks', '1 samosa', 260, 4, 28, 15, 'veg', 'piece'],
  ['pakora', 'Onion Pakora', '🧅', 'snacks', '5 pieces', 200, 4, 18, 13],
  ['kachori', 'Kachori', '🥟', 'snacks', '1 kachori', 190, 4, 20, 11, 'veg', 'piece'],
  ['pani-puri', 'Pani Puri', '🫧', 'snacks', '6 puris', 180, 3, 30, 5],
  ['bhel', 'Bhel Puri', '🥗', 'snacks', '1 plate', 200, 5, 32, 6],
  ['pav-bhaji', 'Pav Bhaji', '🍲', 'snacks', '1 plate (2 pav)', 450, 10, 60, 18],
  ['vada-pav', 'Vada Pav', '🍔', 'snacks', '1 vada pav', 290, 6, 38, 13, 'veg', 'piece'],
  ['roasted-chana', 'Roasted Chana', '🫘', 'snacks', '1 handful (30 g)', 110, 6, 17, 2],
  ['makhana', 'Roasted Makhana', '🍿', 'snacks', '1 cup (30 g)', 120, 3, 20, 2],
  ['nuts', 'Mixed Nuts', '🥜', 'snacks', '1 handful (30 g)', 180, 5, 7, 16],
  ['peanuts', 'Roasted Peanuts', '🥜', 'snacks', '1 handful (30 g)', 170, 7, 5, 14],
  ['murmura', 'Masala Murmura', '🍚', 'snacks', '1 bowl', 130, 3, 24, 3],
  ['khakhra', 'Khakhra', '🫓', 'snacks', '2 khakhras', 120, 4, 18, 4],
  ['sprouts-chaat', 'Sprouts Chaat', '🥗', 'snacks', '1 bowl', 140, 9, 22, 2],
  ['marie', 'Marie Biscuits', '🍪', 'snacks', '4 biscuits', 120, 2, 20, 3.5],
  ['sundal', 'Chana Sundal', '🫘', 'snacks', '1 bowl (100 g)', 150, 8, 22, 4],

  // sweets
  ['gulab-jamun', 'Gulab Jamun', '🟤', 'sweets', '1 piece', 150, 2, 22, 6, 'veg', 'piece'],
  ['rasgulla', 'Rasgulla', '⚪', 'sweets', '1 piece', 110, 2, 23, 1, 'veg', 'piece'],
  ['jalebi', 'Jalebi', '🌀', 'sweets', '2 pieces', 200, 1, 35, 7],
  ['kheer', 'Rice Kheer', '🍮', 'sweets', '1 bowl', 250, 7, 36, 9],
  ['besan-ladoo', 'Besan Ladoo', '🟡', 'sweets', '1 ladoo', 180, 3, 18, 11, 'veg', 'piece'],
  ['sooji-halwa', 'Sooji Halwa', '🍮', 'sweets', '1 bowl', 330, 4, 45, 15],
  ['gajar-halwa', 'Gajar Halwa', '🥕', 'sweets', '1 bowl', 300, 6, 38, 14],
  ['barfi', 'Kaju Barfi', '💠', 'sweets', '1 piece', 140, 3, 16, 7, 'veg', 'piece'],

  // beverages
  ['chai', 'Masala Chai', '☕', 'beverages', '1 cup', 90, 3, 12, 3.5],
  ['chai-nosugar', 'Chai (no sugar)', '☕', 'beverages', '1 cup', 50, 3, 4, 3],
  ['filter-coffee', 'Filter Coffee', '☕', 'beverages', '1 cup', 110, 3, 14, 4],
  ['black-coffee', 'Black Coffee', '☕', 'beverages', '1 cup', 5, 0.3, 0, 0],
  ['green-tea', 'Green Tea', '🍵', 'beverages', '1 cup', 2, 0, 0, 0],
  ['nimbu-pani', 'Nimbu Pani', '🍋', 'beverages', '1 glass', 60, 0, 15, 0],
  ['coconut-water', 'Coconut Water', '🥥', 'beverages', '1 glass (240 ml)', 45, 0.5, 9, 0.5],
  ['sugarcane', 'Sugarcane Juice', '🧃', 'beverages', '1 glass', 180, 0, 45, 0],
  ['mango-shake', 'Mango Shake', '🥭', 'beverages', '1 glass', 250, 6, 40, 7],

  // fruits
  ['banana', 'Banana', '🍌', 'fruits', '1 medium', 105, 1.3, 27, 0.4, 'veg', 'piece'],
  ['apple', 'Apple', '🍎', 'fruits', '1 medium', 95, 0.5, 25, 0.3, 'veg', 'piece'],
  ['mango', 'Mango', '🥭', 'fruits', '1 cup sliced', 100, 1.4, 25, 0.6],
  ['papaya', 'Papaya', '🧡', 'fruits', '1 cup', 60, 0.7, 15, 0.4],
  ['guava', 'Guava', '🍐', 'fruits', '1 medium', 70, 2.6, 14, 1, 'veg', 'piece'],
  ['orange', 'Orange', '🍊', 'fruits', '1 medium', 62, 1.2, 15, 0.2, 'veg', 'piece'],
  ['watermelon', 'Watermelon', '🍉', 'fruits', '1 cup', 46, 1, 12, 0.2],
  ['pomegranate', 'Pomegranate', '🔴', 'fruits', '½ cup arils', 72, 1.5, 16, 1],
  ['grapes', 'Grapes', '🍇', 'fruits', '1 cup', 104, 1, 27, 0.2],
  ['chikoo', 'Chikoo', '🟤', 'fruits', '1 medium', 80, 0.5, 20, 1, 'veg', 'piece'],
  ['dates', 'Dates', '🟫', 'fruits', '3 dates', 67, 0.6, 18, 0],
];

export const FOODS: Food[] = rows.map(
  ([id, name, emoji, category, serving, kcal, protein, carbs, fat, diet = 'veg', unit = 'portion']) => ({
    id, name, emoji, category, serving, kcal, protein, carbs, fat, diet, unit,
  })
);

export const FOOD_CATEGORIES: { key: FoodCategory; label: string }[] = [
  { key: 'breakfast', label: 'Breakfast' },
  { key: 'breads', label: 'Rotis' },
  { key: 'rice', label: 'Rice' },
  { key: 'dal', label: 'Dals' },
  { key: 'sabzi', label: 'Sabzi' },
  { key: 'nonveg', label: 'Non-veg' },
  { key: 'dairy', label: 'Dairy' },
  { key: 'snacks', label: 'Snacks' },
  { key: 'sweets', label: 'Sweets' },
  { key: 'beverages', label: 'Drinks' },
  { key: 'fruits', label: 'Fruits' },
];

const byId = new Map(FOODS.map((f) => [f.id, f]));
export const builtinFoodById = (id: string) => byId.get(id);
