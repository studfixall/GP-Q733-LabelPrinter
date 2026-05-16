#!/usr/bin/env python3
"""
使用 apksigtool 对 APK 进行 V2 签名
"""

import os
import subprocess
import shutil
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes
import datetime

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
APK_INPUT = os.path.join(WORK_DIR, "EnjoyMBM_fixed_final.apk")
APK_OUTPUT = os.path.join(WORK_DIR, "EnjoyMBM_signed.apk")
KEY_FILE = os.path.join(WORK_DIR, "testkey.pem")
CERT_FILE = os.path.join(WORK_DIR, "testkey.x509.pem")

def generate_test_key():
    """生成测试密钥对"""
    if os.path.exists(KEY_FILE) and os.path.exists(CERT_FILE):
        print("Using existing key pair")
        return True
    
    print("Generating test key pair...")
    
    # 生成私钥
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
        backend=default_backend()
    )
    
    # 保存私钥
    with open(KEY_FILE, "wb") as f:
        f.write(private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.TraditionalOpenSSL,
            encryption_algorithm=serialization.NoEncryption()
        ))
    
    # 生成自签名证书
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
        datetime.datetime.utcnow()
    ).not_valid_after(
        datetime.datetime.utcnow() + datetime.timedelta(days=3650)
    ).add_extension(
        x509.SubjectAlternativeName([x509.DNSName(u"localhost")]),
        critical=False,
    ).sign(private_key, hashes.SHA256(), default_backend())
    
    # 保存证书
    with open(CERT_FILE, "wb") as f:
        f.write(cert.public_bytes(serialization.Encoding.PEM))
    
    print("Key pair generated!")
    return True

def sign_apk():
    """使用 apksigtool 签名 APK"""
    print("\nSigning APK with apksigtool...")
    
    # 复制 APK
    shutil.copy(APK_INPUT, APK_OUTPUT)
    
    # 使用 apksigtool 进行 V2 签名
    cmd = [
        "python", "-m", "apksigtool",
        "sign",
        "--key", KEY_FILE,
        "--cert", CERT_FILE,
        APK_OUTPUT
    ]
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    print(result.stdout)
    if result.stderr:
        print("stderr:", result.stderr)
    
    return result.returncode == 0

def main():
    print("=" * 60)
    print("APK Signing with Python (V2 scheme)")
    print("=" * 60)
    
    if not os.path.exists(APK_INPUT):
        print(f"Input APK not found: {APK_INPUT}")
        return
    
    # 生成密钥
    if not generate_test_key():
        print("Failed to generate key pair")
        return
    
    # 签名
    if sign_apk():
        print("\n" + "=" * 60)
        print("Signing successful!")
        print("=" * 60)
        print(f"Output: {APK_OUTPUT}")
        print(f"Size: {os.path.getsize(APK_OUTPUT) / 1024 / 1024:.2f} MB")
        print("\n[NOTE] Uninstall original app before installing this one.")
    else:
        print("\nSigning failed!")

if __name__ == "__main__":
    main()
