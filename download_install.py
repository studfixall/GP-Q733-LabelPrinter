import urllib.request
import ssl

# 禁用SSL验证
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "https://skillhub-1388575217.cos.ap-guangzhou.myqcloud.com/install/install.sh"
print(f"Downloading {url}...")

try:
    with urllib.request.urlopen(url, context=ctx, timeout=30) as response:
        content = response.read().decode('utf-8')
        with open('install.sh', 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Downloaded {len(content)} bytes")
        print("\nFirst 50 lines:")
        print('\n'.join(content.split('\n')[:50]))
except Exception as e:
    print(f"Error: {e}")
