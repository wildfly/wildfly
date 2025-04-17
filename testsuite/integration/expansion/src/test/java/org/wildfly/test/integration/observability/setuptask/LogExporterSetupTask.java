/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.setuptask;

import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;

@DockerRequired
public class LogExporterSetupTask implements ServerSetupTask {
    private static final int PORT = 1223;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        try (CLIWrapper cli =  new CLIWrapper(true)) {
            cli.sendLine("/system-property=otel.logs.exporter:add(value=json)");
            cli.sendLine("/system-property=otel.exporter.otlp.logs.endpoint:add(value=http://localhost:" + PORT + ")");

        }
        ServerReload.executeReloadAndWaitForCompletion(managementClient, 50000);

    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        try (CLIWrapper cli =  new CLIWrapper(true)) {
            cli.sendLine("/system-property=otel.logs.exporter:remove");
            cli.sendLine("/system-property=otel.exporter.otlp.logs.endpoint:remove");
        }
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
