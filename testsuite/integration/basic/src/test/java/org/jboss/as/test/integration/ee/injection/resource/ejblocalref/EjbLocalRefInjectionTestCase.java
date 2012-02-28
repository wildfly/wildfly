/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbLocalRefInjectionTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(EjbLocalRefInjectionTestCase.class, EjbLocalRefInjectionServlet.class, NamedSLSB.class, SimpleSLSB.class, Hello.class);
        war.addAsWebInfResource(getWebXml(), "web.xml");
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testLookup() throws Exception {
        String result = performCall("ejbLocalRef?type=simple");
        assertEquals("Simple Hello", result);
    }

    @Test
    public void testEjbLink() throws Exception {
        String result = performCall("ejbLocalRef?type=named");
        assertEquals("Named Hello", result);
    }

    @Test
    public void testNoInjectionPoint() throws Exception {
        Hello bean = (Hello) new InitialContext().lookup("java:comp/env/noInjection");
        assertEquals("Simple Hello", bean.sayHello());
    }


    private static StringAsset getWebXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<web-app version=\"3.0\"\n" +
                "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
                "         metadata-complete=\"false\">\n" +
                "\n" +
                "    <ejb-local-ref>\n" +
                "        <ejb-ref-name>simple</ejb-ref-name>\n" +
                "        <lookup-name>java:module/SimpleSLSB</lookup-name>\n" +
                "        <injection-target>" +
                "           <injection-target-class>" + EjbLocalRefInjectionServlet.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>simple</injection-target-name>" +
                "        </injection-target>\n" +
                "    </ejb-local-ref>\n" +
                "    <ejb-local-ref>\n" +
                "        <ejb-ref-name>noInjection</ejb-ref-name>\n" +
                "        <lookup-name>java:module/SimpleSLSB</lookup-name>\n" +
                "        <local>" + SimpleSLSB.class.getName() + "</local>" +
                "    </ejb-local-ref>\n" +
                "    <ejb-local-ref>\n" +
                "        <ejb-ref-name>named</ejb-ref-name>\n" +
                "        <ejb-link>namedBean</ejb-link>\n" +
                "        <injection-target>" +
                "           <injection-target-class>" + EjbLocalRefInjectionServlet.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>named</injection-target-name>" +
                "        </injection-target>\n" +
                "    </ejb-local-ref>\n" +
                "\n" +
                "</web-app>");
    }
}
