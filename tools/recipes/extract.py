import re, json, sys
t = open(sys.argv[1]).read().replace('', '–')
parts = re.split(r'\n\n([A-Z\- ]+RECIPES) RECIPE (\d{3})\n', t)
out = []
for i in range(1, len(parts), 3):
    cat_raw, num, body = parts[i], int(parts[i+1]), parts[i+2]
    body = body.split('ROTI TRACK · WEIGHT LOSS RECIPES')[0]
    lines = [l.strip() for l in body.split('\n') if l.strip()]
    title = lines[0]
    m = re.search(r'(\d+) KCAL / SERVING (\d+) g\s+PROTEIN (\d+) min', body)
    head, rest = body.split('INGREDIENTS INSTRUCTIONS', 1)
    mid, ben = rest.split('WEIGHT LOSS B ENEFITS', 1)
    intro = head.split(title, 1)[1].split(m.group(0)[:10])[0]
    intro = re.sub(r'\s+', ' ', intro).strip()
    intro = re.sub(r'^([A-Z]) ([a-z])', r'\1\2', intro)          # "R oasted" -> "Roasted"
    intro = re.sub(r'(\w)- (\w)', r'\1-\2', intro)               # "garlic- tomato"
    benefits = [re.sub(r'\s+', ' ', b).strip() for b in ben.strip().split('\n\n') if b.strip()]
    cat = cat_raw.replace(' ', '')
    out.append(dict(n=num, cat=cat, title=title, intro=intro, kcal=int(m.group(1)), protein=int(m.group(2)), minutes=int(m.group(3)),
                    raw=' | '.join(l.strip() for l in mid.split('\n') if l.strip()), benefits=benefits))
json.dump(out, open(sys.argv[2], 'w'), ensure_ascii=False, indent=0)
print(len(out), sorted(set(o['cat'] for o in out)))
