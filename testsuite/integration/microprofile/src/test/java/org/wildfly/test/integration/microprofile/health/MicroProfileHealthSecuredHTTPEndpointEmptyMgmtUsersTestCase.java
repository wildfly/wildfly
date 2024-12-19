/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileHealthSecuredHTTPEndpointSetupTask.class, EmptyMgmtUsersSetupTask.class})
public class MicroProfileHealthSecuredHTTPEndpointEmptyMgmtUsersTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        // Dummy deployment to trigger @ServerSetup
        return ShrinkWrap.create(JavaArchive.class, "MicroProfileHealthSecuredHTTPEndpointEmptyMgmtUsersTestCase.jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
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

}
