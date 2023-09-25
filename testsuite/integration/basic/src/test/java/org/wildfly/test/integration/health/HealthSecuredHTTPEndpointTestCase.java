/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.health;

import static org.junit.Assert.assertEquals;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(HealthSecuredHTTPEndpointSetupTask.class)
public class HealthSecuredHTTPEndpointTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "HealthSecuredHTTPEndpointTestCase.jar")
                .addClasses(HealthSecuredHTTPEndpointSetupTask.class);
        return deployment;
    }

    @Test
    public void securedHTTPEndpointWithoutUserDefined() throws Exception {
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(healthURL));
            assertEquals(401, resp.getStatusLine().getStatusCode());
            resp.close();
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
            resp.close();
        }
    }
}
