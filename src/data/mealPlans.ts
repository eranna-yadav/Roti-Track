import type { MealSlot, Region } from '@/lib/diet-types';

/** [foodId, servings, fixed?] — fixed items (a cup of chai) are never scaled. */
export type PlanItem = [foodId: string, servings: number, fixed?: boolean];

export type MealTemplate = {
  name: string;
  region: Exclude<Region, 'all'> | 'any';
  items: PlanItem[];
};

export const SLOTS: { key: MealSlot; label: string; emoji: string; share: number; time: string }[] = [
  { key: 'breakfast', label: 'Breakfast', emoji: '🌅', share: 0.25, time: '8:00 AM' },
  { key: 'lunch', label: 'Lunch', emoji: '🍛', share: 0.35, time: '1:00 PM' },
  { key: 'snack', label: 'Evening Snack', emoji: '☕', share: 0.1, time: '5:00 PM' },
  { key: 'dinner', label: 'Dinner', emoji: '🌙', share: 0.3, time: '8:00 PM' },
];

export const slotMeta = (slot: MealSlot) => SLOTS.find((s) => s.key === slot) ?? SLOTS[0];

/**
 * Home-style thalis and plates. The planner picks one per slot and scales the
 * servings to hit that slot's share of the daily calorie target.
 */
