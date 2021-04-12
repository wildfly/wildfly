/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.metrics.secured;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileMetricsSecuredEndpointSetupTask.class})
public class MicroProfileMetricsSecuredEndpointTestCase {
    @ContainerResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "MicroProfileMetricsSecuredEndpointTestCase.jar")
                .addClasses(MicroProfileMetricsSecuredEndpointSetupTask.class);
        return deployment;
    }

    @Test
    public void securedHTTPEndpointWithoutUserDefined() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(endpointURL));
            assertEquals(401, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
            assertTrue("'401 - Unauthorized' message is expected", content.contains("401 - Unauthorized"));
        }
    }

    @Test
    public void securedHTTPEndpoint() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("testSuite", "testSuitePassword"));
            HttpClientContext hcContext = HttpClientContext.create();
            hcContext.setCredentialsProvider(credentialsProvider);

            CloseableHttpResponse resp = client.execute(new HttpGet(endpointURL), hcContext);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
            // MicroProfile Metrics 2.0 has changed its Prometheus conversion format from "base:x" to "base_x"
            String expectedMetric = "base_jvm_uptime_seconds";
            assertTrue(expectedMetric + " metric is expected in the response", content.contains(expectedMetric));
        }
    }
}
