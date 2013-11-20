package org.jboss.as.test.integration.ws.context.application;

import java.net.URL;

import org.junit.Assert;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Base class for context.application tests. Unit test checks if context-root parameter is honored regardles of deploy content.
 *
 * @author baranowb
 */
public class ContextRootTestBase {

    protected static final String TEST_PATH = "/test1/";

    @ArquillianResource
    URL baseUrl;

    protected static WebArchive createWAR(final Class<?> beanClass, final String warDeploymentUnitName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, warDeploymentUnitName);

        war.addClass(beanClass);

        war.addAsWebResource(ContextRootTestBase.class.getPackage(), "index.html", "index.html");

        war.addAsWebInfResource(ContextRootTestBase.class.getPackage(), "beans.xml", "beans.xml");
        war.addAsWebInfResource(ContextRootTestBase.class.getPackage(), "faces-config.xml", "faces-config.xml");
        return war;
    }

    @Test
    public void testContextRoot() {
        Assert.assertEquals("Wrong context root!", TEST_PATH, baseUrl.getPath());
    }

}
