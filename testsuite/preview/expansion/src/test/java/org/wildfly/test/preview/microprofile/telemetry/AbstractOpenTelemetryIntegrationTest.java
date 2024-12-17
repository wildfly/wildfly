/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.microprofile.telemetry;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import java.net.URL;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.observability.setuptasks.ServiceNameSetupTask.SERVICE_NAME;

public abstract class AbstractOpenTelemetryIntegrationTest {

    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    @ArquillianResource
    protected ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            "otel.metric.export.interval=100";

    static WebArchive createDeployment(String deploymentName) {
        return ShrinkWrap
                .create(WebArchive.class, deploymentName + ".war")
                .addClasses(OtelService.class, RestApp.class)
                .addPackage(JaegerResponse.class.getPackage())
                .addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties")
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    protected void requestOpenTelemetryTrace() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final String restUrl = url.toString() + "/rest/otel";
            Response response = client.target(restUrl).request().get();
            Assert.assertEquals(200, response.getStatus());
        }
        otelCollector.assertTraces(SERVICE_NAME, traces -> Assert.assertFalse("Traces not found for service", traces.isEmpty()));
    }

    static String retrieveServerLog(ManagementClient managementClient, String logFileName) throws Exception {
        PathAddress logFileAddress = PathAddress.pathAddress().append(SUBSYSTEM, "logging").append("log-file", logFileName);
        ModelNode op = Util.createEmptyOperation("read-log-file", logFileAddress);
        op.get("lines").set(30);
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        return result.get(RESULT).asList().stream().map(ModelNode::toString).collect(Collectors.joining("\n"));
    }

}
