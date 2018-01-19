/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
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
