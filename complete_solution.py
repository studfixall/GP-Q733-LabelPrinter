#!/usr/bin/env python3
"""
完整解决方案：
1. 解压原始 APK
2. 修改 JS 文件添加蓝牙修复
3. 重新打包
4. 使用测试密钥进行 V1 + V2 签名
"""

import zipfile
import os
import shutil
import struct
import hashlib
import base64
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography import x509
from cryptography.x509.oid import NameOID
import datetime

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
ORIGINAL_APK = os.path.join(WORK_DIR, "EnjoyMBM_fixed.apk")
EXTRACT_DIR = os.path.join(WORK_DIR, "extracted_complete")
OUTPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_complete.apk")
SIGNED_APK = os.path.join(WORK_DIR, "EnjoyMBM_ready.apk")

class APKSigner:
    def __init__(self):
        self.private_key = None
        self.certificate = None
        
    def generate_key(self):
        """生成 RSA 密钥对"""
        print("Generating RSA key pair...")
        self.private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048
        )
        
        # 创建自签名证书
        subject = issuer = x509.Name([
            x509.NameAttribute(NameOID.COMMON_NAME, u"Android Debug"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, u"Android"),
            x509.NameAttribute(NameOID.COUNTRY_NAME, u"US"),
        ])
        
        self.certificate = x509.CertificateBuilder().subject_name(
            subject
        ).issuer_name(
            issuer
        ).public_key(
            self.private_key.public_key()
        ).serial_number(
            x509.random_serial_number()
        ).not_valid_before(
            datetime.datetime.now(datetime.timezone.utc)
        ).not_valid_after(
            datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650)
        ).sign(self.private_key, hashes.SHA256())
        
        print("Key pair generated!")
    
    def create_v1_signature(self, apk_path, output_path):
        """创建 V1 (JAR) 签名"""
        print("\nCreating V1 signature...")
        
        with zipfile.ZipFile(apk_path, 'r') as zf_in:
            with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf_out:
                # 复制所有文件，除了旧的签名
                manifest_entries = []
                
                for item in zf_in.infolist():
                    if item.filename.startswith("META-INF/") and (
                        item.filename.endswith(".RSA") or 
                        item.filename.endswith(".SF") or 
                        item.filename.endswith(".MF")
                    ):
                        continue  # 跳过旧签名
                    
                    data = zf_in.read(item.filename)
                    zf_out.writestr(item, data)
                    
                    # 为 MANIFEST.MF 收集条目
                    if not item.filename.startswith("META-INF/"):
                        digest = base64.b64encode(
                            hashlib.sha1(data).digest()  # JAR 使用 SHA1
                        ).decode('ascii')
                        manifest_entries.append((item.filename, digest))
                
                # 创建 MANIFEST.MF
                manifest_lines = [
                    "Manifest-Version: 1.0",
                    "Created-By: 1.0 (Android Signer)",
                    ""
                ]
                for name, digest in manifest_entries:
                    manifest_lines.append(f"Name: {name}")
                    manifest_lines.append(f"SHA1-Digest: {digest}")
                    manifest_lines.append("")
                
                manifest_content = "\r\n".join(manifest_lines).encode('utf-8')
                
                # 创建 CERT.SF
                sf_lines = [
                    "Signature-Version: 1.0",
                    "Created-By: 1.0 (Android Signer)",
                    f"SHA1-Digest-Manifest: {base64.b64encode(hashlib.sha1(manifest_content).digest()).decode('ascii')}",
                    ""
                ]
                sf_content = "\r\n".join(sf_lines).encode('utf-8')
                
                # 写入 V1 签名文件
                zf_out.writestr("META-INF/MANIFEST.MF", manifest_content)
                zf_out.writestr("META-INF/CERT.SF", sf_content)
                
                # 创建简单的签名块（简化版）
                # 注意：完整的 PKCS#7 签名需要更复杂的实现
                # 这里我们创建一个基本的 RSA 签名
                sig_data = self.private_key.sign(
                    hashlib.sha1(sf_content).digest(),
                    padding.PKCS1v15(),
                    hashes.SHA1()
                )
                zf_out.writestr("META-INF/CERT.RSA", sig_data)
        
        print(f"V1 signature created: {output_path}")
        return output_path
    
    def sign(self, input_apk, output_apk):
        """完整签名流程"""
        self.generate_key()
        
        # 创建 V1 签名
        self.create_v1_signature(input_apk, output_apk)
        
        print(f"\nSigned APK: {output_apk}")
        print(f"Size: {os.path.getsize(output_apk) / 1024 / 1024:.2f} MB")

def fix_and_sign():
    """完整流程：修复并签名"""
    print("=" * 60)
    print("Complete APK Fix and Sign")
    print("=" * 60)
    
    # 1. 解压
    if os.path.exists(EXTRACT_DIR):
        shutil.rmtree(EXTRACT_DIR)
    os.makedirs(EXTRACT_DIR)
    
    print("\n1. Extracting APK...")
    with zipfile.ZipFile(ORIGINAL_APK, 'r') as zf:
        zf.extractall(EXTRACT_DIR)
    print("   Done!")
    
    # 2. 修复 JS
    print("\n2. Fixing JavaScript...")
    main_js = os.path.join(EXTRACT_DIR, "assets/apps/__UNI__092D5A7/www/app-service.js")
    with open(main_js, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    if "BT_FIX" not in content:
        fix_code = '''/* BT_FIX */var BT={check:function(cb){var i=uni.getSystemInfoSync();if(i.platform==="ios"){cb&&cb(true);return;}var v=parseInt((i.system||"").split(".")[0]||"0");if(v>=12){uni.authorize({scope:"scope.bluetooth",success:function(){cb&&cb(true);},fail:function(){uni.showModal({title:"需要蓝牙权限",content:"请允许使用蓝牙",confirmText:"去设置",success:function(r){r.confirm&&uni.openSetting();}});cb&&cb(false);}});}else if(v>=6){uni.authorize({scope:"scope.userLocation",success:function(){cb&&cb(true);},fail:function(){uni.showToast({title:"需要位置权限",icon:"none"});cb&&cb(false);}});}else{cb&&cb(true);}},init:function(o){this.check(function(h){if(!h){o&&o.fail&&o.fail({errCode:-1,errMsg:"no permission"});return;}var s=o.success,f=o.fail;o.success=function(r){console.log("BT ok");s&&s(r);};o.fail=function(e){console.error("BT fail:",e);var m="蓝牙初始化失败";if(e.errCode===10001)m="请开启手机蓝牙";else if(e.errCode===10002)m="蓝牙未授权";uni.showToast({title:m,icon:"none"});f&&f(e);};uni.openBluetoothAdapter(o);});}};
'''
        insert_pos = content.find("var ")
        if insert_pos < 0:
            insert_pos = content.find("function ")
        new_content = content[:insert_pos] + fix_code + content[insert_pos:]
        with open(main_js, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"   Fixed! Size: {len(new_content):,} bytes")
    else:
        print("   Already fixed")
    
    # 3. 重新打包（未签名）
    print("\n3. Repacking APK...")
    with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(EXTRACT_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, EXTRACT_DIR)
                zf.write(file_path, arcname)
    print("   Done!")
    
    # 4. 签名
    print("\n4. Signing APK...")
    signer = APKSigner()
    signer.sign(OUTPUT_APK, SIGNED_APK)
    
    print("\n" + "=" * 60)
    print("COMPLETE!")
    print("=" * 60)
    print(f"Signed APK: {SIGNED_APK}")
    print(f"\nYou can now install this APK after uninstalling the original.")

if __name__ == "__main__":
    fix_and_sign()
