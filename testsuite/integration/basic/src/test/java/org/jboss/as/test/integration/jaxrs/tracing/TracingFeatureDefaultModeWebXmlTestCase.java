/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jaxrs.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class TracingFeatureDefaultModeWebXmlTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "tracing.war")
                .addClasses(TracingConfigResource.class, TracingApp.class)
                .addAsWebInfResource(WebXml.get(
                        "    <context-param>\n" +
                                "        <param-name>resteasy.server.tracing.type</param-name>\n" +
                                "        <param-value>ALL</param-value>\n" +
                                "    </context-param>\n" +
                                "    <context-param>\n" +
                                "        <param-name>resteasy.server.tracing.threshold</param-name>\n" +
                                "        <param-value>VERBOSE</param-value>\n" +
                                "    </context-param>\n"), "web.xml");
    }

    @ArquillianResource
    private URL url;

    private String performCall(Client client, String urlPattern) throws Exception {
        return client.target(url + urlPattern).request().get().readEntity(String.class);
    }

    @Test
    public void testTracingConfig() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            String result = performCall(client, "level");
            assertEquals("VERBOSE", result);
            String result2 = performCall(client, "type");
            assertEquals("ALL", result2);
            String result3 = performCall(client, "logger");
            assertNotEquals("", result3);
        }
    }
}
