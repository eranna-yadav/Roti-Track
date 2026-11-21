# Weight loss recipe book → app data

The recipes come from the "Roti Track Weight Loss Recipes" PDF (text read from Google Drive).
The PDF's ingredients and instructions sit in two columns that come out mixed, so:

1. `extract.py raw.txt partN.json` – name, category, intro, kcal, protein, time, benefits, raw columns
2. `split.py partN.json partNb.json` – separates ingredients from steps (heuristic)
3. `fixN.py` – hand corrections where the columns were mixed
4. `finish.py partNb.json fixN.py recipesN.json` – applies fixes, merges continued steps
5. `gen_kotlin.py app/.../data/RecipeData.kt recipes1.json [recipes2.json …]` – writes the app data

`recipes1.json` holds recipes 1–74 (pages up to 80 of the PDF).
