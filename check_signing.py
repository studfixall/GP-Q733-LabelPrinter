import zipfile
import struct

apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_final.apk"

print("Checking APK signing scheme...")

with zipfile.ZipFile(apk_path, 'r') as zf:
    # 检查 V2/V3 签名块
    # V2/V3 签名在 ZIP 的中央目录之前，有一个签名块
    
    # 读取文件末尾的中央目录定位器
    with open(apk_path, 'rb') as f:
        f.seek(-20, 2)  # 定位到倒数20字节
        data = f.read()
        
        # ZIP 结束记录签名 0x06054b50
        if data[-4:] == b'\x50\x4b\x05\x06':
            print("Found ZIP end of central directory record")
            
            # 解析中央目录偏移
            cd_offset = struct.unpack('<I', data[-16:-12])[0]
            cd_size = struct.unpack('<I', data[-20:-16])[0]
            print(f"  Central directory offset: {cd_offset}")
            print(f"  Central directory size: {cd_size}")
            
            # 检查签名块（在 central directory 之前）
            f.seek(cd_offset - 16)
            sig_block = f.read(16)
            print(f"\n  Data before central directory: {sig_block.hex()}")
            
            # V2 签名块魔数: 0x7109871a
            if b'\x1a\x87\x09\x71' in sig_block or b'\x71\x09\x87\x1a' in sig_block:
                print("  [WARNING] APK Signing Block V2/V3 detected!")
                print("  Modifying APK without re-signing will invalidate V2/V3 signature")
            else:
                print("  No V2/V3 signing block detected (using JAR signature only)")
        
        # 检查文件大小和 ZIP 结构
        f.seek(0, 2)
        file_size = f.tell()
        print(f"\nFile size: {file_size:,} bytes")
        
        # 读取并解析 ZIP 条目
        print("\nFirst few ZIP entries:")
        for name in zf.namelist()[:5]:
            info = zf.getinfo(name)
            print(f"  {name}: {info.compress_size} -> {info.file_size}")
