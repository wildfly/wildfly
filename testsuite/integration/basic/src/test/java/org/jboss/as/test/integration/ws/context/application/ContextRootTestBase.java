package org.jboss.as.test.integration.ws.context.application;

import java.net.URL;

import junit.framework.Assert;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Base class for context.application tests. Unit test checks if context-root parameter is honored regardles of deploy content.
 * 
 * @author baranowb
 * 
 */
public class ContextRootTestBase {

    protected static final String WAR_DEPLOYMENT_NAME = "ws-notannotated";
    protected static final String WAR_DEPLOYMENT_UNIT_NAME = WAR_DEPLOYMENT_NAME + "-XXX.war";
    protected static final String EAR_DEPLOYMENT_NAME = "ws-notannotated";
    protected static final String EAR_DEPLOYMENT_UNIT_NAME = EAR_DEPLOYMENT_NAME + "-XXX.ear";
    protected static final String DEPLOYMENT_RESOURCES = "org/jboss/as/test/integration/ws/context/application/notannotated";

    protected static final String TEST_PATH = "/test1/";

    @ArquillianResource
    URL baseUrl;

    protected static WebArchive createWAR(Class beanClass) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_UNIT_NAME);

        war.addClass(beanClass);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        // war.addAsWebResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/application.xml"), "application.xml");
        war.addAsWebResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/index.xhtml"), "index.xhtml");

        war.addAsWebInfResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/WEB-INF/templates"), "templates");
        war.addAsWebInfResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/WEB-INF/beans.xml"), "beans.xml");
        war.addAsWebInfResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/WEB-INF/faces-config.xml"), "faces-config.xml");
        return war;
    }

    @Test
    public void testContextRoot() {
        Assert.assertEquals("Wrong context root!", TEST_PATH, baseUrl.getPath());
    }

}
