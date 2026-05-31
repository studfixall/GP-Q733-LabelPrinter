"""Fix import sections: remove blank lines between imports, keep one blank line after package"""
import re, glob

for pattern in ['app/src/main/java/**/*.kt', 'app/src/main/java/**/*.kts']:
    for f in glob.glob(pattern, recursive=True):
        if '/build/' in f:
            continue
        with open(f, 'r', encoding='utf-8') as fh:
            content = fh.read()

        # Fix: blank lines between consecutive imports
        # Pattern: import line, blank line(s), import line -> just imports with no blank lines
        fixed = re.sub(r'\n(import [^\n]+)\n\n+(import )', r'\n\1\n\2', content)
        # Repeat until no more matches (handles cascading)
        while fixed != content:
            content = fixed
            fixed = re.sub(r'\n(import [^\n]+)\n\n+(import )', r'\n\1\n\2', content)

        # Also fix blank lines between consecutive fun/class/object/val declarations
        # But keep single blank lines between function definitions (that's normal style)
        # Only fix triple+ blank lines
        fixed = re.sub(r'\n{3,}', '\n\n', content)
        content = fixed

        with open(f, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(content)

print('Done')
