import os, re, xml.etree.ElementTree as ET

label_dir = r'C:\Users\Administrator\Barsoft-Reverse\resources\assets\Label\Templet'
results = {}

for cat in sorted(os.listdir(label_dir)):
    cat_path = os.path.join(label_dir, cat)
    if not os.path.isdir(cat_path):
        continue
    results[cat] = []
    for f in sorted(os.listdir(cat_path)):
        if not f.endswith('.xml'):
            continue
        fp = os.path.join(cat_path, f)
        with open(fp, 'rb') as fh:
            raw = fh.read()
        # Decode as UTF-8
        try:
            text = raw.decode('utf-8')
        except:
            continue
        
        try:
            root = ET.fromstring(text)
        except:
            continue
        
        info = {
            'filename': f,
            'width': root.get('width', ''),
            'height': root.get('height', ''),
            'gap': root.get('gap', ''),
            'density': root.get('density', ''),
            'speed': root.get('speed', ''),
            'items': []
        }
        
        items_elem = root.find('items')
        if items_elem is not None:
            for item in items_elem.findall('item'):
                vt = item.get('viewtype', '')
                item_info = {
                    'viewtype': vt,
                    'type_name': {'0': 'text', '1': 'barcode', '2': 'line', '3': 'rect'}.get(vt, f'unknown({vt})'),
                }
                text_val = item.get('text', '')
                if text_val:
                    item_info['text'] = text_val
                format_val = item.get('format', '')
                if format_val:
                    item_info['format'] = format_val
                textsize = item.get('textsize', '')
                if textsize:
                    item_info['textsize'] = textsize
                font = item.get('font', '')
                if font:
                    item_info['font'] = font
                textName = item.get('textName', '')
                if textName:
                    item_info['textName'] = textName
                info['items'].append(item_info)
        
        results[cat].append(info)

# Output as structured text
for cat, templates in results.items():
    print(f"\n## {cat} ({len(templates)} templates)")
    print("-" * 60)
    for t in templates:
        dim = f"{t['width']}x{t['height']}mm"
        params = []
        if t['gap']:
            params.append(f"gap={t['gap']}")
        if t['density']:
            params.append(f"density={t['density']}")
        if t['speed']:
            params.append(f"speed={t['speed']}")
        param_str = ', '.join(params) if params else ''
        print(f"\n### {t['filename']} ({dim}, {param_str})")
        for item in t['items']:
            line = f"  [{item['type_name']}]"
            if 'text' in item:
                line += f" text=\"{item['text']}\""
            if 'format' in item:
                line += f" format={item['format']}"
            if 'textsize' in item:
                line += f" size={item['textsize']}"
            if 'font' in item:
                line += f" font={item['font']}"
            if 'textName' in item:
                line += f" textName=\"{item['textName']}\""
            print(line)
