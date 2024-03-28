/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class MicroProfileHealthDisabledDefaultProceduresTestBase {

    protected abstract Integer retrieveChecks(ManagementClient managementClient, final String checkType) throws IOException;

    @Deployment(name = "MicroProfileHealthDisabledDefaultProceduresTestBase")
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
        final Integer readyChecks = retrieveChecks(managementClient, "ready");
        final Integer liveChecks = retrieveChecks(managementClient, "live");
        final Integer startupChecks = retrieveChecks(managementClient, "started");
        final Integer allChecks = readyChecks + liveChecks + startupChecks;
        Assert.assertEquals(1, allChecks.intValue());
    }
}
