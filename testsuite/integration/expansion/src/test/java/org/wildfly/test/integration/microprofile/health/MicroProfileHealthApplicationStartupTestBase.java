/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URL;

/**
 * Test that an application with startup probe is correctly recognized by WildFly.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup({MicroProfileHealthApplicationStartupSetupTask.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class MicroProfileHealthApplicationStartupTestBase {

    abstract void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException;

    @Deployment(name = "MicroProfileHealthApplicationStartupTestBaseSetup", testable = false)
    public static Archive<?> deploySetup() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthApplicationStartupTestBaseSetup.war")
                .addClass(MicroProfileHealthApplicationStartupSetupTask.class);
        return war;
    }

    @Deployment(name = "MicroProfileHealthApplicationStartupTestBase", managed = false, testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthApplicationStartupTestBase.war")
                .addClass(SuccessfulStartupProbe.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @Order(1)
    public void testApplicationStartupBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, "check-started", false, null);

        // deploy the archive
        deployer.deploy("MicroProfileHealthApplicationStartupTestBase");
    }

    @Test
    @Order(2)
    public void testApplicationStartupAfterDeployment(@ArquillianResource @OperateOnDeployment("MicroProfileHealthApplicationStartupTestBase") URL url) throws Exception {
        checkGlobalOutcome(managementClient, "check-started", true, "successfulStartupProbe");
    }

    @Test
    @Order(3)
    public void testApplicationStartupAfterUndeployment() throws Exception {

        deployer.undeploy("MicroProfileHealthApplicationStartupTestBase");

        checkGlobalOutcome(managementClient, "check-started", false, null);
    }
}
