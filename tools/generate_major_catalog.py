"""Generate offline major-catalog CSV files from the official MOE source files.

The source URLs and the acquisition date are documented in docs/major-catalog.md.
This script intentionally performs no network access: pass already downloaded official
files to it, so the application and its tests remain fully offline.
"""
import csv
import re
import sys
from pathlib import Path

import pdfplumber
from docx import Document


SOURCE = "中华人民共和国教育部"
UNDERGRADUATE_CODE = "MOE_UNDERGRADUATE_2024"
GRADUATE_CODE = "MOE_GRADUATE_2022"
VOCATIONAL_CODE = "MOE_VOCATIONAL_2021"
HEADER = ["catalogCode", "educationLevel", "categoryCode", "categoryName", "classCode", "className",
          "majorCode", "majorName", "parentCode", "itemLevel", "source"]


def write_csv(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=HEADER)
        writer.writeheader()
        writer.writerows(rows)


def undergraduate(pdf_path):
    rows, seen, current_category = [], set(), ""
    def add(code, name, parent, level, category_code, category_name, class_code="", class_name=""):
        key = (code, level)
        if not code or not name or key in seen:
            return
        seen.add(key)
        rows.append({"catalogCode": UNDERGRADUATE_CODE, "educationLevel": "UNDERGRADUATE",
                     "categoryCode": category_code, "categoryName": category_name,
                     "classCode": class_code, "className": class_name, "majorCode": code,
                     "majorName": name, "parentCode": parent, "itemLevel": level, "source": SOURCE})
    with pdfplumber.open(pdf_path) as document:
        for page in document.pages:
            for table in page.extract_tables() or []:
                for cell_row in table:
                    cells = [(cell or "").replace("\n", "").strip() for cell in cell_row]
                    if len(cells) < 4 or cells[0] == "序号":
                        continue
                    if not re.fullmatch(r"\d+", cells[0]):
                        if cells[0] and not any(cells[1:]):
                            current_category = cells[0]
                        continue
                    code = cells[2].replace(" ", "")
                    if not re.fullmatch(r"\d{6}[TK]*", code):
                        continue
                    category_code, class_code = code[:2], code[:4]
                    class_name, major_name = cells[1], cells[3]
                    add(category_code, current_category, "", "CATEGORY", category_code, current_category)
                    add(class_code, class_name, category_code, "CLASS", category_code, current_category,
                        class_code, class_name)
                    add(code, major_name, class_code, "MAJOR", category_code, current_category,
                        class_code, class_name)
    return rows


def graduate(pdf_path):
    rows, seen, categories = [], set(), {}
    text = ""
    with pdfplumber.open(pdf_path) as document:
        for page in document.pages:
            text += "\n" + (page.extract_text() or "")
    for line in text.splitlines():
        match = re.match(r"^\s*(\d{2}|\d{4})\s+(.+?)\s*$", line)
        if not match:
            continue
        code, name = match.groups()
        name = re.sub(r"\s+", " ", name).strip()
        if code in seen or len(name) > 80 or name.startswith("第"):
            continue
        seen.add(code)
        if len(code) == 2:
            categories[code] = name
            rows.append({"catalogCode": GRADUATE_CODE, "educationLevel": "GRADUATE", "categoryCode": code,
                         "categoryName": name, "classCode": "", "className": "", "majorCode": code,
                         "majorName": name, "parentCode": "", "itemLevel": "CATEGORY", "source": SOURCE})
        else:
            category_name = categories.get(code[:2], "")
            level = "FIELD" if name.endswith("*") else "DISCIPLINE"
            rows.append({"catalogCode": GRADUATE_CODE, "educationLevel": "GRADUATE", "categoryCode": code[:2],
                         "categoryName": category_name, "classCode": "", "className": "", "majorCode": code,
                         "majorName": name.rstrip("*").strip(), "parentCode": code[:2], "itemLevel": level,
                         "source": SOURCE})
    return rows


def vocational(docx_path):
    rows, seen = [], set()
    levels = ["VOCATIONAL_SECONDARY", "VOCATIONAL_ASSOCIATE", "VOCATIONAL_BACHELOR"]
    def add(level, code, name, parent, item_level, category_code, category_name, class_code="", class_name=""):
        key = (level, code)
        if key in seen or not code or not name:
            return
        seen.add(key)
        rows.append({"catalogCode": VOCATIONAL_CODE, "educationLevel": level, "categoryCode": category_code,
                     "categoryName": category_name, "classCode": class_code, "className": class_name,
                     "majorCode": code, "majorName": name, "parentCode": parent, "itemLevel": item_level,
                     "source": SOURCE})
    for table_index, table in enumerate(Document(docx_path).tables):
        level = levels[table_index]
        category_code = category_name = class_code = class_name = ""
        for row in table.rows:
            cells = [cell.text.replace("\n", "").strip() for cell in row.cells]
            if not cells or cells[0] == "序号":
                continue
            heading = re.match(r"^(\d{4}|\d{2})(.+)$", cells[0])
            if heading and len(set(cells)) == 1:
                code, name = heading.groups()
                if len(code) == 2:
                    category_code, category_name = code, name
                    add(level, code, name, "", "CATEGORY", code, name)
                else:
                    class_code, class_name = code, name
                    add(level, code, name, category_code, "CLASS", category_code, category_name, code, name)
                continue
            if len(cells) >= 3 and re.fullmatch(r"\d{6}[A-Z]*", cells[1].replace(" ", "")):
                code = cells[1].replace(" ", "")
                add(level, code, cells[2], class_code, "MAJOR", category_code, category_name, class_code, class_name)
    return rows


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: generate_major_catalog.py <official-source-dir> <output-resource-dir>")
    source_dir, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    write_csv(out_dir / "undergraduate-2024.csv", undergraduate(source_dir / "undergraduate-2024.pdf"))
    write_csv(out_dir / "graduate-2022.csv", graduate(source_dir / "graduate-2022.pdf"))
    write_csv(out_dir / "vocational-2021.csv", vocational(source_dir / "vocational-2021.docx"))


if __name__ == "__main__":
    main()
