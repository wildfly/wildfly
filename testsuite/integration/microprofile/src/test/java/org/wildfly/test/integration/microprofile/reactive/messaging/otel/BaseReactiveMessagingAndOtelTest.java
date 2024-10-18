/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;
import org.wildfly.test.integration.microprofile.reactive.messaging.otel.application.TestReactiveMessagingOtelBean;

public abstract class BaseReactiveMessagingAndOtelTest {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final PathAddress RESOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH);

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    URL url;

    private static final int TIMEOUT = TimeoutUtil.adjust(20000);

    private final String tracingPropertyName;
    private final String tracingAttributeName;

    private static String deploymentName;


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
        BaseReactiveMessagingAndOtelTest.deploymentName = deploymentName;
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName)
            .addPackage(TestReactiveMessagingOtelBean.class.getPackage())
            .addAsWebInfResource(TestReactiveMessagingOtelBean.class.getPackage(), mpCfgPropertiesFileName, "classes/META-INF/microprofile-config.properties")
            .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        war.as(ZipExporter.class).exportTo(new File("target/" + war.getName()));

        return war;
    }

    abstract OpenTelemetryCollectorContainer getCollector();

//    @Test
    public void testNoOtelTracing() throws Exception {
        // None of these should be set at this stage, but make sure they are not just in case
        ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
        ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, null);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()){
            postData(client,"no-trace");
            boolean success = false;
            try {
                waitForData(client, "no-trace");
                success = true;
            } finally {
                HttpDelete delete = new HttpDelete(url.toString());
                try (CloseableHttpResponse response = client.execute(delete)){
                    if (success) {
                        assertEquals(204, response.getStatusLine().getStatusCode());
                    }
                }
            }
        }

        List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
        System.out.println(traces);
    }

    @Test
    public void testOtelTracing() throws Exception {
        try {
            ReactiveMessagingOtelUtils.setConnectorTracingType(managementClient.getControllerClient(), tracingAttributeName, TracingType.OFF);
            ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, true);

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                postData(client, "trace");
                boolean success = false;
                try {
                    waitForData(client, "trace");
                    success = true;
                } finally {
                    HttpDelete delete = new HttpDelete(url.toString());
                    try (CloseableHttpResponse response = client.execute(delete)) {
                        if (success) {
                            assertEquals(204, response.getStatusLine().getStatusCode());
                        }
                    }
                }
            }
        } finally {
            ReactiveMessagingOtelUtils.enableConnectorOpenTelemetryResource(managementClient.getControllerClient(), false);
            ReactiveMessagingOtelUtils.setTracingConfigSystemProperty(managementClient.getControllerClient(), tracingPropertyName, null);
        }

        List<JaegerTrace> traces = getCollector().getTraces(deploymentName);
        System.out.println(traces);
    }



    private void postData(CloseableHttpClient client, String value) throws Exception {
        HttpPost post = new HttpPost(url.toString());
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("value", value));
        post.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = client.execute(post);){
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    private void waitForData(CloseableHttpClient client, String...expected) throws Exception {
        long end = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis() < end) {
            List<String> read = getData(client);
            if (read.size() == expected.length) {
                Assert.assertArrayEquals(expected, read.toArray(new String[0]));
                break;
            }
            Thread.sleep(2000);
        }
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
}
