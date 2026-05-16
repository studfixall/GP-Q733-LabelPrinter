#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
实际修复APK中的蓝牙问题
"""

import os
import re
import json

BASE_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\extracted"
FIXED_COUNT = 0

def fix_bluetooth_in_js(file_path):
    """修复JS文件中的蓝牙问题"""
    global FIXED_COUNT
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        original = content
        
        # 检查是否包含蓝牙相关代码
        if 'openBluetoothAdapter' not in content and 'createBLEConnection' not in content:
            return False
        
        # 修复1: 添加权限检查包装
        if 'uni.openBluetoothAdapter' in content and 'BLUETOOTH_FIX' not in content:
            # 在文件开头添加标记和工具函数
            fix_header = '''/* BLUETOOTH_FIX_APPLIED */
// 蓝牙权限检查和初始化工具
function checkBluetoothPermission() {
  return new Promise((resolve) => {
    const systemInfo = uni.getSystemInfoSync();
    if (systemInfo.platform === 'ios') {
      resolve(true);
      return;
    }
    const version = parseInt((systemInfo.system || '').split('.')[0] || '0');
    if (version >= 12) {
      uni.authorize({
        scope: 'scope.bluetooth',
        success: () => resolve(true),
        fail: () => {
          uni.showModal({
            title: '需要蓝牙权限',
            content: '请允许应用使用蓝牙功能',
            confirmText: '去设置',
            success: (res) => {
              if (res.confirm) uni.openSetting();
            }
          });
          resolve(false);
        }
      });
    } else if (version >= 6) {
      uni.authorize({
        scope: 'scope.userLocation',
        success: () => resolve(true),
        fail: () => {
          uni.showToast({ title: '需要位置权限才能扫描蓝牙设备', icon: 'none' });
          resolve(false);
        }
      });
    } else {
      resolve(true);
    }
  });
}

function initBluetoothAdapterSafe(options) {
  const originalSuccess = options.success;
  const originalFail = options.fail;
  
  checkBluetoothPermission().then((hasPermission) => {
    if (!hasPermission) {
      if (originalFail) originalFail({ errCode: -1, errMsg: '没有蓝牙权限' });
      return;
    }
    
    uni.openBluetoothAdapter({
      success: (res) => {
        console.log('蓝牙适配器初始化成功:', res);
        if (originalSuccess) originalSuccess(res);
      },
      fail: (err) => {
        console.error('蓝牙适配器初始化失败:', err);
        let msg = '蓝牙初始化失败';
        if (err.errCode === 10001) msg = '请开启手机蓝牙';
        else if (err.errCode === 10002) msg = '蓝牙未授权';
        uni.showToast({ title: msg, icon: 'none' });
        if (originalFail) originalFail(err);
      }
    });
  });
}

'''
            content = fix_header + content
            FIXED_COUNT += 1
        
        # 修复2: 替换 uni.openBluetoothAdapter 调用
        # 简单替换：在调用前添加权限检查
        pattern = r'(uni\.openBluetoothAdapter\s*\(\s*\{)'
        replacement = r'checkBluetoothPermission().then((hasPerm) => { if (!hasPerm) return; \1'
        content = re.sub(pattern, replacement, content)
        
        # 修复3: 添加错误处理
        if 'fail:' in content and 'err.errCode' not in content:
            # 增强现有的 fail 处理
            content = content.replace(
                'fail: function(err)',
                'fail: function(err) { console.error("蓝牙操作失败:", err);'
            )
        
        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  [FIXED] {os.path.basename(file_path)}")
            return True
        
        return False
        
    except Exception as e:
        print(f"  [ERROR] {file_path}: {e}")
        return False

def main():
    """主函数"""
    global FIXED_COUNT
    
    print("=" * 60)
    print("开始修复APK中的蓝牙问题")
    print("=" * 60)
    
    # 遍历所有JS文件
    js_files = []
    for root, dirs, files in os.walk(BASE_DIR):
        for file in files:
            if file.endswith('.js'):
                js_files.append(os.path.join(root, file))
    
    print(f"\n找到 {len(js_files)} 个JS文件")
    print("-" * 60)
    
    # 筛选包含蓝牙代码的文件
    bluetooth_files = []
    for js_file in js_files:
        try:
            with open(js_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            if 'openBluetoothAdapter' in content or 'createBLEConnection' in content:
                bluetooth_files.append(js_file)
        except:
            pass
    
    print(f"其中 {len(bluetooth_files)} 个文件包含蓝牙代码")
    print("-" * 60)
    
    # 修复每个文件
    fixed_files = []
    for js_file in bluetooth_files:
        rel_path = os.path.relpath(js_file, BASE_DIR)
        print(f"\n处理: {rel_path}")
        if fix_bluetooth_in_js(js_file):
            fixed_files.append(rel_path)
    
    print("\n" + "=" * 60)
    print(f"修复完成！共修复 {len(fixed_files)} 个文件")
    print("=" * 60)
    
    # 保存修复记录
    record = {
        'fixed_files': fixed_files,
        'total_js_files': len(js_files),
        'bluetooth_files': len(bluetooth_files),
        'fixed_count': len(fixed_files)
    }
    
    record_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\fix_record.json"
    with open(record_path, 'w', encoding='utf-8') as f:
        json.dump(record, f, indent=2, ensure_ascii=False)
    
    print(f"\n修复记录已保存: {record_path}")
    
    return fixed_files

if __name__ == '__main__':
    main()
