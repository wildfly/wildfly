/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.packaging.ear;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests a JAX-RS deployment without an application bundled.
 * <p/>
 * The container should register a servlet with the name
 * <p/>
 * jakarta.ws.rs.core.Application
 * <p/>
 * It is the app providers responsibility to provide a mapping for the servlet
 * <p/>
 * JAX-RS 1.1 2.3.2 bullet point 1
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NoApplicationIntegrationTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxrsapp.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addPackage(HttpRequest.class.getPackage());
        jar.addClasses(NoApplicationIntegrationTestCase.class, HelloWorldResource.class);
        ear.addAsModule(jar);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/myjaxrs/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                    "\n"), "web.xml");

        ear.addAsModule(war);
        return ear;
    }

    @ArquillianResource
    private URL url;

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testJaxRsWithNoApplication() throws Exception {
        String result = performCall("myjaxrs/helloworld");
        assertEquals("Hello World!", result);
    }


}
