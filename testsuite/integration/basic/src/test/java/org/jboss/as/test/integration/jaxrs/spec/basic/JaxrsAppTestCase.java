/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

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
