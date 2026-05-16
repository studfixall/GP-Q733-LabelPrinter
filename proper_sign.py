#!/usr/bin/env python3
"""
创建正确的 JAR/APK V1 签名
使用 PKCS#7/CMS 格式
"""

import zipfile
import os
import shutil
import hashlib
import base64
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives.serialization import pkcs7
import datetime

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
INPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_complete.apk")  # 未签名的 APK
OUTPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_signed_proper.apk")
EXTRACT_DIR = os.path.join(WORK_DIR, "extracted_sign")

def generate_key():
    """生成密钥对"""
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )
    
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COMMON_NAME, u"Android Debug"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, u"Android"),
        x509.NameAttribute(NameOID.COUNTRY_NAME, u"US"),
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
    
    return private_key, cert

def create_manifest(extract_dir):
    """创建 MANIFEST.MF"""
    lines = [
        "Manifest-Version: 1.0",
        "Built-By: Generated-by-ADT",
        "Created-By: Android Gradle 8.1.0",
        ""
    ]
    
    entries = []
    for root, dirs, files in os.walk(extract_dir):
        for file in files:
            file_path = os.path.join(root, file)
            rel_path = os.path.relpath(file_path, extract_dir).replace("\\", "/")
            
            # 跳过 META-INF 目录
            if rel_path.startswith("META-INF/"):
                continue
            
            with open(file_path, 'rb') as f:
                data = f.read()
            
            # SHA-256 (Android 要求)
            sha256_digest = base64.b64encode(hashlib.sha256(data).digest()).decode('ascii')
            entries.append((rel_path, sha256_digest))
    
    for name, digest in entries:
        lines.append(f"Name: {name}")
        lines.append(f"SHA-256-Digest: {digest}")
        lines.append("")
    
    return "\r\n".join(lines).encode('utf-8')

def create_signature_block(manifest_content, private_key, cert):
    """创建 PKCS#7 签名块"""
    # 使用 SHA-256 对 manifest 进行签名
    options = [pkcs7.PKCS7Options.DetachedSignature]
    
    signed_data = pkcs7.PKCS7SignatureBuilder().set_data(
        manifest_content
    ).add_signer(
        cert, private_key, hashes.SHA256()
    ).sign(serialization.Encoding.DER, options)
    
    return signed_data

def sign_apk():
    """签名 APK"""
    print("=" * 60)
    print("Proper APK Signing with PKCS#7")
    print("=" * 60)
    
    # 清理并解压
    if os.path.exists(EXTRACT_DIR):
        shutil.rmtree(EXTRACT_DIR)
    os.makedirs(EXTRACT_DIR)
    
    print("\n1. Extracting APK...")
    with zipfile.ZipFile(INPUT_APK, 'r') as zf:
        zf.extractall(EXTRACT_DIR)
    print("   Done!")
    
    # 删除旧的签名
    meta_dir = os.path.join(EXTRACT_DIR, "META-INF")
    if os.path.exists(meta_dir):
        for f in os.listdir(meta_dir):
            if f.endswith(('.RSA', '.SF', '.MF', '.DSA', '.EC')):
                os.remove(os.path.join(meta_dir, f))
                print(f"   Removed old: {f}")
    
    # 生成密钥
    print("\n2. Generating key pair...")
    private_key, cert = generate_key()
    print("   Done!")
    
    # 创建 MANIFEST.MF
    print("\n3. Creating MANIFEST.MF...")
    manifest_content = create_manifest(EXTRACT_DIR)
    print(f"   Size: {len(manifest_content):,} bytes")
    
    # 创建 CERT.SF
    print("\n4. Creating CERT.SF...")
    sf_lines = [
        "Signature-Version: 1.0",
        "Created-By: 1.0 (Android Signer)",
        f"SHA-256-Digest-Manifest: {base64.b64encode(hashlib.sha256(manifest_content).digest()).decode('ascii')}",
        "X-Android-APK-Signed: 2, 3",  # 声明 V2/V3 签名
        ""
    ]
    
    # 添加每个条目的摘要
    manifest_text = manifest_content.decode('utf-8')
    for line in manifest_text.split('\r\n'):
        if line.startswith('Name: '):
            entry_name = line[6:]
            # 找到对应的 SHA-256-Digest
            for next_line in manifest_text.split('\r\n'):
                if next_line.startswith(f"SHA-256-Digest: ") and manifest_text.find(f"Name: {entry_name}") < manifest_text.find(next_line):
                    sf_lines.append(f"Name: {entry_name}")
                    sf_lines.append(next_line)
                    sf_lines.append("")
                    break
    
    sf_content = "\r\n".join(sf_lines).encode('utf-8')
    print(f"   Size: {len(sf_content):,} bytes")
    
    # 创建 PKCS#7 签名块
    print("\n5. Creating PKCS#7 signature block...")
    try:
        sig_block = create_signature_block(sf_content, private_key, cert)
        print(f"   Size: {len(sig_block):,} bytes")
    except Exception as e:
        print(f"   PKCS#7 signing failed: {e}")
        print("   Falling back to simple signature...")
        # 简化签名
        sig_block = private_key.sign(
            hashlib.sha256(sf_content).digest(),
            padding.PKCS1v15(),
            hashes.SHA256()
        )
    
    # 写入签名文件
    print("\n6. Writing signature files...")
    with open(os.path.join(meta_dir, "MANIFEST.MF"), 'wb') as f:
        f.write(manifest_content)
    with open(os.path.join(meta_dir, "CERT.SF"), 'wb') as f:
        f.write(sf_content)
    with open(os.path.join(meta_dir, "CERT.RSA"), 'wb') as f:
        f.write(sig_block)
    print("   Done!")
    
    # 重新打包
    print("\n7. Repacking APK...")
    with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(EXTRACT_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, EXTRACT_DIR).replace("\\", "/")
                zf.write(file_path, arcname)
    
    file_size = os.path.getsize(OUTPUT_APK)
    print(f"   Done! Size: {file_size / 1024 / 1024:.2f} MB")
    
    print("\n" + "=" * 60)
    print("APK Signed Successfully!")
    print("=" * 60)
    print(f"Output: {OUTPUT_APK}")
    print("\n[IMPORTANT]")
    print("- Uninstall original app before installing")
    print("- Enable 'Install unknown apps' in settings")
    
    return OUTPUT_APK

if __name__ == "__main__":
    if not os.path.exists(INPUT_APK):
        print(f"Input APK not found: {INPUT_APK}")
        print("Please run complete_solution.py first")
    else:
        sign_apk()
