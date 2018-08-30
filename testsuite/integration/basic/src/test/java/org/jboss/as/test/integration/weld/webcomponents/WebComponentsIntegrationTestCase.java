package org.jboss.as.test.integration.weld.webcomponents;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the integration of CDI and optional web components
 *
 * @author Kudrevatykh Alexander
 */
@RunWith(Arquillian.class)
public class WebComponentsIntegrationTestCase {

    @Deployment(name = "war", managed = false, testable = false)
    public static Archive<?> war() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "WebComponentsIntegrationTestCase.war");
        war.addAsLibraries(ShrinkWrap.create(JavaArchive.class, "module.jar").
                addClass(StandardServletAsyncWebRequest.class)
        );
        return war;
    }

    @Deployment(name = "ear", managed = false, testable = false)
    public static Archive<?> ear() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "WebComponentsIntegrationTestCase.ear");
        ear.addAsModule(war());
        ear.addAsModule(ejb());
        return ear;
    }

    @Deployment(name = "ejb", managed = false, testable = false)
    public static Archive<?> ejb() {
        return ShrinkWrap.create(JavaArchive.class, "ejb.jar").
                addClass(EjbService.class);
    }

    @ArquillianResource
    private Deployer deployer;

    @Test
    @RunAsClient
    public void testWar() {
        deployer.deploy("war");
        deployer.undeploy("war");
    }

    @Test
    @RunAsClient
    public void testEar() {
        deployer.deploy("ear");
        deployer.undeploy("ear");
    }

    @Test
    @RunAsClient
    public void testEjb() {
        deployer.deploy("ejb");
        deployer.undeploy("ejb");
    }

}
