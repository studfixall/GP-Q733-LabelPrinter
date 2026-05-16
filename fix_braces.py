import zipfile
import os
import shutil

# 读取当前APK
apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v3.apk"
extract_dir = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\extracted_v3"

# 解压
if os.path.exists(extract_dir):
    shutil.rmtree(extract_dir)
os.makedirs(extract_dir)

print("Extracting APK...")
with zipfile.ZipFile(apk_path, 'r') as zf:
    zf.extractall(extract_dir)

# 读取并修复JS文件
main_js_path = os.path.join(extract_dir, "assets/apps/__UNI__092D5A7/www/app-service.js")
with open(main_js_path, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

print(f"Original size: {len(content):,} bytes")

# 找到BluetoothHelper并检查
helper_start = content.find("var BluetoothHelper")
helper_end = content.find("};", helper_start) + 2  # 找到 "};" 结束

if helper_start > 0 and helper_end > helper_start:
    helper_code = content[helper_start:helper_end]
    
    print(f"\nHelper code length: {len(helper_code)} bytes")
    print(f"Open braces: {helper_code.count('{')}")
    print(f"Close braces: {helper_code.count('}')}")
    
    # 检查helper内部是否平衡
    inner_code = helper_code[helper_code.find('{')+1:helper_code.rfind('}')]
    inner_open = inner_code.count('{')
    inner_close = inner_code.count('}')
    print(f"\nInner braces: {inner_open} open, {inner_close} close")
    
    if inner_open != inner_close:
        print(f"\n[WARNING] Inner mismatch by {inner_open - inner_close}")
        # 尝试修复 - 在末尾添加缺少的 }
        if inner_open > inner_close:
            # 需要添加 }
            print("Adding missing closing brace...")
            # 在 BluetoothHelper 结束前的位置插入
            insert_pos = helper_end - 2  # 在 "};" 之前
            new_content = content[:insert_pos] + "}" + content[insert_pos:]
            
            with open(main_js_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print("Fixed and saved!")
        
# 重新打包
output_apk = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v4.apk"
print(f"\nRepacking to: {output_apk}")

with zipfile.ZipFile(output_apk, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(extract_dir):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, extract_dir)
            zf.write(file_path, arcname)

file_size = os.path.getsize(output_apk)
print(f"Done! Size: {file_size/1024/1024:.2f} MB")
