/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.ReloadServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ReloadServerSetupTask.class)
public abstract class MicroProfileHealthDisabledDefaultProceduresTestBase {

    abstract void checkGlobalOutcome(final ManagementClient managementClient, final String operation, final boolean mustBeUP,
                                     final String probeName, final Integer expectedChecksCount) throws IOException;

    @Deployment(name = "MicroProfileHealthDisabledDefaultProceduresTestBase", testable = false)
    public static Archive<?> deployDisabledDefaultProcedures() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "DisabledProcedures.war")
                .addClass(MyReadyProbe.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                    "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @Test
    @OperateOnDeployment("MicroProfileHealthDisabledDefaultProceduresTestBase")
    public void testDisabledDefaultProcedures() throws IOException {
        checkGlobalOutcome(managementClient, "check", true, "myReadyProbe", 1);
    }
}
