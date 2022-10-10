package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecurityPermission;

import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.opentelemetry.nocdi.NoCdiClient;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
public class OpenTelemetryNoCdiTestCase {
    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, BasicOpenTelemetryTestCase.class.getSimpleName() + ".war")
                .addClasses(OpenTelemetrySetupTask.class, ServerSetupTask.class)
                .addPackage(NoCdiClient.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Required for the client to connect
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" +
                                TestSuiteEnvironment.getHttpPort(), "connect,resolve"),
                        new SecurityPermission("insertProvider"),
                        new MBeanServerPermission("createMBeanServer"),
                        new MBeanPermission("*", "registerMBean, unregisterMBean, invoke"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("modifyThread"),
                        new RuntimePermission("setContextClassLoader")
                ), "permissions.xml");
    }

    @Test
    @RunAsClient
    public void testEndpoint() {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            URI uri = UriBuilder.fromUri(url.toURI()).path("rest/client/json").build();
            System.out.println(uri.toASCIIString());
            Response response = client.target(uri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();
            Assert.assertEquals(200, response.getStatus());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            if (client != null) client.close();
        }
    }
}
