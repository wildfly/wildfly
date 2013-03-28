/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.management.api;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
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
import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;

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

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(address());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            op.get(ModelDescriptionConstants.PORT).set(9999);
            op.get(ModelDescriptionConstants.INTERFACE).set("management");
            op.get(ModelDescriptionConstants.SECURITY_REALM).set("ManagementRealm");
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(address());
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        private ModelNode address() {
            return PathAddress.pathAddress()
                    .append(CORE_SERVICE, MANAGEMENT)
                    .append(MANAGEMENT_INTERFACE, NATIVE_INTERFACE).toModelNode();
        }
    }

    @Deployment
    public static final Archive fakeDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

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
    @Ignore("WFLY-1284")
    public void test700Final() throws Exception {
        test("7.0.0.Final");
    }

    @Test
    @Ignore("WFLY-1284")
    public void test701Final() throws Exception {
        test("7.0.1.Final");
    }

    @Test
    public void test710Final() throws Exception {
        test("7.1.0.Final");
    }

    @Test
    public void test711Final() throws Exception {
        test("7.1.1.Final");
    }

    @Test
    public void test720Final() throws Exception {
        test("7.2.0.Final");
    }

    @Test
    public void testCurrent() throws Exception {
        test(ModelControllerClient.Factory.create("remote", "localhost", 9999));
    }

    protected void test(final String version) throws Exception {
        test(createClient(version, "localhost", 9999));
    }

    protected void test(final ModelControllerClient client) throws Exception {
        try {
            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
            operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            // include a lot of garbage
            operation.get(ModelDescriptionConstants.RECURSIVE).set(true);
            operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
            operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set(true);
            final ModelNode result = client.execute(operation);
            Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

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

    protected static ModelControllerClient createClient(final String version, final String host, final int port) throws Exception {
        final ChildFirstClassLoaderBuilder classLoaderBuilder = new ChildFirstClassLoaderBuilder();
        classLoaderBuilder.addRecursiveMavenResourceURL("org.jboss.as:jboss-as-controller-client:" + version, excludes);
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.ModelControllerClientConfiguration");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.ModelControllerClient");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.OperationMessageHandler");
        classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.controller.client.Operation");

        final ClassLoader classLoader = classLoaderBuilder.build();
        final Class<?> clazz = classLoader.loadClass("org.jboss.as.controller.client.impl.ClientConfigurationImpl");
        // create(final String hostName, final int port
        final Method method = clazz.getMethod("create", String.class, int.class);
        final Object configuration = method.invoke(null, host, port);

        final Class<?> clazz2 = classLoader.loadClass("org.jboss.as.controller.client.impl.RemotingModelControllerClient");
        Constructor constructor = clazz2.getConstructor(ModelControllerClientConfiguration.class);
        final Object client = constructor.newInstance(configuration);
        final InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return method.invoke(client, args);
            }
        };
        final Class<?>[] interfaces = new Class<?>[]{ModelControllerClient.class};
        return (ModelControllerClient) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
    }

}
