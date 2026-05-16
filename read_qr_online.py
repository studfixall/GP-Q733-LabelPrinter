import base64
import urllib.request
import urllib.parse
import json
import sys

# Read the image file
with open(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg', 'rb') as f:
    img_data = f.read()

# Try to use an online QR decoder API
# Using qrserver.com API (free, no key required)
url = 'https://api.qrserver.com/v1/read-qr-code/'

# Create multipart form data
boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW'
body = []
body.append(f'--{boundary}'.encode())
body.append(b'Content-Disposition: form-data; name="file"; filename="qr.jpg"')
body.append(b'Content-Type: image/jpeg')
body.append(b'')
body.append(img_data)
body.append(f'--{boundary}--'.encode())
body.append(b'')

body = b'\r\n'.join(body)

headers = {
    'Content-Type': f'multipart/form-data; boundary={boundary}',
    'Content-Length': str(len(body))
}

try:
    req = urllib.request.Request(url, data=body, headers=headers, method='POST')
    with urllib.request.urlopen(req, timeout=30) as response:
        result = response.read().decode('utf-8')
        print(f'Response: {result}')
        
        data = json.loads(result)
        if data and len(data) > 0:
            for item in data:
                if 'symbol' in item and len(item['symbol']) > 0:
                    for symbol in item['symbol']:
                        if 'data' in symbol and symbol['data']:
                            print(f'QR DATA: {symbol["data"]}')
                            sys.exit(0)
                        if 'error' in symbol:
                            print(f'Error: {symbol["error"]}')
except Exception as e:
    print(f'Error: {e}')

print('Could not decode QR code online')
