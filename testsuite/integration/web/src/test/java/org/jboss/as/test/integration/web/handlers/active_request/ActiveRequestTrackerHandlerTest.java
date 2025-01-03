package org.jboss.as.test.integration.web.handlers.active_request;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ActiveRequestTrackerHandlerTest.ActiveRequestTrackingSetupTask.class)
public class ActiveRequestTrackerHandlerTest {

    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "active-request-tracking.war")
                .addClasses(DelayServlet.class);
    }

    @ArquillianResource
    protected URL url;

    @Test
    public void testActiveRequest() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                    TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");


            new Thread(() -> makeRequest(httpClient)).start();
            // This test is somewhat time-sensitive in that an in-flight request is needed to verify that the handler
            // is correctly tracking and returning active requests. To that end, the test submits a request in a
            // Thread (above) to let the request be made in a background thread while the main thread attempts to
            // get the active request information. There is a short sleep here to force/encourage the JVM to allow the
            // background thread to start to initiate the request. That thread has a short delay in it as well (see
            // servlet) to give this thread time to retrieve the data.
            Thread.sleep(500);

            ModelNode response = managementClient.getControllerClient().execute(
                    Operations.createOperation("list-active-requests",
                            Operations.createAddress(
                                    "subsystem", "undertow",
                                    "server", "default-server",
                                    "host", "default-host")
            ));

            assertEquals(new ModelNode("success"), response.get("outcome"));
            ModelNode result = response.get("result");
            assertEquals(ModelType.LIST, result.getType());
            assertEquals(1, result.asList().size());
        }
    }

    private void makeRequest(CloseableHttpClient httpClient) {
        try {
            httpClient.execute(new HttpGet(url.toExternalForm() + "test"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ActiveRequestTrackingSetupTask implements ServerSetupTask {
        public static final ModelNode subsystemAddress = Operations.createAddress(
                "subsystem", "undertow");
        public static final ModelNode hostAddress = Operations.createAddress(
                "subsystem", "undertow",
                "server", "default-server",
                "host", "default-host");

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            AssumeTestGroupUtil.assumeWildFlyPreview();
            // /subsystem=undertow/server=default-server/host=default-host:write-attribute(name="active-request-tracking-enabled", value="true")
            execute(managementClient,
                    Operations.createWriteAttributeOperation(subsystemAddress, "statistics-enabled", true),
                    true);
            execute(managementClient,
                    Operations.createWriteAttributeOperation(hostAddress, "active-request-tracking-enabled", true),
                    true);
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            execute(managementClient,
                    Operations.createWriteAttributeOperation(subsystemAddress, "statistics-enabled", false),
                    true);
            execute(managementClient,
                    Operations.createWriteAttributeOperation(hostAddress, "active-request-tracking-enabled", false),
                    true);
            ServerReload.reloadIfRequired(managementClient);
        }

        private ModelNode execute(final ManagementClient managementClient,
                                  final ModelNode op,
                                  final boolean expectSuccess) throws IOException {
            ModelNode response = managementClient.getControllerClient().execute(op);
            final String outcome = response.get("outcome").asString();
            if (expectSuccess) {
                assertEquals(response.toString(), "success", outcome);
                return response.get("result");
            } else {
                assertEquals("failed", outcome);
                return response.get("failure-description");
            }
        }
    }

}
