/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
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

/**
 * Validate behavior of MicroProfile Health integration with respect to default procedures, and the behavior when
 * those are disabled by multiple deployments.
 *
 * @author <a href="mailto:fburzigo@redhat.com">Fabio Burzigotti</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ReloadServerSetupTask.class)
public class MicroProfileHealthDisabledDefaultProceduresDeployUndeployTest {

    private static final String DEPLOYMENT_NAME = "DisabledProcedures1.war";

    private static final String ANOTHER_DEPLOYMENT_NAME = "DisabledProcedures2.war";

    @ArquillianResource
    private Deployer deployer;

    @ContainerResource
    ManagementClient managementClient;

    /**
     * A deployment that contains a user defined readiness check. The deployment also sets
     * {@code mp.health.disable-default-procedures=true}
     *
     * @return A deployment that contains one readiness check and that disables default procedures via MP Config.
     */
    @Deployment(name = DEPLOYMENT_NAME, testable = false, managed = false)
    public static Archive<?> deployDisabledDefaultProceduresWithOneUserDefinedReadinessCheck() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClass(MyReadyProbe.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                    "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    /**
     * A deployment that contains no user defined checks, and defining {@code mp.health.disable-default-procedures=true}
     *
     * @return A deployment that contains no user defined Health checks, and that disables default procedures via MP Config.
     */
    @Deployment(name = ANOTHER_DEPLOYMENT_NAME, testable = false, managed = false)
    public static Archive<?> deployDisabledDefaultProceduresWithNoUserDefinedChecks() {
        return ShrinkWrap.create(WebArchive.class, ANOTHER_DEPLOYMENT_NAME)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    /**
     * Verify the server behavior with respect to the MicroProfile Health default procedures, before and after
     * multiple applications are deployed.
     *
     * The test first verifies that all the checks are reported when nothing has been deployed.
     *
     * Then, the first application is deployed, which sets {@code mp.health.disable-default-procedures=true}. The test
     * verifies that default procedures have been disabled, and that they no longer appear in the Health check
     * response payload. This application also defines a readiness user defined check.
     *
     * The same conditions will be verified after another application defining {@code mp.health.disable-default-procedures=true}
     * is deployed. This application does not implement any user defined checks, so the test expects no changes in the
     * returned response payload.
     *
     * Now the first deployment is undeployed, and the test verifies that default procedures are still disabled because
     * of the remaining deployment configuration, while no checks are reported at all, as no user checks are defined
     * anymore.
     *
     * Finally, the last deployment is undeployed too, and the test verifies that default procedures and related checks
     * are exposed again.
     */
    @Test
    public void testDisabledDefaultProceduresMultiDeployment() throws IOException {
        final String httpEndpoint = "/health/ready";
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + httpEndpoint;
        // Before we deploy, the server procedures are enabled and since the value of `empty-readiness-checks` is "UP" by default,
        // then the overall status must be "UP", and contain 5 checks
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "empty-readiness-checks", 5);
        // The first deployment is deployed, it defines mp.health.disable-default-procedures=true, so it disables the
        // server procedures, and since the value of `empty-readiness-checks` is "UP" by default,
        // then the overall status must be "UP", and contain 1 user defined check
        deployer.deploy(DEPLOYMENT_NAME);
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "myReadyProbe", 1);
        // Let's deploy the second deployment as well, and we should see no changes, since both are disabling
        // the default procedures via MP Config
        deployer.deploy(ANOTHER_DEPLOYMENT_NAME);
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "myReadyProbe", 1);
        // Now, let's undeploy the first deployment. Here default procedures are still expected to be disabled, because
        // the remaining deployment defines mp.health.disable-default-procedures=true. Also, since it does not contain
        // any user defined check, we expect for 0 checks and an "UP" overall status
        deployer.undeploy(DEPLOYMENT_NAME);
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, null, 0);
        // Finally, both the deployments have been removed. Nothing should affect the availability of default procedures
        // anymore, and since the value of `empty-readiness-checks` is "UP" by default, then the overall
        // status must be "UP", and contain 5 checks, again.
        deployer.undeploy(ANOTHER_DEPLOYMENT_NAME);
        MicroProfileHealthUtils.testHttpEndPoint(healthURL, true, "empty-readiness-checks", 5);
    }
}
