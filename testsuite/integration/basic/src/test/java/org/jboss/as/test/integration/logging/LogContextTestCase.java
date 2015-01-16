/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.logging;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Note that the servlet uses a reflection hack to get the size of the log context map. This is fragile and may break,
 * but should be rather obvious if it does.
 * <p/>
 * This tests that after an undeploy the {@link org.jboss.logmanager.LogContext log contexts} and class loaders are
 * cleaned up during an undeploy. Attempts to find leaking class loaders.
 * <p/>
 * This test can't be moved to core at this point as it needs to have processing for EAR's and WAR's.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ServerSetup(LogContextTestCase.LoggingProfileSetup.class)
@RunWith(Arquillian.class)
public class LogContextTestCase {
    private static final String LOG_FILE_NAME = "log-context-profile-test.log";

    @Deployment(name = "logcontext", testable = false)
    public static WebArchive createDeployment() {
        return logContextDeployment("logcontext-test.war");
    }

    @Deployment(name = "logging", testable = false, managed = false)
    public static WebArchive createLoggingDeployment() {
        return loggingDeployment("logging-test.war")
                .addAsManifestResource(LogContextTestCase.class.getPackage(),
                        "logging.properties", "logging.properties");
    }

    @Deployment(name = "logging-ear", testable = false, managed = false)
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "logging.ear")
                .addAsManifestResource(LogContextTestCase.class.getPackage(),
                        "logging.properties", "logging.properties")
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.logmanager, org.jboss.as.logging")
                                .exportAsString()))
                .addAsModules(logContextDeployment("logcontext-in-ear.war"), loggingDeployment("logging-in-ear.war"));
    }

    @Deployment(name = "logging-profile", testable = false, managed = false)
    public static WebArchive createLoggingProfileDeployment() {
        return loggingDeployment("logging-profile-test.war")
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Logging-Profile", "test")
                                .exportAsString()));
    }

    @Deployment(name = "logging-profile-ear", testable = false, managed = false)
    public static EnterpriseArchive createEarProfileDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "logging-profile.ear")
                .addAsManifestResource(LogContextTestCase.class.getPackage(),
                        "logging.properties", "logging.properties")
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.logmanager, org.jboss.as.logging")
                                .attribute("Logging-Profile", "test")
                                .exportAsString()))
                .addAsModules(logContextDeployment("logcontext-profile-in-ear.war"), loggingDeployment("logging-profile-in-ear.war"));
    }

    @ArquillianResource
    public Deployer deployer;

    @Test
    @OperateOnDeployment("logcontext")
    public void contextTest(@ArquillianResource(LogContextHackServlet.class) final URL url) throws Exception {
        final String u = url + LogContextHackServlet.SERVLET_URL;
        String result = performCall(u);
        Assert.assertEquals("No LogContexts should exist in WAR deployment", 0, Integer.valueOf(result).intValue());
        deployer.deploy("logging");
        result = performCall(u);
        Assert.assertEquals("One LogContexts should exist in WAR deployment", 1, Integer.valueOf(result).intValue());
        deployer.undeploy("logging");
        result = performCall(u);
        Assert.assertEquals("No LogContexts should exist in WAR deployment", 0, Integer.valueOf(result).intValue());

        // Deploy an EAR
        deployer.deploy("logging-ear");
        result = performCall(u);
        Assert.assertEquals("Three LogContexts should exist in EAR deployment", 3, Integer.valueOf(result).intValue());
        deployer.undeploy("logging-ear");
        result = performCall(u);
        Assert.assertEquals("No LogContexts should exist in EAR deployment", 0, Integer.valueOf(result).intValue());

        // Deploy with a profile
        deployer.deploy("logging-profile");
        result = performCall(u);
        Assert.assertEquals("One LogContexts should exist in WAR deployment with logging-profile", 1, Integer.valueOf(result).intValue());
        deployer.undeploy("logging-profile");
        result = performCall(u);
        Assert.assertEquals("No LogContexts should exist in WAR deployment with logging-profile", 0, Integer.valueOf(result).intValue());

        // Deploy an EAR with a profile
        deployer.deploy("logging-profile-ear");
        result = performCall(u);
        Assert.assertEquals("Three LogContexts should exist in EAR deployment with logging-profile", 3, Integer.valueOf(result).intValue());
        deployer.undeploy("logging-profile-ear");
        result = performCall(u);
        Assert.assertEquals("No LogContexts should exist in EAR deployment with logging-profile", 0, Integer.valueOf(result).intValue());
    }

    private static WebArchive loggingDeployment(final String name) {
        return ShrinkWrap
                .create(WebArchive.class, name)
                .addClasses(LogContextHackServlet.class);
    }

    private static WebArchive logContextDeployment(final String name) {
        return ShrinkWrap
                .create(WebArchive.class, name)
                .addClasses(LogContextHackServlet.class)
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.logmanager, org.jboss.as.logging")
                                .exportAsString()));

    }

    private static String performCall(final String url) throws ExecutionException, IOException, TimeoutException {
        return HttpRequest.get(url, TimeoutUtil.adjust(10), TimeUnit.SECONDS);
    }

    private static ModelNode createAddress(final String... paths) {
        PathAddress address = PathAddress.pathAddress("subsystem", "logging");
        for (int i = 0; i < paths.length; i++) {
            final String key = paths[i];
            if (++i < paths.length) {
                address = address.append(PathElement.pathElement(key, paths[i]));
            } else {
                address = address.append(PathElement.pathElement(key));
            }
        }
        return address.toModelNode();
    }

    private static ModelNode executeOperation(final ManagementClient client, final Operation op) throws IOException {
        ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.assertTrue(Operations.getFailureDescription(result).toString(), false);
        }
        return result;
    }

    static class LoggingProfileSetup implements ServerSetupTask {
        private final Deque<ModelNode> tearDownOps;

        public LoggingProfileSetup() {
            tearDownOps = new ArrayDeque<>();
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
            // Setup a logging-profile
            final ModelNode loggingProfileAddress = createAddress("logging-profile", "test");

            // Add the profile and add a remove operation
            builder.addStep(Operations.createAddOperation(loggingProfileAddress));
            tearDownOps.addFirst(Operations.createRemoveOperation(loggingProfileAddress));

            final ModelNode fileHandlerAddress = createAddress("logging-profile", "test", "file-handler", "test-handler");
            ModelNode op = Operations.createAddOperation(fileHandlerAddress);
            op.get("append").set(true);
            final ModelNode file = op.get("file");
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(LOG_FILE_NAME);
            op.get("formatter").set("[test-profile] %d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
            builder.addStep(op);
            tearDownOps.addFirst(Operations.createRemoveOperation(fileHandlerAddress));

            final ModelNode rootLoggerAddress = createAddress("logging-profile", "test", "root-logger", "ROOT");
            op = Operations.createAddOperation(rootLoggerAddress);
            op.get("level").set("INFO");
            final ModelNode handlers = op.get("handlers").setEmptyList();
            handlers.add("test-handler");
            builder.addStep(op);
            tearDownOps.addFirst(Operations.createRemoveOperation(rootLoggerAddress));

            executeOperation(managementClient, builder.build());
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
            ModelNode op;
            while ((op = tearDownOps.poll()) != null) {
                builder.addStep(op);
            }
            executeOperation(managementClient, builder.build());
        }
    }

}
