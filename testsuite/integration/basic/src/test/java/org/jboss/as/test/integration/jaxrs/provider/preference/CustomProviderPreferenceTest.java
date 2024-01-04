/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider.preference;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests that user-provided providers are given priority over built-in providers.
 *
 * AS7-1400
 *
 * @author Jozef Hartinger
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomProviderPreferenceTest {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "providers.war");
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        war.addClasses(CustomProviderPreferenceTest.class, CustomMessageBodyWriter.class, Resource.class);
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n"
                + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                + "        <url-pattern>/api/*</url-pattern>\n" + "    </servlet-mapping>\n" + "\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testCustomMessageBodyWriterIsUsed() throws Exception {
        assertEquals("true", performCall("api/user"));
    }
}
