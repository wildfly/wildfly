/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
@RunAsClient
public class ServerSentEventsTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "sse.war");
        // war.addPackage(HttpRequest.class.getPackage());
        // war.addPackage(JaxrsAsyncTestCase.class.getPackage());
        war.addClasses(SseHandler.class, SimpleServlet.class);
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testServletStillWorks() throws Exception {
        final String response = HttpRequest.get(url.toExternalForm() + "simple", 20, TimeUnit.SECONDS);
        Assert.assertEquals(SimpleServlet.SIMPLE_SERVLET, response);
    }

    @Test
    public void testSSEConnection() throws Exception {
        final String response = HttpRequest.get(url.toExternalForm() + "foo/Stuart", 20, TimeUnit.SECONDS);
        Assert.assertEquals("data:Hello Stuart\n" +
                "\n" +
                "data:msg2\n" +
                "\n" +
                "data:msg3\n" +
                "\n", response);
    }
}
