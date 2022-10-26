package org.jboss.as.test.integration.jaxrs.tracing;

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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


@RunWith(Arquillian.class)
@RunAsClient
public class TracingFeatureDefaultModeTestCase {
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "tracing.war");
        war.addClasses(TracingConfigResource.class, TracingApp.class);
        war.addAsWebInfResource(WebXml.get(
                "    <context-param>\n" +
                        "        <param-name>resteasy.server.tracing.type</param-name>\n" +
                        "        <param-value>ALL</param-value>\n" +
                        "    </context-param>\n" +
                        "    <context-param>\n" +
                        "        <param-name>resteasy.server.tracing.threshold</param-name>\n" +
                        "        <param-value>VERBOSE</param-value>\n" +
                        "    </context-param>\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private String performCall(Client client, String urlPattern) throws Exception {
        return client.target(url + urlPattern).request().get().readEntity(String.class);
    }

    @Test
    public void testTracingConfig() throws Exception {
        final Client client = ClientBuilder.newClient();
        try {
            String result = performCall(client, "level");
            assertEquals("VERBOSE", result);
            String result2 = performCall(client, "type");
            assertEquals("ALL", result2);
            String result3 = performCall(client, "logger");
            assertNotEquals("", result3);
        } catch (Exception e) {
            client.close();
        }
    }
}
