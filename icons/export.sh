#!/bin/bash
SRC="icons/icon-1024.png"
OUT="composeApp/src/jvmMain/resources"
mkdir -p "$OUT/iconset"

# --- Linux PNGs ---
for size in 512; do
  magick "$SRC" -resize ${size}x${size} -filter Lanczos "$OUT/icon-${size}.png"
done

# --- Windows .ico (multi-size container) ---
magick "$SRC" \
  \( -clone 0 -resize 16x16   -filter Lanczos \) \
  \( -clone 0 -resize 32x32   -filter Lanczos \) \
  \( -clone 0 -resize 48x48   -filter Lanczos \) \
  \( -clone 0 -resize 64x64   -filter Lanczos \) \
  \( -clone 0 -resize 128x128 -filter Lanczos \) \
  \( -clone 0 -resize 256x256 -filter Lanczos \) \
  -delete 0 "$OUT/icon.ico"

# --- macOS .icns ---
# iconutil expects a specific folder structure
ICONSET="$OUT/iconset/AppIcon.iconset"
mkdir -p "$ICONSET"

magick "$SRC" -resize 16x16    -filter Lanczos "$ICONSET/icon_16x16.png"
magick "$SRC" -resize 32x32    -filter Lanczos "$ICONSET/icon_16x16@2x.png"
magick "$SRC" -resize 32x32    -filter Lanczos "$ICONSET/icon_32x32.png"
magick "$SRC" -resize 64x64    -filter Lanczos "$ICONSET/icon_32x32@2x.png"
magick "$SRC" -resize 128x128  -filter Lanczos "$ICONSET/icon_128x128.png"
magick "$SRC" -resize 256x256  -filter Lanczos "$ICONSET/icon_128x128@2x.png"
magick "$SRC" -resize 256x256  -filter Lanczos "$ICONSET/icon_256x256.png"
magick "$SRC" -resize 512x512  -filter Lanczos "$ICONSET/icon_256x256@2x.png"
magick "$SRC" -resize 512x512  -filter Lanczos "$ICONSET/icon_512x512.png"
magick "$SRC" -resize 1024x1024 -filter Lanczos "$ICONSET/icon_512x512@2x.png"

iconutil -c icns "$ICONSET" -o "$OUT/icon.icns"

rm -r "$OUT/iconset"

echo "Done. Files in $OUT/"