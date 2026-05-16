import cv2
import numpy as np
import sys

img = cv2.imread(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
if img is None:
    print('ERROR: Cannot load image')
    sys.exit(1)

h, w = img.shape[:2]
print(f'Image size: {w}x{h}')

# Try detectAndDecodeMulti which can find multiple QR codes
detector = cv2.QRCodeDetector()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Try multi detection
ret, decoded_info, points, _ = detector.detectAndDecodeMulti(gray)
if ret:
    print(f'Found {len(decoded_info)} QR codes:')
    for i, info in enumerate(decoded_info):
        if info:
            print(f'  [{i}]: {info}')
    sys.exit(0)

# Try single detection
data, bbox, _ = detector.detectAndDecode(gray)
if data:
    print(f'Found single: {data}')
    sys.exit(0)

# Try with inverted colors
inv_gray = cv2.bitwise_not(gray)
data, bbox, _ = detector.detectAndDecode(inv_gray)
if data:
    print(f'Found with inverted: {data}')
    sys.exit(0)

print('ERROR: No QR code found with any method')
