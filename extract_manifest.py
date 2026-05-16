import zipfile
import re

apk_path = 'EnjoyMBM.apk'

with zipfile.ZipFile(apk_path, 'r') as z:
    # 读取 AndroidManifest.xml
    manifest_data = z.read('AndroidManifest.xml')
    
    # AndroidManifest.xml 是二进制格式，尝试提取可读的权限信息
    # 查找权限模式
    manifest_text = manifest_data.decode('utf-8', errors='ignore')
    
    print('=== AndroidManifest.xml 原始内容（部分） ===')
    print(manifest_text[:3000])
    print('\n...\n')
    
    # 尝试提取所有权限
    print('=== 提取的权限 ===')
    
    # 查找 android.permission 模式
    perms = []
    idx = 0
    while True:
        idx = manifest_text.find('android.permission.', idx)
        if idx == -1:
            break
        end_idx = manifest_text.find('"', idx)
        if end_idx == -1:
            end_idx = idx + 50
        perm = manifest_text[idx:end_idx].strip()
        if perm and len(perm) < 100:
            perms.append(perm)
        idx += 1
    
    # 去重并排序
    unique_perms = sorted(set(perms))
    for p in unique_perms:
        print(f'  {p}')
    
    print(f'\n总共找到 {len(unique_perms)} 个权限')
    
    # 检查蓝牙权限
    bluetooth_perms = [p for p in unique_perms if 'BLUETOOTH' in p.upper()]
    print(f'\n蓝牙权限 ({len(bluetooth_perms)} 个):')
    for p in bluetooth_perms:
        print(f'  {p}')
    
    if not bluetooth_perms:
        print('  ⚠️ 未找到任何蓝牙权限！')
        print('  这是导致蓝牙信息缺失的根本原因！')
