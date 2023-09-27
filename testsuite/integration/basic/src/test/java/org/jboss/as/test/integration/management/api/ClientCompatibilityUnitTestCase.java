/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.api;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.SecureRandom;
import java.util.Random;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ChildFirstClassLoaderBuilder;
import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test supported remoting libraries combinations.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ClientCompatibilityUnitTestCase.ClientCompatibilityUnitTestCaseServerSetup.class)
public class ClientCompatibilityUnitTestCase {
    static class ClientCompatibilityUnitTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            ModelNode socketBindingOp = Util.createAddOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=management-native"));
            socketBindingOp.get("interface").set("management");
            socketBindingOp.get("port").set("9999");

            ManagementOperations.executeOperation(managementClient.getControllerClient(),socketBindingOp);

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(address());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get(ModelDescriptionConstants.SOCKET_BINDING).set("management-native");

            op.get(ModelDescriptionConstants.SASL_AUTHENTICATION_FACTORY).set("management-sasl-authentication");

            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(address());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            ManagementOperations.executeOperation(managementClient.getControllerClient(),
                    Util.createRemoveOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=management-native")));
        }

        private ModelNode address() {
            return PathAddress.pathAddress()
                    .append(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT)
                    .append(ModelDescriptionConstants.MANAGEMENT_INTERFACE, ModelDescriptionConstants.NATIVE_INTERFACE).toModelNode();
        }
    }

    // Arquillian requires a deployment to trigger @ServerSetup handling
    @Deployment
    public static Archive fakeDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    private static final String CONTROLLER_ADDRESS = System.getProperty("node0", "localhost");

    private static final String WFCORE_CLIENT = "org.wildfly.core:wildfly-controller-client";

    private static final String[] excludes = new String[]{"org.jboss.threads:jboss-threads", "org.jboss:jboss-dmr", "org.jboss.logging:jboss-logging"};
    private static final Archive deployment;

    static {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class);
        // Create basic archive which exceeds the remoting window size
        for (int i = 0; i < 10; i++) {
            final byte[] data = new byte[8096];
            new Random(new SecureRandom().nextLong()).nextBytes(data);
            archive.add(new ByteArrayAsset(data), "data" + i);
        }
        deployment = archive;
    }

    @Test
    public void testCore101Final() throws Exception {
        testWF("1.0.1.Final", 9999);
    }

    @Test
    public void testCore101FinalHttp() throws Exception {
        testWF("1.0.1.Final", 9990);
    }

    @Test
    public void testCore210Final() throws Exception {
        testWF("2.1.0.Final", 9999);
    }

    @Test
    public void testCore210FinalHttp() throws Exception {
        testWF("2.1.0.Final", 9990);
    }

    @Test
    public void testCore221Final() throws Exception {
        testWF("2.2.1.Final", 9999);
    }

    @Test
    public void testCore221FinalHttp() throws Exception {
        testWF("2.2.1.Final", 9990);
    }

    @Test
    public void testCore3010Final() throws Exception {
        testWF("3.0.10.Final", 9999);
    }

    @Test
    public void testCore3010FinalHttp() throws Exception {
        testWF("3.0.10.Final", 9990);
    }

    // https://issues.redhat.com/browse/WFLY-18171
    // Test WF Core versions that went into WildFly 23 and later (latest micro for each major/minor release)
    // Tests WF Core 15.0.1.Final for WildFly 23.0.2.Final.
    @Test
    public void testCore1501Final() throws Exception {
        testWF("15.0.1.Final", 9999);
    }

    @Test
    public void testCore1501FinalHttp() throws Exception {
        testWF("15.0.1.Final", 9990);
    }

    // Tests WF Core 16.0.1.Final for WildFly 24.0.1.Final. WildFly 24 was the last release that supported Picketbox
    @Test
    public void testCore1601Final() throws Exception {
        testWF("16.0.1.Final", 9999);
    }

    @Test
    public void testCore1601FinalHttp() throws Exception {
        testWF("16.0.1.Final", 9990);
    }

    // Tests WF Core 17.0.3.Final for WildFly 25.0.1.Final
    @Test
    public void testCore1703Final() throws Exception {
        testWF("17.0.3.Final", 9999);
    }

    @Test
    public void testCore1703FinalHttp() throws Exception {
        testWF("17.0.3.Final", 9990);
    }

    // WildFly 26.0 is skipped as it was soon followed up by WildFly 26.1
    // Tests WF Core 18.1.2.Final for WildFly 26.1.3.Final.
    @Test
    public void testCore1812Final() throws Exception {
        testWF("18.1.2.Final", 9999);
    }

    @Test
    public void testCore1812FinalHttp() throws Exception {
        testWF("18.1.2.Final", 9990);
    }

    // Tests WF Core 19.0.1.Final for WildFly 27.0.1.Final
    @Test
    public void testCore1901Final() throws Exception {
        testWF("19.0.1.Final", 9999);
    }

    @Test
    public void testCore1901FinalHttp() throws Exception {
        testWF("19.0.1.Final", 9990);
    }

    // Tests WF Core 20.0.2.Final for WildFly 28.0.1.Final
    @Test
    public void testCore2002Final() throws Exception {
        testWF("20.0.2.Final", 9999);
    }

    @Test
    public void testCore2002FinalHttp() throws Exception {
        testWF("20.0.2.Final", 9990);
    }

    // Tests WF Core 21.1.0.Final for WildFly 29.0.0.Final
    @Test
    public void testCore2110Final() throws Exception {
        testWF("21.1.0.Final", 9999);
    }

    @Test
    public void testCore2110FinalHttp() throws Exception {
        testWF("21.1.0.Final", 9990);
    }

    @Test
    public void testCurrent() throws Exception {
        test(ModelControllerClient.Factory.create(CONTROLLER_ADDRESS, 9999));
    }

    @Test
    public void testCurrentHttp() throws Exception {
        test(ModelControllerClient.Factory.create(CONTROLLER_ADDRESS, 9990));
    }

    private void testWF(final String version, int port) throws Exception {
        test(createClient(version, port));
    }

    private void test(final ModelControllerClient client) throws Exception {
        try {
            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
            operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            // include a lot of garbage
            operation.get(ModelDescriptionConstants.RECURSIVE).set(true);
            operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
            operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set(true);
            final ModelNode result = client.execute(operation);
            Assert.assertEquals(operation.toString(), ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

            final ModelNode deploy = new ModelNode();
            deploy.get(ModelDescriptionConstants.OP).set("add");
            deploy.get(ModelDescriptionConstants.OP_ADDR).add("deployment", "compat-test.war");
            deploy.get("content").get(0).get("input-stream-index").set(0);
            deploy.get("auto-start").set(true);

            final Operation o = OperationBuilder.create(deploy)
                    .addInputStream(deployment.as(ZipExporter.class).exportAsInputStream()).build();

            try {
                final ModelNode deployResult = client.execute(o);
                Assert.assertEquals(deployResult.toString(), ModelDescriptionConstants.SUCCESS, deployResult.get(ModelDescriptionConstants.OUTCOME).asString());

            } finally {
                final ModelNode undeploy = new ModelNode();
                undeploy.get(ModelDescriptionConstants.OP).set("remove");
                undeploy.get(ModelDescriptionConstants.OP_ADDR).add("deployment", "compat-test.war");

                try {
                    client.execute(undeploy);
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }

            }
        } finally {
            StreamUtils.safeClose(client);
        }
    }

    protected static ModelControllerClient createClient(final String version, final int port) throws Exception {

        final ChildFirstClassLoaderBuilder classLoaderBuilder = new ChildFirstClassLoaderBuilder(false);
        classLoaderBuilder.addRecursiveMavenResourceURL(WFCORE_CLIENT + ":" + version, excludes);
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.ModelControllerClient");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.OperationMessageHandler");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.Operation");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.OperationResponse*");

        final ClassLoader classLoader = classLoaderBuilder.build();
        final Class<?> factoryClass = classLoader.loadClass("org.jboss.as.controller.client.ModelControllerClient$Factory");
        final Method factory = factoryClass.getMethod("create", String.class, int.class);
        try {
            final Object client = factory.invoke(null, CONTROLLER_ADDRESS, port);

            final InvocationHandler invocationHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return method.invoke(client, args);
                }
            };
            final Class<?>[] interfaces = new Class<?>[]{ModelControllerClient.class};
            return (ModelControllerClient) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t == null) {
                throw e;
            }
            throw t instanceof Exception ? (Exception) t : new RuntimeException(t);
        }
    }

}
