"""
Builds a genuinely transparent Vela (SHADE) character asset for Nocta.

Why this exists
---------------
The shipped companion/coach artwork are JPEGs: JPEG has no alpha channel, so the
character always carried a rectangular photographic background. This script
runs an offline segmentation model (IS-Net "anime", the model the `rembg`
project ships as `isnet-anime`) to produce a real alpha channel, cleans the
matte, trims the subject and re-encodes the result as transparent WebP files:

  app/src/main/res/drawable-nodpi/vela_companion.webp   full-body cutout
  app/src/main/res/drawable-nodpi/vela_portrait.webp    head/shoulders cutout

The portrait is cropped from the cleaned full-body cutout so both surfaces
frame the same artwork consistently.

It is a build-time asset tool, never part of the Android app. Run it only when
the source art changes:

    python3 -m venv /tmp/velaenv
    /tmp/velaenv/bin/pip install onnxruntime pillow numpy scipy
    curl -L -o /tmp/isnet-anime.onnx \
        https://github.com/danielgatis/rembg/releases/download/v0.0.0/isnet-anime.onnx
    /tmp/velaenv/bin/python tools/cutout_vela.py
"""

from __future__ import annotations

import pathlib
import sys

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

MODEL = "/tmp/isnet-anime.onnx"
ROOT = pathlib.Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
SOURCE = ROOT / "tools/source-art/coach_avatar_thinking.jpg"
BACKGROUND = (10, 10, 13)  # NoctaBackground


def alpha_matte(image: Image.Image) -> np.ndarray:
    """Runs the segmentation model and returns a 0-255 float alpha matte."""
    import onnxruntime as ort

    session = ort.InferenceSession(MODEL, providers=["CPUExecutionProvider"])
    input_name = session.get_inputs()[0].name

    resized = np.asarray(
        image.convert("RGB").resize((1024, 1024), Image.BILINEAR), dtype=np.float32
    ) / 255.0
    normalized = (resized - 0.5) / 1.0
    batch = np.transpose(normalized, (2, 0, 1))[None, ...]

    prediction = session.run(None, {input_name: batch})[0][0, 0]
    low, high = float(prediction.min()), float(prediction.max())
    matte = (prediction - low) / max(high - low, 1e-6)
    matte = np.clip(matte, 0.0, 1.0)

    full = np.asarray(
        Image.fromarray((matte * 255).astype(np.uint8)).resize(
            image.size, Image.BILINEAR
        ),
        dtype=np.float32,
    )
    return full


def clean_matte(matte: np.ndarray, rgb: np.ndarray) -> np.ndarray:
    """
    Drops background fragments and fills the subject.

    The model leaves faint background patches (window frames, floor shadows)
    with mid-range alpha. Anything under the noise floor is removed, then only
    the largest connected region — the character — is kept, holes inside the
    silhouette are filled, and the edge is feathered by a sub-pixel blur so the
    cutout has no stair-stepping.
    """
    # The model leaves "ghost" background patches around 80-95/255 and paints
    # the character at ~250/255, so a mid threshold separates them cleanly.
    mask = matte >= 128.0

    labels, count = ndimage.label(mask)
    if count > 1:
        sizes = ndimage.sum(mask, labels, index=range(1, count + 1))
        keep = int(np.argmax(sizes)) + 1
        mask = labels == keep

    mask = ndimage.binary_fill_holes(mask)

    # Regions the model counted as "subject" but which are actually cool-grey
    # background seen through gaps (between an arm and the torso, behind the
    # legs) would otherwise appear as pale blue patches on the dark UI. Large
    # blue-dominant components are cleared, keeping hair highlights and skin.
    blue = rgb[:, :, 2].astype(np.int16) - rgb[:, :, 0].astype(np.int16)
    brightness = rgb.astype(np.float32).mean(axis=2)
    residual = mask & (blue > 22) & (brightness > 95)
    labels, count = ndimage.label(residual)
    for index in range(1, count + 1):
        component = labels == index
        area = int(component.sum())
        if area < 100:
            continue
        mean_blue = float(blue[component].mean())
        if mean_blue < 26:
            continue
        # Dilate by a pixel so the light fringe at the hole's edge goes too.
        mask &= ~ndimage.binary_dilation(component, iterations=1)

    # Opaque subject, soft one-pixel edge, nothing outside the silhouette.
    alpha = np.where(mask, np.maximum(matte, 240.0), 0.0)
    alpha = ndimage.gaussian_filter(alpha, sigma=0.7)
    alpha[alpha < 40] = 0.0
    return np.clip(alpha, 0.0, 255.0)


