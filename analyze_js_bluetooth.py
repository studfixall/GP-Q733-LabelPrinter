import zipfile
import re

apk_path = 'EnjoyMBM.apk'

with zipfile.ZipFile(apk_path, 'r') as z:
    print('=== 蓝牙 JS 代码深度分析 ===\n')
    
    # 读取主要的蓝牙相关 JS 文件
    target_files = [
        'assets/apps/__UNI__092D5A7/www/hybrid/html/RmisViews/DiscLablePrint/bluetooch.js',
        'assets/apps/__UNI__092D5A7/www/hybrid/html/RmisViews/DiscLablePrint/index.js',
        'assets/apps/__UNI__092D5A7/www/hybrid/html/CrmViews/OrderBillVerify/OrderList.js',
        'assets/apps/__UNI__092D5A7/www/hybrid/html/js/gprint/blue.js',
        'assets/apps/__UNI__092D5A7/www/hybrid/html/SelectorViews/bluetoothdevice_vue.js'
    ]
    
    for js_file in target_files:
        print(f'=== 分析: {js_file.split("/")[-1]} ===')
        try:
            content = z.read(js_file).decode('utf-8', errors='ignore')
            
            # 查找蓝牙 API 调用
            bluetooth_apis = [
                'openBluetoothAdapter',
                'closeBluetoothAdapter',
                'getBluetoothAdapterState',
                'startBluetoothDevicesDiscovery',
                'stopBluetoothDevicesDiscovery',
                'getBluetoothDevices',
                'getConnectedBluetoothDevices',
                'createBLEConnection',
                'closeBLEConnection',
                'getBLEDeviceServices',
                'getBLEDeviceCharacteristics',
                'readBLECharacteristicValue',
                'writeBLECharacteristicValue',
                'notifyBLECharacteristicValueChange',
                'onBLEConnectionStateChange',
                'onBLECharacteristicValueChange',
                'onBluetoothDeviceFound',
                'onBluetoothAdapterStateChange'
            ]
            
            found_apis = []
            for api in bluetooth_apis:
                if api in content:
                    count = content.count(api)
                    found_apis.append((api, count))
            
            if found_apis:
                print(f'使用的蓝牙 API ({len(found_apis)} 个):')
                for api, count in found_apis:
                    print(f'  - {api} ({count} 次)')
            
            # 查找错误处理
            error_handlers = re.findall(r'(fail|error|catch)[\s\S]{0,50}?bluetooth', content, re.IGNORECASE)
            if error_handlers:
                print(f'\n错误处理代码片段:')
                for i, handler in enumerate(error_handlers[:3]):
                    print(f'  {i+1}. {handler[:100]}...')
            
            # 查找 webBluetoothDevice 自定义函数
            if 'webBluetoothDevice' in content:
                print('\n[发现] 使用自定义 webBluetoothDevice 函数')
                # 提取函数定义
                func_match = re.search(r'webBluetoothDevice[\s\S]{0,500}?function[\s\S]{0,1000}', content)
                if func_match:
                    print(f'  函数片段: {func_match.group(0)[:200]}...')
            
            print('')
        except Exception as e:
            print(f'Error: {e}\n')
    
    # 分析 app-service.js 中的蓝牙实现
    print('=== 分析 app-service.js 蓝牙实现 ===')
    try:
        content = z.read('assets/apps/__UNI__092D5A7/www/app-service.js').decode('utf-8', errors='ignore')
        
        # 查找蓝牙相关代码块
        bluetooth_sections = re.findall(r'[Bb]luetooth[\w]*[\s\S]{0,500}', content)
        print(f'找到 {len(bluetooth_sections)} 个蓝牙相关代码段')
        
        # 查找 uni.openBluetoothAdapter 调用
        adapter_calls = re.findall(r'uni\.openBluetoothAdapter[\s\S]{0,300}', content)
        print(f'\nopenBluetoothAdapter 调用 ({len(adapter_calls)} 处):')
        for i, call in enumerate(adapter_calls[:3]):
            print(f'  {i+1}. {call[:200]}...')
        
        # 查找错误码处理
        error_codes = re.findall(r'errCode[\s\S]{0,20}\d+', content)
        if error_codes:
            print(f'\n错误码处理 ({len(error_codes)} 处):')
            for code in error_codes[:5]:
                print(f'  {code}')
        
    except Exception as e:
        print(f'Error: {e}')
    
    print('\n=== 问题分析 ===')
    print('''
根据代码分析，发现以下问题：

1. **使用了自定义蓝牙函数 webBluetoothDevice**
   - 这不是 UniApp 标准 API
   - 可能是封装层或插件提供的
   - 需要确认该函数是否正确调用了底层 API

2. **权限已声明但可能未动态申请**
   - Android 6.0+ 需要动态申请位置权限
   - Android 12+ 需要动态申请新的蓝牙权限
   - 代码中可能缺少权限检查

3. **可能的解决方案：**
   
   a) 检查权限申请代码：
   ```javascript
   // 在调用蓝牙功能前检查并申请权限
   async function checkBluetoothPermission() {
     // Android 12+ 新权限
     if (uni.getSystemInfoSync().platform === 'android') {
       const version = parseInt(uni.getSystemInfoSync().system.split('.')[0]);
       if (version >= 12) {
         // 申请 BLUETOOTH_SCAN 和 BLUETOOTH_CONNECT
         const res = await uni.authorize({ scope: 'scope.bluetooth' });
         return res;
       }
     }
     return true;
   }
   ```
   
   b) 确保正确初始化蓝牙适配器：
   ```javascript
   uni.openBluetoothAdapter({
     success: (res) => {
       console.log('蓝牙适配器初始化成功', res);
       // 继续扫描设备
     },
     fail: (err) => {
       console.error('蓝牙适配器初始化失败', err);
       if (err.errCode === 10001) {
         uni.showToast({ title: '请开启蓝牙', icon: 'none' });
       }
     }
   });
   ```
   
   c) 监听蓝牙状态变化：
   ```javascript
   uni.onBluetoothAdapterStateChange((res) => {
     console.log('蓝牙状态变化:', res);
     if (!res.available) {
       uni.showToast({ title: '蓝牙已关闭', icon: 'none' });
     }
   });
   ```
''')
