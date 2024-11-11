/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.AbstractSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

@RunWith(Arquillian.class)
@ServerSetup(BasicOpenTelemetryTestCase.OpenTelemetrySetupTask.class)
public class BasicOpenTelemetryTestCase {
    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    @Inject
    private Baggage baggage;

    @Inject
    private Meter meter;

    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, "basic-otel.war")
            .addClasses(
                JaxRsActivator.class,
                OtelMetricResource.class
            )
            .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    public void openTelemetryInjection() {
        Assert.assertNotNull("Injection of OpenTelemetry instance failed", openTelemetry);
    }

    @Test
    public void traceInjection() {
        Assert.assertNotNull("Injection of Tracer instance failed", tracer);
    }

    @Test
    public void baggageInjection() {
        Assert.assertNotNull("Injection of Baggage instance failed", baggage);
    }

    @Test
    public void meterInjection() {
        Assert.assertNotNull("Injection of Meter instance failed", meter);
    }

    @Test
    public void restClientHasFilterAdded() throws ClassNotFoundException {
        try (Client client = ClientBuilder.newClient()) {
            Assert.assertTrue(
                    client.getConfiguration()
                            .isRegistered(Class.forName("io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter"))
            );
        }
    }

    /**
     * Unlike other OpenTelemetry-based tests, this just needs the subsystem. No data needs to be exported, as this test
     * only checks that injection works as expected. This ServerSetupTask, then, merely adds the extension and subsystem,
     * no Docker required.
     */
    static class OpenTelemetrySetupTask extends AbstractSetupTask {
        protected static final String SUBSYSTEM_NAME = "opentelemetry";
        protected static final ModelNode extensionAddress = Operations.createAddress("extension", "org.wildfly.extension.opentelemetry");
        protected static final ModelNode subsystemAddress = Operations.createAddress("subsystem", SUBSYSTEM_NAME);

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            if (!Operations.isSuccessfulOutcome(executeRead(managementClient, extensionAddress))) {
                executeOp(managementClient, Operations.createAddOperation(extensionAddress));
            }

            if (!Operations.isSuccessfulOutcome(executeRead(managementClient, subsystemAddress))) {
                executeOp(managementClient, Operations.createAddOperation(subsystemAddress));
            }

            executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "batch-delay", "1"));
            executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "sampler-type", "on"));

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            executeOp(managementClient, Operations.createRemoveOperation(subsystemAddress));
            executeOp(managementClient, Operations.createRemoveOperation(extensionAddress));

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }
}
