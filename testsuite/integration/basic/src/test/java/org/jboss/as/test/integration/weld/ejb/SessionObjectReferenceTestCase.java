/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.ejb.ConcurrentAccessException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.stdio.WriterOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test two things:
 * 1. EJBTHREE-697: First concurrent call doesn't throw exception
 * 2. make sure the SFSB is instantiated in the same thread as the Servlet, so propagation works
 * <p/>
 * Make sure a concurrent call to a SFSB proxy over Weld gets a ConcurrentAccessException.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SessionObjectReferenceTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war")
                .addClasses(HttpRequest.class, SimpleServlet.class, SimpleStatefulSessionBean.class, WriterOutputStream.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        return war;
    }

    private String performCall(String urlPattern, String param) {
        String spec = url.toExternalForm() + urlPattern + "?input=" + param;
        try {
            return HttpRequest.get(spec, 30, SECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEcho() throws Exception {
        String result = performCall("simple", "Hello+world");
        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
        List<String> results = (List<String>) decoder.readObject();
        List<Exception> exceptions = (List<Exception>) decoder.readObject();
        String sharedContext = (String) decoder.readObject();
        decoder.close();

        assertEquals(1, results.size());
        assertEquals("Echo Hello world", results.get(0));
        assertEquals(1, exceptions.size());
        assertTrue(exceptions.get(0) instanceof ConcurrentAccessException);
        assertEquals("Shared context", sharedContext);
    }

    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "<!--org.jboss.as.weld.deployment.processors.WebIntegrationProcessor checks for the existence of WebMetaData -->\n" +
            "<web-app version=\"3.0\" xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_0.xsd\">\n" +
            "    <!-- if I have a web.xml, annotations won't work anymore -->\n" +
            "    <servlet>\n" +
            "        <servlet-name>SimpleServlet</servlet-name>\n" +
            "        <servlet-class>org.jboss.as.test.integration.weld.ejb.SimpleServlet</servlet-class>\n" +
            "    </servlet>\n" +
            "    <servlet-mapping>\n" +
            "        <servlet-name>SimpleServlet</servlet-name>\n" +
            "        <url-pattern>/simple</url-pattern>\n" +
            "    </servlet-mapping>\n" +
            "</web-app>";
}
