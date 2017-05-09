/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxrs.spec.basic;

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsApp;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsAppTestCase {
    @ArquillianResource
    URL baseUrl;

    private static final String CONTENT_ERROR_MESSAGE = "Wrong content of response";

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, JaxrsAppTestCase.class.getSimpleName() + ".war");
        war.addAsWebInfResource(JaxrsAppTestCase.class.getPackage(), "JaxrsAppWeb.xml", "web.xml");
        war.addClasses(JaxrsAppResource.class,
            JaxrsApp.class);
        return war;
    }


    /**
     * The jaxrs 2.0 spec says that when a Application subclass returns
     * empty collections for getClasses and getSingletons methods the
     * resource and provider classes should be dynamically found.
     * This test shows that the server deployment processing code performs
     * the required scanning.
     */
    @Test
    public void testDemo() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            String url = baseUrl.toString() + "resources";
            WebTarget base = client.target(url);
            String value = base.path("example").request().get(String.class);
            Assert.assertEquals(CONTENT_ERROR_MESSAGE, "Hello world!", value);
        } finally {
            client.close();
        }
    }
}
