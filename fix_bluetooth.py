import zipfile
import re
import struct

apk_path = 'EnjoyMBM.apk'

with zipfile.ZipFile(apk_path, 'r') as z:
    manifest_data = z.read('AndroidManifest.xml')
    
    print('=== 蓝牙问题分析与修复方案 ===\n')
    
    # 尝试从二进制 manifest 中提取字符串
    strings = set()
    i = 0
    while i < len(manifest_data) - 4:
        # 尝试读取长度前缀的字符串 (UTF-16LE)
        try:
            length = struct.unpack('<H', manifest_data[i:i+2])[0]
            if 0 < length < 200:
                str_data = manifest_data[i+2:i+2+length*2]
                try:
                    s = str_data.decode('utf-16le', errors='ignore')
                    if s and len(s) > 3 and s.isprintable():
                        strings.add(s)
                except:
                    pass
        except:
            pass
        i += 1
    
    # 查找蓝牙相关字符串
    bluetooth_strings = [s for s in strings if 'bluetooth' in s.lower() or '蓝牙' in s]
    print(f'从 Manifest 提取到 {len(bluetooth_strings)} 个蓝牙相关字符串:')
    for s in bluetooth_strings[:20]:
        print(f'  {s}')
    
    # 查找权限字符串
    perm_strings = [s for s in strings if s.startswith('android.permission.')]
    print(f'\n从 Manifest 提取到 {len(perm_strings)} 个权限字符串:')
    for s in perm_strings[:50]:
        print(f'  {s}')
    
    # 检查蓝牙权限
    bluetooth_perms = [s for s in perm_strings if 'BLUETOOTH' in s.upper()]
    print(f'\n=== 蓝牙权限检查结果 ===')
    print(f'找到 {len(bluetooth_perms)} 个蓝牙权限:')
    for p in bluetooth_perms:
        print(f'  [OK] {p}')
    
    if not bluetooth_perms:
        print('  [MISSING] 未找到任何蓝牙权限！')
        print('\n=== 问题确认 ===')
        print('AndroidManifest.xml 中确实缺少蓝牙权限声明！')
        print('这是导致蓝牙功能无法正常工作的根本原因。')
        
        print('\n=== 修复方案 ===')
        print('''
需要在 AndroidManifest.xml 中添加以下权限：

<!-- 基本蓝牙权限 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

<!-- Android 6.0+ 扫描蓝牙设备需要位置权限 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Android 12+ 新权限模型 -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- 声明蓝牙硬件特性 -->
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />

=== 修复步骤 ===

1. **修改 UniApp 项目的 manifest.json:**
   在 "permissions" 中添加：
   {
     "Bluetooth": {
       "description": "蓝牙通信"
     }
   }

2. **修改 AndroidManifest.xml:**
   添加上述权限声明

3. **在代码中动态申请权限:**
   - Android 6.0+ 需要申请位置权限
   - Android 12+ 需要申请新的蓝牙权限

4. **重新打包 APK**

=== 代码示例：动态申请权限 ===

// 在调用蓝牙功能前申请权限
uni.authorize({
  scope: 'scope.bluetooth',
  success() {
    console.log('蓝牙授权成功');
    // 初始化蓝牙适配器
    uni.openBluetoothAdapter({
      success(res) {
        console.log('蓝牙初始化成功', res);
      },
      fail(err) {
        console.error('蓝牙初始化失败', err);
      }
    });
  },
  fail(err) {
    console.error('蓝牙授权失败', err);
    uni.showModal({
      title: '提示',
      content: '需要蓝牙权限才能使用该功能',
      success(res) {
        if (res.confirm) {
          uni.openSetting();
        }
      }
    });
  }
});
''')
