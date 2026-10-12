/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ExtendWith(ArquillianExtension.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, MicrometerSetupTask.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestcontainersRequired
@RunAsClient
public class ConflictingPrometheusContextTestCase {
    private static final ModelNode metricsExtension = Operations.createAddress("extension", "org.wildfly.extension.metrics");
    private static final ModelNode metricsSubsystem = Operations.createAddress("subsystem", "metrics");

    public static final ModelNode PROMETHEUS_REGISTRY_ADDRESS = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "prometheus");

    private boolean metricsExtAdded = false;
    private boolean metricsSubsystemAdded = false;

    @ContainerResource
    protected ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-prometheus.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @BeforeAll
    public static void beforeClass() {
        assumeFalse(AssumeTestGroupUtil.isBootableJar() || AssumeTestGroupUtil.isWildFlyPreview() || isGalleon(), "Not supported in this configuration");
    }

    private static boolean isGalleon() {
        return System.getProperty("ts.layers") != null || System.getProperty("ts.galleon") != null;
    }

    @Test
    @Order(1)
    public void setupMetrics() throws IOException {
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, metricsExtension))) {
            executeOp(managementClient, Operations.createAddOperation(metricsExtension));
            metricsExtAdded = true;
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, metricsSubsystem))) {
            executeOp(managementClient, Operations.createAddOperation(metricsSubsystem));
            metricsSubsystemAdded = true;
        }

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Test
    @Order(2)
    public void configureConflictingContexts() throws Exception {
        ModelNode addOperation = Operations.createAddOperation(PROMETHEUS_REGISTRY_ADDRESS);
        addOperation.get("context").set("${no.such.property:/metrics}");
        addOperation.get("security-enabled").set("false");

        ModelNode response = managementClient.getControllerClient().execute(Operation.Factory.create(addOperation));
        assertTrue(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION)
            .asString().contains("WFLYCTL0436"), response.asString());
    }

    @Test
    @Order(3)
    public void tearDown() throws IOException {
        if (Operations.isSuccessfulOutcome(executeRead(managementClient, PROMETHEUS_REGISTRY_ADDRESS))) {
            executeOp(managementClient, Operations.createRemoveOperation(PROMETHEUS_REGISTRY_ADDRESS));
        }
        if (metricsSubsystemAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(metricsSubsystem));
        }
        if (metricsExtAdded) {
            executeOp(managementClient, Operations.createRemoveOperation(metricsExtension));
        }
    }

    public ModelNode executeRead(final ManagementClient managementClient, ModelNode address) throws IOException {
        return managementClient.getControllerClient().execute(Operations.createReadResourceOperation(address));
    }

    private void executeOp(final ManagementClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.getControllerClient().execute(Operation.Factory.create(op));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }
}
