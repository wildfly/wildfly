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
package org.jboss.as.test.integration.ws.cdi;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1429
 * <p/>
 * Tests that adding a web service does not break CDI + JSF
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CdiJsfWebServicesTestCase {

    @ArquillianResource
    URL baseUrl;

    public static final String ARCHIVE_NAME = "testCdiJsfWebServices";

    @Deployment
    public static Archive<?> archive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(CdiJsfWebServicesTestCase.class.getPackage());
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.add(new StringAsset("<html><body>#{myBean.message}</body></html>"), "index.xhtml");
        war.addAsWebInfResource(new StringAsset(FACES_CONFIG), "faces-config.xml");
        return war;

    }


    @Test
    public void testWebServicesDoNotBreakCDI() throws IOException, ExecutionException, TimeoutException {
        URL webRoot = new URL(baseUrl, "/");
        Assert.assertEquals("<html><body>" + MyBean.MESSAGE + "</body></html>", HttpRequest.get(webRoot.toString() + ARCHIVE_NAME + "/index.jsf", 20, TimeUnit.SECONDS));
    }


    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "<web-app version=\"3.0\" xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\">\n" +
            "  <servlet>\n" +
            "        <servlet-name>Faces Servlet</servlet-name>\n" +
            "      <servlet-class>javax.faces.webapp.FacesServlet</servlet-class>\n" +
            "      <load-on-startup>1</load-on-startup>\n" +
            "  </servlet>\n" +
            "  <servlet-mapping>\n" +
            "       <servlet-name>Faces Servlet</servlet-name>\n" +
            "       <url-pattern>*.jsf</url-pattern>\n" +
            "  </servlet-mapping>\n" +
            "</web-app>";

    public static final String FACES_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!-- This file is not required if you don't need any extra configuration. -->\n" +
            "<faces-config version=\"2.0\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "   xsi:schemaLocation=\"\n" +
            "        http://java.sun.com/xml/ns/javaee\n" +
            "        http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\">\n" +
            "</faces-config>";
}
