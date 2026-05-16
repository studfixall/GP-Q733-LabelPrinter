import cv2
import numpy as np
import sys

# Load the cropped QR code
img = cv2.imread('qr_crop_0.png')
if img is None:
    print('ERROR: Cannot load cropped image')
    sys.exit(1)

print(f'Cropped image size: {img.shape}')

# Convert to grayscale
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Try various preprocessing methods
methods = [
    ('original', gray),
    ('gaussian_blur', cv2.GaussianBlur(gray, (3, 3), 0)),
    ('median_blur', cv2.medianBlur(gray, 3)),
    ('threshold_127', cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)[1]),
    ('threshold_100', cv2.threshold(gray, 100, 255, cv2.THRESH_BINARY)[1]),
    ('threshold_150', cv2.threshold(gray, 150, 255, cv2.THRESH_BINARY)[1]),
    ('adaptive', cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)),
    ('adaptive_mean', cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 11, 2)),
    ('otsu', cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]),
    ('invert', cv2.bitwise_not(gray)),
    ('invert_thresh', cv2.bitwise_not(cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)[1])),
]

detector = cv2.QRCodeDetector()

for name, processed in methods:
    # Try single decode
    data, bbox, _ = detector.detectAndDecode(processed)
    if data:
        print(f'Found with method: {name}')
        print(f'DATA: {data}')
        sys.exit(0)
    
    # Try multi decode
    ret, decoded_info, points, _ = detector.detectAndDecodeMulti(processed)
    if ret and decoded_info:
        for info in decoded_info:
            if info:
                print(f'Found with method: {name} (multi)')
                print(f'DATA: {info}')
                sys.exit(0)
    
    print(f'{name}: not found')

# Try resizing to make it larger
print('Trying resize...')
for scale in [2, 4, 8]:
    resized = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)
    data, bbox, _ = detector.detectAndDecode(resized)
    if data:
        print(f'Found with resize {scale}x')
        print(f'DATA: {data}')
        sys.exit(0)
    print(f'resize {scale}x: not found')

print('ERROR: Could not decode QR code')
