/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
