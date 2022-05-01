/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

    @Deployment(name = "MicroProfileHealthApplicationStartupTestBaseSetup")
    public static Archive<?> deploySetup() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthApplicationStartupTestBaseSetup.war")
                .addClass(MicroProfileHealthApplicationStartupSetupTask.class);
        return war;
    }

    @Deployment(name = "MicroProfileHealthApplicationStartupTestBase", managed = false)
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
