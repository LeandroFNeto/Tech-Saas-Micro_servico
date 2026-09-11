"""Gera o banner do README com o degradê da tela de login (roxo + laranja)."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

LARGURA, ALTURA = 1280, 540
BRAND_900 = (26, 11, 46)
BRAND_500 = (124, 58, 173)
ACCENT = (247, 147, 30)
RAIZ = Path(__file__).resolve().parents[1]
LOGO = RAIZ / "frontend" / "public" / "logo-gamb.jpg"
SAIDA = Path(__file__).with_name("readme-hero.png")


def misturar(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    t = max(0.0, min(1.0, t))
    return (
        int(a[0] * (1 - t) + b[0] * t),
        int(a[1] * (1 - t) + b[1] * t),
        int(a[2] * (1 - t) + b[2] * t),
    )


def fundo_login() -> Image.Image:
    img = Image.new("RGB", (LARGURA, ALTURA), BRAND_900)
    pix = img.load()
    cx, cy = LARGURA / 2, 0
    raio = ALTURA * 1.35
    for y in range(ALTURA):
        for x in range(LARGURA):
            dist = math.hypot(x - cx, y - cy) / raio
            alpha = 0.32 * max(0.0, 1.0 - dist)
            pix[x, y] = misturar(BRAND_900, ACCENT, alpha)

    blob = Image.new("RGBA", (LARGURA, ALTURA), (0, 0, 0, 0))
    draw = ImageDraw.Draw(blob)
    draw.ellipse((-180, ALTURA - 280, 280, ALTURA + 180), fill=(*BRAND_500, 70))
    blob = blob.filter(ImageFilter.GaussianBlur(70))
    img = Image.alpha_composite(img.convert("RGBA"), blob).convert("RGB")
    return img


def logo_arredondado(tamanho: int) -> Image.Image:
    logo = Image.open(LOGO).convert("RGBA").resize((tamanho, tamanho), Image.Resampling.LANCZOS)
    mascara = Image.new("L", (tamanho, tamanho), 0)
    ImageDraw.Draw(mascara).rounded_rectangle((0, 0, tamanho - 1, tamanho - 1), radius=36, fill=255)
    logo.putalpha(mascara)

    anel = Image.new("RGBA", (tamanho + 8, tamanho + 8), (0, 0, 0, 0))
    ImageDraw.Draw(anel).rounded_rectangle(
        (0, 0, tamanho + 7, tamanho + 7),
        radius=40,
        outline=(255, 140, 66, 140),
        width=3,
    )
    anel.paste(logo, (4, 4), logo)
    return anel


def fonte(tamanho: int, *candidatos: str) -> ImageFont.ImageFont:
    for caminho in candidatos:
        if Path(caminho).exists():
            return ImageFont.truetype(caminho, tamanho)
    return ImageFont.load_default()


def fonte_titulo(tamanho: int) -> ImageFont.ImageFont:
    return fonte(
        tamanho,
        "C:/Windows/Fonts/georgia.ttf",
        "C:/Windows/Fonts/times.ttf",
        "C:/Windows/Fonts/arial.ttf",
    )


def fonte_texto(tamanho: int) -> ImageFont.ImageFont:
    return fonte(
        tamanho,
        "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/calibri.ttf",
    )


def texto_centralizado(
    draw: ImageDraw.ImageDraw,
    mensagem: str,
    y: int,
    font: ImageFont.ImageFont,
    cor: tuple[int, int, int, int],
) -> int:
    caixa = draw.textbbox((0, 0), mensagem, font=font)
    x = (LARGURA - (caixa[2] - caixa[0])) // 2
    draw.text((x, y), mensagem, font=font, fill=cor)
    return y + (caixa[3] - caixa[1])


def main() -> None:
    img = fundo_login().convert("RGBA")
    logo = logo_arredondado(168)
    lx = (LARGURA - logo.width) // 2
    ly = 72
    img.alpha_composite(logo, (lx, ly))

    draw = ImageDraw.Draw(img)
    y = texto_centralizado(draw, "gamb", ly + logo.height + 18, fonte_titulo(52), (255, 255, 255, 245))
    y = texto_centralizado(
        draw,
        "SaaS de reservas pelo WhatsApp + Google Agenda",
        y + 18,
        fonte_texto(26),
        (235, 224, 245, 240),
    )
    texto_centralizado(
        draw,
        "MVP rodando: https://gamb.site   ·   API: https://api.gamb.site",
        y + 12,
        fonte_texto(22),
        (255, 140, 66, 245),
    )

    # --- INÍCIO DA ALTERAÇÃO ---
    # Cria uma máscara para arredondar o banner inteiro
    raio_borda = 40 # Ajuste o valor para deixar mais ou menos redondo
    mascara_banner = Image.new("L", (LARGURA, ALTURA), 0)
    ImageDraw.Draw(mascara_banner).rounded_rectangle((0, 0, LARGURA, ALTURA), radius=raio_borda, fill=255)

    # Aplica a máscara na imagem principal
    img.putalpha(mascara_banner)

    # Remove o .convert("RGB") para manter a transparência (RGBA) no PNG final
    img.save(SAIDA, "PNG", optimize=True)
    # --- FIM DA ALTERAÇÃO ---

    print(f"Gerado: {SAIDA}")


if __name__ == "__main__":
    main()
