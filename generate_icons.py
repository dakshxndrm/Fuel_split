#!/usr/bin/env python3
"""
Android App Icon Generator for Fuel_split App
Generates PNG icons for all density buckets from SVG specification
"""

try:
    from PIL import Image, ImageDraw
    import os
except ImportError:
    print("Installing required packages...")
    import subprocess
    subprocess.check_call([__import__('sys').executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageDraw

def create_icon(size):
    """Create an app icon with gas pump design"""

    # Colors (matching app theme)
    DARK_BG = (11, 11, 26)  # #0B0B1A
    TEAL = (0, 201, 167)    # #00C9A7
    GOLD = (255, 209, 102)  # #FFD166
    RED = (255, 107, 107)   # #FF6B6B
    WHITE = (240, 240, 255) # #F0F0FF

    # Create image with dark background
    img = Image.new('RGBA', (size, size), DARK_BG + (255,))
    draw = ImageDraw.Draw(img)

    # Scale factor (base design is for 108dp)
    scale = size / 108.0

    def scaled(value):
        return int(value * scale)

    # Draw subtle corner accents
    corner_r = scaled(8)
    corner_pos_tl = (scaled(15) - corner_r, scaled(15) - corner_r, scaled(15) + corner_r, scaled(15) + corner_r)
    draw.ellipse(corner_pos_tl, fill=TEAL + (int(0.15 * 255),))

    corner_r2 = scaled(10)
    corner_pos_br = (scaled(93) - corner_r2, scaled(93) - corner_r2, scaled(93) + corner_r2, scaled(93) + corner_r2)
    draw.ellipse(corner_pos_br, fill=GOLD + (int(0.1 * 255),))

    # Gas pump body (rounded rectangle-like shape)
    pump_left = scaled(38)
    pump_top = scaled(26)
    pump_right = scaled(70)
    pump_bottom = scaled(78)
    pump_radius = scaled(6)

    # Draw rounded pump body
    draw.rectangle([pump_left + pump_radius, pump_top, pump_right - pump_radius, pump_bottom], fill=TEAL)
    draw.rectangle([pump_left, pump_top + pump_radius, pump_right, pump_bottom - pump_radius], fill=TEAL)
    draw.ellipse([pump_left, pump_top, pump_left + 2*pump_radius, pump_top + 2*pump_radius], fill=TEAL)
    draw.ellipse([pump_right - 2*pump_radius, pump_top, pump_right, pump_top + 2*pump_radius], fill=TEAL)
    draw.ellipse([pump_left, pump_bottom - 2*pump_radius, pump_left + 2*pump_radius, pump_bottom], fill=TEAL)
    draw.ellipse([pump_right - 2*pump_radius, pump_bottom - 2*pump_radius, pump_right, pump_bottom], fill=TEAL)

    # Pump nozzle
    nozzle_start_x = pump_right
    nozzle_start_y = scaled(42)
    nozzle_end_x = scaled(85)
    nozzle_end_y = scaled(28)

    # Draw nozzle line (thick)
    draw.line([(nozzle_start_x, nozzle_start_y), (nozzle_end_x, nozzle_end_y)],
              fill=TEAL, width=scaled(4))

    # Nozzle tip
    draw.ellipse([nozzle_end_x - scaled(3), nozzle_end_y - scaled(3),
                  nozzle_end_x + scaled(3), nozzle_end_y + scaled(3)], fill=TEAL)

    # Display screen (dark)
    screen_left = scaled(42)
    screen_top = scaled(24)
    screen_right = scaled(66)
    screen_bottom = scaled(37)
    screen_radius = scaled(2)

    draw.rectangle([screen_left + screen_radius, screen_top, screen_right - screen_radius, screen_bottom], fill=DARK_BG)
    draw.rectangle([screen_left, screen_top + screen_radius, screen_right, screen_bottom - screen_radius], fill=DARK_BG)
    draw.ellipse([screen_left, screen_top, screen_left + 2*screen_radius, screen_top + 2*screen_radius], fill=DARK_BG)
    draw.ellipse([screen_right - 2*screen_radius, screen_top, screen_right, screen_top + 2*screen_radius], fill=DARK_BG)

    # Display lines (golden)
    line_thickness = max(1, scaled(1))
    draw.line([(screen_left + scaled(2), screen_top + scaled(3)),
               (screen_right - scaled(2), screen_top + scaled(3))], fill=GOLD, width=line_thickness)
    draw.line([(screen_left + scaled(2), screen_top + scaled(7)),
               (screen_right - scaled(2), screen_top + scaled(7))], fill=GOLD, width=line_thickness)
    draw.line([(screen_left + scaled(2), screen_top + scaled(11)),
               (screen_right - scaled(5), screen_top + scaled(11))], fill=GOLD, width=line_thickness)

    # Button (golden)
    btn_left = scaled(42)
    btn_top = scaled(40)
    btn_right = scaled(66)
    btn_bottom = scaled(49)
    btn_radius = scaled(2)

    draw.rectangle([btn_left + btn_radius, btn_top, btn_right - btn_radius, btn_bottom], fill=GOLD)
    draw.rectangle([btn_left, btn_top + btn_radius, btn_right, btn_bottom - btn_radius], fill=GOLD)
    draw.ellipse([btn_left, btn_top, btn_left + 2*btn_radius, btn_top + 2*btn_radius], fill=GOLD)
    draw.ellipse([btn_right - 2*btn_radius, btn_top, btn_right, btn_top + 2*btn_radius], fill=GOLD)

    # Button inner area (dark)
    inner_left = scaled(46)
    inner_top = scaled(42)
    inner_right = scaled(62)
    inner_bottom = scaled(47)
    draw.rectangle([inner_left, inner_top, inner_right, inner_bottom], fill=DARK_BG)

    # Base/feet of pump
    base_left = scaled(42)
    base_top = scaled(72)
    base_right = scaled(66)
    base_bottom = scaled(78)
    draw.rectangle([base_left + scaled(1), base_top, base_right - scaled(1), base_bottom], fill=TEAL)

    # Left person/participant (Red) - Circle A
    person_a_x = scaled(28)
    person_a_y = scaled(68)
    person_a_r = scaled(5)
    draw.ellipse([person_a_x - person_a_r, person_a_y - person_a_r,
                  person_a_x + person_a_r, person_a_y + person_a_r], fill=RED)

    # Right person/participant (Teal) - Circle B
    person_b_x = scaled(80)
    person_b_y = scaled(68)
    person_b_r = scaled(5)
    draw.ellipse([person_b_x - person_b_r, person_b_y - person_b_r,
                  person_b_x + person_b_r, person_b_y + person_b_r], fill=TEAL)

    # Connection lines showing split
    line_width = max(1, scaled(1))
    draw.line([(person_a_x + person_a_r, person_a_y), (pump_left - scaled(2), person_a_y)],
              fill=RED, width=line_width)
    draw.line([(person_b_x - person_b_r, person_b_y), (pump_right + scaled(2), person_b_y)],
              fill=TEAL, width=line_width)

    return img

def main():
    # Define density configurations
    # Format: (density_name, size_in_pixels)
    densities = [
        ('mdpi', 108),
        ('hdpi', 162),
        ('xhdpi', 216),
        ('xxhdpi', 324),
        ('xxxhdpi', 432),
    ]

    base_path = r"C:\Users\Daksh\Desktop\CODING\Fuel_split\app\src\main\res"

    print("Generating app icons for all densities...")
    print("-" * 50)

    for density, size in densities:
        # Create icon
        icon = create_icon(size)

        # Create square icon
        folder = os.path.join(base_path, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)

        # Save regular icon
        icon_path = os.path.join(folder, "ic_launcher.png")
        icon.save(icon_path, 'PNG', quality=95)
        print(f"✓ Created {density}: {size}x{size}px - {icon_path}")

        # Save rounded icon
        icon_round_path = os.path.join(folder, "ic_launcher_round.png")

        # Create rounded version
        rounded = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        rounded_draw = ImageDraw.Draw(rounded)

        # Draw circle mask
        rounded_draw.ellipse([0, 0, size - 1, size - 1], fill=(255, 255, 255, 255))

        # Apply mask
        icon_rgba = icon.convert('RGBA')
        icon_rounded = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        icon_rounded.paste(icon_rgba, (0, 0), rounded)

        icon_rounded.save(icon_round_path, 'PNG', quality=95)
        print(f"✓ Created {density} (round): {size}x{size}px - {icon_round_path}")

    print("-" * 50)
    print("✅ All icons generated successfully!")
    print("\nNext steps:")
    print("1. Build and run the app to see the new icons")
    print("2. Test on multiple devices and Android versions")
    print("3. Check the app drawer and settings to verify appearance")

if __name__ == "__main__":
    main()

