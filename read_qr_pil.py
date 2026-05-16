from PIL import Image, ImageEnhance, ImageFilter
import cv2
import numpy as np
import sys

# Open with PIL
pil_img = Image.open(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
print(f'PIL Image size: {pil_img.size}')

# Convert to OpenCV format
opencv_img = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
h, w = opencv_img.shape[:2]

# Try different enhancements
enhancements = [
    ('original', lambda img: img),
    ('sharpen', lambda img: img.filter(ImageFilter.SHARPEN)),
    ('contrast', lambda img: ImageEnhance.Contrast(img).enhance(2.0)),
    ('brightness', lambda img: ImageEnhance.Brightness(img).enhance(1.2)),
    ('sharpen+contrast', lambda img: ImageEnhance.Contrast(img.filter(ImageFilter.SHARPEN)).enhance(2.0)),
]

detector = cv2.QRCodeDetector()

for name, enhance_func in enhancements:
    enhanced = enhance_func(pil_img)
    # Convert to OpenCV
    cv_img = cv2.cvtColor(np.array(enhanced), cv2.COLOR_RGB2BGR)
    gray = cv2.cvtColor(cv_img, cv2.COLOR_BGR2GRAY)
    
    data, bbox, _ = detector.detectAndDecode(gray)
    if data:
        print(f'Found with enhancement: {name}')
        print(f'DATA: {data}')
        sys.exit(0)
    print(f'{name}: not found')

print('ERROR: No QR code found')
