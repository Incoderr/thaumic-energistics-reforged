import colorsys
import struct
import zlib

SIGNATURE = b"\x89PNG\r\n\x1a\n"
CHANNELS = {0: 1, 2: 3, 4: 2, 6: 4}


def paeth(a, b, c):
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def read(path):
    data = path.read_bytes()
    if not data.startswith(SIGNATURE):
        raise ValueError(f"{path} is not a png")
    offset = len(SIGNATURE)
    chunks = []
    idat = b""
    palette = None
    transparency = None
    while offset < len(data):
        length, kind = struct.unpack(">I4s", data[offset:offset + 8])
        body = data[offset + 8:offset + 8 + length]
        offset += 12 + length
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"PLTE":
            palette = body
        elif kind == b"tRNS":
            transparency = body
        elif kind == b"IDAT":
            idat += body
        chunks.append(kind)
    if depth != 8 or interlace:
        raise ValueError(f"{path}: unsupported png layout")
    channels = 1 if color == 3 else CHANNELS[color]
    stride = width * channels
    raw = zlib.decompress(idat)
    rows = []
    previous = bytearray(stride)
    position = 0
    for _ in range(height):
        kind = raw[position]
        row = bytearray(raw[position + 1:position + 1 + stride])
        position += 1 + stride
        for index in range(stride):
            left = row[index - channels] if index >= channels else 0
            up = previous[index]
            corner = previous[index - channels] if index >= channels else 0
            if kind == 1:
                row[index] = (row[index] + left) & 255
            elif kind == 2:
                row[index] = (row[index] + up) & 255
            elif kind == 3:
                row[index] = (row[index] + (left + up) // 2) & 255
            elif kind == 4:
                row[index] = (row[index] + paeth(left, up, corner)) & 255
        rows.append(row)
        previous = row
    pixels = []
    for row in rows:
        for x in range(width):
            if color == 6:
                pixels.append(tuple(row[x * 4:x * 4 + 4]))
            elif color == 2:
                pixels.append((*row[x * 3:x * 3 + 3], 255))
            elif color == 0:
                pixels.append((row[x], row[x], row[x], 255))
            elif color == 4:
                pixels.append((row[x * 2], row[x * 2], row[x * 2], row[x * 2 + 1]))
            else:
                index = row[x]
                alpha = transparency[index] if transparency and index < len(transparency) else 255
                pixels.append((*palette[index * 3:index * 3 + 3], alpha))
    return width, height, pixels


def chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)


def write(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for pixel in pixels[y * width:(y + 1) * width]:
            raw.extend(pixel)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        SIGNATURE
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def rotate_hue(source, destination, degrees):
    width, height, pixels = read(source)
    shift = degrees / 360.0
    tinted = []
    for red, green, blue, alpha in pixels:
        hue, lightness, saturation = colorsys.rgb_to_hls(red / 255.0, green / 255.0, blue / 255.0)
        red, green, blue = colorsys.hls_to_rgb((hue + shift) % 1.0, lightness, saturation)
        tinted.append((round(red * 255), round(green * 255), round(blue * 255), alpha))
    write(destination, width, height, tinted)
