import cv2
from pyzbar.pyzbar import decode
import sys

img = cv2.imread(r'C:\Users\Administrator\.qclaw\media\inbound\34e91e56c3bc9a8342b08fd72679243f_compress_1---25a2ec6b-555a-4a40-85f4-49f19d06530a.jpg')
if img is None:
    print('ERROR: Cannot load image')
    sys.exit(1)

decoded = decode(img)
if not decoded:
    print('ERROR: No QR code found')
    sys.exit(1)

for d in decoded:
    print(f'TYPE: {d.type}')
    print(f'DATA: {d.data.decode("utf-8")}')
