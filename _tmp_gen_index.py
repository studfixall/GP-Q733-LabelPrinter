import os, re, xml.etree.ElementTree as ET, json

label_dir = r'C:\Users\Administrator\Barsoft-Reverse\resources\assets\Label\Templet'
all_templates = []

for cat in sorted(os.listdir(label_dir)):
    cat_path = os.path.join(label_dir, cat)
    if not os.path.isdir(cat_path):
        continue
    for f in sorted(os.listdir(cat_path)):
        if not f.endswith('.xml'):
            continue
        fp = os.path.join(cat_path, f)
        with open(fp, 'rb') as fh:
            raw = fh.read()
        try:
            text = raw.decode('utf-8')
        except:
            continue
        try:
            root = ET.fromstring(text)
        except:
            continue
        
        info = {
            'category': cat,
            'filename': f,
            'width_mm': root.get('width', ''),
            'height_mm': root.get('height', ''),
            'gap_mm': root.get('gap', ''),
            'density': root.get('density', ''),
            'speed': root.get('speed', ''),
            'items': []
        }
        
        items_elem = root.find('items')
        if items_elem is not None:
            for item in items_elem.findall('item'):
                vt = item.get('viewtype', '')
                type_name = {'0': 'text', '1': 'barcode', '2': 'line', '3': 'rect', '4': 'image'}.get(vt, f'unknown_{vt}')
                item_info = {'viewtype': int(vt) if vt.isdigit() else vt, 'type': type_name}
                for attr in ['text', 'textName', 'format', 'textsize', 'font', 'left', 'top', 'width', 'height']:
                    val = item.get(attr, '')
                    if val:
                        item_info[attr] = val
                info['items'].append(item_info)
        
        all_templates.append(info)

out_path = r'C:\Users\Administrator\Barsoft-Reverse\templates_index.json'
with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(all_templates, f, ensure_ascii=False, indent=2)

print(f"Written {len(all_templates)} templates to {out_path}")

# Also print a summary of textName fields used across templates
textnames = set()
for t in all_templates:
    for item in t['items']:
        if 'textName' in item:
            textnames.add(item['textName'])
print(f"\nUnique textName values ({len(textnames)}):")
for tn in sorted(textnames):
    print(f"  {tn}")
