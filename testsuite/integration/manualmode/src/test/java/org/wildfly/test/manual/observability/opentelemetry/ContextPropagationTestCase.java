/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.manual.observability.opentelemetry;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.FilePermission;
import java.io.IOException;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.util.JacksonFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.images.PullPolicy;
import org.wildfly.test.manual.observability.opentelemetry.deployment1.TestApplication1;
import org.wildfly.test.manual.observability.opentelemetry.deployment2.TestApplication2;

/**
 * This test exercises the context propagation functionality. Two services are deployed, with the first calling the
 * second. The second service attempts to retrieve the trace propagation header and return it. The first returns a JSON
 * object containing the traceparent header value and the traceId. We then query the Jaeger collector, started using the
 * ClassRule, to verify that the trace was successfully exported.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ContextPropagationTestCase.OpenTelemetrySetupTask.class)
public class ContextPropagationTestCase {
    private static final String CONTAINER = "otel";
    public static final String DEPLOYMENTA = "otel-service1";
    public static final String DEPLOYMENTB = "otel-service2";
    public static final String JAEGER_IMAGE = "jaegertracing/all-in-one:latest";

    @ArquillianResource
    protected static ContainerController containerController;

    public static GenericContainer jaeger = new FixedHostPortGenericContainer(JAEGER_IMAGE)
            .withFixedExposedPort(5775, 5775, InternetProtocol.UDP)
            .withFixedExposedPort(5778, 5778)
            .withFixedExposedPort(6831, 6831, InternetProtocol.UDP)
            .withFixedExposedPort(6832, 6832, InternetProtocol.UDP)
            .withFixedExposedPort(9411, 9411)
            .withFixedExposedPort(14250, 14250)
            .withFixedExposedPort(14268, 14268)
            .withFixedExposedPort(16686, 16686)
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withEnv("COLLECTOR_ZIPKIN_HOST_PORT", "9411")
            .waitingFor(new HostPortWaitStrategy() {
                @Override
                protected Set<Integer> getLivenessCheckPorts() {
                    Set<Integer> ports = new HashSet<>(1);
                    ports.addAll(Arrays.asList(9411, 5778, 14250, 14268, 16686));
                    return ports;
                }
            });

    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENTA, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentA() {
        return buildBaseArchive(DEPLOYMENTA)
                .addPackage(TestApplication1.class.getPackage());
    }

    @Deployment(name = DEPLOYMENTB, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentB() {
        return buildBaseArchive(DEPLOYMENTB)
                .addPackage(TestApplication2.class.getPackage());
    }

    private static WebArchive buildBaseArchive(String name) {
        return ShrinkWrap
                .create(WebArchive.class, name + ".war")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Required for the ClientBuilder.newBuilder() so the ServiceLoader will work
                        new FilePermission("<<ALL FILES>>", "read"),
                        // Required for com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider. During <init> there is a
                        // reflection test to check for JAXRS 2.0.
                        new RuntimePermission("accessDeclaredMembers"),
                        new NetPermission("getProxySelector"),
                        // Required for the client to connect
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":14250", "connect,resolve"),
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":16686", "connect,resolve"),
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" +
                                TestSuiteEnvironment.getHttpPort(), "connect,resolve")
                ), "permissions.xml");
    }

    @Before
    public void setup() throws Exception {
        Assume.assumeTrue(System.getProperty("os.name").equalsIgnoreCase("Linux"));
        jaeger.start();
        containerController.start(CONTAINER);

        deployer.deploy(DEPLOYMENTA);
        deployer.deploy(DEPLOYMENTB);
    }


    @After
    public void teardown() {
        jaeger.stop();
        executeCleanup(() -> deployer.undeploy(DEPLOYMENTA));
        executeCleanup(() -> deployer.undeploy(DEPLOYMENTB));
        executeCleanup(() -> containerController.stop(CONTAINER));
    }


    @Test
    @RunAsClient
    public void testContextPropagation() throws Exception {
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");

        URL serviceUrl = new URL(managementClient.getWebUri().toURL(), '/' + DEPLOYMENTA);
        Response response = ClientBuilder.newClient()
                .register(JacksonFeature.class)
                .target(serviceUrl.toURI())
                .request()
                .get();

        debugLog("Headers: " + response.getHeaders());

        Assert.assertEquals(200, response.getStatus());
        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> entity = mapper.readValue(response.readEntity(String.class), Map.class);

        String traceId = entity.get("traceId");
        String traceParent = entity.get("traceParent");

        Assert.assertNotNull(traceId);
        Assert.assertNotNull(traceParent);
        Assert.assertTrue(traceParent.contains(traceId));

        verifyTrace(traceId);
    }

    private void verifyTrace(String traceId) throws InterruptedException {
        String uri = "http://" + jaeger.getHost() + ":" + jaeger.getMappedPort(16686)
                + "/api/traces/" + traceId;

        debugLog("Trace API URL: " + uri);
        for (int count = 0; count < 30; count++) {
            Response response = ClientBuilder.newClient()
                    .target(uri)
                    .request()
                    .get();
            int status = response.getStatus();
            if (status == 404) {
                // Spans may not have been flushed yet. Sleep and try again
                debugLog("Trace not found. Sleeping");
                Thread.sleep(1000);
            } else if (status == 200) {
                Assert.assertTrue("The traceId was not reported to Jaeger",
                        response.readEntity(String.class).contains("\"traceID\":\"" + traceId + "\""));
                return;
            }
        }
        Assert.fail("Trace " + traceId + " could not be retrieved from Jaeger");
    }

    protected void executeCleanup(Runnable func) {
        try {
            func.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void debugLog(String msg) {
        System.out.println("***** " + msg);
    }

    static class OpenTelemetrySetupTask implements ServerSetupTask {

        private final String WILDFLY_EXTENSION_OPENTELEMETRY = "org.wildfly.extension.opentelemetry";
        private final ModelNode address = Operations.createAddress("subsystem", "opentelemetry");

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            execute(managementClient, Operations.createAddOperation(Operations.createAddress("extension",
                    WILDFLY_EXTENSION_OPENTELEMETRY)), true);
            execute(managementClient, Operations.createAddOperation(address), true);

            execute(managementClient, Operations.createWriteAttributeOperation(address,
                    "span-processor-type", "simple"), true);
            execute(managementClient, Operations.createWriteAttributeOperation(address,
                    "batch-delay", "1"), true);
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode address = Operations.createAddress("subsystem", "opentelemetry");
            execute(managementClient, Operations.createRemoveOperation(address), true);
            execute(managementClient, Operations.createRemoveOperation(Operations.createAddress("extension",
                    WILDFLY_EXTENSION_OPENTELEMETRY)), true);
            ServerReload.reloadIfRequired(managementClient);
        }

        private ModelNode execute(final ManagementClient managementClient,
                                  final ModelNode op,
                                  final boolean expectSuccess) throws IOException {
            ModelNode response = managementClient.getControllerClient().execute(op);
            final String outcome = response.get("outcome").asString();
            if (expectSuccess) {
                assertEquals(response.toString(), "success", outcome);
                return response.get("result");
            } else {
                assertEquals("failed", outcome);
                return response.get("failure-description");
            }
        }
    }
}




