/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jsapi;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the resteasy JavaScript API
 *
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsJSApiTestCase {

    private static final String depName = "jsapi";

    @Deployment(name = depName)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addPackage(JaxrsJSApiTestCase.class.getPackage());
        war.addAsWebInfResource(WebXml.get(
                "<servlet-mapping>\n" +
                        "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
                        "        <url-pattern>/myjaxrs/*</url-pattern>\n" +
                        "</servlet-mapping>\n" +
                        "\n" +
                        "<servlet>\n" +
                        "        <servlet-name>RESTEasy JSAPI</servlet-name>\n" +
                        "        <servlet-class>org.jboss.resteasy.jsapi.JSAPIServlet</servlet-class>\n" +
                        "</servlet>\n" +
                        "\n" +
                        "<servlet-mapping>" +
                        "        <servlet-name>RESTEasy JSAPI</servlet-name>\n" +
                        "        <url-pattern>/rest-JS</url-pattern>\n" +
                        "</servlet-mapping>\n" +
                        "\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    @OperateOnDeployment(depName)
    static URL url;

    private static String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url.toString() + urlPattern, 5, TimeUnit.SECONDS);
    }

    @Test
    @InSequence(1)
    public void testJaxRsWithNoApplication() throws Exception {
        String result = performCall("myjaxrs/jsapi");
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><customer><first>John</first><last>Citizen</last></customer>", result);
    }

    @Test
    @InSequence(2)
    public void testJaxRsJSApis() throws Exception {
        String result = performCall("/rest-JS");
        Assert.assertTrue(result.contains("var CustomerResource"));
    }

}
