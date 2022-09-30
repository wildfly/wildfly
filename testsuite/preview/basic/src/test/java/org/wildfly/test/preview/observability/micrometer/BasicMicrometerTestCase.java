package org.wildfly.test.preview.observability.micrometer;

import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.util.JacksonFeature;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BasicMicrometerTestCase {
    @ArquillianResource
    private URL url;
    private Client client = ClientBuilder.newClient().register(JacksonFeature.class);
    private ManagementClient managementClient;

    private static final String WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "\n"
                    + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
                    + "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\" version=\"4.0\">\n"
                    + "    <servlet-mapping>\n"
                    + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                    + "        <url-pattern>/*</url-pattern>\n"
                    + "    </servlet-mapping>"
                    + "</web-app>\n";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, BasicMicrometerTestCase.class.getSimpleName() + ".war")
                .addClasses(ServerSetupTask.class, MetricResource.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Spot check some metrics as a sanity check
     *
     * @throws URISyntaxException
     */
    @Test
    @RunAsClient
    public void checkSystemMetrics() throws URISyntaxException {
        List<String> metrics = fetchMicrometerMetrics();
        Set<String> meterNames = metrics.stream()
                .map(m -> m.replaceAll("\\{.*", ""))
                .map(m -> m.replaceAll(" .*", ""))
                .collect(Collectors.toSet());

        meterNames.forEach(m -> System.out.println("\n\n\n\nMeter name: " + m));
        // Check on meter from each of the preconfigured MeterBinders to help catch unintended changes
        Arrays.asList(
                "memory_used_heap_bytes",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time_seconds_total",
                "thread_count",
                "undertow_bytes_received_bytes_total"
        ).forEach(m -> assertThat("Meter '" + m + "' not found.", meterNames.contains(m)));
    }

    @Test
    @RunAsClient
    public void customMetricTest() throws URISyntaxException {
        Assert.assertEquals(200, client.target(url.toURI())
                .request()
                .get()
                .getStatus());

        assertThat(fetchMicrometerMetrics().size(), CoreMatchers.not(0));
    }

    private List<String> fetchMicrometerMetrics() throws URISyntaxException {
        Response response = client.target(getMicrometerUrl()).request().get();
        List<String> metrics = Arrays.stream(response.readEntity(String.class).split("\n"))
                .collect(Collectors.toList());
        return metrics;
    }

    private URI getMicrometerUrl() throws URISyntaxException {
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http");

        return new URI(managementClient.getMgmtProtocol() + "://" +
                managementClient.getMgmtAddress() + ":" +
                managementClient.getMgmtPort() +
                "/metrics");
    }
}
