import os
import xml.etree.ElementTree as ET

from pathlib import Path


SUREFIRE_REPORTS = Path("target/surefire-reports")
JACOCO_REPORT = Path("target/site/jacoco/jacoco.xml")
ARTIFACT = Path("target/rule-engine.jar")

TEST_COUNTERS = ("tests", "failures", "errors", "skipped")
COVERAGE_COUNTERS = ("INSTRUCTION", "LINE", "BRANCH")


def read_test_results():
    reports = list(SUREFIRE_REPORTS.glob("TEST-*.xml"))

    if not reports:
        return None

    totals = dict.fromkeys(TEST_COUNTERS, 0)

    for report in reports:
        root = ET.parse(report).getroot()

        for counter in TEST_COUNTERS:
            totals[counter] += int(root.get(counter, "0"))

    return totals


def read_coverage():
    if not JACOCO_REPORT.is_file():
        return None

    root = ET.parse(JACOCO_REPORT).getroot()
    coverage = {}

    for counter in root.findall("counter"):
        counter_type = counter.get("type")

        if counter_type not in COVERAGE_COUNTERS:
            continue

        covered = int(counter.get("covered", "0"))
        missed = int(counter.get("missed", "0"))
        total = covered + missed

        coverage[counter_type] = (
            f"{covered / total:.2%}" if total else "N/A"
        )

    return coverage


def read_artifact_size():
    if not ARTIFACT.is_file():
        return "N/A"

    size = ARTIFACT.stat().st_size
    return f"{size / (1024 ** 2):.2f} MiB"


def render_table(rows):
    lines = [
        "| Parameter | Value |",
        "|---|---|"
    ]

    for name, value in rows:
        lines.append(f"| {name} | {value} |")

    return "\n".join(lines)


def generate_summary():
    sections = [
        "## Rule Engine Build",
        render_table([
            ("Operating system", os.getenv("RUNNER_OS", "N/A")),
            ("Architecture", os.getenv("RUNNER_ARCH", "N/A")),
            ("Java version", os.getenv("BUILD_JAVA_VERSION", "N/A")),
            ("Maven cache hit", os.getenv("BUILD_CACHE_HIT", "N/A")),
            ("Build status", os.getenv("BUILD_STATUS", "N/A")),
            ("Commit", os.getenv("GITHUB_SHA", "N/A")),
            ("Artifact size", read_artifact_size())
        ])
    ]

    tests = read_test_results()

    sections.append("## Test Results")

    if tests is None:
        sections.append("Test reports are not available.")
    else:
        sections.append(render_table([
            (name.capitalize(), value)
            for name, value in tests.items()
        ]))

    coverage = read_coverage()

    sections.append("## Code Coverage")

    if coverage is None:
        sections.append("Coverage report is not available.")
    else:
        sections.append(render_table([
            (name.capitalize(), coverage.get(name, "N/A"))
            for name in COVERAGE_COUNTERS
        ]))

    return "\n\n".join(sections)


def main():
    summary = generate_summary()
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")

    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as stream:
            stream.write(summary + "\n")
    else:
        print(summary)


if __name__ == "__main__":
    main()
