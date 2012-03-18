package org.jboss.as.test.integration.jsf.jta.validator;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jsf.jta.JTATestsBase;
import org.jboss.as.test.integration.jsf.jta.SimpleLogin;
import org.jboss.as.test.integration.jsf.jta.phase.PhaseListenerTestCase;
import org.jboss.jsfunit.api.InitialPage;
import org.jboss.jsfunit.jsfsession.JSFClientSession;
import org.jboss.jsfunit.jsfsession.JSFServerSession;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ValidatorTestCase extends JTATestsBase {

    private static final String DEPLOYMENT_PHASE_CONTEXT = "jsf-jta-validator";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_PHASE_CONTEXT + ".war";

    // ----------------- DEPLOYMENTS ------------

    @Deployment
    @OverProtocol("Servlet 3.0")
    public static Archive<WebArchive> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addPackage(ValidatorTestCase.class.getPackage())
                .addClasses(SimpleLogin.class, JTATestsBase.class)
                .addAsWebInfResources(ValidatorTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(ValidatorTestCase.class.getPackage(), "faces-config.xml", "faces-config.xml")
                .addAsWebResource(ValidatorTestCase.class.getPackage(), "index.xhtml", "index.xhtml");
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