def apply_matte(image: Image.Image, matte: np.ndarray) -> Image.Image:
    rgba = image.convert("RGBA")
    rgba.putalpha(Image.fromarray(matte.astype(np.uint8)))
    return rgba


def trim(rgba: Image.Image, padding_ratio: float = 0.015) -> Image.Image:
    bbox = rgba.getchannel("A").point(lambda v: 255 if v > 10 else 0).getbbox()
    if bbox is None:
        return rgba
    left, top, right, bottom = bbox
    pad = int(round(max(right - left, bottom - top) * padding_ratio))
    return rgba.crop(
        (
            max(0, left - pad),
            max(0, top - pad),
            min(rgba.width, right + pad),
            min(rgba.height, bottom + pad),
        )
    )


def with_aura(rgba: Image.Image, radius: int = 26, opacity: int = 120) -> Image.Image:
    """
    Adds a faint violet aura that hugs the silhouette, so the cutout sits in
    Nocta's dark surface instead of looking pasted on. It is painted into the
    alpha channel, never a card or frame.
    """
    silhouette = rgba.getchannel("A").filter(ImageFilter.GaussianBlur(radius))
    glow = Image.new("RGBA", rgba.size, (0, 0, 0, 0))
    glow.paste(Image.new("RGBA", rgba.size, (139, 124, 246, opacity)), (0, 0), silhouette)
    glow.alpha_composite(rgba)
    return glow


def soften_bottom(rgba: Image.Image, fade_ratio: float = 0.055) -> Image.Image:
    """Multiplies the last few percent of the cutout down so it dissolves into black."""
    height = max(1, int(rgba.height * fade_ratio))
    alpha = np.asarray(rgba.getchannel("A")).astype(np.float32)
    # 1.0 at the top of the fade band down to 0 at the bottom edge.
    ramp = (np.linspace(1.0, 0.0, height) ** 1.5)[:, None]
    alpha[-height:, :] *= ramp
    rgba.putalpha(Image.fromarray(np.clip(alpha, 0, 255).astype(np.uint8)))
    return rgba


def save_webp(rgba: Image.Image, path: pathlib.Path, target_height: int) -> None:
    scale = target_height / rgba.height
    size = (max(1, int(round(rgba.width * scale))), target_height)
    resized = rgba.resize(size, Image.LANCZOS)
    path.parent.mkdir(parents=True, exist_ok=True)
    resized.save(path, "WEBP", lossless=True, method=6)
    print(
        f"wrote {path.relative_to(ROOT)}  {size[0]}x{size[1]}  "
        f"{path.stat().st_size // 1024} KiB"
    )


def preview(rgba: Image.Image, path: pathlib.Path, target_height: int = 900) -> None:
    scale = target_height / rgba.height
    size = (max(1, int(round(rgba.width * scale))), target_height)
    canvas = Image.new("RGBA", size, BACKGROUND + (255,))
    canvas.alpha_composite(rgba.resize(size, Image.LANCZOS))
    canvas.convert("RGB").save(path)
    print(f"preview {path}")


def main() -> int:
    if not pathlib.Path(MODEL).exists():
        print(f"model missing: {MODEL}", file=sys.stderr)
        return 1

    source = Image.open(SOURCE)
    pixels = np.asarray(source.convert("RGB"))
    body = trim(apply_matte(source, clean_matte(alpha_matte(source), pixels)))
    body = with_aura(body)
    soften_bottom(body)
    save_webp(body, DRAWABLE / "vela_companion.webp", target_height=1200)
    preview(body, pathlib.Path("/tmp/vela_preview_companion.png"))

    # Head-and-shoulders portrait from the same cleaned body: consistent framing,
    # no second segmentation pass (the close-up source segmented poorly).
    width, height = body.size
    head = body.crop(
        (
            int(width * 0.06),
            0,
            int(width * 0.94),
            int(height * 0.42),
        )
    )
    # The crop cuts across the chest, so fade that edge out rather than leaving
    # a straight line inside the header.
    soften_bottom(head, fade_ratio=0.16)
    save_webp(head, DRAWABLE / "vela_portrait.webp", target_height=640)
    preview(head, pathlib.Path("/tmp/vela_preview_portrait.png"), target_height=640)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
