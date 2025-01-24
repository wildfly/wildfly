/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
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
public class MicroProfileHealthDisabledDefaultProceduresDeployUndeployTest {

    private static final String DEPLOYMENT_NAME = "DisabledProcedures.war";

    @ArquillianResource
    private Deployer deployer;

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(name = DEPLOYMENT_NAME, testable = false, managed = false)
    public static Archive<?> deployDisabledDefaultProcedures() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClass(MyReadyProbe.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                    "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_NAME)
    public void testDisabledDefaultProceduresUndeployment() throws IOException {
        // The deployment disables the server procedures and since the value of `empty-readiness-checks` is "UP" by default,
        // then the overall status must be "UP", and contain 5 checks
        final String httpEndpoint = "/health/ready";
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "empty-readiness-checks", 5);
        deployer.deploy(DEPLOYMENT_NAME);
        // The deployment disables the server procedures and since the value of `empty-readiness-checks` is "UP" by default,
        // then the overall status must be "UP", and contain 0 checks
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "myReadyProbe", 1);
        deployer.undeploy(DEPLOYMENT_NAME);
        // The deployment has been removed, therefore it should no longer affect the availability of default procedures,
        // and since the value of `empty-readiness-checks` is "UP" by default, then the overall status must be "DOWN",
        // and contain 5 checks
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "empty-readiness-checks", 5);
    }
}
