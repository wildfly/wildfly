# Contributing guide

**First of all, thank you for taking the time to contribute to WildFly!**
The below contents will help you through the steps for getting started with WildFly.
Please make sure to read the relevant section before making your contribution.
It will make it a lot easier for us maintainers and smooth out the experience for all involved.
The community looks forward to your contributions.

* Git setup: https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/github_setup.adoc
* Contributing: https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/contributing.adoc
* Pull request standard: https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/pullrequest_standards.adoc

[//]: # ( TODO Replace these links with published version of the documents once we do publish it with a permalink)

If you like our project, but just don’t have time to contribute, that’s fine.
There are other easy ways to support the project and show your appreciation.

* Mention the project at local meetups and tell your friends/colleagues.
* Post about it and also check out our [X.com (Twitter) page](https://x.com/WildFlyAS) and [Mastodon page](https://fosstodon.org/@wildflyas).
* Check out our [YouTube](https://www.youtube.com/@WildFlyAS) videos.

# Issues

---

WildFly uses Jira to manage issues.
All issues can be found [here](https://issues.redhat.com/projects/WFLY/issues/).

To create a new issue, comment on an existing issue, or assign an issue to yourself, you'll need to first [create a Jira account](https://issues.redhat.com/).

# Good First Issues

---

Check out our issues with the `good-first-issue` label.
These are a triaged set of issues that are great for getting started on our project.
These can be found [here](https://issues.redhat.com/issues/?filter=12403174).

Once you have selected an issue you'd like to work on, make sure it's not already assigned to someone else.
To assign an issue to yourself, simply click on "Start Progress".
This will automatically assign the issue to you.
If you're not able to assign it to yourself that way, post a message in the [wildfly-developers Zulip stream](https://wildfly.zulipchat.com/#narrow/stream/174184-wildfly-developers) and someone will help get it assigned to you.

Lastly, this project is an open source project.
Please act responsibly, be nice, polite and enjoy!

---

# Using Eclipse

1. Install the latest version of eclipse
2. Make sure Xmx in eclipse.ini is at least 1280M, and it's using JDK 17
3. Launch eclipse and install the m2e plugin, make sure it uses your repo configs
   (get it from: http://www.eclipse.org/m2e/
   or install "Maven Integration for Eclipse" from the Eclipse Marketplace)
4. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted
   set forbidden reference to WARNING
5. In eclipse preferences Java->Code Style, import the cleanup, templates, and
   formatter configs in [ide-configs/eclipse](https://github.com/wildfly/wildfly-core/tree/main/ide-configs) in the wildfly-core repository.
6. In eclipse preferences Java->Editor->Save Actions enable "Additional Actions",
   and deselect all actions except for "Remove trailing whitespace"
7. Use import on the root pom, which will pull in all modules
8. Wait (m2e takes a while on initial import)
