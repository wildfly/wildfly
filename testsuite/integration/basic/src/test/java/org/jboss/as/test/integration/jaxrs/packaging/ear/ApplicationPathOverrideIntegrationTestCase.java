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
 * Tests a JAX-RS deployment with an application bundled, that has an @ApplicationPath annotation.
 * <p/>
 * This annotation is overridden by a mapping in web.xml
 * <p/>
 * JAX-RS 1.1 2.3.2 bullet point 3
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ApplicationPathOverrideIntegrationTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxrsapp.ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addPackage(HttpRequest.class.getPackage());
        jar.addClasses(ApplicationPathOverrideIntegrationTestCase.class, HelloWorldResource.class, HelloWorldPathApplication.class);
        ear.addAsModule(jar);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsapp.war");
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>" + HelloWorldPathApplication.class.getName() + "</servlet-name>\n" +
                "        <url-pattern>/override/*</url-pattern>\n" +
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
        String result = performCall("override/helloworld");
        assertEquals("Hello World!", result);
    }


}
