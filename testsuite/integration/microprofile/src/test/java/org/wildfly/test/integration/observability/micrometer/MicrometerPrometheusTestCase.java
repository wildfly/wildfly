package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.test.integration.observability.setuptask.AbstractSetupTask.executeOp;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
public class MicrometerPrometheusTestCase {
    private static final boolean dockerAvailable = AssumeTestGroupUtil.isDockerAvailable();
    private static final int REQUEST_COUNT = 5;
    @ArquillianResource
    private URL url;
    @ContainerResource
    protected ManagementClient managementClient;

    // The @ServerSetup(MicrometerSetupTask.class) requires Docker to be available.
    // Otherwise the org.wildfly.extension.micrometer.registry.NoOpRegistry is installed which will result in 0 counters,
    // and cause the test fail seemingly intermittently on machines with broken Docker setup.
    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    @Deployment
    public static Archive<?> deploy() {
        return dockerAvailable ?
                ShrinkWrap.create(WebArchive.class, "micrometer-prometheus.war")
                        .addClasses(MicrometerApplication.class,
                                MicrometerResource.class)
                        .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml") :
                AssumeTestGroupUtil.emptyWar();
    }

    @Test
    @RunAsClient
    public void test() throws URISyntaxException, InterruptedException, IOException {
        makeRequests();
        List<String> metricsToTest = Arrays.asList(
                "demo_counter",
                "demo_timer"
        );

        final String response = fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n, response.contains(n)));

        final String context = "/prometheus";
        final String promUrl = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + context;

        addPrometheusRegistry(context);
        checkCounter(promUrl, "demo_counter_total 0.0");
        makeRequests();
        checkCounter(promUrl, "demo_counter_total " + REQUEST_COUNT);
    }

    private static void checkCounter(String promUrl, String text) {
        try (Client client = ClientBuilder.newClient()) {
            Assert.assertTrue(client.target(promUrl).request().get()
                    .readEntity(String.class)
                    .contains(text));
        }
    }

    private void addPrometheusRegistry(String context) throws IOException {
        final ModelNode prometheusRegistryAddress = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "prometheus");
        ModelNode addOperation = Operations.createAddOperation(prometheusRegistryAddress);
        addOperation.get("context").set(context);
        executeOp(managementClient, addOperation);
    }

    private void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    private String fetchMetrics(String nameToMonitor) throws InterruptedException {
        String body = "";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(OpenTelemetryCollectorContainer.getInstance().getPrometheusUrl());

            int attemptCount = 0;
            boolean found = false;

            // Request counts can vary. Setting high to help ensure test stability
            while (!found && attemptCount < 30) {
                // Wait to give Micrometer time to export
                Thread.sleep(1000);

                body = target.request().get().readEntity(String.class);
                found = body.contains(nameToMonitor);
                attemptCount++;
            }
        }

        return body;
    }
}