export const TEMPLATES: Record<MealSlot, MealTemplate[]> = {
  breakfast: [
    { name: 'Kanda Poha & Chai', region: 'west', items: [['poha', 1], ['chai', 1, true]] },
    { name: 'Idli Sambar', region: 'south', items: [['idli', 3], ['sambar', 1], ['coconut-chutney', 1, true]] },
    { name: 'Dosa & Sambar', region: 'south', items: [['dosa', 2], ['sambar', 1], ['coconut-chutney', 1, true]] },
    { name: 'Rava Upma & Coffee', region: 'south', items: [['upma', 1], ['filter-coffee', 1, true]] },
    { name: 'Pesarattu', region: 'south', items: [['pesarattu', 2], ['coconut-chutney', 1, true]] },
    { name: 'Ragi Dosa & Sambar', region: 'south', items: [['ragi-dosa', 2], ['sambar', 1]] },
    { name: 'Aloo Paratha & Dahi', region: 'north', items: [['aloo-paratha', 1], ['curd', 1], ['chai-nosugar', 1, true]] },
    { name: 'Paneer Paratha', region: 'north', items: [['paneer-paratha', 1], ['curd', 0.5]] },
    { name: 'Besan Chilla & Curd', region: 'north', items: [['besan-chilla', 2], ['curd', 1]] },
    { name: 'Moong Dal Chilla', region: 'any', items: [['moong-chilla', 2], ['curd', 1]] },
    { name: 'Methi Thepla & Dahi', region: 'west', items: [['thepla', 2], ['curd', 1], ['chai', 1, true]] },
    { name: 'Khaman Dhokla', region: 'west', items: [['dhokla', 1.5], ['chai-nosugar', 1, true]] },
    { name: 'Oats Porridge & Banana', region: 'any', items: [['oats-milk', 1], ['banana', 1]] },
    { name: 'Masala Oats & Fruit', region: 'any', items: [['oats', 1], ['papaya', 1]] },
    { name: 'Sprouts & Milk', region: 'any', items: [['sprouts', 1], ['milk', 1], ['apple', 1]] },
    { name: 'Poha & Sprouts', region: 'east', items: [['poha', 1], ['sprouts', 0.5], ['chai-nosugar', 1, true]] },
    { name: 'Egg Omelette & Toast', region: 'any', items: [['omelette', 1], ['bread', 2], ['chai-nosugar', 1, true]] },
    { name: 'Boiled Eggs & Toast', region: 'any', items: [['boiled-egg', 2], ['bread', 2], ['orange', 1]] },
    { name: 'Egg Bhurji & Roti', region: 'north', items: [['egg-bhurji', 1], ['roti', 1]] },
    { name: 'Appam & Egg Curry', region: 'south', items: [['appam', 2], ['egg-curry', 0.5]] },
  ],
  lunch: [
    { name: 'Dal Roti Sabzi', region: 'north', items: [['roti', 2], ['dal-tadka', 1], ['aloo-gobi', 1], ['salad', 1, true], ['curd', 0.5]] },
    { name: 'Rajma Chawal', region: 'north', items: [['rice', 1], ['rajma', 1], ['salad', 1, true]] },
    { name: 'Chole & Roti', region: 'north', items: [['roti', 2], ['chole', 1], ['raita', 0.5], ['salad', 1, true]] },
    { name: 'Palak Paneer Thali', region: 'north', items: [['roti', 2], ['palak-paneer', 1], ['moong-dal', 0.5], ['salad', 1, true]] },
    { name: 'Sambar Rice Meal', region: 'south', items: [['rice', 1], ['sambar', 1], ['poriyal', 1], ['curd', 0.5]] },
    { name: 'Rasam Rice & Avial', region: 'south', items: [['rice', 1], ['rasam', 1], ['avial', 1], ['curd', 0.5]] },
    { name: 'Bisi Bele Bath', region: 'south', items: [['bisi-bele-bath', 1], ['raita', 0.5], ['salad', 1, true]] },
    { name: 'Gujarati Khichdi Kadhi', region: 'west', items: [['khichdi', 1], ['kadhi', 1], ['salad', 1, true]] },
    { name: 'Bajra Roti & Bharta', region: 'west', items: [['bajra-roti', 2], ['baingan-bharta', 1], ['chaas', 1, true]] },
    { name: 'Usal & Bhakri', region: 'west', items: [['jowar-roti', 2], ['usal', 1], ['salad', 1, true]] },
    { name: 'Dal Bhaat & Bhindi', region: 'east', items: [['rice', 1], ['moong-dal', 1], ['bhindi', 1], ['salad', 1, true]] },
    { name: 'Veg Pulao & Raita', region: 'any', items: [['veg-pulao', 1], ['raita', 1], ['moong-dal', 0.5]] },
    { name: 'Brown Rice Power Bowl', region: 'any', items: [['brown-rice', 1], ['chana-dal', 1], ['mixed-veg', 1], ['curd', 0.5]] },
    { name: 'Soya Curry & Roti', region: 'any', items: [['roti', 2], ['soya-curry', 1], ['salad', 1, true]] },
    { name: 'Egg Curry Rice', region: 'east', items: [['rice', 1], ['egg-curry', 1], ['salad', 1, true]] },
    { name: 'Fish Curry Rice', region: 'east', items: [['rice', 1], ['fish-curry', 1], ['poriyal', 0.5]] },
    { name: 'Kerala Fish Meal', region: 'south', items: [['rice', 1], ['fish-curry', 1], ['beans-poriyal', 1]] },
    { name: 'Chicken Curry & Roti', region: 'north', items: [['roti', 2], ['chicken-curry', 1], ['salad', 1, true], ['curd', 0.5]] },
    { name: 'Chicken Biryani & Raita', region: 'any', items: [['chicken-biryani', 1], ['raita', 0.5]] },
  ],
  snack: [
    { name: 'Roasted Chana & Chai', region: 'any', items: [['roasted-chana', 1], ['chai-nosugar', 1, true]] },
    { name: 'Makhana & Green Tea', region: 'any', items: [['makhana', 1], ['green-tea', 1, true]] },
    { name: 'Fruit & Nuts', region: 'any', items: [['apple', 1], ['nuts', 0.5]] },
    { name: 'Chaas & Peanuts', region: 'west', items: [['chaas', 1, true], ['peanuts', 0.5]] },
    { name: 'Sprouts Chaat', region: 'north', items: [['sprouts-chaat', 1]] },
    { name: 'Sundal', region: 'south', items: [['sundal', 1], ['filter-coffee', 0.5, true]] },
    { name: 'Khakhra & Chai', region: 'west', items: [['khakhra', 1], ['chai-nosugar', 1, true]] },
    { name: 'Coconut Water & Guava', region: 'south', items: [['coconut-water', 1, true], ['guava', 1]] },
    { name: 'Masala Murmura', region: 'east', items: [['murmura', 1], ['chai-nosugar', 1, true]] },
    { name: 'Banana & Milk', region: 'any', items: [['banana', 1], ['milk', 0.5]] },
    { name: 'Boiled Eggs', region: 'any', items: [['boiled-egg', 2], ['green-tea', 1, true]] },
  ],
  dinner: [
    { name: 'Moong Dal & Lauki', region: 'north', items: [['roti', 2], ['moong-dal', 1], ['lauki', 1], ['salad', 1, true]] },
    { name: 'Roti & Mixed Veg', region: 'any', items: [['roti', 2], ['mixed-veg', 1], ['dal-tadka', 0.5], ['curd', 0.5]] },
    { name: 'Phulka & Palak Paneer', region: 'north', items: [['phulka', 3], ['palak-paneer', 1], ['salad', 1, true]] },
    { name: 'Matar Paneer & Bhakri', region: 'west', items: [['jowar-roti', 2], ['matar-paneer', 1], ['salad', 1, true]] },
    { name: 'Light Khichdi', region: 'any', items: [['khichdi', 1.5], ['curd', 1], ['salad', 1, true]] },
    { name: 'Dosa Night', region: 'south', items: [['dosa', 2], ['sambar', 1], ['coconut-chutney', 1, true]] },
    { name: 'Uttapam & Sambar', region: 'south', items: [['uttapam', 2], ['sambar', 1]] },
    { name: 'Curd Rice & Poriyal', region: 'south', items: [['curd-rice', 1], ['beans-poriyal', 1]] },
    { name: 'Roti & Aloo Methi', region: 'north', items: [['roti', 2], ['methi-aloo', 1], ['moong-dal', 0.5]] },
    { name: 'Soya & Mushroom', region: 'any', items: [['phulka', 3], ['mushroom-masala', 1], ['soya-curry', 0.5]] },
    { name: 'Dal Bhaat Light', region: 'east', items: [['rice', 1], ['moong-dal', 1], ['baingan-bharta', 0.5]] },
    { name: 'Egg Bhurji & Dal', region: 'any', items: [['roti', 2], ['egg-bhurji', 1], ['moong-dal', 0.5]] },
    { name: 'Tandoori Chicken Plate', region: 'north', items: [['roti', 2], ['tandoori-chicken', 1], ['salad', 1, true]] },
    { name: 'Grilled Chicken Bowl', region: 'any', items: [['brown-rice', 1], ['grilled-chicken', 1.5], ['mixed-veg', 1]] },
    { name: 'Fish Curry & Roti', region: 'east', items: [['roti', 2], ['fish-curry', 1], ['salad', 1, true]] },
  ],
};
