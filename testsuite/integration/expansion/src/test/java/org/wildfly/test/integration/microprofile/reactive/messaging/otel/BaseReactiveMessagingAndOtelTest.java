/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.test.integration.microprofile.reactive.messaging.otel.application.TestReactiveMessagingOtelBean;

public abstract class BaseReactiveMessagingAndOtelTest {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final PathAddress RESOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH);

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    URL url;

    private static final int REACTIVE_MESSAGING_TIMEOUT = TimeoutUtil.adjust(20000);
    private static final int JAEGER_TIMEOUT = TimeoutUtil.adjust(60000);

    private final String connectorTracingPropertyName;
    private final String tracingAttributeName;
    final String incomingChannelProperty = "mp.messaging.incoming.sink.tracing-enabled";
    final String outgoingChannelProperty = "mp.messaging.outgoing.source.tracing-enabled";
    private final String connectorSuffix;

    private static String deploymentName;

    // these must be static and final so that they are same objects for test instances / thread
    private static final Set<String> previousTestsTraceIds = new HashSet<>();
    private static final Set<String> currentTraceIds = new HashSet<>();

    private int ITERATIONS = Integer.valueOf(WildFlySecurityManager.getPropertyPrivileged("wildfly.mp.rm.otel.iterations", "2"));

    public BaseReactiveMessagingAndOtelTest(String connectorSuffix) {
        this.connectorTracingPropertyName = String.format("mp.messaging.connector.smallrye-%s.tracing-enabled", connectorSuffix);
        this.tracingAttributeName = String.format("%s-connector", connectorSuffix);
        this.connectorSuffix = connectorSuffix;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void testName() {
        System.out.println("=============================");
        System.out.println("TestName: " + name.getMethodName());
        System.out.println("=============================");
    }

    @After
    public void after() throws Exception {
        previousTestsTraceIds.addAll(currentTraceIds);
        currentTraceIds.clear();
    }

    protected static WebArchive createDeployment(String deploymentName, String mpCfgPropertiesFileName) {
        BaseReactiveMessagingAndOtelTest.deploymentName = deploymentName;
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName)
            .addPackage(TestReactiveMessagingOtelBean.class.getPackage())
            .addAsWebInfResource(TestReactiveMessagingOtelBean.class.getPackage(), mpCfgPropertiesFileName, "classes/META-INF/microprofile-config.properties")
            .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    abstract OpenTelemetryCollectorContainer getCollector();

    @Test
    public void testNoOpenTelemetryTracing() throws Exception {
        ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
        ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), connectorTracingPropertyName, null);
        ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());

        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"no-trace");
            waitForData(client, "no-trace");

            // We should not get any traces for reactive messaging. Since traces are not synchronous,
            // sleep a little bit so that rogue ones can have time to come through.
            List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
            checkJaegerTraces(JAEGER_TIMEOUT,
                    ReactiveMessagingOtelAssertUtils.createChecker(1, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                                    .zeroDisabledReceiveAndPublish()
                                    .singlePost()
                                    .zeroTracedPublish()
                                    .zeroTracedReceive()));
        }
    }

    private void testOpenTelemetryTraces(Map<String, TracingType> tracingAttributes, Map<String, Boolean> tracingConfigSystemProperties, int iterations, ReactiveMessagingOtelAssertUtils.TraceChecker... traceCheckers) throws Exception {
        String[] values = new String[iterations];
        for (int i = 0; i < iterations; i++) {
            values[i] = "trace-" + i;
        }
        try {
            // configure connector tracing type (NEVER, OFF, ON, ALWAYS)
            for (Map.Entry<String, TracingType> stringTracingTypeEntry : tracingAttributes.entrySet()) {
                ReactiveMessagingOtelUtils.setConnectorTracingType(managementClient.getControllerClient(), stringTracingTypeEntry.getKey(), "${NON-EXISTING:" + stringTracingTypeEntry.getValue() + "}");
            }
            // configure system properties for connector and channels (true,false)
            for (Map.Entry<String, Boolean> stringBooleanEntry : tracingConfigSystemProperties.entrySet()) {
                ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), stringBooleanEntry.getKey(), stringBooleanEntry.getValue());
            }
            ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());

            // send data
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                for (String value : values) {
                    postData(client, value);
                }
                waitForData(client, values);
            }
            checkJaegerTraces(JAEGER_TIMEOUT, traceCheckers);
        } finally {
            ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
            for (String tracingConfigSystemProperty : tracingConfigSystemProperties.keySet()) {
                ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingConfigSystemProperty, null);
            }
            ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());
        }
    }

    @Test
    public void testOpenTelemetryTracingOffEnabledAtConnectorLevel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.OFF),
                Map.of(connectorTracingPropertyName, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                                .zeroDisabledReceiveAndPublish()
                                .singlePost()
                                .singleTracedPublish()
                                .singleTracedReceive())
                );
    }

    @Test
    public void testOpenTelemetryTracingOffEnabledAtConnectorLevelDisabledAtIncomingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.OFF),
                Map.of(connectorTracingPropertyName, true, incomingChannelProperty, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                                .zeroDisabledReceiveAndPublish()
                                .singlePost()
                                .singleTracedPublish()
                                .zeroTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOffEnabledAtConnectorLevelDisabledAtOutgoingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.OFF),
                Map.of(connectorTracingPropertyName, true, outgoingChannelProperty, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()),
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .zeroPost()
                        .zeroTracedPublish()
                        .singleTracedReceive())
                );
    }

    @Test
    public void testOpenTelemetryTracingOffEnabledAtIncomingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.OFF),
                Map.of(incomingChannelProperty, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()),
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .zeroPost()
                        .zeroTracedPublish()
                        .singleTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOffEnabledAtOutgoingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.OFF),
                Map.of(outgoingChannelProperty, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .singleTracedPublish()
                        .zeroTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOnDisabledAtConnectorLevel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ON),
                Map.of(connectorTracingPropertyName, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()));
    }
    @Test
    public void testOpenTelemetryTracingOnDisabledAtConnectorLevelEnabledAtIncomingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ON),
                Map.of(connectorTracingPropertyName, false, incomingChannelProperty, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()),
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .zeroPost()
                        .zeroTracedPublish()
                        .singleTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOnDisabledAtConnectorLevelEnabledAtOutgoingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ON),
                Map.of(connectorTracingPropertyName, false, outgoingChannelProperty, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .singleTracedPublish()
                        .zeroTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOnDisabledAtIncomingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ON),
                Map.of(incomingChannelProperty, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .singleTracedPublish()
                        .zeroTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingOnDisabledAtOutgoingChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ON),
                Map.of(outgoingChannelProperty, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()),
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .zeroPost()
                        .zeroTracedPublish()
                        .singleTracedReceive()));

    }

    @Test
    public void testOpenTelemetryTracingAlwaysDisabledAtConnectorLevel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ALWAYS),
                Map.of(connectorTracingPropertyName, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .singlePost()
                        .singleTracedPublish()
                        .singleTracedReceive()
                        .singleDisabledReceive()
                        .singleDisabledPublish()));
    }

    @Test
    public void testOpenTelemetryTracingAlwaysDisabledAtChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.ALWAYS),
                Map.of(outgoingChannelProperty, false, incomingChannelProperty, false),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .singlePost()
                        .singleTracedPublish()
                        .singleTracedReceive()
                        .singleDisabledPublish()
                        .singleDisabledReceive()));
    }

    @Test
    public void testOpenTelemetryTracingNeverEnabledAtConnectorLevel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.NEVER),
                Map.of(connectorTracingPropertyName, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()));
    }

    @Test
    public void testOpenTelemetryTracingNeverEnabledAtChannel() throws Exception {
        testOpenTelemetryTraces(
                Map.of(tracingAttributeName, TracingType.NEVER),
                Map.of(outgoingChannelProperty, true, incomingChannelProperty, true),
                ITERATIONS,
                ReactiveMessagingOtelAssertUtils.createChecker(ITERATIONS, ReactiveMessagingOtelAssertUtils.spanSet(connectorSuffix)
                        .zeroDisabledReceiveAndPublish()
                        .singlePost()
                        .zeroTracedPublish()
                        .zeroTracedReceive()));
    }

    private void postData(CloseableHttpClient client, String value) throws Exception {
        HttpPost post = new HttpPost(url.toString());
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("value", value));
        post.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = client.execute(post)){
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    private void waitForData(CloseableHttpClient client, String...expected) throws Exception {
        long end = System.currentTimeMillis() + REACTIVE_MESSAGING_TIMEOUT;
        while (System.currentTimeMillis() < end) {
            List<String> read = getData(client);
            if (read.size() == expected.length) {
                Assert.assertArrayEquals(expected, read.toArray(new String[0]));
                return;
            }
            Thread.sleep(2000);
        }
        Assert.fail("Could not read data in time");
    }

    private List<String> getData(CloseableHttpClient client) throws Exception {
        HttpGet get = new HttpGet(url.toString());
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

    private void checkJaegerTraces(int timeout, ReactiveMessagingOtelAssertUtils.TraceChecker... traceCheckers) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout;
        String errMessage = null;
        List<JaegerTrace> currentDebug = new ArrayList<>();

        int lastSeenTraceCount = 0;
        int stallCount = 0;
        final int MAX_STALL_ITERATIONS = TimeoutUtil.adjust(5); // 5 seconds of no progress

        while (System.currentTimeMillis() < end) {
            List<JaegerTrace> current = new ArrayList<>();
            List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
            for (JaegerTrace trace : traces) {
                if (previousTestsTraceIds.contains(trace.getTraceID())) {
                    continue;
                }
                currentTraceIds.add(trace.getTraceID());
                current.add(trace);
            }

            // Track progress - are we seeing new traces?
            if (current.size() > lastSeenTraceCount) {
                lastSeenTraceCount = current.size();
                stallCount = 0; // Reset stall counter when we see progress
            } else if (current.size() > 0) {
                stallCount++; // We have traces but count isn't increasing
            }

            boolean allGood = true;
            for (ReactiveMessagingOtelAssertUtils.TraceChecker traceChecker : traceCheckers) {
                if (!traceChecker.areValidTraces(current)) {
                    allGood = false;
                    errMessage = traceChecker.errorMessaage();
                    break;
                }
            }
            if (allGood) {
                return;
            }

            // Early exit if we've stalled - don't wait full timeout
            if (stallCount >= MAX_STALL_ITERATIONS && current.size() > 0) {
                System.out.println("Traces have stalled at count=" + current.size() + " for " + stallCount + " iterations");
                break; // Exit early and fail with diagnostic info
            }

            Thread.sleep(1000);
            currentDebug = new ArrayList<>(current);
        }
        System.out.println("Current traces:");
        currentDebug.forEach(trace -> {
                System.out.println("TraceID: " + trace.getTraceID());
                trace.getSpans().forEach(s -> System.out.println(s.getOperationName()));
                System.out.println("-------------------------");
        });
        if (errMessage != null) {
            // some checker produced false and err message
            Assert.fail(errMessage + " (final count: " + currentDebug.size() + ", stalled after seeing " + lastSeenTraceCount + " traces)");
        } else {
            // should not really happen, just to be sure. If everything is good, method returns without getting here
            Assert.fail("Trace validation failed (final count: " + currentDebug.size() + ")");
        }
    }
}
