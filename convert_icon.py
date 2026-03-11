import sys
from PIL import Image

input_path = sys.argv[1]
output_ico_path = sys.argv[2]
output_png_path = sys.argv[3]

img = Image.open(input_path)
# Ensure square aspect ratio
size = min(img.size)
img = img.crop((0, 0, size, size))

# Save as ICO
img.save(output_ico_path, format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

# Save a copy as PNG for generic usage
img.save(output_png_path, format='PNG')
print("Icons generated successfully.")
