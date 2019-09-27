package org.wildfly.test.integration.microprofile.metrics.application;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.application.resource.ResourceWithTags;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricValueFromPrometheusOutput;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getPrometheusMetrics;

@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsTagsTestCase {

    @ContainerResource
    ManagementClient managementClient;
    @ArquillianResource
    URL url;

    String greetings = this.getClass().getPackage().getName().replaceAll("\\.", "_")
            + "_resource_ResourceWithTags_greetings_total";
    String unknownGreetings = "UnknownGreetings_total";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileMetricsTagsTestCase.war")
                .addClasses(TestApplication.class, ResourceWithTags.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Check declarative approach for metrics tags
     */
    @Test
    public void testDeclarativeMetricsTags() throws Exception {
        performSingleCall(url, "greetings/hi");
        String metrics = getPrometheusMetrics(managementClient, "application", true);
        double counter = getMetricValueFromPrometheusOutput(metrics, "application", greetings + "{greeting=\"casual\"}");
        assertEquals(1.0, counter, 0.0);

        performSingleCall(url, "greetings/hi");
        metrics = getPrometheusMetrics(managementClient, "application", true);
        counter = getMetricValueFromPrometheusOutput(metrics, "application", greetings + "{greeting=\"casual\"}");
        assertEquals(2.0, counter, 0.0);

        performSingleCall(url, "greetings/hello");
        metrics = getPrometheusMetrics(managementClient, "application", true);
        counter = getMetricValueFromPrometheusOutput(metrics, "application", greetings + "{greeting=\"formal\"}");
        assertEquals(1.0, counter, 0.0);
    }

    /**
     * Check programmatic approach for metrics tags
     */
    @Test
    public void testProgrammaticMetricsTags() throws Exception {
        performSingleCall(url, "greetings/foo");
        String metrics = getPrometheusMetrics(managementClient, "application", true);
        double counter = getMetricValueFromPrometheusOutput(metrics, "application", unknownGreetings + "{greeting=\"foo\"}");
        assertEquals(1.0, counter, 0.0);

        performSingleCall(url, "greetings/foo");
        metrics = getPrometheusMetrics(managementClient, "application", true);
        counter = getMetricValueFromPrometheusOutput(metrics, "application", unknownGreetings + "{greeting=\"foo\"}");
        assertEquals(2.0, counter, 0.0);

        performSingleCall(url, "greetings/bar");
        metrics = getPrometheusMetrics(managementClient, "application", true);
        counter = getMetricValueFromPrometheusOutput(metrics, "application", unknownGreetings + "{greeting=\"bar\"}");
        assertEquals(1.0, counter, 0.0);
    }

    /**
     * Perform a call that should be analyzed by Metrics
     */
    private static String performSingleCall(URL url, String urlSuffix) throws Exception {
        URL appURL = new URL(url.toExternalForm() + "microprofile-metrics-app/" + urlSuffix);
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }

}