"""Offline regression checks for dependency access and release failures."""

import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import runpy


ROOT = Path(__file__).resolve().parents[1]


class BuildScriptTests(unittest.TestCase):
    def test_macos_hash_fallback_and_cached_dependency(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "scripts").mkdir()
            (root / "patches").mkdir()
            shutil.copy(ROOT / "prepare-dependencies.sh", root / "scripts")
            patch = root / "patches/ivl-custom-hit-once.patch"
            patch.write_text("test patch\n")
            dependency = root / ".dependencies/IVL"
            (dependency / ".git").mkdir(parents=True)
            (dependency / "build/libs").mkdir(parents=True)
            (dependency / "build/libs/immersivevehicleslegacy-0.1.0-ntmv2-dev.jar").touch()
            (dependency / ".bridge-patch-sha256").write_text(
                hashlib.sha256(patch.read_bytes()).hexdigest() + "\n")
            core = dependency / "MinecraftTransportSimulator"
            bullet = core / "mccore/src/main/java/minecrafttransportsimulator/entities/instances/EntityBullet.java"
            bullet.parent.mkdir(parents=True)
            bullet.touch()
            commands = root / "bin"
            commands.mkdir()
            for name in ("bash", "dirname", "cut", "cat", "shasum"):
                executable = shutil.which(name)
                self.assertIsNotNone(executable, name)
                (commands / name).symlink_to(executable)
            git = commands / "git"
            git.write_text("""#!/bin/bash
case "$*" in
  *MinecraftTransportSimulator*rev-parse*) echo cd9cfb8fe74dbcc426eb830f14822adf7402261f ;;
  *rev-parse*) echo d32bf237d2eb741ce11f86285a7bbe6157722be1 ;;
  *apply*) exit 0 ;;
  *) exit 99 ;;
esac
""")
            git.chmod(0o755)
            result = subprocess.run([shutil.which("bash"), str(root / "scripts/prepare-dependencies.sh")],
                                    env={**os.environ, "PATH": str(commands)}, capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)

    def test_askpass_limits_hosts_and_supplies_token(self):
        script = ROOT / "git-askpass.sh"
        environment = {**os.environ, "IVL_READ_TOKEN": "test-secret"}
        for prompt, expected in (("Username for 'https://github.com':", "x-access-token"),
                                 ("Password for 'https://x-access-token@github.com':", "test-secret")):
            result = subprocess.run(["bash", str(script), prompt], env=environment,
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode)
            self.assertEqual(expected, result.stdout.strip())
        result = subprocess.run(["bash", str(script), "Password for 'https://other.example':"],
                                env=environment, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("test-secret", result.stdout + result.stderr)

    def test_maven_verification_rejects_wrong_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "build/publications/maven").mkdir(parents=True)
            (root / "build/libs").mkdir(parents=True)
            (root / "build/publications/maven/pom-default.xml").write_text('''
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <groupId>example.bridge</groupId><artifactId>ntm_vehicles</artifactId>
  <version>1.0.0-1.7.10</version>
</project>''')
            (root / "build/libs/ntm_vehicles-1.0.0-1.7.10.jar").write_bytes(b"release artifact")
            environment = {"MAVEN_PUBLISHING_URL": "https://maven.example/releases/",
                           "MAVEN_USER": "test", "MAVEN_PASSWORD": "secret"}
            previous = Path.cwd()
            try:
                os.chdir(root)
                with patch.dict(os.environ, environment), patch("urllib.request.urlopen") as open_url:
                    response = open_url.return_value.__enter__.return_value
                    response.read.return_value = b"release artifact"
                    runpy.run_path(str(ROOT / "verify-maven.py"))
                    request = open_url.call_args.args[0]
                    self.assertEqual("https://maven.example/releases/example/bridge/ntm_vehicles/"
                                     "1.0.0-1.7.10/ntm_vehicles-1.0.0-1.7.10.jar", request.full_url)
                    response.read.return_value = b"wrong artifact"
                    with self.assertRaises(SystemExit):
                        runpy.run_path(str(ROOT / "verify-maven.py"))
            finally:
                os.chdir(previous)

    def test_release_creation_upload_and_verification_fail_closed(self):
        for failure in ("create", "upload", "download", "mismatch", "none"):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "build/libs").mkdir(parents=True)
                (root / "build/libs/ntm_vehicles-1.0.0-1.7.10.jar").write_bytes(b"release artifact")
                commands = root / "bin"
                commands.mkdir()
                git_hub = commands / "gh"
                git_hub.write_text("""#!/bin/bash
echo "$*" >> "$CALLS"
case "$2" in
  view) [[ "$*" == *--json* ]] && echo false || exit 1 ;;
  create|upload) [[ "$FAILURE" != "$2" ]] ;;
  download)
    [[ "$FAILURE" != download ]] || exit 1
    while [[ "$1" != --dir ]]; do shift; done
    if [[ "$FAILURE" == mismatch ]]; then
      echo wrong > "$2/ntm_vehicles-1.0.0-1.7.10.jar"
    else
      cp build/libs/ntm_vehicles-1.0.0-1.7.10.jar "$2/"
    fi ;;
  edit) exit 0 ;;
  *) exit 99 ;;
esac
""")
                git_hub.chmod(0o755)
                calls = root / "calls"
                result = subprocess.run(["bash", str(ROOT / "publish-release.sh")], cwd=root,
                                        env={**os.environ, "PATH": str(commands) + ":" + os.environ["PATH"],
                                             "VERSION": "1.0.0-1.7.10", "FAILURE": failure, "CALLS": str(calls)},
                                        capture_output=True, text=True)
                self.assertEqual(failure == "none", result.returncode == 0, result.stderr)
                if failure != "none":
                    self.assertNotIn("release edit", calls.read_text())


if __name__ == "__main__":
    unittest.main()
