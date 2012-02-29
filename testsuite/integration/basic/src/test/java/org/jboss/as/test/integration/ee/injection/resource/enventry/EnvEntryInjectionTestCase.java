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
package org.jboss.as.test.integration.ee.injection.resource.enventry;

import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EnvEntryInjectionTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(
                EnvEntryInjectionServlet.class,
                EnvEntryManagedBean.class,
                EnvEntryInjectionTestCase.class
                );
        war.addAsWebInfResource(getWebXml(), "web.xml");
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get("http://" + managementClient.getMgmtAddress() + ":8080/war-example/" + urlPattern, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testEnvEntryInjectionIntoServlet() throws Exception {
        String result = performCall("envEntry");
        assertEquals("injection!", result);
    }

    @Test
    public void testEnvEntryInjectionIntoManagedBean() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assert.assertEquals("bye", bean.getExistingString());
    }

    @Test
    public void testEnvEntryNonExistantManagedBean() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assert.assertEquals("hi", bean.getNonExistantString());
    }

    @Test(expected = NamingException.class)
    public void testNonExistantResourceCannotBeLookedUp() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        initialContext.lookup("java:module/env/" + EnvEntryManagedBean.class.getName() + "/nonExistantString");
    }

    @Test
    public void testBoxedUnboxedMismatch() throws NamingException {
        final InitialContext initialContext = new InitialContext();
        final EnvEntryManagedBean bean = (EnvEntryManagedBean) initialContext.lookup("java:module/" + EnvEntryManagedBean.class.getName());
        Assert.assertEquals(10, bean.getByteField());
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
                "    <env-entry>\n" +
                "        <env-entry-name>foo</env-entry-name>\n" +
                "        <env-entry-value>injection!</env-entry-value>\n" +
                "        <injection-target>" +
                "           <injection-target-class>" + EnvEntryInjectionServlet.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>field</injection-target-name>" +
                "        </injection-target>\n" +
                "    </env-entry>\n" +
                "\n" +
                "    <env-entry>\n" +
                "        <env-entry-name>" + EnvEntryManagedBean.class.getName() + "/existingString</env-entry-name>\n" +
                "        <env-entry-value>bye</env-entry-value>\n" +
                "        <env-entry-type>java.lang.String</env-entry-type>\n" +
                "    </env-entry>\n" +
                "\n" +
                "\n" +
                "    <env-entry>\n" +
                "        <env-entry-name>otherByte</env-entry-name>\n" +
                "        <env-entry-value>10</env-entry-value>\n" +
                "        <env-entry-type>java.lang.Byte</env-entry-type>\n" +
                "        <injection-target>" +
                "           <injection-target-class>" + EnvEntryManagedBean.class.getName() + "</injection-target-class>" +
                "           <injection-target-name>byteField</injection-target-name>" +
                "        </injection-target>\n" +
                "    </env-entry>\n" +
                "\n" +
                "</web-app>");
    }
}
