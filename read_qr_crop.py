import cv2
import numpy as np
import sys

img = cv2.imread(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
if img is None:
    print('ERROR: Cannot load image')
    sys.exit(1)

h, w = img.shape[:2]
print(f'Image size: {w}x{h}')

# Crop to center region where QR code is (estimated based on typical app layout)
# QR code is roughly in the middle, let's try different crop regions
crops = [
    # Center crop
    (int(w*0.15), int(h*0.35), int(w*0.85), int(h*0.75)),
    # Upper center
    (int(w*0.2), int(h*0.4), int(w*0.8), int(h*0.7)),
    # Wider center
    (int(w*0.1), int(h*0.3), int(w*0.9), int(h*0.8)),
]

detector = cv2.QRCodeDetector()

for i, (x1, y1, x2, y2) in enumerate(crops):
    cropped = img[y1:y2, x1:x2]
    gray = cv2.cvtColor(cropped, cv2.COLOR_BGR2GRAY)
    
    # Try original and threshold
    for name, processed in [('original', gray), ('thresh', cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)[1])]:
        data, bbox, _ = detector.detectAndDecode(processed)
        if data:
            print(f'Found in crop {i} with {name}')
            print(f'DATA: {data}')
            sys.exit(0)
    
    print(f'Crop {i} ({x1},{y1},{x2},{y2}): not found')

print('ERROR: No QR code found in any crop region')
