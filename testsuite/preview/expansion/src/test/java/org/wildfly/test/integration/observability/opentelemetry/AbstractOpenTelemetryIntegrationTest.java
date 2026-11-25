/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import java.net.URL;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

public abstract class AbstractOpenTelemetryIntegrationTest extends BaseOpenTelemetryTest{

    @ArquillianResource
    protected ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    protected void requestOpenTelemetryTrace(String serviceName) throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final String restUrl = url.toString() + "/?name=value";
            Response response = client.target(restUrl).request().get();
            Assert.assertEquals(200, response.getStatus());
        }
        otelCollector.assertTraces(serviceName, traces -> Assert.assertFalse("Traces not found for service", traces.isEmpty()));
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
