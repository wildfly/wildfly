package org.jboss.as.test.integration.jsf.jta;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jsf.jta.login.SimpleLogin;
import org.jboss.as.test.integration.jsf.jta.login.validator.LoginValidator;
import org.jboss.jsfunit.api.InitialPage;
import org.jboss.jsfunit.jsfsession.JSFClientSession;
import org.jboss.jsfunit.jsfsession.JSFServerSession;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ValidatorTestCase extends JTATestsBase {

    private static final Logger log = Logger.getLogger(ValidatorTestCase.class.getName());
    private static final String DEPLOYMENT_PHASE_CONTEXT = "jsf-jta-validator";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_PHASE_CONTEXT + ".war";
    private static final String RESOURCES_LOCATION = "org/jboss/as/test/integration/jsf/jta/validator";

    // ----------------- DEPLOYMENTS ------------

    // @ArquillianResource
    // URL deploymentURL;

    @Deployment
    public static Archive<WebArchive> createDeployment() {

        // add test classes
        Class[] classes = new Class[] { SimpleLogin.class, ManagedBeanTestCase.class, LoginValidator.class };
        String[] resources = new String[] { "index.xhtml" };
        String[] webInfResources = new String[] { "web.xml", "faces-config.xml", "jboss-deployment-structure.xml" };

        final WebArchive archive = createArchive(DEPLOYMENT_NAME, classes, null, RESOURCES_LOCATION, resources, webInfResources);
        log.info(archive.toString(true));

        return archive;
    }

    @Test
    @InitialPage("/index.jsf")
    public void testPhaseListener(JSFServerSession server, JSFClientSession client) throws Exception {
        // NOTE: if I fail, check server log, JSFUnit masks real cause cause it seems to be last in chain
        // , so if assertFails in PhaseListener class, it will show '500' return status saying
        // 'cant inject parameters' - it happens even when JSFSession is created by hand.

        Assert.assertNotNull(server);
        Assert.assertNotNull(client);
        Assert.assertEquals("Wrong view ID!!!", "/index.xhtml", server.getCurrentViewID());

        // insert data

        client.setValue("login_name", "root");
        client.setValue("password", "password");
        client.click("login_button");

        Assert.assertEquals("Wrong view ID!!!", "/index.xhtml", server.getCurrentViewID());

    }
}