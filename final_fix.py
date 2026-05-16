#!/usr/bin/env python3
"""
生成已修复但未签名的 APK，用于在线签名
"""

import zipfile
import os

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
OUTPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_for_online_sign.apk")

def find_source_apk():
    """查找源 APK"""
    for name in ["EnjoyMBM.apk", "EnjoyMBM_fixed.apk", "EnjoyMBM_fixed_final.apk"]:
        path = os.path.join(WORK_DIR, name)
        if os.path.exists(path):
            return path
    return None

def main():
    print("=" * 60)
    print("Generating APK for Online Signing")
    print("=" * 60)
    
    source = find_source_apk()
    if not source:
        print("Error: No source APK found!")
        return
    
    print(f"Source: {source}")
    
    with zipfile.ZipFile(source, 'r') as zf_in:
        js_name = "assets/apps/__UNI__092D5A7/www/app-service.js"
        
        try:
            js_content = zf_in.read(js_name).decode('utf-8', errors='ignore')
        except KeyError:
            print(f"Error: {js_name} not found!")
            return
        
        print(f"Original JS: {len(js_content):,} bytes")
        
        # 修复代码
        fix_code = '''/* BT_FIX */var BT={check:function(cb){var i=uni.getSystemInfoSync();if(i.platform==="ios"){cb&&cb(true);return;}var v=parseInt((i.system||"").split(".")[0]||"0");if(v>=12){uni.authorize({scope:"scope.bluetooth",success:function(){cb&&cb(true);},fail:function(){uni.showModal({title:"需要蓝牙权限",content:"请允许使用蓝牙",confirmText:"去设置",success:function(r){r.confirm&&uni.openSetting();}});cb&&cb(false);}});}else if(v>=6){uni.authorize({scope:"scope.userLocation",success:function(){cb&&cb(true);},fail:function(){uni.showToast({title:"需要位置权限",icon:"none"});cb&&cb(false);}});}else{cb&&cb(true);}},init:function(o){this.check(function(h){if(!h){o&&o.fail&&o.fail({errCode:-1,errMsg:"no permission"});return;}var s=o.success,f=o.fail;o.success=function(r){console.log("BT ok");s&&s(r);};o.fail=function(e){console.error("BT fail:",e);var m="蓝牙初始化失败";if(e.errCode===10001)m="请开启手机蓝牙";else if(e.errCode===10002)m="蓝牙未授权";uni.showToast({title:m,icon:"none"});f&&f(e);};uni.openBluetoothAdapter(o);});}};
'''
        
        insert_pos = js_content.find("var ")
        if insert_pos < 0:
            insert_pos = js_content.find("function ")
        if insert_pos < 0:
            insert_pos = 0
        
        new_js = js_content[:insert_pos] + fix_code + js_content[insert_pos:]
        new_js_bytes = new_js.encode('utf-8')
        
        print(f"Fixed JS: {len(new_js_bytes):,} bytes")
        
        # 创建新 APK
        print("\nCreating APK...")
        with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zf_out:
            for item in zf_in.infolist():
                if item.filename == js_name:
                    zf_out.writestr(item, new_js_bytes)
                else:
                    zf_out.writestr(item, zf_in.read(item.filename))
    
    size = os.path.getsize(OUTPUT_APK)
    print(f"Done! Size: {size / 1024 / 1024:.2f} MB")
    print(f"\nOutput: {OUTPUT_APK}")

if __name__ == "__main__":
    main()
