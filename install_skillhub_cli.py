import urllib.request
import ssl
import tarfile
import os
import shutil

# 禁用SSL验证
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def download(url, dest):
    print(f"Downloading {url}...")
    with urllib.request.urlopen(url, context=ctx, timeout=60) as response:
        with open(dest, 'wb') as f:
            f.write(response.read())
    print(f"  Saved to {dest}")

def main():
    work_dir = os.path.expanduser("~/.qclaw/skillhub_install")
    os.makedirs(work_dir, exist_ok=True)
    
    # Download latest.tar.gz
    kit_url = "https://skillhub-1388575217.cos.ap-guangzhou.myqcloud.com/install/latest.tar.gz"
    tar_path = os.path.join(work_dir, "latest.tar.gz")
    download(kit_url, tar_path)
    
    # Extract
    extract_dir = os.path.join(work_dir, "extracted")
    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)
    os.makedirs(extract_dir)
    
    print(f"Extracting to {extract_dir}...")
    with tarfile.open(tar_path, 'r:gz') as tar:
        tar.extractall(extract_dir)
    
    # List contents
    print("\nExtracted contents:")
    for root, dirs, files in os.walk(extract_dir):
        level = root.replace(extract_dir, '').count(os.sep)
        indent = ' ' * 2 * level
        print(f'{indent}{os.path.basename(root)}/')
        subindent = ' ' * 2 * (level + 1)
        for file in files[:10]:  # Limit output
            print(f'{subindent}{file}')
        if len(files) > 10:
            print(f'{subindent}... and {len(files)-10} more files')
    
    # Find cli directory
    cli_dir = os.path.join(extract_dir, "cli")
    if os.path.exists(cli_dir):
        print(f"\nCLI directory found: {cli_dir}")
        # List install scripts
        for f in os.listdir(cli_dir):
            print(f"  - {f}")
    
    print(f"\nInstall files ready at: {work_dir}")

if __name__ == "__main__":
    main()
