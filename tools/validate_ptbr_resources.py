#!/usr/bin/env python3
"""Validate Brazilian Portuguese Android resources against the default locale."""
from pathlib import Path
import re
import sys
from collections import Counter
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = ROOT / "app/src/main/res/values/strings.xml"
PTBR = ROOT / "app/src/main/res/values-pt-rBR/strings.xml"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[diouxXeEfgGaAcsbBhH%]")

def resource_map(path):
    root = ET.parse(path).getroot()
    result = {}
    for node in root:
        name = node.get("name")
        if name:
            result[name] = node
    return result

def text(node):
    return "".join(node.itertext())

def placeholders(node):
    return sorted(PLACEHOLDER.findall(text(node)))

errors = []
for path in (ROOT / "app/src/main/res").glob("values*/strings.xml"):
    try:
        parsed = ET.parse(path)
        names = [node.get("name") for node in parsed.getroot() if node.get("name")]
        duplicates = sorted(name for name, count in Counter(names).items() if count > 1)
        if duplicates:
            errors.append(f"Recursos duplicados em {path.relative_to(ROOT)}: " + ", ".join(duplicates))
    except ET.ParseError as error:
        errors.append(f"XML inválido: {path.relative_to(ROOT)}: {error}")

base = resource_map(DEFAULT)
pt = resource_map(PTBR)
missing = sorted(base.keys() - pt.keys())
extra = sorted(pt.keys() - base.keys())
if missing:
    errors.append("Recursos ausentes em pt-BR: " + ", ".join(missing))
if extra:
    errors.append("Recursos sem equivalente padrão: " + ", ".join(extra))

for name in sorted(base.keys() & pt.keys()):
    source, translated = base[name], pt[name]
    if source.tag != translated.tag:
        errors.append(f"Tipo divergente em {name}: {source.tag} != {translated.tag}")
    if placeholders(source) != placeholders(translated):
        errors.append(f"Placeholders divergentes em {name}: {placeholders(source)} != {placeholders(translated)}")
    if source.tag in ("string-array", "plurals"):
        source_items = list(source)
        translated_items = list(translated)
        if len(source_items) != len(translated_items):
            errors.append(f"Quantidade/ordem de itens divergente em {name}")
        for index, (left, right) in enumerate(zip(source_items, translated_items)):
            if source.tag == "plurals" and left.get("quantity") != right.get("quantity"):
                errors.append(f"Categoria plural divergente em {name}[{index}]")
            if placeholders(left) != placeholders(right):
                errors.append(f"Placeholders divergentes em {name}[{index}]")

required_redirect = {
    "redirect_help", "redirect_off", "redirect_open", "redirect_open_app",
    "redirect_picker_title", "redirect_setting", "redirect_unavailable",
}
if not required_redirect.issubset(pt):
    errors.append("Strings do seletor de redirecionamento ausentes em pt-BR")
lang = (ROOT / "app/src/main/java/neth/iecal/curbox/utils/LanguageUtils.kt").read_text()
if 'Locale.forLanguageTag("pt-BR")' not in lang:
    errors.append("pt-BR não está disponível no seletor de idioma")

coverage = len(base.keys() & pt.keys())
print(f"Cobertura pt-BR: {coverage}/{len(base)} recursos ({100 * coverage / max(1, len(base)):.1f}%)")
print(f"Placeholders verificados: {sum(len(placeholders(node)) for node in base.values())}")
print(f"Recursos de lista/plurais verificados: {sum(node.tag in ('string-array', 'plurals') for node in base.values())}")
if errors:
    print("\n".join("ERRO: " + error for error in errors), file=sys.stderr)
    sys.exit(1)
print("Validação XML, cobertura, placeholders, listas e redirect: OK")
