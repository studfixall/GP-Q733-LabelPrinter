import zipfile
import re
import json

apk_path = 'EnjoyMBM.apk'
report = []

with zipfile.ZipFile(apk_path, 'r') as z:
    report.append('=== 蓝牙功能深度分析 ===\n')
    
    # 1. 分析 AndroidManifest.xml 中的蓝牙权限
    report.append('=== 1. AndroidManifest 蓝牙权限 ===')
    try:
        manifest = z.read('AndroidManifest.xml')
        manifest_text = manifest.decode('utf-8', errors='ignore')
        
        # 查找蓝牙相关权限
        bluetooth_perms = re.findall(r'android\.permission\.[A-Z_]*BLUETOOTH[A-Z_]*', manifest_text)
        bluetooth_features = re.findall(r'android\.hardware\.bluetooth[^"\']*', manifest_text)
        
        report.append(f'蓝牙权限: {set(bluetooth_perms)}')
        report.append(f'蓝牙硬件声明: {set(bluetooth_features)}')
        
        # 查找所有权限
        all_perms = re.findall(r'android\.permission\.[A-Z_]+', manifest_text)
        bluetooth_related = [p for p in all_perms if 'BLUETOOTH' in p]
        report.append(f'\n所有蓝牙相关权限 ({len(bluetooth_related)} 个):')
        for p in bluetooth_related:
            report.append(f'  - {p}')
    except Exception as e:
        report.append(f'Error: {e}')
    
    report.append('\n')
    
    # 2. 分析 dcloud_properties.xml 中的蓝牙配置
    report.append('=== 2. DCloud 蓝牙配置 ===')
    try:
        props = z.read('assets/data/dcloud_properties.xml').decode('utf-8', errors='ignore')
        
        # 查找蓝牙相关配置
        if 'Bluetooth' in props:
            bluetooth_configs = re.findall(r'<feature[^>]*Bluetooth[^>]*>', props)
            report.append(f'蓝牙 Feature 配置:')
            for cfg in bluetooth_configs:
                report.append(f'  {cfg}')
        
        # 查找参数配置
        param_matches = re.findall(r'<param[^>]*name="[^"]*bluetooth[^"]*"[^>]*>', props, re.IGNORECASE)
        if param_matches:
            report.append(f'\n蓝牙参数配置:')
            for p in param_matches:
                report.append(f'  {p}')
    except Exception as e:
        report.append(f'Error: {e}')
    
    report.append('\n')
    
    # 3. 分析 manifest.json 中的蓝牙权限
    report.append('=== 3. UniApp manifest.json 蓝牙配置 ===')
    try:
        manifest_json = z.read('assets/apps/__UNI__092D5A7/www/manifest.json')
        manifest_data = json.loads(manifest_json.decode('utf-8'))
        
        # 检查 permissions
        perms = manifest_data.get('permissions', {})
        if 'Bluetooth' in perms:
            report.append(f'UniApp 蓝牙权限配置:')
            report.append(f'  {json.dumps(perms["Bluetooth"], indent=2, ensure_ascii=False)}')
        else:
            report.append('警告: manifest.json 中未找到 Bluetooth 权限配置！')
            report.append('现有权限: ' + ', '.join(perms.keys()))
        
        # 检查 dcloud 配置
        dcloud = manifest_data.get('dcloud', {})
        if dcloud:
            report.append(f'\nDCloud 配置: {dcloud}')
    except Exception as e:
        report.append(f'Error: {e}')
    
    report.append('\n')
    
    # 4. 搜索蓝牙相关的 JS 代码
    report.append('=== 4. 蓝牙相关 JS 代码分析 ===')
    js_files = [f for f in z.namelist() if f.endswith('.js')]
    
    bluetooth_refs = []
    for js_file in js_files:
        try:
            content = z.read(js_file).decode('utf-8', errors='ignore')
            if 'bluetooth' in content.lower() or '蓝牙' in content:
                # 找到蓝牙引用
                matches = re.findall(r'[a-zA-Z_][a-zA-Z0-9_]*[Bb]luetooth[a-zA-Z0-9_]*', content)
                if matches:
                    bluetooth_refs.append((js_file, list(set(matches))[:5]))
        except:
            pass
    
    report.append(f'找到 {len(bluetooth_refs)} 个包含蓝牙引用的 JS 文件:')
    for ref in bluetooth_refs[:20]:
        report.append(f'  {ref[0]}: {ref[1]}')
    
    report.append('\n')
    
    # 5. 分析原生库中的蓝牙支持
    report.append('=== 5. 原生库蓝牙支持 ===')
    so_files = [f for f in z.namelist() if f.endswith('.so')]
    
    # 检查是否有专门的蓝牙库
    bluetooth_libs = [f for f in so_files if 'bluetooth' in f.lower() or 'ble' in f.lower()]
    if bluetooth_libs:
        report.append(f'蓝牙相关原生库:')
        for lib in bluetooth_libs:
            report.append(f'  {lib}')
    else:
        report.append('未找到专门的蓝牙原生库（蓝牙功能通过 Android API 实现）')
    
    # 检查 DCloud 蓝牙实现
    try:
        dex = z.read('classes.dex')
        dex_text = dex.decode('utf-8', errors='ignore')
        
        bluetooth_classes = re.findall(r'io/dcloud/feature/bluetooth/[a-zA-Z]+', dex_text)
        if bluetooth_classes:
            report.append(f'\nDCloud 蓝牙类:')
            for cls in set(bluetooth_classes):
                report.append(f'  {cls}')
    except Exception as e:
        report.append(f'Error analyzing DEX: {e}')
    
    report.append('\n')
    
    # 6. 问题诊断
    report.append('=== 6. 蓝牙信息缺失问题诊断 ===')
    report.append('''
可能的原因:

1. **权限未正确声明**
   - AndroidManifest.xml 需要 BLUETOOTH 和 BLUETOOTH_ADMIN 权限
   - Android 6.0+ 需要 ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION
   - Android 12+ 需要 BLUETOOTH_SCAN/BLUETOOTH_CONNECT/BLUETOOTH_ADVERTISE

2. **UniApp 配置问题**
   - manifest.json 中 permissions.Bluetooth 配置可能不完整
   - 需要检查 dcloud_properties.xml 中的 feature 声明

3. **运行时权限问题**
   - Android 6.0+ 需要动态申请位置权限才能扫描蓝牙设备
   - Android 12+ 需要动态申请新的蓝牙权限

4. **代码调用问题**
   - 检查 JS 代码中是否正确调用了 uni.openBluetoothAdapter
   - 检查是否正确处理了蓝牙状态回调

建议修复:
1. 确保 AndroidManifest.xml 包含所有必要的蓝牙权限
2. 在 manifest.json 中正确配置 Bluetooth 权限
3. 在运行时动态申请权限（特别是 Android 6.0+ 和 12+）
4. 添加蓝牙状态监听和错误处理
''')

# Write report
with open('bluetooth_analysis.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(report))

print('蓝牙分析报告已保存: bluetooth_analysis.txt')
