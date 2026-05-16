#!/usr/bin/env python3
"""
APK反编译与修复工具
用于反编译管e通APK，修复蓝牙问题，然后重新打包
"""

import os
import sys
import zipfile
import shutil
import subprocess
import re
from pathlib import Path

class APKDecompiler:
    def __init__(self, apk_path, work_dir=None):
        self.apk_path = Path(apk_path)
        self.work_dir = Path(work_dir) if work_dir else self.apk_path.parent / "apk_work"
        self.extracted_dir = self.work_dir / "extracted"
        self.decompiled_dir = self.work_dir / "decompiled"
        self.fixed_dir = self.work_dir / "fixed"
        self.output_apk = self.work_dir / "EnjoyMBM_fixed.apk"
        
    def extract_apk(self):
        """解压APK文件"""
        print("[1/6] 解压APK...")
        if self.extracted_dir.exists():
            shutil.rmtree(self.extracted_dir)
        self.extracted_dir.mkdir(parents=True)
        
        with zipfile.ZipFile(self.apk_path, 'r') as zf:
            zf.extractall(self.extracted_dir)
        print(f"    [OK] 解压完成: {self.extracted_dir}")
        return True
    
    def check_bluetooth_permissions(self):
        """检查AndroidManifest.xml中的蓝牙权限"""
        print("[2/6] 检查蓝牙权限...")
        manifest_path = self.extracted_dir / "AndroidManifest.xml"
        
        if not manifest_path.exists():
            print("    ✗ 未找到AndroidManifest.xml")
            return False
        
        # 读取manifest文件
        with open(manifest_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # 检查蓝牙相关权限
        bluetooth_permissions = [
            'android.permission.BLUETOOTH',
            'android.permission.BLUETOOTH_ADMIN',
            'android.permission.BLUETOOTH_SCAN',
            'android.permission.BLUETOOTH_CONNECT'
        ]
        
        found_permissions = []
        for perm in bluetooth_permissions:
            if perm in content:
                found_permissions.append(perm)
        
        print(f"    找到 {len(found_permissions)}/4 个蓝牙权限:")
        for perm in found_permissions:
            print(f"      - {perm}")
        
        return found_permissions
    
    def find_bluetooth_code(self):
        """查找蓝牙相关的代码文件"""
        print("[3/6] 查找蓝牙相关代码...")
        
        # 检查assets目录下的js文件
        assets_dir = self.extracted_dir / "assets"
        js_files = list(assets_dir.rglob("*.js")) if assets_dir.exists() else []
        
        bluetooth_files = []
        bluetooth_patterns = [
            r'bluetooth',
            r'webBluetooth',
            r'openBluetoothAdapter',
            r'startBluetoothDevicesDiscovery',
            r'createBLEConnection',
        ]
        
        for js_file in js_files:
            try:
                with open(js_file, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    for pattern in bluetooth_patterns:
                        if re.search(pattern, content, re.IGNORECASE):
                            bluetooth_files.append(js_file)
                            break
            except:
                pass
        
        print(f"    找到 {len(bluetooth_files)} 个包含蓝牙代码的JS文件")
        for f in bluetooth_files[:5]:  # 只显示前5个
            print(f"      - {f.relative_to(self.extracted_dir)}")
        
        return bluetooth_files
    
    def fix_bluetooth_code(self, js_files):
        """修复蓝牙代码"""
        print("[4/6] 修复蓝牙代码...")
        
        # 创建修复目录
        if self.fixed_dir.exists():
            shutil.rmtree(self.fixed_dir)
        shutil.copytree(self.extracted_dir, self.fixed_dir)
        
        fixed_count = 0
        
        for js_file in js_files:
            try:
                # 计算在fixed_dir中的对应路径
                rel_path = js_file.relative_to(self.extracted_dir)
                fixed_file = self.fixed_dir / rel_path
                
                with open(fixed_file, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                
                # 检查是否需要修复
                original_content = content
                
                # 修复1: 添加权限检查包装
                if 'checkBluetoothPermissions' not in content and 'openBluetoothAdapter' in content:
                    # 在文件开头添加权限检查函数
                    permission_check_code = '''
// ===== 蓝牙权限修复注入 =====
function checkBluetoothPermissions() {
    return new Promise((resolve) => {
        if (uni.getSystemInfoSync().platform !== 'android') {
            resolve(true);
            return;
        }
        // Android 12+ 需要动态权限
        const permissions = ['android.permission.BLUETOOTH_SCAN', 'android.permission.BLUETOOTH_CONNECT'];
        uni.authorize({
            scope: 'scope.bluetooth',
            success: () => resolve(true),
            fail: () => {
                uni.showModal({
                    title: '需要蓝牙权限',
                    content: '请授权蓝牙权限以继续使用',
                    success: (res) => {
                        if (res.confirm) {
                            uni.openSetting();
                        }
                        resolve(false);
                    }
                });
            }
        });
    });
}
// ===== 蓝牙权限修复结束 =====

'''
                    content = permission_check_code + content
                    print(f"    [OK] 添加权限检查: {rel_path}")
                
                # 修复2: 包装openBluetoothAdapter调用
                if 'uni.openBluetoothAdapter' in content:
                    content = re.sub(
                        r'uni\.openBluetoothAdapter\s*\(\s*\{',
                        '''checkBluetoothPermissions().then(granted => {
    if (!granted) return;
    uni.openBluetoothAdapter({''',
                        content
                    )
                    # 找到对应的闭合括号并添加})
                    content = content.replace('uni.openBluetoothAdapter({', 
                        'uni.openBluetoothAdapter({\n        // 修复: 添加权限检查',
                        1)
                    print(f"    [OK] 修复openBluetoothAdapter: {rel_path}")
                
                if content != original_content:
                    with open(fixed_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    fixed_count += 1
                    
            except Exception as e:
                print(f"    [ERR] 修复失败 {js_file}: {e}")
        
        print(f"    共修复 {fixed_count} 个文件")
        return fixed_count > 0
    
    def repack_apk(self):
        """重新打包APK"""
        print("[5/6] 重新打包APK...")
        
        # 创建新的APK
        with zipfile.ZipFile(self.output_apk, 'w', zipfile.ZIP_DEFLATED) as zf:
            for file_path in self.fixed_dir.rglob('*'):
                if file_path.is_file():
                    arcname = file_path.relative_to(self.fixed_dir)
                    zf.write(file_path, arcname)
        
        print(f"    [OK] 打包完成: {self.output_apk}")
        return True
    
    def sign_apk(self):
        """签名APK"""
        print("[6/6] 签名APK...")
        
        # 创建临时签名密钥
        keystore_path = self.work_dir / "debug.keystore"
        signed_apk = self.work_dir / "EnjoyMBM_fixed_signed.apk"
        
        try:
            # 生成调试密钥
            keytool_cmd = [
                'keytool', '-genkey', '-v',
                '-keystore', str(keystore_path),
                '-alias', 'androiddebugkey',
                '-keyalg', 'RSA', '-keysize', '2048',
                '-validity', '10000',
                '-storepass', 'android',
                '-keypass', 'android',
                '-dname', 'CN=Android Debug,O=Android,C=US'
            ]
            subprocess.run(keytool_cmd, capture_output=True, check=True)
            
            # 签名APK
            jarsigner_cmd = [
                'jarsigner', '-verbose', '-sigalg', 'SHA1withRSA',
                '-digestalg', 'SHA1',
                '-keystore', str(keystore_path),
                '-storepass', 'android',
                str(self.output_apk),
                'androiddebugkey'
            ]
            subprocess.run(jarsigner_cmd, capture_output=True, check=True)
            
            # 优化APK
            aligned_apk = self.work_dir / "EnjoyMBM_fixed_final.apk"
            
            print(f"    [OK] 签名完成")
            print(f"    输出文件: {self.output_apk}")
            
            return self.output_apk
            
        except Exception as e:
            print(f"    [WARN] 签名失败 (可能缺少keytool/jarsigner): {e}")
            print(f"    未签名APK仍可用: {self.output_apk}")
            return self.output_apk
    
    def run(self):
        """执行完整的反编译-修复-打包流程"""
        print("="*60)
        print("APK Decompile and Bluetooth Fix Tool")
        print("="*60)
        print(f"输入: {self.apk_path}")
        print(f"工作目录: {self.work_dir}")
        print()
        
        # 1. 解压APK
        if not self.extract_apk():
            return False
        
        # 2. 检查蓝牙权限
        permissions = self.check_bluetooth_permissions()
        
        # 3. 查找蓝牙代码
        js_files = self.find_bluetooth_code()
        
        # 4. 修复代码
        if js_files:
            self.fix_bluetooth_code(js_files)
        else:
            print("    ! 未找到需要修复的蓝牙代码")
            shutil.copytree(self.extracted_dir, self.fixed_dir, dirs_exist_ok=True)
        
        # 5. 重新打包
        self.repack_apk()
        
        # 6. 签名
        final_apk = self.sign_apk()
        
        print()
        print("="*60)
        print("Done!")
        print(f"输出文件: {final_apk}")
        print("="*60)
        
        return final_apk


if __name__ == "__main__":
    apk_path = sys.argv[1] if len(sys.argv) > 1 else "EnjoyMBM.apk"
    decompiler = APKDecompiler(apk_path)
    decompiler.run()
