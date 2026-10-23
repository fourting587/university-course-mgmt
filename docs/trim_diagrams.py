# -*- coding: utf-8 -*-
"""裁掉示意图四周白边，让图片在 A4 页面上尽量大。"""
import os
from PIL import Image, ImageChops

base = os.path.dirname(os.path.abspath(__file__))
files = ["功能模块图.png", "ER图-基础数据.png", "ER图-教学数据.png", "部署架构图.png"]
for name in files:
    path = os.path.join(base, name)
    im = Image.open(path).convert("RGB")
    bg = Image.new("RGB", im.size, (255, 255, 255))
    diff = ImageChops.difference(im, bg)
    bbox = diff.getbbox()
    if not bbox:
        continue
    margin = 24
    left = max(0, bbox[0] - margin)
    top = max(0, bbox[1] - margin)
    right = min(im.width, bbox[2] + margin)
    bottom = min(im.height, bbox[3] + margin)
    cropped = im.crop((left, top, right, bottom)).convert("RGBA")
    px = cropped.load()
    for yy in range(cropped.height):
        for xx in range(cropped.width):
            r, g, b, a = px[xx, yy]
            if r > 248 and g > 248 and b > 248:
                px[xx, yy] = (255, 255, 255, 0)
    cropped.save(path)
    print(name, im.size, "->", (right - left, bottom - top))
