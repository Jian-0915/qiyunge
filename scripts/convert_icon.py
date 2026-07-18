from PIL import Image
import sys

src = r"d:\QiyunGe\应用图标.jpg"
png_target = r"d:\QiyunGe\src\main\resources\images\icon.png"
ico_target = r"d:\QiyunGe\src\main\resources\images\icon.ico"

img = Image.open(src).convert("RGBA")
print(f"Source: {img.size}")

sizes = [(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
resized = []
for s in sizes:
    r = img.resize(s, Image.LANCZOS)
    resized.append(r)

resized[0].save(png_target, "PNG")
print(f"PNG saved: {png_target}")

resized[0].save(ico_target, format="ICO", sizes=sizes)
print(f"ICO saved: {ico_target}")
