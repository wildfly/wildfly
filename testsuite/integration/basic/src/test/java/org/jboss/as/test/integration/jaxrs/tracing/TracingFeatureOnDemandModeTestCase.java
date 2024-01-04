/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.tracing.api.RESTEasyTracing;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(TracingFeatureOnDemandModeTestCase.TracingSetupServerSetupTask.class)
public class TracingFeatureOnDemandModeTestCase {

    public static class TracingSetupServerSetupTask extends JaxrsSubsystemServerSetupTask {
        @Override
        protected Map<String, Object> subsystemAttributes() {
            return Map.of("tracing-type", "ON_DEMAND");
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "tracing.war")
                .addClasses(TracingConfigResource.class, TracingApp.class);
    }

    @ArquillianResource
    private URL url;

    private jakarta.ws.rs.core.Response performCall(Client client, String urlPattern) {
        return client.target(url + urlPattern).request().header(RESTEasyTracing.HEADER_ACCEPT, "").
                header(RESTEasyTracing.HEADER_THRESHOLD, ResteasyContextParameters.RESTEASY_TRACING_LEVEL_VERBOSE)
                .get();
    }

    @Test
    public void testTracingConfig() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            String result2 = performCall(client, "type").readEntity(String.class);
            assertEquals("ON_DEMAND", result2);
            String result3 = performCall(client, "logger").getHeaderString(RESTEasyTracing.HEADER_TRACING_PREFIX + "001");
            assertNotNull(result3);
        }
    }
}
