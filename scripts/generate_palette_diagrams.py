#!/usr/bin/env python3
"""Generate the "Color palettes" Mermaid diagrams from the theme CSS.

Reads the design tokens straight from the two theme stylesheets (the source
of truth) and prints a Markdown section containing one Mermaid `block-beta`
swatch grid per theme and mode. GitHub renders Mermaid natively, so the
palette is visible in the browser without committing any image.

Each swatch is filled with the token's color; the label color (dark or
light) is chosen by WCAG relative luminance so the token name stays
readable on its own swatch.

Usage:
  python3 scripts/generate_palette_diagrams.py
      Print the Markdown section; paste it over the "Color palettes"
      section of docs/design/theme-exploration.md.
  python3 scripts/generate_palette_diagrams.py --html
      Write docs/design/palettes.html, the standalone French palette page
      for the dossier: same data, plus the contrast ratio of every token
      against its reference background and the AA verdict.

Run both after any token change.
"""

import re
import sys

THEMES = [
    ("Catppuccin", "src/main/resources/static/css/theme-catppuccin.css",
     "Latte (light)", "Mocha (dark)"),
    ("Soft Paper", "src/main/resources/static/css/theme-soft-paper.css",
     "light", "dark"),
]

COLUMNS = 5

TOKEN_RE = re.compile(r"^\s*(--[a-z-]+)\s*:\s*(#[0-9a-fA-F]{3,6})\s*;")


def parse_blocks(path: str) -> tuple[dict[str, str], dict[str, str]]:
    """Return (light, dark) ordered token->hex maps from a theme stylesheet.

    The light palette is the first `:root` block; the dark palette is the
    `:root` inside `@media (prefers-color-scheme: dark)`.
    """
    light: dict[str, str] = {}
    dark: dict[str, str] = {}
    in_dark = False
    for line in open(path):
        if "prefers-color-scheme: dark" in line:
            in_dark = True
        match = TOKEN_RE.match(line)
        if match:
            (dark if in_dark else light)[match.group(1)] = match.group(2).lower()
    return light, dark


def luminance(hex_color: str) -> float:
    """WCAG relative luminance of a #rrggbb color."""
    hex_color = hex_color.lstrip("#")
    if len(hex_color) == 3:
        hex_color = "".join(c * 2 for c in hex_color)
    channels = []
    for i in (0, 2, 4):
        c = int(hex_color[i:i + 2], 16) / 255
        channels.append(c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4)
    r, g, b = channels
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def label_color(background: str) -> str:
    """Dark label on light swatches, light label on dark ones."""
    return "#11111b" if luminance(background) > 0.35 else "#f8f8f8"


def diagram(tokens: dict[str, str]) -> str:
    """A flowchart whose disconnected rows stack vertically: each row is a
    chain of COLUMNS nodes joined by invisible links (~~~). block-beta would
    be the natural fit but it ignores the label color, which makes dark
    swatches unreadable; flowchart honors it."""
    lines = ["```mermaid", "flowchart LR"]
    ids = []
    row: list[str] = []
    for index, (token, value) in enumerate(tokens.items()):
        short = token.removeprefix("--color-").removeprefix("--")
        block_id = f"t{index}"
        ids.append((block_id, value))
        row.append(f'{block_id}["{short}<br/>{value}"]')
        if len(row) == COLUMNS:
            lines.append("  " + " ~~~ ".join(row))
            row = []
    if row:
        lines.append("  " + " ~~~ ".join(row))
    for block_id, value in ids:
        lines.append(
            f"  style {block_id} fill:{value},stroke:#7f7f7f,"
            f"color:{label_color(value)}")
    lines.append("```")
    return "\n".join(lines)


def contrast(color_a: str, color_b: str) -> float:
    """WCAG contrast ratio between two #rrggbb colors."""
    lum_a, lum_b = luminance(color_a), luminance(color_b)
    lighter, darker = max(lum_a, lum_b), min(lum_a, lum_b)
    return (lighter + 0.05) / (darker + 0.05)


# How each token is judged: (reference token or None, threshold or None).
# None reference means decorative/surface: no contrast requirement of its own.
JUDGMENTS = {
    "--color-text": ("--color-bg", 4.5),
    "--color-text-muted": ("--color-bg", 4.5),
    "--color-primary": ("--color-bg", 4.5),
    "--color-on-primary": ("--color-primary", 4.5),
    "--color-link": ("--color-bg", 4.5),
    "--color-success": ("--color-bg", 4.5),
    "--color-warning": ("--color-bg", 4.5),
    "--color-error": ("--color-bg", 4.5),
    "--color-focus": ("--color-bg", 3.0),  # UI component (RGAA/WCAG 1.4.11)
    "--code-text": ("--code-bg", 4.5),
    "--code-keyword": ("--code-bg", 4.5),
    "--code-string": ("--code-bg", 4.5),
    "--code-function": ("--code-bg", 4.5),
    "--code-comment": ("--code-bg", 4.5),
}


