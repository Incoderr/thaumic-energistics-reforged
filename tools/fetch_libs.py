import hashlib
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LIBS = ROOT / "libs"
JARS = [
    (
        "appliedenergistics2-19.2.18.jar",
        "https://cdn.modrinth.com/data/XxWD5pD3/versions/KDnFUmMm/appliedenergistics2-19.2.18.jar",
        "39bc4cf733a4321631e2637a1a6dfd3527135fdac1b572534e6fd1f9eab7a6dd92d56c5fa066263c95149c9b20cabdd1fd3dff80bea515381b1d2d754cad26c3",
    ),
    (
        "guideme-21.1.22.jar",
        "https://cdn.modrinth.com/data/Ck4E7v7R/versions/QeYKsstN/guideme-21.1.22.jar",
        "2a7b29143c1bef18375f221c337c6e744d04b79bc3cc10cadac46b7d6bb217ad78c51a3c33cd802b221cc7b38ff04a3f7064bd63d292e53bf9f3e09047eeda24",
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
