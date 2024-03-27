package org.wildfly.test.integration.observability.micrometer;

import java.net.URISyntaxException;
import java.net.URL;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.util.JacksonFeature;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask;

@RunWith(Arquillian.class)
@ServerSetup({MicrometerSetupTask.class, PrometheusSetupTask.class})
public class PrometheusExporterTestCase {
    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        return AssumeTestGroupUtil.isDockerAvailable() ?
                ShrinkWrap.create(WebArchive.class, PrometheusExporterTestCase.class.getName() + ".war")
                        .addClasses(ServerSetupTask.class,
                                RestActivator.class,
                                MetricResource.class,
                                AssumeTestGroupUtil.class)
                        .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml") :
                AssumeTestGroupUtil.emptyWar();
    }

    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    @Test
    @RunAsClient
    public void foo() throws URISyntaxException {

        try (Client client = getClient()) {
            String address = "http://" + managementClient.getMgmtAddress() +
                    ":" + managementClient.getMgmtPort() +
                    "/" + PrometheusSetupTask.CONTEXT_PROMETHEUS;
            System.err.println("Address = " + address);
            WebTarget target = client.target(address);
            Response response = target.request().get();
            Assert.assertEquals(200, response.getStatus());
            // Prometheus output will have something like this:
            // undertow_highest_session_count{app="org.wildfly.test.integration.observability.micrometer.PrometheusExporterTestCase.war",
            //   deployment="org.wildfly.test.integration.observability.micrometer.PrometheusExporterTestCase.war",
            //   subdeployment="org.wildfly.test.integration.observability.micrometer.PrometheusExporterTestCase.war",} 0.0
            Assert.assertTrue(response.readEntity(String.class).contains(getClass().getCanonicalName()));
        }
    }

    private Client getClient() {
        return ClientBuilder.newClient().register(JacksonFeature.class);
    }
}
