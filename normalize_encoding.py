"""One-time: normalize all .kt files to UTF-8 no-BOM + LF"""
import os, glob

fixed = 0
for pattern in ['app/src/main/java/**/*.kt', 'app/src/main/java/**/*.kts']:
    for f in glob.glob(pattern, recursive=True):
        if '/build/' in f:
            continue
        with open(f, 'rb') as fh:
            raw = fh.read()

        # Strip BOM
        if raw.startswith(b'\xef\xbb\xbf'):
            raw = raw[3:]

        # Decode as UTF-8
        text = raw.decode('utf-8')

        # CRLF -> LF
        if '\r\n' in text:
            text = text.replace('\r\n', '\n')

        # Ensure file ends with newline
        if not text.endswith('\n'):
            text += '\n'

        # Write back as UTF-8 no-BOM + LF
        with open(f, 'wb') as fh:
            fh.write(text.encode('utf-8'))

        fixed += 1
        rel = os.path.relpath(f)
        print(f'  Fixed: {rel}')

print(f'\nTotal fixed: {fixed} files')
print('All files are now UTF-8 no-BOM + LF')
