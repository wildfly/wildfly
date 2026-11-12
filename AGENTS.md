# WildFly agent guidance

WildFly is a multi-module Java/Maven project. Check [README.md](README.md) for the build baseline and [testsuite/README.md](testsuite/README.md) for the testsuite documentation. Follow the conventions of the affected module.

## Subsystem changes

Before changing or reviewing a subsystem, read the [subsystem development guide](.agents/subsystem-development.md). It shows how to find the extension, model, XML, transformers, and tests; implementations differ between subsystems. For subsystem management or XML reviews, use the [review checklist](.agents/subsystem-review-checklist.md).

For a change to a management attribute, resource, operation, default, capability, or persistence behavior, check the whole management contract: registration and runtime handling, descriptions, management model version, compatibility with older model versions, XML parser and writer, XML namespace and schema when applicable, and focused tests. Record the evidence for each decision, including a decision that no version or transformer change is needed. If the model or XML schema version has already changed since the last release on the current stream, that version does not need another bump for this change. Passing tests alone does not establish compatibility.

**Review rule (subsystem management changes only):** If the diff changes the management contract but omits an applicable version, transformer, XML, schema, or test step without evidence that it is unnecessary, report a finding at the affected code. State which older model or XML form may behave incorrectly and the safe action: implement and test the missing step, or document why the existing version or behavior already covers it. Do not approve on test status alone.

Use the [subsystem build and test guide](.agents/subsystem-build-and-test.md) for commands and prerequisites. Keep review comments tied to the actual subsystem change; do not apply this checklist to unrelated code.
