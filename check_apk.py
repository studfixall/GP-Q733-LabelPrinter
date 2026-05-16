#!/usr/bin/env python3
import zipfile

apk_path = r'C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_signed_final.apk'

with zipfile.ZipFile(apk_path, 'r') as zf:
    # 列出所有 META-INF 文件
    meta_files = [f for f in zf.namelist() if f.startswith('META-INF/')]
    print('META-INF 文件列表:')
    for f in meta_files:
        if not 'version' in f:
            print(f'  {f}')
    
    # 检查签名文件是否存在
    print()
    print('签名文件检查:')
    has_mf = 'META-INF/MANIFEST.MF' in zf.namelist()
    has_sf = 'META-INF/CERT.SF' in zf.namelist()
    has_rsa = 'META-INF/CERT.RSA' in zf.namelist()
    print(f'  MANIFEST.MF: {has_mf}')
    print(f'  CERT.SF: {has_sf}')
    print(f'  CERT.RSA: {has_rsa}')
    
    # 读取 RSA 文件大小
    if has_rsa:
        rsa_data = zf.read('META-INF/CERT.RSA')
        print(f'  CERT.RSA 大小: {len(rsa_data)} bytes')
        
        # 检查是否是有效的 PKCS#7 结构
        if len(rsa_data) > 2:
            # ASN.1 SEQUENCE tag
            if rsa_data[0] == 0x30:
                print('  RSA 文件格式: 看起来是 ASN.1/DER 格式')
                print(f'  前 10 bytes: {rsa_data[:10].hex()}')
            else:
                print(f'  RSA 文件格式: 未知 (首字节: 0x{rsa_data[0]:02x})')
