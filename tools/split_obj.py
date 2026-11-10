import argparse
import re
from pathlib import Path


def material_textures(mtl):
    textures = {}
    current = None
    for line in mtl.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if parts[:1] == ["newmtl"]:
            current = parts[1]
        elif parts[:1] == ["map_Kd"] and current:
            textures[current] = Path(parts[1]).stem
    return textures


def split(source, destination):
    textures = material_textures(source.with_suffix(".mtl"))
    out = []
    obj_name = ""
    for line in source.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if not parts or parts[0] in ("#", "mtllib", "s"):
            continue
        if parts[0] == "o":
            obj_name = parts[1] if len(parts) > 1 else ""
            continue
        if parts[0] == "usemtl":
            texture = textures.get(parts[1])
            if texture is None:
                texture = "glass" if obj_name == "glass" else "frame"
            out.append(f"o {re.sub(r'[^a-z0-9_]', '_', texture.lower())}")
            continue
        out.append(line)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text("\n".join(out) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    split(args.source, args.destination)


if __name__ == "__main__":
    main()
