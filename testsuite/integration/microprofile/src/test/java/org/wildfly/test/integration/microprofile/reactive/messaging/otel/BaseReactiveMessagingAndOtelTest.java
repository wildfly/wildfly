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
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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

    private final String tracingPropertyName;
    private final String tracingAttributeName;
    private final String connectorSuffix;

    private static String deploymentName;

    private Set<String> previousTestsTraceIds = new HashSet<>();
    private Set<String> currentTraceIds = new HashSet<>();

    private int ITERATIONS = Integer.valueOf(WildFlySecurityManager.getPropertyPrivileged("wildfly.mp.rm.otel.iterations", "2"));

    public BaseReactiveMessagingAndOtelTest(String connectorSuffix) {
        this.tracingPropertyName = String.format("mp.messaging.connector.smallrye-%s.tracing-enabled", connectorSuffix);
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
        //war.as(ZipExporter.class).exportTo(new File("target/" + war.getName()));
        return war;
    }

    abstract OpenTelemetryCollectorContainer getCollector();

    @Test
    public void testNoOpenTelemetryTracing() throws Exception {
        // None of these should be set at this stage, but make sure they are not just in case
        ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
        ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, null);
        ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());

        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"no-trace");
            waitForData(client, "no-trace");

            // We should not get any traces for reactive messaging. Since traces are not synchronous,
            // sleep a little bit so that rogue ones can have time to come through.
            List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
            System.out.println(traces);
        }

    }

    @Test
    public void testOpenTelemetryTracing() throws Exception {
        String[] values = new String[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            values[i] = "trace-" + i;
        }
        try {
            ReactiveMessagingOtelUtils.setConnectorTracingType(managementClient.getControllerClient(), tracingAttributeName, TracingType.OFF);
            ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, true);
            ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                for (String value : values) {
                    postData(client, value);
                }
                waitForData(client, values);
                checkJaegerTraces(JAEGER_TIMEOUT, ITERATIONS);
            }
        } finally {
            ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
            ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, null);
            ReactiveMessagingOtelUtils.reload(managementClient.getControllerClient());
        }
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

    private void checkJaegerTraces(int timeout, int expectedCount) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout;
        int count = 0;
        while (System.currentTimeMillis() < end) {
            count = 0;
            List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
            for (JaegerTrace trace : traces) {
                if (previousTestsTraceIds.contains(trace.getTraceID())) {
                    continue;
                }
                currentTraceIds.add(trace.getTraceID());
                if (hasPostAndReactiveMessagingSpans(trace)) {
                    count++;
                }
            }

            if (count == expectedCount) {
                return;
            }

            Thread.sleep(1000);
        }

        Assert.assertEquals("Number of traces containing microprofile messaging traces was different from expected", expectedCount, count);
    }

    private boolean hasPostAndReactiveMessagingSpans(JaegerTrace trace) {
        List<JaegerSpan> spans = trace.getSpans();

        boolean post = false;
        boolean publish = false;
        boolean receive = false;

        for (JaegerSpan span : spans) {
            if (span.getOperationName().equals(String.format("POST /mp-rm-%s-otel/", connectorSuffix))) {
                post = true;
            } else if (span.getOperationName().equals(String.format("test%s publish", connectorSuffix))) {
                publish = true;
            } else if (span.getOperationName().equals(String.format("test%s receive", connectorSuffix))) {
                receive = true;
            }
        }

        return post && publish && receive;
    }
}
