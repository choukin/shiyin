#!/usr/bin/env python3
"""
拾音 · 像素级还原验收工具

把「设计稿单屏导出图」与「真机/模拟器截图」对齐后做逐像素差异统计，
输出一张 diff 热力图和一份量化报告。用法见 docs/PIXEL_PERFECT.md。

依赖 Pillow: python3 -m pip install Pillow

示例:
  python3 tools/pixel_diff.py \
      --design generated-images/screen_02.png \
      --capture /tmp/shiyin/screen_02.png \
      --tolerance 8 --output /tmp/shiyin/diff_02.png
"""

import argparse
import json
import sys
from pathlib import Path


def parse_args():
    p = argparse.ArgumentParser(description="设计稿与设备截图的像素差异统计")
    p.add_argument("--design", required=True, help="设计稿单屏 PNG，建议 390x844 @1x 或 1170x2532 @3x")
    p.add_argument("--capture", required=True, help="adb screencap 得到的设备截图 PNG")
    p.add_argument("--tolerance", type=int, default=8, help="单通道允许的最大差值，默认 8")
    p.add_argument("--output", help="diff 热力图输出路径（可选）")
    p.add_argument("--report", help="JSON 报告输出路径（可选）")
    p.add_argument(
        "--align",
        choices=["width", "exact"],
        default="width",
        help="width=按截图宽度等比缩放设计稿（默认）；exact=直接拉伸到截图尺寸（不推荐）",
    )
    p.add_argument(
        "--design-width",
        type=int,
        default=390,
        help="设计稿的 @1x 逻辑宽度，默认 390dp，用于打印 dp 换算提示",
    )
    return p.parse_args()


def main():
    args = parse_args()
    try:
        from PIL import Image, ImageChops, ImageStat
    except ImportError:
        print("缺少 Pillow，请先执行: python3 -m pip install Pillow", file=sys.stderr)
        return 2

    design_path = Path(args.design)
    capture_path = Path(args.capture)
    for f in (design_path, capture_path):
        if not f.is_file():
            print(f"文件不存在: {f}", file=sys.stderr)
            return 2

    design = Image.open(design_path).convert("RGB")
    capture = Image.open(capture_path).convert("RGB")

    # Pillow 10+ 把 LANCZOS 移到 Image.Resampling，旧版本仍在 Image 上，做兼容。
    try:
        resample = Image.Resampling.LANCZOS
    except AttributeError:
        resample = Image.LANCZOS

    if args.align == "width":
        if design.width == 0:
            print("设计稿宽度为 0", file=sys.stderr)
            return 2
        ratio = capture.width / design.width
        target_h = round(design.height * ratio)
        design = design.resize((capture.width, target_h), resample)
    else:
        design = design.resize(capture.size, resample)

    # 高度不一致时按较小高度裁切，避免把状态栏/导航栏差异计入统计。
    h = min(design.height, capture.height)
    design = design.crop((0, 0, capture.width, h))
    capture = capture.crop((0, 0, capture.width, h))

    diff = ImageChops.difference(design, capture)
    stat = ImageStat.Stat(diff)
    mae = sum(stat.mean) / 3.0

    # 逐像素「任一 RGB 通道」的最大差值 > tolerance 的比例。
    # 不能用 convert("L")：那是加权平均，会低估单通道偏色。
    r, g, b = diff.split()
    max_channel = ImageChops.lighter(ImageChops.lighter(r, g), b)
    hist = max_channel.histogram()
    over = sum(hist[args.tolerance + 1:])
    total = capture.width * h
    over_ratio = over / total if total else 0.0

    report = {
        "design": str(design_path),
        "capture": str(capture_path),
        "aligned_size": [capture.width, h],
        "align": args.align,
        "tolerance": args.tolerance,
        "mae": round(mae, 3),
        "over_tolerance_ratio": round(over_ratio, 5),
        "over_tolerance_pixels": over,
        "total_pixels": total,
        "targets": {"mae": "<= 6", "over_tolerance_ratio": "<= 0.03"},
        "pass": mae <= 6 and over_ratio <= 0.03,
        "design_width_dp_hint": args.design_width,
        "note": "阴影/抗锯齿/动态内容需列入白名单，见 docs/PIXEL_PERFECT.md",
    }

    print(json.dumps(report, ensure_ascii=False, indent=2))

    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        # 放大差异便于肉眼定位
        enhanced = diff.point(lambda v: min(255, v * 4))
        enhanced.save(out)
        print(f"diff 热力图: {out}", file=sys.stderr)

    if args.report:
        rp = Path(args.report)
        rp.parent.mkdir(parents=True, exist_ok=True)
        rp.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    return 0 if report["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
