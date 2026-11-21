# Writes data/RecipeData.kt from the clean recipe JSON files given as arguments.
import json, sys
recipes = []
for f in sys.argv[2:]: recipes += json.load(open(f))
recipes.sort(key=lambda r: r['n'])
for r in recipes: r.pop('n', None)
parts, cur = [], []
for r in recipes:
    cur.append(r)
    if len(json.dumps(cur, ensure_ascii=False).encode()) > 40_000: parts.append(cur[:-1]); cur = [r]
parts.append(cur)
out = ['package com.rotitrack.app.data', '',
       '// Generated from the "Roti Track Weight Loss Recipes" book. Edit the source JSON and regenerate.', '']
for i, p in enumerate(parts):
    js = json.dumps(p, ensure_ascii=False, separators=(',', ':'))
    assert '$' not in js and '"""' not in js
    out.append(f'private const val PART_{i + 1} = """{js}"""')
    out.append('')
out.append('internal val RECIPE_JSON_PARTS: List<String> = listOf(' + ', '.join(f'PART_{i + 1}' for i in range(len(parts))) + ')')
open(sys.argv[1], 'w').write('\n'.join(out) + '\n')
print(len(recipes), 'recipes in', len(parts), 'parts;', sum(len(json.dumps(p, ensure_ascii=False).encode()) for p in parts), 'bytes')
