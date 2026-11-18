#!/usr/bin/env python3
"""Adds the checksums of new dependencies to gradle/verification-metadata.xml.

Gradle records only what this machine downloads, which for aapt2 is the one jar built for its own
platform, so the other platforms' checksums are taken from the .sha256 files Google Maven publishes.
Entries already in the file are kept: Gradle reads some of them only while writing the lockfile.
"""
import pathlib
import re
import subprocess
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
METADATA = ROOT / "gradle" / "verification-metadata.xml"
# Every task that resolves dependencies, so local builds, lint and R8 all find their checksums
TASKS = ["test", "assembleDebug", "assembleRelease", "lintDebug"]
AAPT2_PLATFORMS = ["linux", "osx", "windows"]
AAPT2_URL = "https://dl.google.com/android/maven2/com/android/tools/build/aapt2/{version}/{artifact}.sha256"
PUBLISHED_ORIGIN = "Google Maven published checksum"

AAPT2_COMPONENT = re.compile(
    r'(?P<head>      <component group="com\.android\.tools\.build" name="aapt2" version="(?P<version>[^"]+)">\n)'
    r'(?P<artifacts>(?:         <artifact .*?\n         </artifact>\n)*)'
    r'(?P<tail>      </component>\n)', re.S)
ARTIFACT = re.compile(r'         <artifact name="(?P<name>[^"]+)">\n.*?         </artifact>\n', re.S)
SHA256 = re.compile(r"[0-9a-f]{64}")


def artifact_entry(name, sha256):
    return (f'         <artifact name="{name}">\n'
            f'            <sha256 value="{sha256}" origin="{PUBLISHED_ORIGIN}"/>\n'
            f'         </artifact>\n')


def add_aapt2_platforms(metadata, fetch_sha256):
    """Returns metadata with every aapt2 version pinned for all platforms.

    fetch_sha256(version, artifact) returns the published checksum of one aapt2 artifact.
    """
    def complete(component):
        version = component["version"]
        artifacts = {match["name"]: match[0] for match in ARTIFACT.finditer(component["artifacts"])}
        for platform in AAPT2_PLATFORMS:
            name = f"aapt2-{version}-{platform}.jar"
            if name not in artifacts:
                artifacts[name] = artifact_entry(name, fetch_sha256(version, name))
        # Gradle lists a component's artifacts sorted by name
        return component["head"] + "".join(artifacts[name] for name in sorted(artifacts)) + component["tail"]

    completed, components = AAPT2_COMPONENT.subn(complete, metadata)
    # Every Android build resolves aapt2, so no match means Gradle changed its layout and the pins would go stale
    if components == 0:
        raise ValueError("Found no aapt2 component in the verification metadata")
    return completed


def parse_published_sha256(artifact, published):
    """Returns the checksum in a .sha256 file, which may follow it with the file name."""
    fields = published.split()
    if not fields or not SHA256.fullmatch(fields[0].lower()):
        raise ValueError(f"Google Maven published no SHA-256 for {artifact}")
    return fields[0].lower()


def fetch_published_sha256(version, artifact):
    with urllib.request.urlopen(AAPT2_URL.format(version=version, artifact=artifact), timeout=30) as response:
        return parse_published_sha256(artifact, response.read().decode("ascii", errors="replace"))


def main():
    subprocess.run([str(ROOT / "gradlew"), "--write-verification-metadata", "sha256", *TASKS], cwd=ROOT, check=True)
    metadata = add_aapt2_platforms(METADATA.read_text(encoding="utf-8"), fetch_published_sha256)
    METADATA.write_text(metadata, encoding="utf-8")


if __name__ == "__main__":
    main()
