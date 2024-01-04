/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URL;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(TracingFeatureDefaultModeTestCase.TracingSetupServerSetupTask.class)
public class TracingFeatureDefaultModeTestCase {

    public static class TracingSetupServerSetupTask extends JaxrsSubsystemServerSetupTask {
        @Override
        protected Map<String, Object> subsystemAttributes() {
            return Map.of("tracing-type", "ALL", "tracing-threshold", "VERBOSE");
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "tracing.war")
                .addClasses(TracingConfigResource.class, TracingApp.class);
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
