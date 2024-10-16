package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extension.microprofile.reactive.messaging.ConnectorOpenTelemetryTracingResourceDefinition;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;
import org.wildfly.test.integration.microprofile.reactive.messaging.otel.application.TestReactiveMessagingOtelBean;

public class BaseReactiveMessagingAndOtelTest {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final PathAddress RESOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(ConnectorOpenTelemetryTracingResourceDefinition.PATH);

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


    public BaseReactiveMessagingAndOtelTest(String tracingPropertyName, String tracingAttributeName) {
        this.tracingPropertyName = tracingPropertyName;
        this.tracingAttributeName = tracingAttributeName;
    }

    protected static WebArchive createDeployment(String deploymentName, String mpCfgPropertiesFileName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war")
            .addPackage(TestReactiveMessagingOtelBean.class.getPackage())
            .addAsWebInfResource(TestReactiveMessagingOtelBean.class.getPackage(), mpCfgPropertiesFileName, "classes/META-INF/microprofile-config.properties")
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
        enableConnectorOpenTelemetryResource(false);
        setTracingConfigSystemProperty(property);
        reload();
        boolean calculatedValue = readEffectiveTracingEnabledValueFromConfig();
        Assert.assertEquals(expectedConfigValue, calculatedValue);
    }

    private void adjustSubsystemConfigAndSystemPropertyAndCheck(TracingType subsystemAttributeValue,
                                                                Boolean property,
                                                                boolean expectedConfigValue) throws Exception {
        setTracingConfigSystemProperty(property);
        setConnectorTracingType(subsystemAttributeValue);
        reload();
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

    private List<String> getData(CloseableHttpClient client, String url) throws Exception {
        HttpGet get = new HttpGet(url);
        List<String> lines = new ArrayList<>();

        try (CloseableHttpResponse response = client.execute(get)){
            assertEquals(200, response.getStatusLine().getStatusCode());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    return ModelNode.fromJSONString(line).asList().stream().map(ModelNode::asString).collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }


    private Set<String> readChildrenNames(PathAddress addr, String childType) throws Exception{
        ModelNode readChildren = Operations.createOperation(ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION, addr.toModelNode());
        readChildren.get(CHILD_TYPE).set(new ModelNode(childType));
        ModelNode result = managementClient.getControllerClient().execute(readChildren);
        Assert.assertTrue(Operations.isSuccessfulOutcome(result));
        result = Operations.readResult(result);
        return result.asList().stream().map(ModelNode::asString).collect(Collectors.toSet());
    }

    private void enableConnectorOpenTelemetryResource(boolean add) throws Exception {
        Set<String> names =
                readChildrenNames(SUBSYSTEM_ADDRESS, ConnectorOpenTelemetryTracingResourceDefinition.PATH.getKey());


        ModelNode op = null;
        if (names.contains(ConnectorOpenTelemetryTracingResourceDefinition.PATH.getValue())) {
            if (!add) {
                // Remove it
                op = Operations.createRemoveOperation(RESOURCE_ADDRESS.toModelNode());
            }
        } else {
            if (add) {
                // Add it
                op = Operations.createAddOperation(RESOURCE_ADDRESS.toModelNode());
            }
        }

        if (op != null) {
            ModelNode result = managementClient.getControllerClient().execute(op);
            Assert.assertTrue(Operations.isSuccessfulOutcome(result));
        }
    }

    private void setTracingConfigSystemProperty(Boolean value) throws Exception {
        Set<String> names =
                readChildrenNames(PathAddress.EMPTY_ADDRESS, SYSTEM_PROPERTY);

        PathAddress propAddr = PathAddress.pathAddress(SYSTEM_PROPERTY, tracingPropertyName);

        ModelNode op = null;
        if (value == null) {
            if (names.contains(tracingPropertyName)) {
                // Remove this
                op = Operations.createRemoveOperation(propAddr.toModelNode());
            }
        } else {
            if (names.contains(tracingPropertyName)) {
                // Write the value
                op = Operations.createWriteAttributeOperation(propAddr.toModelNode(), VALUE, value);
            } else {
                // Add the resource
                op = Operations.createAddOperation(propAddr.toModelNode());
                op.get(VALUE).set(new ModelNode(value));
            }
        }

        if (op != null) {
            ModelNode result = managementClient.getControllerClient().execute(op);
            Assert.assertTrue(Operations.isSuccessfulOutcome(result));
        }
    }

    private void setConnectorTracingType(TracingType tracingType) throws Exception {
        enableConnectorOpenTelemetryResource(true);
        ModelNode op =
                Operations.createWriteAttributeOperation(
                        RESOURCE_ADDRESS.toModelNode(), tracingAttributeName, tracingType.toString());
            ModelNode result = managementClient.getControllerClient().execute(op);
            Assert.assertTrue(Operations.isSuccessfulOutcome(result));
    }

    private void reload() throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
}
