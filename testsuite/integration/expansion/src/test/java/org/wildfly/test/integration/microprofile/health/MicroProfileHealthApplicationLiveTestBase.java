/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.ReloadServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup(ReloadServerSetupTask.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class MicroProfileHealthApplicationLiveTestBase {

    abstract void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException;

    @Deployment(name = "MicroProfileHealthTestCase", managed = false, testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class, MyLiveProbe.class, HealthConfigSource.class)
                .addAsServiceProvider(ConfigSource.class, HealthConfigSource.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @Order(1)
    public void healthCheckBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, "check", true, null);
        checkGlobalOutcome(managementClient, "check-live", true, null);

        // deploy the archive
        deployer.deploy("MicroProfileHealthTestCase");
    }

    @Test
    @Order(2)
    public void healthCheckAfterDeployment(@ArquillianResource @OperateOnDeployment("MicroProfileHealthTestCase") URL url) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            checkGlobalOutcome(managementClient, "check", true, "myLiveProbe");
            checkGlobalOutcome(managementClient, "check-live", true, "myLiveProbe");

            HttpPost request = new HttpPost(url + "microprofile/myApp");
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("up", "false"));
            request.setEntity(new UrlEncodedFormEntity(nvps));

            CloseableHttpResponse response = client.execute(request);
            assertEquals(200, response.getStatusLine().getStatusCode());

            checkGlobalOutcome(managementClient, "check", false, "myLiveProbe");
            checkGlobalOutcome(managementClient, "check-live", false, "myLiveProbe");
        }
    }

    @Test
    @Order(3)
    public void healthCheckAfterUndeployment() throws Exception {

        deployer.undeploy("MicroProfileHealthTestCase");

        checkGlobalOutcome(managementClient, "check", true, null);
        checkGlobalOutcome(managementClient, "check-live", true, null);
    }


}
