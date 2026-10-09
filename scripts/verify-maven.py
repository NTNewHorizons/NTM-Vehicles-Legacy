#!/usr/bin/env python3
"""Verify the published Maven JAR matches the local release artifact."""

import base64
import os
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET

pom = ET.parse("build/publications/maven/pom-default.xml").getroot()
namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
group, artifact, version = (
    pom.findtext(f"m:{name}", namespaces=namespace)
    for name in ("groupId", "artifactId", "version")
)
if not all((group, artifact, version)):
    raise SystemExit("Missing Maven publication coordinates")
name = f"{artifact}-{version}.jar"
url = "/".join((os.environ["MAVEN_PUBLISHING_URL"].rstrip("/"),
                group.replace(".", "/"), artifact, version, name))
credentials = f'{os.environ["MAVEN_USER"]}:{os.environ["MAVEN_PASSWORD"]}'
request = urllib.request.Request(url, headers={
    "Authorization": "Basic " + base64.b64encode(credentials.encode()).decode(),
})
with urllib.request.urlopen(request, timeout=60) as response:
    if response.read() != Path("build/libs", name).read_bytes():
        raise SystemExit("Published Maven JAR differs from the local release JAR")
print("Verified Maven release JAR")
