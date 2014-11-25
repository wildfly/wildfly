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
package org.jboss.as.test.integration.logging.perdeploy;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.logging.util.AbstractLoggingTest;
import org.jboss.as.test.integration.logging.util.LoggingBean;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
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
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ServerSetup(LogContextTestCase.LoggingProfileSetup.class)
@RunWith(Arquillian.class)
public class LogContextTestCase extends AbstractLoggingTest {
    private static final String LOG_FILE_NAME = "log-context-profile-test.log";

    @Deployment(name = "logcontext", testable = false)
    public static WebArchive createDeployment() {
        return logContextDeployment("logcontext-test.war");
    }

    @Deployment(name = "logging", testable = false, managed = false)
    public static WebArchive createLoggingDeployment() {
        return loggingDeployment("logging-test.war")
                .addAsManifestResource(LoggingBean.class.getPackage(),
                        "logging.properties", "logging.properties");
    }

    @Deployment(name = "logging-ear", testable = false, managed = false)
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "logging.ear")
                .addAsManifestResource(LoggingBean.class.getPackage(),
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
                .addAsManifestResource(LoggingBean.class.getPackage(),
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
        final String u = UrlBuilder.of(url, LogContextHackServlet.SERVLET_URL).build();
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
                .addClasses(LoggingBean.class);
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

    static class LoggingProfileSetup extends AbstractMgmtServerSetupTask {
        private final Deque<ModelNode> tearDownOps;

        public LoggingProfileSetup() {
            tearDownOps = new ArrayDeque<ModelNode>();
        }

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            // Setup a logging-profile
            final ModelNode loggingProfileAddress = AddressBuilder.create()
                    .add("logging-profile", "test")
                    .build();

            // Add the profile and add a remove operation
            ModelNode op = Operations.createAddOperation(loggingProfileAddress);
            executeOperation(op);
            tearDownOps.addFirst(Operations.createRemoveOperation(loggingProfileAddress));

            final ModelNode fileHandlerAddress = AddressBuilder.create(loggingProfileAddress)
                    .add("file-handler", "test-handler")
                    .build();
            op = Operations.createAddOperation(fileHandlerAddress);
            op.get("append").set("true");
            final ModelNode file = op.get("file");
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(LOG_FILE_NAME);
            op.get("formatter").set("[test-profile] %d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
            executeOperation(op);
            tearDownOps.addFirst(Operations.createRemoveOperation(fileHandlerAddress));

            final ModelNode rootLoggerAddress = AddressBuilder.create(loggingProfileAddress)
                    .add("root-logger", "ROOT")
                    .build();
            op = Operations.createAddOperation(rootLoggerAddress);
            op.get("level").set("INFO");
            final ModelNode handlers = op.get("handlers").setEmptyList();
            handlers.add("test-handler");
            executeOperation(op);
            tearDownOps.addFirst(Operations.createRemoveOperation(rootLoggerAddress));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op;
            while ((op = tearDownOps.poll()) != null) {
                try {
                    executeOperation(op);
                } catch (Exception ignore) {
                }
            }
        }
    }

}
