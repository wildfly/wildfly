package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;
import org.wildfly.test.integration.microprofile.reactive.messaging.otel.application.ConfigBeanAndEndpoint;
import org.wildfly.test.integration.microprofile.reactive.messaging.otel.application.TestReactiveMessagingOtelApplication;

/**
 * Tests that the MP Config values for turning on/off OTel tracing for the connectors have the expected
 * values depending on:
 * - subsystem configuration
 * - MP config values for the deployment
 *
 * We're only testing the value of the connector level property, as this is enough to ensure that the subsystem
 * configuration is getting propagated to the TracingTypeInterceptor when integrated in the server.
 * The tests inheriting from TracingTypeConfigTest do more in depth checking of values at the channel level.
 */
public abstract class BaseReactiveMessagingAndOtelConfigTest {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    URL url;

    private final String tracingPropertyName;
    private final String tracingAttributeName;


    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }


    public BaseReactiveMessagingAndOtelConfigTest(String tracingPropertyName, String tracingAttributeName) {
        this.tracingPropertyName = tracingPropertyName;
        this.tracingAttributeName = tracingAttributeName;
    }

    protected static WebArchive createDeployment(String deploymentName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName)
            .addClasses(ConfigBeanAndEndpoint.class, TestReactiveMessagingOtelApplication.class)
            .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;

    }

    @Test
    public void testNoConnectorOtelResourceAndMPConfigTracingUndefined() throws Exception {
        ensureNoSubsystemConfigResourceAndSetSystemPropertyAndCheck(null, false);
    }

    @Test
    public void testNoConnectorOtelResourceAndMPConfigTracingTrue() throws Exception {
        ensureNoSubsystemConfigResourceAndSetSystemPropertyAndCheck(true, false);
    }

    @Test
    public void testNoConnectorOtelResourceAndMPConfigTracingFalse() throws Exception {
        ensureNoSubsystemConfigResourceAndSetSystemPropertyAndCheck(false, false);
    }

    @Test
    public void testConnectorOtelResourceNeverAndMPConfigTracingUndefined() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.NEVER, null, false);
    }

    @Test
    public void testConnectorOtelResourceNeverAndMPConfigTracingTrue() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.NEVER, true, false);
    }

    @Test
    public void testConnectorOtelResourceNeverAndMPConfigTracingFalse() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.NEVER, false, false);
    }

    @Test
    public void testConnectorOtelResourceOffAndMPConfigTracingUndefined() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.OFF, null, false);
    }

    @Test
    public void testConnectorOtelResourceOffAndMPConfigTracingTrue() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.OFF, true, true);
    }

    @Test
    public void testConnectorOtelResourceOffAndMPConfigTracingFalse() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.OFF, false, false);
    }


    @Test
    public void testConnectorOtelResourceOnAndMPConfigTracingUndefined() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ON, null, true);
    }

    @Test
    public void testConnectorOtelResourceOnAndMPConfigTracingTrue() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ON, true, true);
    }

    @Test
    public void testConnectorOtelResourceOnAndMPConfigTracingFalse() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ON, false, false);
    }

    @Test
    public void testConnectorOtelResourceAlwaysAndMPConfigTracingUndefined() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ALWAYS, null, true);
    }

    @Test
    public void testConnectorOtelResourceAlwaysAndMPConfigTracingTrue() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ALWAYS, true, true);
    }

    @Test
    public void testConnectorOtelResourceAlwaysAndMPConfigTracingFalse() throws Exception {
        adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType.ALWAYS, false, true);
    }

    private void ensureNoSubsystemConfigResourceAndSetSystemPropertyAndCheck(Boolean property,
                                                                             boolean expectedConfigValue) throws Exception {
        ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
        ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, property);
        ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());
        boolean calculatedValue = readEffectiveTracingEnabledValueFromConfig();
        Assert.assertEquals(expectedConfigValue, calculatedValue);
    }

    private void adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType subsystemAttributeValue,
                                                                Boolean property,
                                                                boolean expectedConfigValue) throws Exception {
        ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, property);
        ReactiveMessagingOtelUtils.setConnectorTracingType(managementClient.getControllerClient(), tracingAttributeName, subsystemAttributeValue);
        ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());
        boolean calculatedValue = readEffectiveTracingEnabledValueFromConfig();
        Assert.assertEquals(expectedConfigValue, calculatedValue);
    }

    private boolean readEffectiveTracingEnabledValueFromConfig() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            HttpGet get = new HttpGet(url.toString() + "property?prop=" + tracingPropertyName);
            try (CloseableHttpResponse response = client.execute(get)){
                assertEquals(200, response.getStatusLine().getStatusCode());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    String line = reader.readLine();
                    Assert.assertNotNull(line);
                    return Boolean.parseBoolean(line);
                }
            }
        }
    }
}

