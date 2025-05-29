/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
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
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, MicrometerSetupTask.class})
@DockerRequired
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

    @BeforeClass
    public static void beforeClass() {
        if (AssumeTestGroupUtil.isBootableJar() || AssumeTestGroupUtil.isWildFlyPreview() || isGalleon()) {
            throw new AssumptionViolatedException("Not supported in this configuration");
        }
    }

    private static boolean isGalleon() {
        return System.getProperty("ts.layers") != null || System.getProperty("ts.galleon") != null;
    }

    @Test
    @InSequence(1)
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
    @InSequence(2)
    public void configureConflictingContexts() throws Exception {
        ModelNode addOperation = Operations.createAddOperation(PROMETHEUS_REGISTRY_ADDRESS);
        addOperation.get("context").set("${no.such.property:/metrics}");
        addOperation.get("security-enabled").set("false");

        ModelNode response = managementClient.getControllerClient().execute(Operation.Factory.create(addOperation));
        assertTrue(response.asString(), response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION)
            .asString().contains("WFLYCTL0436"));
    }

    @Test
    @InSequence(3)
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
