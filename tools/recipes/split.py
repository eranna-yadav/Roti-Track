import re, json, sys
ING_START = re.compile(r'^(\d|½|¼|¾|[A-Z])')
TRAIL_SENT = re.compile(r'^(.*?[.!])\s+((?:\d|½|¼|¾)[^.]*|[A-Z][a-z]+ (?:and|to|for|as|,)[^.]*|Salt[^.]*|Pepper[^.]*|Fresh [^.]*)$')
TRAIL_COMMA = re.compile(r'^(.*?,)\s+(\d+(?:/\d+)? (?:[a-z]|g\b|ml\b)[^,.]*)$')
TRAIL_NUM = re.compile(r'^(.*?\b\d+)\s+(\d+(?:/\d+)? [a-z][^.]*)$')
def split_trailing(text):
    for rx in (TRAIL_SENT, TRAIL_NUM, TRAIL_COMMA):
        m = rx.match(text)
        if m: return m.group(1).strip(), m.group(2).strip()
    return text, None
def parse(raw):
    steps, ings, nxt = [], [], 1
    for tok in raw.split(' | '):
        m = re.match(r'^(\d{1,2}) (\S.*)$', tok)
        if m and int(m.group(1)) == nxt and (m.group(2)[0].isupper() or (steps and steps[-1].endswith('.'))):
            text, extra = split_trailing(m.group(2))
            steps.append(text); nxt += 1
            if extra: ings.append(extra)
            continue
        if ING_START.match(tok):
            ings.append(tok); continue
        # lowercase continuation: instruction or ingredient?
        step_open = steps and not steps[-1].rstrip().endswith(('.', '!'))
        ing_open = ings and (ings[-1].rstrip().endswith((',', 'and', 'or', 'of', 'into', 'with')) or len(ings[-1]) > 22)
        if step_open and (tok.rstrip().endswith('.') or len(tok) > 25 or not ing_open):
            text, extra = split_trailing(tok)
            steps[-1] += ' ' + text
            if extra: ings.append(extra)
        elif ings and not tok.rstrip().endswith('.'):
            ings[-1] += ' ' + tok
        elif steps:
            text, extra = split_trailing(tok); steps[-1] += ' ' + text
            if extra: ings.append(extra)
        else:
            ings.append(tok)
    clean = lambda s: re.sub(r'\s+', ' ', re.sub(r'(\w)- (\w)', r'\1-\2', s)).strip()
    return [clean(i) for i in ings], [clean(s) for s in steps]
d = json.load(open(sys.argv[1]))
for r in d:
    r['ingredients'], r['steps'] = parse(r['raw'])
    r['intro'] = re.sub(r'^A([a-z])', r'A \1', r['intro'])
json.dump(d, open(sys.argv[2], 'w'), ensure_ascii=False, indent=0)
for r in d:
    print(f"#{r['n']} {r['title']}\n  I: " + ' ; '.join(r['ingredients']) + "\n  S: " + ' / '.join(f"{i+1}) {s}" for i, s in enumerate(r['steps'])))
