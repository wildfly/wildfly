/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/** Verifies that deployment configuration controls both CDI and globally routed tracing. */
@RunAsClient
@ServerSetup(DeploymentSdkDisabledTestCase.TelemetrySetup.class)
public class DeploymentSdkDisabledTestCase extends BaseOpenTelemetryTest {
    /** Enables both OpenTelemetry and MicroProfile Telemetry for this test. */
    public static class TelemetrySetup extends OpenTelemetrySetupTask {
        private static final ModelNode EXTENSION = Operations.createAddress(
                "extension", "org.wildfly.extension.microprofile.telemetry");
        private static final ModelNode SUBSYSTEM = Operations.createAddress(
                "subsystem", "microprofile-telemetry");

        /** {@inheritDoc} */
        @Override
        public void setup(ManagementClient client, String containerId) throws Exception {
            super.setup(client, containerId);
            executeOp(client, Operations.createAddOperation(EXTENSION));
            executeOp(client, Operations.createAddOperation(SUBSYSTEM));
            ServerReload.executeReloadAndWaitForCompletion(client);
        }

        /** {@inheritDoc} */
        @Override
        public void tearDown(ManagementClient client, String containerId) throws Exception {
            executeOp(client, Operations.createRemoveOperation(SUBSYSTEM));
            executeOp(client, Operations.createRemoveOperation(EXTENSION));
            super.tearDown(client, containerId);
        }
    }

    /** Creates a deployment with the MicroProfile Telemetry default. */
    @Deployment(name = "default-disabled", order = 1, testable = false)
    public static WebArchive defaultDisabled() {
        return archive("default-disabled", null);
    }

    /** Creates a deployment that explicitly disables its SDK. */
    @Deployment(name = "explicit-disabled", order = 2, testable = false)
    public static WebArchive explicitDisabled() {
        return archive("explicit-disabled", "otel.sdk.disabled=true");
    }

    /** Creates a deployment that explicitly enables its SDK. */
    @Deployment(name = "explicit-enabled", order = 3, testable = false)
    public static WebArchive explicitEnabled() {
        return archive("explicit-enabled", "otel.sdk.disabled=false");
    }

    /** Creates an enabled deployment that disables sampling independently of the server exporter. */
    @Deployment(name = "sampler-off", order = 4, testable = false)
    public static WebArchive samplerOff() {
        return archive("sampler-off", "otel.sdk.disabled=false\notel.traces.sampler=always_off");
    }

    /**
     * Creates an archive with the requested deployment configuration.
     *
     * @param name the deployment name
     * @param config deployment configuration, or {@code null} for no override
     * @return the archive
     */
    private static WebArchive archive(String name, String config) {
        WebArchive archive = buildBaseArchive(name);
        archive.delete("/META-INF/microprofile-config.properties");
        archive.addClasses(DeploymentSdkDisabledTestCase.class, TraceStatusResource.class);
        if (config != null) {
            archive.addAsManifestResource(new StringAsset(config), "microprofile-config.properties");
        }
        return archive;
    }

    /** Confirms the default and explicit true disable recording while explicit false enables it. */
    @Test
    public void deploymentSdkDisabledControlsRecording() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Assert.assertEquals("false,false", recording(client, "default-disabled"));
            Assert.assertEquals("false,false", recording(client, "explicit-disabled"));
            Assert.assertEquals("true,true", recording(client, "explicit-enabled"));
        }
    }

    /** Confirms a deployment sampler override wins while the server sampler remains enabled. */
    @Test
    public void deploymentSamplerOverridesServerSampler() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Assert.assertEquals("false,false", recording(client, "sampler-off"));
            Assert.assertEquals("true,true", recording(client, "explicit-enabled"));
        }
    }

    /**
     * Reads the recording state exposed by one deployment.
     *
     * @param client the HTTP client
     * @param deploymentName the deployment to query
     * @return injected and routed span recording states
     * @throws Exception if the request fails
     */
    private String recording(Client client, String deploymentName) throws Exception {
        return client.target(new URL(getDeploymentUrl(deploymentName) + "trace-status").toURI())
                .request().get(String.class);
    }

    /** Exposes whether injected and globally routed tracers record spans. */
    @RequestScoped
    @Path("trace-status")
    public static class TraceStatusResource {
        @Inject
        private Tracer tracer;

        /**
         * Returns recording states without depending on asynchronous exporter delivery.
         *
         * @return injected and routed span recording states
         */
        @GET
        public String recording() {
            Span injected = tracer.spanBuilder("injected").startSpan();
            Span routed = GlobalOpenTelemetry.getTracer("deployment-sdk-disabled")
                    .spanBuilder("routed").startSpan();
            try {
                return injected.isRecording() + "," + routed.isRecording();
            } finally {
                injected.end();
                routed.end();
            }
        }
    }
}
