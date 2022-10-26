package org.jboss.as.test.integration.jaxrs.tracing;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.tracing.api.RESTEasyTracing;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(Arquillian.class)
@RunAsClient
public class TracingFeatureOnDemandModeTestCase {
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "tracing.war");
        war.addClasses(TracingConfigResource.class, TracingApp.class);
        war.addAsWebInfResource(WebXml.get(
                "    <context-param>\n" +
                        "        <param-name>resteasy.server.tracing.type</param-name>\n" +
                        "        <param-value>ON_DEMAND</param-value>\n" +
                        "    </context-param>\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private jakarta.ws.rs.core.Response performCall(Client client, String urlPattern) throws Exception {
        return client.target(url + urlPattern).request().header(RESTEasyTracing.HEADER_ACCEPT, "").
                header(RESTEasyTracing.HEADER_THRESHOLD, ResteasyContextParameters.RESTEASY_TRACING_LEVEL_VERBOSE)
                .get();
    }

    @Test
    public void testTracingConfig() throws Exception {
        final Client client = ClientBuilder.newClient();
        try {
            String result2 = performCall(client, "type").readEntity(String.class);
            assertEquals("ON_DEMAND", result2);
            String result3 = performCall(client, "logger").getHeaderString(RESTEasyTracing.HEADER_TRACING_PREFIX + "001");
            assertNotNull(result3);
        } catch (Exception e) {
            client.close();
        }
    }
}
