"""One-time: remove stray blank lines between consecutive code lines.
Only removes blank lines between:
- consecutive imports
- consecutive val/val declarations inside a function body
- consecutive fun/class/object declarations

Preserves intentional blank lines (before class, before fun, after package, etc.)
"""
import re, glob

total_fixes = 0
for pattern in ['app/src/main/java/**/*.kt', 'app/src/main/java/**/*.kts']:
    for f in glob.glob(pattern, recursive=True):
        if '/build/' in f:
            continue
        with open(f, 'r', encoding='utf-8') as fh:
            content = fh.read()

        original = content

        # 1. Remove blank lines between consecutive imports
        content = re.sub(r'(import [^\n]+)\n\n+(import )', r'\1\n\2', content)
        # Repeat until stable
        prev = None
        while prev != content:
            prev = content
            content = re.sub(r'(import [^\n]+)\n\n+(import )', r'\1\n\2', content)

        # 2. Remove blank lines between consecutive val/var declarations inside functions
        # Only when indentation is same (4+ spaces)
        content = re.sub(r'(\n( {4,})[^\n]+)\n\n+( {4,}val | {4,}var | {4,}cmd\.| {4,}textSetting\.| {4,}barcodeSetting\.| {4,}commonSetting\.)', r'\1\n\3', content)
        prev = None
        while prev != content:
            prev = content
            content = re.sub(r'(\n( {4,})[^\n]+)\n\n+( {4,}val | {4,}var | {4,}cmd\.| {4,}textSetting\.| {4,}barcodeSetting\.| {4,}commonSetting\.)', r'\1\n\3', content)

        # 3. Collapse triple+ blank lines to double (one intentional blank line)
        content = re.sub(r'\n{3,}', '\n\n', content)

        if content != original:
            with open(f, 'w', encoding='utf-8', newline='\n') as fh:
                fh.write(content)
            total_fixes += 1
            print(f'Fixed: {f}')

print(f'\nTotal files fixed: {total_fixes}')