def html_rows(tokens: dict[str, str]) -> str:
    rows = []
    for token, value in tokens.items():
        short = token.removeprefix("--color-").removeprefix("--")
        reference, threshold = JUDGMENTS.get(token, (None, None))
        if reference:
            ratio = contrast(value, tokens[reference])
            verdict = "pass" if ratio >= threshold else "fail"
            badge = (f'<td class="ratio">{ratio:.2f}:1 sur '
                     f'<code>{reference.removeprefix("--color-").removeprefix("--")}</code></td>'
                     f'<td><span class="badge badge--{verdict}">'
                     f'{"AA" if threshold == 4.5 else "UI 3:1"}'
                     f'{" ✓" if verdict == "pass" else " ✗"}</span></td>')
        else:
            badge = '<td class="ratio">décor</td><td></td>'
        rows.append(
            f'      <tr><td><span class="swatch" style="background:{value}">'
            f'</span></td><td><code>{short}</code></td>'
            f'<td><code>{value}</code></td>{badge}</tr>')
    return "\n".join(rows)


HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>learn-dev : palettes de couleurs</title>
<style>
  :root {{ color-scheme: light; }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; padding: 2rem; font: 1rem/1.6 system-ui, sans-serif;
         background: #fafafa; color: #222; }}
  h1 {{ font-size: 1.6rem; }}
  h2 {{ font-size: 1.25rem; margin: 2.5rem 0 1rem; }}
  .intro {{ max-width: 60rem; }}
  .modes {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(28rem, 1fr));
           gap: 1.5rem; }}
  .mode {{ border-radius: 0.75rem; padding: 1.25rem 1.5rem; border: 1px solid; }}
  .mode h3 {{ margin: 0 0 1rem; font-size: 1.05rem; }}
  table {{ border-collapse: collapse; width: 100%; font-size: 0.85rem; }}
  td {{ padding: 0.3rem 0.6rem 0.3rem 0; vertical-align: middle; }}
  .swatch {{ display: inline-block; width: 2.4rem; height: 1.5rem;
            border-radius: 0.25rem; border: 1px solid rgba(127,127,127,.55); }}
  .ratio {{ white-space: nowrap; }}
  .badge {{ font-size: 0.75rem; font-weight: 700; padding: 0.1rem 0.45rem;
           border-radius: 0.75rem; white-space: nowrap; }}
  .badge--pass {{ background: #d9efd7; color: #1d4d12; }}
  .badge--fail {{ background: #f6d3da; color: #7a1024; }}
  code {{ font-family: ui-monospace, monospace; font-size: 0.95em; }}
  footer {{ margin-top: 3rem; font-size: 0.85rem; color: #555; }}
  @media print {{ body {{ padding: 0; background: #fff; }}
                 .mode {{ break-inside: avoid; }} }}
</style>
</head>
<body>
<h1>learn-dev : palettes de couleurs</h1>
<p class="intro">
  Annexe du dossier projet. Chaque carte ci-dessous est rendue avec les
  couleurs de sa propre palette (fond, texte et bordure réels) ; chaque
  jeton indique son ratio de contraste WCAG calculé contre son fond de
  référence, avec le verdict AA (texte : 4,5:1 ; composant d'interface :
  3:1). « décor » signale les jetons de surface sans exigence de contraste
  propre. Les valeurs proviennent directement des feuilles de style du
  thème ; ce fichier est généré par
  <code>scripts/generate_palette_diagrams.py --html</code>.
</p>
{sections}
<footer>
  Source : <code>src/main/resources/static/css/theme-*.css</code> ·
  Étude complète : <a href="theme-exploration.md">theme-exploration.md</a> ·
  Méthode des ratios : formule de luminance relative WCAG.
</footer>
</body>
</html>
"""


def html_section(name: str, modes: list[tuple[str, dict[str, str]]]) -> str:
    cards = []
    for label, tokens in modes:
        bg = tokens["--color-bg"]
        text = tokens["--color-text"]
        border = tokens["--color-border"]
        cards.append(f"""  <div class="mode" style="background:{bg};color:{text};border-color:{border}">
    <h3>{name} {label}</h3>
    <table>
{html_rows(tokens)}
    </table>
  </div>""")
    return (f"<h2>{name}</h2>\n"
            f'<div class="modes">\n' + "\n".join(cards) + "\n</div>")


def write_html(path: str) -> None:
    sections = []
    for name, css_path, light_label, dark_label in THEMES:
        light, dark = parse_blocks(css_path)
        sections.append(html_section(
            name, [(light_label, light), (dark_label, dark)]))
    with open(path, "w") as handle:
        handle.write(HTML_TEMPLATE.format(sections="\n".join(sections)))
    print(f"Wrote {path}")


def main() -> int:
    if "--html" in sys.argv:
        write_html("docs/design/palettes.html")
        return 0
    print("## Color palettes")
    print()
    print("<!-- Generated by scripts/generate_palette_diagrams.py from the")
    print("     theme stylesheets. Regenerate after any token change. -->")
    print()
    print("Every swatch below comes straight from the theme stylesheets;")
    print("GitHub renders these Mermaid blocks with the real colors, so no")
    print("image is committed. Values are the shipped (contrast-adjusted)")
    print("tokens, not the upstream palettes. A standalone HTML version for")
    print("the dossier, with each token's computed contrast ratio and AA")
    print("verdict, lives in [palettes.html](palettes.html) (same generator,")
    print("`--html` flag).")
    for name, path, light_label, dark_label in THEMES:
        light, dark = parse_blocks(path)
        for label, tokens in ((light_label, light), (dark_label, dark)):
            print()
            print(f"### {name} {label}")
            print()
            print(diagram(tokens))
    return 0


if __name__ == "__main__":
    sys.exit(main())
