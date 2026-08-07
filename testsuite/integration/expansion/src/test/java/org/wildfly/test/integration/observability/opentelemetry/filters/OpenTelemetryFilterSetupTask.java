package org.wildfly.test.integration.observability.opentelemetry.filters;

import java.util.List;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;

@TestcontainersRequired
public class OpenTelemetryFilterSetupTask extends OpenTelemetryWithCollectorSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        super.setup(managementClient, containerId);

        try (CLIWrapper cli = new CLIWrapper(true)) {
            // Reject all system metrics by prefix (STARTS_WITH condition)
            List.of("buffer_pool", "classloader", "cpu", "datasources", "ee", "gc", "io", "jca", "jvm",
                            "memory", "messaging", "system", "thread", "transactions", "undertow")
                    .forEach(name -> {
                        System.err.println("=== CLI: /subsystem=opentelemetry/filter=reject-" + name +
                                ":add(outcome=reject, field=meter-name, condition=starts-with, value=" + name + ")");
                        cli.sendLine("/subsystem=opentelemetry/filter=reject-" + name +
                                ":add(outcome=reject, field=meter-name, condition=starts-with, value=" + name + ")");
                    });

            // Reject all demo meters by prefix (STARTS_WITH condition)
            cli.sendLine("/subsystem=opentelemetry/filter=reject-demo:add(outcome=reject, field=meter-name, condition=starts-with, value=demo)");

            // Accept specific demo meters by exact name (EQUALS condition)
            List.of("demo2", "demo5")
                    .forEach(name -> cli.sendLine("/subsystem=opentelemetry/filter=accept-" + name +
                            ":add(outcome=accept, field=meter-name, condition=equals, value=" + name + ")"));

            // Accept meters ending with "3" (ENDS_WITH condition) -> catches demo3
            cli.sendLine("/subsystem=opentelemetry/filter=accept-endswith-3" +
                    ":add(outcome=accept, field=meter-name, condition=ends-with, value=3)");

            // Accept meters containing "2-" (CONTAINS condition) -> catches demo2-1
            cli.sendLine("/subsystem=opentelemetry/filter=accept-contains-2dash:add(outcome=accept, field=meter-name, condition=contains, value=2-)");

            // Accept tagged.alpha by exact meter name
            cli.sendLine("/subsystem=opentelemetry/filter=accept-alpha:add(outcome=accept, field=meter-name, condition=equals, value=tagged.alpha)");

            // Reject meters with tag value "staging" (TAG_VALUE field)
            cli.sendLine("/subsystem=opentelemetry/filter=reject-tag-value-staging:add(outcome=reject, field=tag-value, condition=equals, value=staging)");

            // Reject meters with tag name "priority" (TAG_NAME field)
            cli.sendLine("/subsystem=opentelemetry/filter=reject-tag-name-priority:add(outcome=reject, field=tag-name, condition=equals, value=priority)");

            // Reject meters whose tag values do NOT include "prod" (NEGATE with TAG_VALUE)
            cli.sendLine("/subsystem=opentelemetry/filter=reject-negate-tag-value-prod:add(outcome=reject, field=tag-value, condition=equals, negate=true, value=prod)");

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

    }
}
