import hashlib
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LIBS = ROOT / "libs"
JARS = [
    (
        "appliedenergistics2-26.3.1-alpha.jar",
        "https://cdn.modrinth.com/data/XxWD5pD3/versions/PDPQ4WfW/appliedenergistics2-26.3.1-alpha.jar",
        "0b2cd09cb2fc63c39e600d2989edb6c8ac2ceac396cbb15224aadd87c555ffa8465503af165a02df73646f1e0cc2f3a6d711f669fa7ba9a5dd9fe88d0b9533cc",
    ),
    (
        "guideme-26.3.7-alpha.jar",
        "https://cdn.modrinth.com/data/Ck4E7v7R/versions/fDhc3PEw/guideme-26.3.7-alpha.jar",
        "8baf0f39168b59faed239689a220fe041c490c09d71086ec5986e6b34463f0c56230cc0ba48c75fe8aaba9e30716416118d15d27ea938bee816d4f91557466f4",
    ),
]


def sha512(path):
    return hashlib.sha512(path.read_bytes()).hexdigest()


def main():
    LIBS.mkdir(exist_ok=True)
    for name, url, digest in JARS:
        target = LIBS / name
        if target.exists() and sha512(target) == digest:
            print(f"ok {name}")
            continue
        partial = target.with_suffix(".part")
        subprocess.run(["curl", "-fsSL", "-o", str(partial), url], check=True)
        if sha512(partial) != digest:
            partial.unlink()
            print(f"sha512 mismatch for {name}")
            return 1
        partial.replace(target)
        print(f"downloaded {name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
