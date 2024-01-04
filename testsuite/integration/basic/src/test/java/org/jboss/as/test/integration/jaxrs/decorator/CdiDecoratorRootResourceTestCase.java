/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.decorator;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.Application;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests a Jakarta RESTful Web Services deployment with an application bundled, that has no @ApplicationPath annotation.
 * <p/>
 * The container should register a servlet with the name that matches the application name
 * <p/>
 * It is the app providers responsibility to provide a mapping for the servlet
 * <p/>
 * JAX-RS 1.1 2.3.2 bullet point 3
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CdiDecoratorRootResourceTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsapp.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addPackage(CdiDecoratorRootResourceTestCase.class.getPackage());
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>" + Application.class.getName() + "</servlet-name>\n" +
                "        <url-pattern>/rest/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"><decorators><class>" + ResourceDecorator.class.getName() + "</class></decorators></beans>"), "beans.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testJaxRsWithDecoratedResource() throws Exception {
        String result = performCall("rest/decorator");
        assertEquals("DECORATED Hello World!", result);
    }

}
