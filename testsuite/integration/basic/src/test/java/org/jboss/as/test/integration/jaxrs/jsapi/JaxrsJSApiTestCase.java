/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
                        "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
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
