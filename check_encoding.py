"""Check all .kt files for BOM and CRLF"""
import os, glob

stats = {'bom': 0, 'crlf': 0, 'ok': 0, 'total': 0}
for pattern in ['app/src/main/java/**/*.kt', 'app/src/main/java/**/*.kts']:
    for f in glob.glob(pattern, recursive=True):
        if '/build/' in f:
            continue
        stats['total'] += 1
        with open(f, 'rb') as fh:
            raw = fh.read()
        has_bom = raw.startswith(b'\xef\xbb\xbf')
        has_crlf = b'\r\n' in raw
        if has_bom:
            stats['bom'] += 1
        if has_crlf:
            stats['crlf'] += 1
        if not has_bom and not has_crlf:
            stats['ok'] += 1
        rel = os.path.relpath(f)
        if has_bom or has_crlf:
            flags = []
            if has_bom:
                flags.append('BOM')
            if has_crlf:
                flags.append('CRLF')
            tag = ' '.join(flags)
            print(f'  {tag:8s} {rel}')

print(f'\nTotal: {stats["total"]} | Clean: {stats["ok"]} | BOM: {stats["bom"]} | CRLF: {stats["crlf"]}')
