# Implementation, AI assistance, and vibe coding

QuietGate Google and QuietGate FOSS were developed using an AI-assisted, conversational "vibe coding" workflow. The maintainer supplied the product direction, reviewed proposed changes, made release and privacy decisions, ran builds and inspections, and performed device verification. AI assistance was used to help draft, explain, refactor, test, and review code and documentation; it was not treated as an autonomous author or as proof that the software is correct or secure.

## Languages and tools

- Java: Android application and instrumentation code.
- XML/vector XML: manifest, resources and icon assets.
- Groovy Gradle: dependency and build configuration.
- Python 3: release-folder packaging helper; not part of the installed Android app.
- Markdown: project and release documentation.
- JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Android SDK/build tools 35.

## Development assistance

This project was developed iteratively with OpenAI Codex, an AI coding assistant based on GPT-6 in this session, with maintainer-directed specifications and real-device feedback. The same disclosure applies to both the Google and FOSS editions because they share the maintained project and build workflow. Earlier user-supplied design/fix documents were described as Gemini-generated. This provenance is based on the conversation, not an independent verification of those documents' origin.

The exact model snapshot used across all earlier turns is not available and is not claimed. A ChatGPT subscription tier does not identify the exact model used. No AI/LLM is bundled with or invoked by QuietGate at runtime: matching and categorization are conventional local code, not AI fraud classification.

AI assistance does not establish correctness, authorship of third-party materials, privacy certification, security assurance, or store/F-Droid acceptance. Human review and testing remain the maintainer's responsibility. Verification status and remaining limitations are documented in DEVICE-VERIFICATION.md.

