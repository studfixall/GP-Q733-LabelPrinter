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
print(f'ret: {ret}')
print(f'decoded_info: {decoded_info}')
print(f'points: {points}')

if ret and decoded_info:
    for i, info in enumerate(decoded_info):
        print(f'  QR [{i}]: "{info}" (type: {type(info)})')
        if info:
            print(f'  DATA: {info}')

# Save the cropped QR region for inspection
if ret and points is not None:
    for i, pts in enumerate(points):
        print(f'Points [{i}]: {pts}')
        if pts is not None:
            # Get bounding box
            x_min = int(min(pts[:, 0]))
            x_max = int(max(pts[:, 0]))
            y_min = int(min(pts[:, 1]))
            y_max = int(max(pts[:, 1]))
            # Add padding
            padding = 20
            x_min = max(0, x_min - padding)
            y_min = max(0, y_min - padding)
            x_max = min(w, x_max + padding)
            y_max = min(h, y_max + padding)
            
            cropped = img[y_min:y_max, x_min:x_max]
            cv2.imwrite(f'qr_crop_{i}.png', cropped)
            print(f'Saved qr_crop_{i}.png ({x_max-x_min}x{y_max-y_min})')
