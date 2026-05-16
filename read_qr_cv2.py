import cv2
import sys

img = cv2.imread(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
if img is None:
    print('ERROR: Cannot load image')
    sys.exit(1)

# Convert to grayscale
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Use OpenCV's built-in QR detector
detector = cv2.QRCodeDetector()
data, bbox, _ = detector.detectAndDecode(gray)

if data:
    print(f'DATA: {data}')
else:
    print('ERROR: No QR code found')
    sys.exit(1)
