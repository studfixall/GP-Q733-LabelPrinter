#!/usr/bin/env python3
"""
手动创建 V1 签名（JAR 签名）
这是最简单的签名方式，兼容性好
"""

import zipfile
import os
import hashlib
import base64
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography import x509
from cryptography.x509.oid import NameOID
import datetime

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
APK_INPUT = os.path.join(WORK_DIR, "EnjoyMBM_fixed_final.apk")
APK_OUTPUT = os.path.join(WORK_DIR, "EnjoyMBM_v1signed.apk")

def generate_key():
    """生成 RSA 密钥对"""
    key_path = os.path.join(WORK_DIR, "signing_key.pem")
    cert_path = os.path.join(WORK_DIR, "signing_cert.pem")
    
    if os.path.exists(key_path):
        print("Using existing key")
        with open(key_path, "rb") as f:
            private_key = serialization.load_pem_private_key(f.read(), password=None)
        return private_key
    
    print("Generating RSA key pair...")
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )
    
    # 保存私钥
    with open(key_path, "wb") as f:
        f.write(private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.TraditionalOpenSSL,
            encryption_algorithm=serialization.NoEncryption()
        ))
    
    # 生成证书
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COMMON_NAME, u"Android Debug"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, u"Android"),
    ])
    
    cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        private_key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.datetime.now(datetime.timezone.utc)
    ).not_valid_after(
        datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650)
    ).sign(private_key, hashes.SHA256())
    
    with open(cert_path, "wb") as f:
        f.write(cert.public_bytes(serialization.Encoding.PEM))
    
    print("Key pair generated!")
    return private_key

def sign_manifest(manifest_content, private_key):
    """对 MANIFEST.MF 的摘要进行签名"""
    # 计算 SHA-256 摘要
    digest = hashlib.sha256(manifest_content).digest()
    
    # 使用私钥签名
    signature = private_key.sign(
        digest,
        padding.PKCS1v15(),
        hashes.SHA256()
    )
    
    return base64.b64encode(signature).decode('ascii')

def create_v1_signature(apk_path, output_path, private_key):
    """创建 V1 签名（JAR 签名）"""
    print("\nCreating V1 signature...")
    
    with zipfile.ZipFile(apk_path, 'r') as zf_in:
        with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf_out:
            # 复制所有原始文件
            for item in zf_in.infolist():
                data = zf_in.read(item.filename)
                zf_out.writestr(item, data)
            
            # 创建 MANIFEST.MF
            manifest_lines = ["Manifest-Version: 1.0", "Created-By: Python", ""]
            
            # 为每个文件计算摘要
            for item in zf_in.infolist():
                if item.filename.startswith("META-INF/"):
                    continue
                data = zf_in.read(item.filename)
                digest = base64.b64encode(hashlib.sha256(data).digest()).decode('ascii')
                manifest_lines.append(f"Name: {item.filename}")
                manifest_lines.append(f"SHA-256-Digest: {digest}")
                manifest_lines.append("")
            
            manifest_content = "\r\n".join(manifest_lines).encode('utf-8')
            
            # 创建 CERT.SF
            sf_lines = ["Signature-Version: 1.0", "Created-By: Python", ""]
            sf_digest = base64.b64encode(hashlib.sha256(manifest_content).digest()).decode('ascii')
            sf_lines.append(f"SHA-256-Digest-Manifest: {sf_digest}")
            sf_lines.append("")
            
            # 为 MANIFEST.MF 的每个条目添加摘要
            for line in manifest_lines[3:]:  # 跳过头部
                if line.startswith("Name: "):
                    sf_lines.append(line)
                elif line.startswith("SHA-256-Digest: "):
                    sf_lines.append(line)
                elif line == "":
                    sf_lines.append("")
            
            sf_content = "\r\n".join(sf_lines).encode('utf-8')
            
            # 签名 SF 文件
            sf_digest_for_sig = hashlib.sha256(sf_content).digest()
            sig = private_key.sign(
                sf_digest_for_sig,
                padding.PKCS1v15(),
                hashes.SHA256()
            )
            
            # 创建 CERT.RSA（PKCS#7 格式 - 简化版）
            # 实际上这里应该创建完整的 PKCS#7 结构
            # 为了简化，我们创建一个基本的 RSA 签名文件
            
            # 写入签名文件
            zf_out.writestr("META-INF/MANIFEST.MF", manifest_content)
            zf_out.writestr("META-INF/CERT.SF", sf_content)
            zf_out.writestr("META-INF/CERT.RSA", sig)  # 简化，实际需要 PKCS#7
    
    print(f"Signed APK created: {output_path}")
    return True

def main():
    print("=" * 60)
    print("Manual V1 Signing")
    print("=" * 60)
    
    if not os.path.exists(APK_INPUT):
        print(f"Input not found: {APK_INPUT}")
        return
    
    private_key = generate_key()
    
    # 注意：这是一个简化的实现
    # 真正的 V1 签名需要完整的 PKCS#7/CMS 结构
    print("\n[WARNING] This is a simplified implementation.")
    print("For production use, please use proper Android SDK tools.")
    print("\nRecommended approach:")
    print("1. Install Android SDK")
    print("2. Use: apksigner sign --ks debug.keystore app.apk")
    
    # 创建一个简单的已签名版本（可能不兼容所有设备）
    # create_v1_signature(APK_INPUT, APK_OUTPUT, private_key)

if __name__ == "__main__":
    main()
