/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileHealthSecuredHTTPEndpointSetupTask.class})
public class MicroProfileHealthSecuredHTTPEndpointTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "MicroProfileHealthSecuredHTTPEndpointTestCase.jar")
                .addClasses(MicroProfileHealthSecuredHTTPEndpointSetupTask.class);
        return deployment;
    }

    @Test
    public void securedHTTPEndpointWithoutUserDefined() throws Exception {
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            assertEquals(401, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
            assertTrue("'401 - Unauthorized' message is expected", content.contains("401 - Unauthorized"));
        }
    }

    @Test
    public void securedHTTPEndpoint() throws Exception {
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health";

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("testSuite", "testSuitePassword"));
            HttpClientContext hcContext = HttpClientContext.create();
            hcContext.setCredentialsProvider(credentialsProvider);

            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL), hcContext);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(resp.getEntity());
            resp.close();
            assertTrue("'UP' message is expected", content.contains("UP"));
        }
    }
}
