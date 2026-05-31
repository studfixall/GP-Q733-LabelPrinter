"""Clean up double blank lines in all .kt files"""
import glob

for pattern in ['app/src/main/java/**/*.kt', 'app/src/main/java/**/*.kts']:
    for f in glob.glob(pattern, recursive=True):
        if '/build/' in f:
            continue
        with open(f, 'r', encoding='utf-8') as fh:
            content = fh.read()

        # Replace triple+ newlines with double (one blank line)
        import re
        cleaned = re.sub(r'\n{3,}', '\n\n', content)

        if cleaned != content:
            with open(f, 'w', encoding='utf-8', newline='\n') as fh:
                fh.write(cleaned)
            print(f'Fixed: {f}')

print('Done')
