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
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppResourceTwo;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppTwo;
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
public class JaxrsAppTwoTestCase {
    @ArquillianResource
    URL baseUrl;

    private static final String CONTENT_ERROR_MESSAGE = "Wrong content of response";

    @Deployment
    public static Archive<?> deploySimpleResource() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, JaxrsAppTwoTestCase.class.getSimpleName() + ".war");
        war.addAsWebInfResource(JaxrsAppTwoTestCase.class.getPackage(), "JaxrsAppTwoWeb.xml", "web.xml");
        war.addClasses(JaxrsAppResource.class, JaxrsAppResourceTwo.class,
            JaxrsAppTwo.class);
        return war;
    }

    /**
     * The jaxrs 2.0 spec says that when a Application subclass returns
     * values for getClasses or getSingletons methods the returned classes
     * should be the only ones used.
     * This test confirms that returning a value for getClasses is
     * handled properly by the server.
     */
    @Test
    public void testDemo() throws Exception {
        // check the returned resource class is available
        Client client = ClientBuilder.newClient();
        try {
            String url = baseUrl.toString() + "resources";
            WebTarget base = client.target(url);
            String value = base.path("example").request().get(String.class);
            Assert.assertEquals(CONTENT_ERROR_MESSAGE, "Hello world!", value);
        } finally {
            client.close();
        }

        // check the undeclared resource class is NOT available
        client = ClientBuilder.newClient();
        try {
            String url = baseUrl.toString() + "resources";
            WebTarget base = client.target(url);
            Response r = base.path("exampleTwo").request().get();
            Assert.assertEquals("404 error not received", 404, r.getStatus());
        } finally {
            client.close();
        }
    }
}