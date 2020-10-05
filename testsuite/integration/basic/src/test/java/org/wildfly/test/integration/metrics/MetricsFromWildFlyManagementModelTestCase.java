package org.wildfly.test.integration.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.ServerReload.reloadIfRequired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.wildfly.test.integration.metrics.MetricsHelper.getPrometheusMetrics;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.metrics.application.TestApplication;
import org.wildfly.test.integration.metrics.application.TestResource;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MetricsFromWildFlyManagementModelTestCase.EnablesUndertowStatistics.class})
public class MetricsFromWildFlyManagementModelTestCase {

    static class EnablesUndertowStatistics implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(true));
            reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(false));
            reloadIfRequired(managementClient);
        }

        private ModelNode enableStatistics(boolean enabled) {
            final ModelNode address = Operations.createAddress(SUBSYSTEM, "undertow");
            return Operations.createWriteAttributeOperation(address, STATISTICS_ENABLED, enabled);
        }
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "MetricsFromWildFlyManagementModelTestCase", managed = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MetricsFromWildFlyManagementModelTestCase.war")
                .addClasses(TestApplication.class)
                .addClass(TestResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Test
    @InSequence(1)
    public void testMetricsBeforeDeployment() throws Exception {
        // the request-count from the deployment must not exist
        checkMetricExistence( "deployment=\"MetricsFromWildFlyManagementModelTestCase.war\"", false);

        // test the request-count metric on the deployment's undertow resources
        checkRequestCount(0, false);

        // deploy the archive
        deployer.deploy("MetricsFromWildFlyManagementModelTestCase");

    }
    @Test
    @InSequence(2)
    @OperateOnDeployment("MetricsFromWildFlyManagementModelTestCase")
    public void testAffterDeployment(@ArquillianResource URL url) throws Exception {
        // test the request-count metric on the deployment's undertow resources
        checkRequestCount(0, true);
        performCall(url);
        performCall(url);
        performCall(url);
        checkRequestCount(3, true);

        // the request-count in the http-listner will have the same value
        checkRequestCount(3, false);

    }

    @Test
    @InSequence(3)
    public void testMetricsAfterUndeployment() throws Exception {
        deployer.undeploy("MetricsFromWildFlyManagementModelTestCase");

        // the request-count in the http-listener will still be present after the undeployment
        checkRequestCount(3, false);

        // the request-count from the deployment must no longer exist
        checkMetricExistence( "deployment=\"MetricsFromWildFlyManagementModelTestCase.war\"", false);
    }

    private static String performCall(URL url) throws Exception {
        URL appURL = new URL(url.toExternalForm() + "metrics-app/hello");
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    private void checkMetricExistence(String label, boolean metricMustExist) throws IOException {
        String metricName = "wildfly_undertow_request_count_total";
        String metrics = getPrometheusMetrics(managementClient, true);
        for (String line : metrics.split("\\R")) {
            if (line.startsWith(metricName)) {
                String[] split = line.split("\\s+");
                String labels = split[0].substring((metricName).length());
                if (labels.contains(label)) {
                    if (metricMustExist) {
                        return;
                    } else {
                        fail("Metric " + metricName + " was found");
                    }
                }
            }
        }
        if (metricMustExist) {
            fail("Metric " + metricName + " was not found");
        }
    }

    private void checkRequestCount(int expectedCount, boolean metricForDeployment) throws IOException {
        String metricName = "wildfly_undertow_request_count_total";
        String metrics = getPrometheusMetrics(managementClient, true);
        System.out.println(">>> metrics = " + metrics);
        for (String line : metrics.split("\\R")) {
            if (line.startsWith(metricName)) {
                String[] split = line.split("\\s+");
                String labels = split[0].substring((metricName).length());

                // we are only interested by the metric for this deployment
                if (metricForDeployment) {
                    if (labels.contains("deployment=\"MetricsFromWildFlyManagementModelTestCase.war\"")) {
                        Double value = Double.valueOf(split[1]);

                        assertTrue(labels.contains("deployment=\"MetricsFromWildFlyManagementModelTestCase.war\""));
                        assertTrue(labels.contains("subdeployment=\"MetricsFromWildFlyManagementModelTestCase.war\""));
                        assertTrue(labels.contains("servlet=\"" + TestApplication.class.getName() + "\""));
                        assertEquals(Integer.valueOf(expectedCount).doubleValue(), value, 0);

                        return;
                    }
                } else {
                    // check the metrics from the http-listener in the undertow subsystem
                    if (labels.contains("http_listener=\"default\"")) {
                        Double value = Double.valueOf(split[1]);
                        assertTrue(labels.contains("server=\"default-server\""));
                        assertEquals(Integer.valueOf(expectedCount).doubleValue(), value, 0);
                        return;

                    }
                }
            }
        }
        fail(metricName + "metric not found");
    }
}