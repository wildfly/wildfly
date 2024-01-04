/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.health;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HealthHTTPEndpointTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @Test
    public void testHealthEndpoint() throws Exception {
        testHealthEndpoint("/health", 200);
    }

    @Test
    public void testHealthLiveEndpoint() throws Exception {
        testHealthEndpoint("/health/live", 200);
    }

    @Test
    public void testHealthReadyEndpoint() throws Exception {
        testHealthEndpoint("/health/ready", 200);
    }

    /**
     * Test that when a server is suspended, its readiness probe starts to fail (until it is resumed).
     * Test also that suspending a server has no impact on its liveness probe.
     * @throws Exception
     */
    @Test
    public void testHealthReadyEndpointWhenServerIsSuspended() throws Exception {
        // server is live
        testHealthEndpoint("/health/live", 200);
        // and ready
        testHealthEndpoint("/health/ready", 200);

        // suspend the server
        changeSuspendState("suspend");

        // server is still live
        testHealthEndpoint("/health/live", 200);
        // but no longer ready
        testHealthEndpoint("/health/ready", 503);

        // resume the server
        changeSuspendState("resume");

        // server is still live
        testHealthEndpoint("/health/live", 200);
        // and ready again
        testHealthEndpoint("/health/ready", 200);
    }

    private void testHealthEndpoint(String requestPath, int expectedStatusCode) throws IOException {
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + requestPath;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            String content = EntityUtils.toString(resp.getEntity());
            assertEquals(expectedStatusCode, resp.getStatusLine().getStatusCode());
            resp.close();
        }
    }

    private void changeSuspendState(String operation) throws IOException {
        final ModelNode op = Operations.createOperation(operation, new ModelNode().add(""));
        managementClient.getControllerClient().execute(op);
    }
}
