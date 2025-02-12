package org.wildfly.test.integration.microprofile.health;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class MicroProfileHealthDisabledDefaultProceduresMultiWarTestBase {
    static final String SERVICE_ONE = "service-one";
    static final String SERVICE_TWO = "service-two";

    abstract void checkGlobalOutcome(final ManagementClient managementClient, final String operation, final boolean mustBeUP,
                                     final String probeName, final Integer expectedChecksCount) throws IOException;

    @Deployment(name = SERVICE_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_ONE + ".war")
                // no deployment health checks provided, we want for WildFly to automatically add readiness + startup checks here
                // see MicroProfile_Health.adoc
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    @Deployment(name = SERVICE_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_TWO + ".war")
                // we want the deployment to provide ready and startup checks here
                .addClasses(MyReadyProbe.class, SuccessfulStartupProbe.class)
                // but we disable all the server default procedures too
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    @ContainerResource
    ManagementClient managementClient;

    @Test
    public void testDisabledDefaultProcedures() throws IOException {
        // 1. ready-service-one.war (automatically added)
        // 2. started-service-one.war (automatically added)
        // 3. MyReadyProbe (added by the service-two deployment)
        // 4. SuccessfulStartupCheck (added by the service-two deployment)
        checkGlobalOutcome(managementClient, "check", true, "myReadyProbe", 4);
    }
}
