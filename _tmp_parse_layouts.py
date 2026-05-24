import os

layout_dir = r'C:\Users\Administrator\Barsoft-Reverse\resources\res\layout'
key_layouts = ['activity_main.xml', 'fragment_home.xml', 'fragment_label.xml', 
               'fragment_setting.xml', 'fragment_my.xml', 'dialog_print.xml',
               'activity_bluetooth_list.xml', 'activity_text_edit.xml',
               'activity_barcode_edit.xml']

results = {}
for f in os.listdir(layout_dir):
    if f.endswith('.xml'):
        fp = os.path.join(layout_dir, f)
        with open(fp, 'r', encoding='utf-8') as fh:
            content = fh.read()
        results[f] = content

# Print key layout summaries
for f in key_layouts:
    if f in results:
        print(f"=== {f} ===")
        print(results[f][:2000])
        print()
