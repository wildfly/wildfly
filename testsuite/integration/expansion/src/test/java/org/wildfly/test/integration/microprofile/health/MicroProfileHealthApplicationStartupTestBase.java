/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

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

import java.io.IOException;
import java.net.URL;

/**
 * Test that an application with startup probe is correctly recognized by WildFly.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileHealthApplicationStartupSetupTask.class})
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
    @InSequence(1)
    public void testApplicationStartupBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, "check-started", false, null);

        // deploy the archive
        deployer.deploy("MicroProfileHealthApplicationStartupTestBase");
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("MicroProfileHealthApplicationStartupTestBase")
    public void testApplicationStartupAfterDeployment(@ArquillianResource URL url) throws Exception {
        checkGlobalOutcome(managementClient, "check-started", true, "successfulStartupProbe");
    }

    @Test
    @InSequence(3)
    public void testApplicationStartupAfterUndeployment() throws Exception {

        deployer.undeploy("MicroProfileHealthApplicationStartupTestBase");

        checkGlobalOutcome(managementClient, "check-started", false, null);
    }
}
