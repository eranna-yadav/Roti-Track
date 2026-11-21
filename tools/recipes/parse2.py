# Parses recipe pages laid out by `pdftotext -layout` (one recipe per page) into JSON.
import re, json, subprocess, sys
pdf, out = sys.argv[1], sys.argv[2]
pages = int(re.search(r'Pages:\s+(\d+)', subprocess.run(['pdfinfo', pdf], capture_output=True, text=True).stdout).group(1))
CATS = {'VEGETARIAN': 'veg', 'NON-VEG': 'nonveg', 'EGG': 'egg', 'DRINKS': 'drink', 'SWEETS': 'sweet', 'DIPS': 'dip', 'CHUTNEYS': 'dip'}
def tidy(s):
    s = re.sub(r'\s+', ' ', s).strip()
    s = re.sub(r'(\w)- (\w)', r'\1-\2', s)
    return s
recipes = []
for p in range(1, pages + 1):
    txt = subprocess.run(['pdftotext', '-layout', '-f', str(p), '-l', str(p), pdf, '-'], capture_output=True, text=True).stdout
    m = re.search(r'RECIPE\s+(\d)\s*(\d)\s*(\d)', txt)
    if not m or 'INGREDIENTS' not in txt: continue
    n = int(''.join(m.groups()))
    head = txt.split('\n')[0].upper().replace(' ', '')
    cat = next((v for k, v in CATS.items() if k.replace(' ', '') in head), None)
    lines = txt.split('\n')
    # title: first non-empty line after the header, at the left margin
    body = lines[1:]
    i = next(k for k, l in enumerate(body) if l.strip())
    title_lines, intro = [], []
    k = i
    while k < len(body) and 'KCAL' not in body[k]:
        l = body[k]
        left, right = l[:48], l[48:]
        if left.strip() and not right.strip() and not intro: title_lines.append(left.strip())
        elif left.strip() and len(l) - len(l.lstrip()) < 10 and not intro: title_lines.append(left.strip()); intro.append(right.strip())
        else: intro.append(l.strip())
        k += 1
    nums = [x for x in re.findall(r'\d+', body[k - 1] if k else '')]
    numline = next(l for l in reversed(body[:k]) if re.search(r'\d', l))
    kcal, prot, mins = [int(x) for x in re.findall(r'\d+', numline)[:3]]
    intro_text = tidy(' '.join(x for x in intro if x and not re.fullmatch(r'[\d\sg]+(min)?', x) and 'g ' not in x[:6]))
    intro_text = re.sub(r'^([A-Z]) ([a-z])', r'\1\2', intro_text)
    if intro_text.startswith('A') and re.match(r'^A[a-z]', intro_text) and not intro_text.startswith(('Aromatic', 'Air', 'Aloo', 'All', 'Almond', 'An', 'Amla', 'Ajwain', 'Apple')): pass
    # ingredients / instructions columns
    a = txt.index('INGREDIENTS'); b = txt.index('W E I G H T') if 'W E I G H T' in txt else txt.index('WEIGHT LOSS B')
    block = txt[a:b].split('\n')
    hdr = block[0]; col = hdr.index('INSTRUCTIONS')
    ings, steps = [], []
    for l in block[1:]:
        if not l.strip(): continue
        left, right = l[:col].strip(), l[col:].rstrip()
        if left:
            # an indented line continues the previous ingredient only when that one looks unfinished
            if ings and (ings[-1].endswith((',', ' and', ' or', ' of', '/', 'with')) or (left[:1].islower() and not re.match(r'^\d', left))): ings[-1] += ' ' + left
            else: ings.append(left)
        if right.strip():
            r = right.strip()
            mm = re.match(r'^(\d{1,2})\s{2,}(.*)$', r)
            if mm: steps.append(mm.group(2))
            elif steps: steps[-1] += ' ' + r
    # benefits: two columns of short sentences until the footer
    ben_txt = txt[b:].split('ROTI TRACK')[0].split('\n')[1:]
    lefts, rights = [], []
    for l in ben_txt:
        if not l.strip(): continue
        parts = re.split(r'\s{3,}', l.strip())
        lefts.append(parts[0]); rights += parts[1:2]
    benefits = [tidy(x) for x in lefts + rights if x.strip()]
    recipes.append(dict(n=n, page=p, cat=cat, title=tidy(' '.join(title_lines)), intro=intro_text, kcal=kcal, protein=prot, minutes=mins,
                        ingredients=[tidy(x) for x in ings], steps=[tidy(x) for x in steps], benefits=benefits))
json.dump(recipes, open(out, 'w'), ensure_ascii=False, indent=1)
print(len(recipes), 'recipes', [r['n'] for r in recipes][:3], '...', [r['n'] for r in recipes][-3:], sorted(set(r['cat'] for r in recipes)))
