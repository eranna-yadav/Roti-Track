# Cleans recipes parsed from the PDF layout (parse2.py) into the app's recipe JSON.
import json, re, sys
KEEP_A = ('Apple', 'Almond', 'Aromatic', 'Air', 'Aloo', 'Amla', 'And', 'Also', 'Ajwain', 'An ', 'A ')
INTRO_FIX = {102: 'An age-old Indian remedy made by boiling'}
d = json.load(open(sys.argv[1]))
out = []
for r in d:
    intro = r['intro']
    if r['n'] in INTRO_FIX:
        intro = re.sub(r'^Anbyage-old Indian remedy made boiling', INTRO_FIX[r['n']], intro)
    elif re.match(r'^A[a-z]', intro) and not intro.startswith(KEEP_A):
        intro = 'A ' + intro[1:]                     # drop-cap "A warming" read as "Awarming"
    ings = []
    for i in r['ingredients']:
        if ings and (i.startswith('(') or i[:1].islower()): ings[-1] += ' ' + i
        else: ings.append(i)
    steps = []
    for s in r['steps']:
        if steps and s[:1].islower(): steps[-1] = steps[-1].rstrip('.') + ', ' + s
        else: steps.append(s[:1].upper() + s[1:])
    out.append(dict(id=f"r{r['n']:03d}", n=r['n'], cat=r['cat'], title=r['title'], intro=intro, kcal=r['kcal'], protein=r['protein'],
                    minutes=r['minutes'], ingredients=ings, steps=steps, benefits=r['benefits']))
json.dump(out, open(sys.argv[2], 'w'), ensure_ascii=False, indent=1)
print(len(out), 'recipes')
