# WildFly agent guidance

WildFly is a multi-module Java/Maven project. See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution and PR standards, [README.md](README.md) for the build baseline, and [testsuite/README.md](testsuite/README.md) for testsuite documentation. Follow the conventions of the affected module.

## Subsystem changes

This guidance applies to subsystem code in this repository. For changes in WildFly Core, use that repository's guidance and build. Before changing or reviewing a subsystem here, read the [subsystem development guide](.agents/subsystem-development.md); for management or XML reviews, use the [review checklist](.agents/subsystem-review-checklist.md).

For a change to a management attribute, resource, operation, default, capability, or persistence behavior, check registration and runtime handling, descriptions, model version, older-model compatibility, XML reader and writer, namespace and XSD where applicable, and focused tests. Assess model and XML versions separately against the last release on the current stream; a version already changed since that release needs no second bump. Record evidence when no version or transformer change is needed, and flag an uncertain release baseline.

**Review rule (subsystem management changes only):** If the diff changes the management contract but omits an applicable version, transformer, XML, schema, or test step without evidence that it is unnecessary, report a finding at the affected code. State which older model or XML form may behave incorrectly and the safe action: implement and test the missing step, or document why the existing behavior covers it. Passing tests alone do not establish compatibility.

Use the [subsystem build and test guide](.agents/subsystem-build-and-test.md) for commands and prerequisites. Keep review comments tied to the actual subsystem change; do not apply this checklist to unrelated code.
