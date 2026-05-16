#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复APK中的蓝牙问题 - 安全版本
"""

import os
import re
import json
import shutil

BASE_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\extracted"

def fix_app_service_js(file_path):
    """修复主JS文件 - 在第一个函数定义前插入工具函数"""
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # 如果已经修复过，跳过
        if "BLUETOOTH_FIX_APPLIED" in content:
            print("  Already fixed, skipping")
            return True
        
        # 找到第一个 "function" 或 "var" 的位置，在其之前插入
        # 寻找常见的模块开头模式
        insert_marker = None
        for marker in ["var App", "function", "(function", "define(", "require(", "module.exports"]:
            pos = content.find(marker)
            if pos > 0 and (insert_marker is None or pos < insert_marker[0]):
                insert_marker = (pos, marker)
        
        if not insert_marker:
            print("  Could not find insertion point")
            return False
        
        insert_pos = insert_marker[0]
        print(f"  Inserting before: {insert_marker[1]}")
        
        # 简化的修复代码 - 只添加必要的包装函数
        fix_code = '''/* BLUETOOTH_FIX_APPLIED */
// Bluetooth permission helper
var BluetoothHelper = {
  checkPermission: function(callback) {
    var systemInfo = uni.getSystemInfoSync();
    if (systemInfo.platform === 'ios') {
      if (callback) callback(true);
      return;
    }
    var version = parseInt((systemInfo.system || '').split('.')[0] || '0');
    if (version >= 12) {
      uni.authorize({
        scope: 'scope.bluetooth',
        success: function() { if (callback) callback(true); },
        fail: function() {
          uni.showModal({
            title: '需要蓝牙权限',
            content: '请允许应用使用蓝牙功能',
            confirmText: '去设置',
            success: function(res) { if (res.confirm) uni.openSetting(); }
          });
          if (callback) callback(false);
        }
      });
    } else if (version >= 6) {
      uni.authorize({
        scope: 'scope.userLocation',
        success: function() { if (callback) callback(true); },
        fail: function() {
          uni.showToast({ title: '需要位置权限才能扫描蓝牙设备', icon: 'none' });
          if (callback) callback(false);
        }
      });
    } else {
      if (callback) callback(true);
    }
  },
  initAdapter: function(options) {
    BluetoothHelper.checkPermission(function(hasPerm) {
      if (!hasPerm) {
        if (options && options.fail) options.fail({ errCode: -1, errMsg: 'no permission' });
        return;
      }
      var opts = options || {};
      var origSuccess = opts.success;
      var origFail = opts.fail;
      opts.success = function(res) {
        console.log('Bluetooth adapter initialized');
        if (origSuccess) origSuccess(res);
      };
      opts.fail = function(err) {
        console.error('Bluetooth init failed:', err);
        var msg = '蓝牙初始化失败';
        if (err.errCode === 10001) msg = '请开启手机蓝牙';
        else if (err.errCode === 10002) msg = '蓝牙未授权';
        uni.showToast({ title: msg, icon: 'none' });
        if (origFail) origFail(err);
      };
      uni.openBluetoothAdapter(opts);
    });
  }
};

'''
        
        # 插入修复代码
        new_content = content[:insert_pos] + fix_code + content[insert_pos:]
        
        # 保存
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"  [FIXED] {os.path.basename(file_path)}")
        return True
        
    except Exception as e:
        print(f"  [ERROR] {e}")
        return False

def main():
    """主函数"""
    
    print("=" * 60)
    print("Applying Bluetooth Fix v2")
    print("=" * 60)
    
    # 重新解压原始APK
    original_apk = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed.apk"
    extract_dir = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\extracted_v2"
    
    # 清理并重新解压
    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)
    os.makedirs(extract_dir)
    
    print(f"\nExtracting original APK...")
    import zipfile
    with zipfile.ZipFile(original_apk, 'r') as zf:
        zf.extractall(extract_dir)
    print("Extraction complete")
    
    # 修复主JS文件
    main_js = os.path.join(extract_dir, "assets/apps/__UNI__092D5A7/www/app-service.js")
    print(f"\nFixing: {main_js}")
    
    if os.path.exists(main_js):
        fix_app_service_js(main_js)
    else:
        print("  Main JS file not found!")
        return
    
    # 重新打包
    output_apk = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v3.apk"
    print(f"\nRepacking to: {output_apk}")
    
    with zipfile.ZipFile(output_apk, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(extract_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, extract_dir)
                zf.write(file_path, arcname)
    
    file_size = os.path.getsize(output_apk)
    print(f"Done! Size: {file_size/1024/1024:.2f} MB")
    print(f"Output: {output_apk}")

if __name__ == '__main__':
    main()
