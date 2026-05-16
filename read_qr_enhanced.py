import cv2
import numpy as np
import sys

img = cv2.imread(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
if img is None:
    print('ERROR: Cannot load image')
    sys.exit(1)

print(f'Image size: {img.shape}')

# Convert to grayscale
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Try multiple preprocessing methods
methods = [
    ('original', gray),
    ('gaussian', cv2.GaussianBlur(gray, (5, 5), 0)),
    ('threshold', cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)[1]),
    ('adaptive', cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)),
    ('sharpen', cv2.filter2D(gray, -1, np.array([[-1,-1,-1],[-1,9,-1],[-1,-1,-1]]))),
]

detector = cv2.QRCodeDetector()

for name, processed in methods:
    data, bbox, _ = detector.detectAndDecode(processed)
    if data:
        print(f'Found with method: {name}')
        print(f'DATA: {data}')
        sys.exit(0)
    print(f'Method {name}: not found')

print('ERROR: No QR code found with any method')
