import json, re, sys, importlib.util
spec = importlib.util.spec_from_file_location('fix', sys.argv[2]); fix = importlib.util.module_from_spec(spec); spec.loader.exec_module(fix)
d = json.load(open(sys.argv[1]))
CATS = {'VEGETARIANRECIPES': 'veg', 'NON-VEGRECIPES': 'nonveg', 'EGGRECIPES': 'egg', 'WEIGHTLOSSDRINKS': 'drink', 'DRINKS': 'drink',
        'WEIGHTLOSSSWEETS': 'sweet', 'SWEETS': 'sweet', 'DIPS&CHUTNEYS': 'dip', 'DIPSANDCHUTNEYS': 'dip'}
out = []
for r in d:
    f = fix.FIX.get(r['n'], {})
    ing, steps = list(f.get('I', r['ingredients'])), list(f.get('S', r['steps']))
    for kind, i, text in f.get('p', []):
        (ing if kind == 'i' else steps)[i] = text
    merged = []                              # a step starting in lowercase continues the one before
    for s in steps:
        if merged and s[:1].islower(): merged[-1] = merged[-1].rstrip('.') + ', ' + s
        else: merged.append(s[:1].upper() + s[1:])
    cat = CATS.get(r['cat'])
    assert cat, r['cat']
    out.append(dict(id=f"r{r['n']:03d}", n=r['n'], cat=cat, title=r['title'], intro=r['intro'], kcal=r['kcal'], protein=r['protein'],
                    minutes=r['minutes'], ingredients=ing, steps=merged, benefits=r['benefits']))
json.dump(out, open(sys.argv[3], 'w'), ensure_ascii=False, indent=1)
bad = [(o['n'], s) for o in out for s in o['steps'] + o['ingredients'] if re.search(r'\b(chopped|cubed|sliced|pureed|masala|paste|powder)\.?$', s) and s in o['steps']]
print(len(out), 'recipes; suspicious step endings:', bad[:10])
