/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jackson;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.as.test.shared.SecurityManagerFailure;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the resteasy multipart provider
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsJacksonProviderTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addPackage(JaxrsJacksonProviderTestCase.class.getPackage());
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/myjaxrs/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @BeforeClass
    public static void beforeClass() {
        SecurityManagerFailure.thisTestIsFailingUnderSM("https://github.com/FasterXML/jackson-databind/pull/1585");
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testSimpleJacksonResource() throws Exception {
        String result = performCall("myjaxrs/jackson");
        Assert.assertEquals("{\"first\":\"John\",\"last\":\"Citizen\"}", result);
    }

    @Test
    public void testDurationJsr310() throws Exception {
        String result = performCall("myjaxrs/jackson/duration");
        Assert.assertEquals("\"PT1S\"", result);
    }

    @Test
    public void testDurationJsrjdk8() throws Exception {
        String result = performCall("myjaxrs/jackson/optional");
        Assert.assertEquals("\"optional string\"", result);
    }
    /**
     * AS7-1276
     */
    @Test
    public void testJacksonWithJsonIgnore() throws Exception {
        String result = performCall("myjaxrs/country");
        Assert.assertEquals("{\"name\":\"Australia\",\"temperature\":\"Hot\"}", result);
    }


}
