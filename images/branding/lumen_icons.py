"""Draws the Lumen TV launcher icon, TV banner and in-app logos.

Usage: python3 -I lumen_icons.py <out_dir>
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

SS = 4  # supersampling factor for smooth edges

BG = (23, 24, 28)          # #17181C
HALO = (40, 41, 48)        # soft light behind the play mark
RED = (226, 75, 74)        # #E24B4A
WHITE = (242, 242, 243)
GREY = (138, 139, 146)     # #8A8B92
BETA_BG = (121, 31, 31)    # #791F1F


def font(size, weight="Demi Bold"):
    path = "/System/Library/Fonts/Avenir Next.ttc"
    for i in range(16):
        try:
            f = ImageFont.truetype(path, size, index=i)
        except OSError:
            break
        if f.getname()[1] == weight:
            return f
    return ImageFont.truetype(path, size)


def play_mark(draw, cx, cy, size, color):
    """Rounded play triangle centred (optically) on cx, cy."""
    h = size
    w = size * 0.9
    left = cx - w * 0.42
    pts = [(left, cy - h / 2), (left, cy + h / 2), (left + w, cy)]
    r = size * 0.11
    draw.polygon(pts, fill=color)
    draw.line(pts + [pts[0]], fill=color, width=int(r * 2), joint="curve")
    for x, y in pts:
        draw.ellipse((x - r, y - r, x + r, y + r), fill=color)


def sparkle(draw, cx, cy, r, color):
    """Four-point star: the 'lumen' light."""
    t = r * 0.28
    draw.polygon([(cx, cy - r), (cx + t, cy - t), (cx + r, cy), (cx + t, cy + t),
                  (cx, cy + r), (cx - t, cy + t), (cx - r, cy), (cx - t, cy - t)], fill=color)


def mark(size, bg=BG, play=RED, halo=True, beta=False):
    """Square app mark: dark rounded tile, soft halo, red play button, small sparkle."""
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = s * 0.05
    d.rounded_rectangle((pad, pad, s - pad, s - pad), radius=s * 0.22, fill=bg)

    if halo:
        glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        r = s * 0.30
        gd.ellipse((s / 2 - r, s / 2 - r, s / 2 + r, s / 2 + r), fill=HALO + (255,))
        glow = glow.filter(ImageFilter.GaussianBlur(s * 0.06))
        mask = Image.new("L", (s, s), 0)
        ImageDraw.Draw(mask).rounded_rectangle((pad, pad, s - pad, s - pad), radius=s * 0.22, fill=255)
        img.paste(glow, (0, 0), Image.composite(glow.split()[3], Image.new("L", (s, s), 0), mask))
        d = ImageDraw.Draw(img)

    play_mark(d, s * 0.5, s * (0.46 if beta else 0.5), s * 0.36, play)
    sparkle(d, s * 0.71, s * (0.27 if beta else 0.29), s * 0.07, WHITE)

    if beta:
        band_top = s * 0.70
        band = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        bd = ImageDraw.Draw(band)
        bd.rectangle((0, band_top, s, s), fill=BETA_BG)
        tile = Image.new("L", (s, s), 0)
        ImageDraw.Draw(tile).rounded_rectangle((pad, pad, s - pad, s - pad), radius=s * 0.22, fill=255)
        img.paste(band, (0, 0), Image.composite(band.split()[3], Image.new("L", (s, s), 0), tile))
        d = ImageDraw.Draw(img)
        f = font(int(s * 0.13))
        d.text((s / 2, (band_top + s - pad) / 2), "beta", font=f, fill=WHITE, anchor="mm")

    return img.resize((size, size), Image.LANCZOS)


def wordmark(draw, x, cy, height, primary=WHITE, secondary=GREY):
    f = font(int(height))
    draw.text((x, cy), "Lumen", font=f, fill=primary, anchor="lm")
    w = draw.textlength("Lumen ", font=f)
    draw.text((x + w, cy), "TV", font=font(int(height), "Regular"), fill=secondary, anchor="lm")
    return w + draw.textlength("TV", font=font(int(height), "Regular"))


def banner(w, h, beta=False):
    """TV launcher banner (16:9): mark on the left, wordmark on the right."""
    s_w, s_h = w * SS, h * SS
    img = Image.new("RGBA", (s_w, s_h), BG + (255,))
    m = mark(int(s_h * 0.52), bg=(34, 35, 41))
    d = ImageDraw.Draw(img)
    f_h = s_h * 0.17
    text_w = d.textlength("Lumen TV", font=font(int(f_h)))
    gap = s_h * 0.07
    total = m.width + gap + text_w
    x0 = (s_w - total) / 2
    img.paste(m, (int(x0), int((s_h - m.height) / 2)), m)
    wordmark(d, x0 + m.width + gap, s_h * (0.45 if beta else 0.5), f_h)
    if beta:
        f = font(int(s_h * 0.075))
        tx = x0 + m.width + gap
        ty = s_h * 0.66
        tw = d.textlength("beta", font=f)
        d.rounded_rectangle((tx, ty - s_h * 0.055, tx + tw + s_h * 0.06, ty + s_h * 0.055),
                            radius=s_h * 0.05, fill=BETA_BG)
        d.text((tx + s_h * 0.03, ty), "beta", font=f, fill=WHITE, anchor="lm")
    return img.resize((w, h), Image.LANCZOS)


def logo(w, h, play=RED, text=WHITE):
    """Transparent top-right badge used in the browse screen title."""
    s_w, s_h = w * SS, h * SS
    img = Image.new("RGBA", (s_w, s_h), (0, 0, 0, 0))
    m = mark(int(s_h * 0.5), bg=(34, 35, 41), play=play, halo=False)
    d = ImageDraw.Draw(img)
    f_h = s_h * 0.26
    gap = s_h * 0.06
    text_w = d.textlength("Lumen", font=font(int(f_h)))
    x0 = s_w - (m.width + gap + text_w)
    img.paste(m, (int(x0), int((s_h - m.height) / 2)), m)
    d.text((x0 + m.width + gap, s_h / 2), "Lumen", font=font(int(f_h)), fill=text, anchor="lm")
    return img.resize((w, h), Image.LANCZOS)


def main(out):
    os.makedirs(out, exist_ok=True)
    mark(320).save(os.path.join(out, "app_icon.png"))
    mark(320, beta=True).save(os.path.join(out, "app_icon_beta.png"))
    banner(640, 360).save(os.path.join(out, "app_banner.png"))
    banner(640, 360, beta=True).save(os.path.join(out, "app_banner_beta.png"))
    logo(320, 180).save(os.path.join(out, "app_logo.png"))
    logo(320, 180).save(os.path.join(out, "app_logo_semi_red.png"))
    logo(320, 180, play=GREY, text=GREY).save(os.path.join(out, "app_logo_semi_grey.png"))

    # Preview sheet
    sheet = Image.new("RGBA", (1400, 520), (10, 10, 12, 255))
    sheet.paste(mark(320), (30, 40), mark(320))
    sheet.paste(mark(320, beta=True), (30, 40 + 0), None) if False else None
    b = banner(640, 360)
    sheet.paste(b, (380, 40))
    bb = banner(640, 360, beta=True).resize((320, 180), Image.LANCZOS)
    sheet.paste(bb, (1050, 40))
    ib = mark(160, beta=True)
    sheet.paste(ib, (1130, 250), ib)
    lg = logo(320, 180)
    sheet.paste(lg, (30, 380 - 30), lg)
    d = ImageDraw.Draw(sheet)
    f = font(22, "Regular")
    d.text((30, 32), "App icon", font=f, fill=GREY, anchor="lb")
    d.text((380, 32), "TV launcher banner", font=f, fill=GREY, anchor="lb")
    d.text((1050, 32), "Beta banner and icon", font=f, fill=GREY, anchor="lb")
    d.text((380, 440), "Top-right logo in the app (bottom left)", font=f, fill=GREY, anchor="lb")
    sheet.convert("RGB").save(os.path.join(out, "preview.png"))


if __name__ == "__main__":
    main(sys.argv[1])
