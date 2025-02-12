/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;
import java.net.URL;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileHealthApplicationReadySetupTask.class})
public abstract class MicroProfileHealthApplicationReadyTestBase {

    abstract void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException;

    @Deployment(name = "MicroProfileHealthApplicationReadyTestBaseSetup", testable = false)
    public static Archive<?> deploySetup() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthApplicationReadyTestBaseSetup.war")
                .addClass(MicroProfileHealthApplicationReadySetupTask.class);
        return war;
    }

    @Deployment(name = "MicroProfileHealthApplicationReadyTestBase", managed = false, testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthApplicationReadyTestBase.war")
                .addClass(MyReadyProbe.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(1)
    public void testApplicationReadinessBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, "check-ready", false, null);

        // deploy the archive
        deployer.deploy("MicroProfileHealthApplicationReadyTestBase");
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("MicroProfileHealthApplicationReadyTestBase")
    public void testApplicationReadinessAfterDeployment(@ArquillianResource URL url) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            checkGlobalOutcome(managementClient, "check-ready", true, "myReadyProbe");
        }
    }

    @Test
    @InSequence(3)
    public void testHealthCheckAfterUndeployment() throws Exception {

        deployer.undeploy("MicroProfileHealthApplicationReadyTestBase");

        checkGlobalOutcome(managementClient, "check-ready", false, null);
    }
}
