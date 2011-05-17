/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.surefire.servermodule;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.modular.utils.ShrinkWrapUtils;
import org.jboss.as.test.surefire.servermodule.archive.sar.Simple;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests deployment to a standalone server, both via the client API and by the
 * filesystem scanner.
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Ignore("Test migrated to managed container")
public class ServerInModuleDeploymentTestCase extends AbstractServerInModuleTestCase {

    @Test
    public void testDeploymentStreamApi() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final ServerDeploymentManager manager = ServerDeploymentManager.Factory
                .create(InetAddress.getByName("localhost"), 9999);

        testDeployments(new DeploymentExecutor() {

            @Override
            public void initialDeploy() {
                Future<?> future = manager.execute(manager.newDeploymentPlan()
                        .add("test-deployment.sar", archive.as(ZipExporter.class).exportZip()).deploy("test-deployment.sar")
                        .build());
                awaitDeploymentExecution(future);
            }

            @Override
            public void fullReplace() {
                Future<?> future = manager.execute(manager.newDeploymentPlan()
                        .replace("test-deployment.sar", archive.as(ZipExporter.class).exportZip()).build());
                awaitDeploymentExecution(future);
            }

            @Override
            public void undeploy() {
                Future<?> future = manager.execute(manager.newDeploymentPlan().undeploy("test-deployment.sar")
                        .remove("test-deployment.sar").build());
                awaitDeploymentExecution(future);
            }
        });
    }

    @Test
    public void testDeploymentFileApi() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final ServerDeploymentManager manager = ServerDeploymentManager.Factory
                .create(InetAddress.getByName("localhost"), 9999);
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ZipExporter.class).exportZip(file, true);

        testDeployments(new DeploymentExecutor() {

            @Override
            public void initialDeploy() throws IOException {
                Future<?> future = manager.execute(manager.newDeploymentPlan().add("test-deployment.sar", file)
                        .deploy("test-deployment.sar").build());
                awaitDeploymentExecution(future);
            }

            @Override
            public void fullReplace() throws IOException {
                Future<?> future = manager.execute(manager.newDeploymentPlan().replace("test-deployment.sar", file).build());
                awaitDeploymentExecution(future);
            }

            @Override
            public void undeploy() {
                Future<?> future = manager.execute(manager.newDeploymentPlan().undeploy("test-deployment.sar")
                        .remove("test-deployment.sar").build());
                awaitDeploymentExecution(future);
            }
        });
    }

    @Test
    public void testFilesystemDeployment() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ZipExporter.class).exportZip(file, true);

        final File deployDir = createDeploymentDir("deployments");

        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        ModelNode result = addDeploymentScanner(deployDir, client, "zips");

        try {
            final File target = new File(deployDir, "test-deployment.sar");
            final File deployed = new File(deployDir, "test-deployment.sar.deployed");
            Assert.assertFalse(target.exists());

            testDeployments(new DeploymentExecutor() {
                @Override
                public void initialDeploy() throws IOException {
                    // Copy file to deploy directory
                    final InputStream in = new BufferedInputStream(new FileInputStream(file));
                    try {
                        final OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
                        try {
                            int i = in.read();
                            while (i != -1) {
                                out.write(i);
                                i = in.read();
                            }
                        } finally {
                            StreamUtils.safeClose(out);
                        }
                    } finally {
                        StreamUtils.safeClose(in);
                    }
                    // Create the .dodeploy file
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(dodeploy));
                    try {
                        out.write("test-deployment.sar".getBytes());
                    } finally {
                        StreamUtils.safeClose(out);
                    }
                    Assert.assertTrue(dodeploy.exists());
                }

                @Override
                public void fullReplace() throws IOException {
                    // The test is going to call this as soon as the deployment
                    // sends a notification
                    // but often before the scanner has completed the process
                    // and deleted the
                    // .dodpeloy put down by initialDeploy(). So pause a bit to
                    // let that complete
                    // so we don't end up having our own file deleted
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    for (int i = 0; i < 500; i++) {
                        if (!dodeploy.exists()) {
                            break;
                        }
                        // Wait for the last action to complete :(
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    if (dodeploy.exists()) {
                        Assert.fail("initialDeploy step did not complete in a reasonably timely fashion");
                    }

                    // Copy file to deploy directory again
                    initialDeploy();
                }

                @Override
                public void undeploy() {
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    for (int i = 0; i < 500; i++) {
                        if (!dodeploy.exists() && deployed.exists()) {
                            break;
                        }
                        // Wait for the last action to complete :(
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    if (dodeploy.exists() || !deployed.exists()) {
                        Assert.fail("fullReplace step did not complete in a reasonably timely fashion");
                    }

                    // Delete file from deploy directory
                    deployed.delete();
                }
            });
        } finally {
            try {
                client.execute(result.get(ModelDescriptionConstants.COMPENSATING_OPERATION));
            } catch (Exception e) {
                client.close();
            }
        }
    }

    @Test
    public void testExplodedFilesystemDeployment() throws Exception {

        final File deployDir = createDeploymentDir("exploded-deployments");

        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        ModelNode result = addDeploymentScanner(deployDir, client, "exploded");

        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ExplodedExporter.class).exportExploded(deployDir);

        try {
            final File deployed = new File(deployDir, "test-deployment.sar.deployed");
            Assert.assertFalse(deployed.exists());

            testDeployments(new DeploymentExecutor() {
                @Override
                public void initialDeploy() throws IOException {

                    // Create the .dodeploy file
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(dodeploy));
                    try {
                        out.write("test-deployment.sar".getBytes());
                    } finally {
                        StreamUtils.safeClose(out);
                    }
                    Assert.assertTrue(dodeploy.exists());
                }

                @Override
                public void fullReplace() throws IOException {
                    // The test is going to call this as soon as the deployment
                    // sends a notification
                    // but often before the scanner has completed the process
                    // and deleted the
                    // .dodpeloy put down by initialDeploy(). So pause a bit to
                    // let that complete
                    // so we don't end up having our own file deleted
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    for (int i = 0; i < 500; i++) {
                        if (!dodeploy.exists()) {
                            break;
                        }
                        // Wait for the last action to complete :(
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    if (dodeploy.exists()) {
                        Assert.fail("initialDeploy step did not complete in a reasonably timely fashion");
                    }

                    // Copy file to deploy directory again
                    initialDeploy();
                }

                @Override
                public void undeploy() {
                    final File dodeploy = new File(deployDir, "test-deployment.sar.dodeploy");
                    for (int i = 0; i < 500; i++) {
                        if (!dodeploy.exists() && deployed.exists()) {
                            break;
                        }
                        // Wait for the last action to complete :(
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    if (dodeploy.exists() || !deployed.exists()) {
                        Assert.fail("fullReplace step did not complete in a reasonably timely fashion");
                    }

                    // Delete file from deploy directory
                    deployed.delete();
                }
            });
        } finally {
            try {
                client.execute(result.get(ModelDescriptionConstants.COMPENSATING_OPERATION));
            } catch (Exception e) {
                client.close();
            }
        }
    }

    private ModelNode addDeploymentScanner(final File deployDir, final ModelControllerClient client, final String scannerName)
            throws IOException {
        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        ModelNode addr = new ModelNode();
        addr.add("subsystem", "deployment-scanner");
        addr.add("scanner", scannerName);
        add.get(OP_ADDR).set(addr);
        add.get("path").set(deployDir.getAbsolutePath());
        add.get("scan-enabled").set(true);
        add.get("scan-interval").set(1000);

        ModelNode result = client.execute(add);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.require(ModelDescriptionConstants.OUTCOME).asString());
        return result;
    }

    private File createDeploymentDir(String dir) {
        final File deployDir = new File("target", dir);
        cleanFile(deployDir);
        deployDir.mkdirs();
        Assert.assertTrue(deployDir.exists());
        return deployDir;
    }

    private void testDeployments(DeploymentExecutor deploymentExecutor) throws Exception {
        final MBeanServerConnectionProvider provider = new MBeanServerConnectionProvider(InetAddress.getLocalHost(), 1090);
        final MBeanServerConnection mbeanServer = provider.getConnection();
        final ObjectName name = new ObjectName("jboss.test:service=testdeployments");
        final TestNotificationListener listener = new TestNotificationListener(name);
        mbeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, null, null);
        try {
            // Initial deploy
            deploymentExecutor.initialDeploy();
            listener.await();
            Assert.assertNotNull(mbeanServer.getMBeanInfo(name));

            // Full replace
            listener.reset(2);
            deploymentExecutor.fullReplace();
            listener.await();
            Assert.assertNotNull(mbeanServer.getMBeanInfo(name));

            // Undeploy
            listener.reset(1);
            deploymentExecutor.undeploy();
            listener.await();
            try {
                mbeanServer.getMBeanInfo(name);
                Assert.fail("Should not have found MBean");
            } catch (InstanceNotFoundException expected) {
            }
        } finally {
            mbeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
        }

    }

    private void awaitDeploymentExecution(Future<?> future) {
        try {
            future.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    private static void cleanFile(File toClean) {
        if (toClean.isDirectory()) {
            for (File child : toClean.listFiles())
                cleanFile(child);
        }
        toClean.delete();
    }

    private interface DeploymentExecutor {
        void initialDeploy() throws IOException;

        void fullReplace() throws IOException;

        void undeploy() throws IOException;
    }

    private static class TestNotificationListener implements NotificationListener {
        private final ObjectName name;
        private volatile CountDownLatch latch = new CountDownLatch(1);

        private TestNotificationListener(ObjectName name) {
            this.name = name;
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification instanceof MBeanServerNotification == false) {
                return;
            }

            MBeanServerNotification mBeanServerNotification = (MBeanServerNotification) notification;
            if (!name.equals(mBeanServerNotification.getMBeanName())) {
                return;
            }

            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mBeanServerNotification.getType())) {
                latch.countDown();
            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mBeanServerNotification.getType())) {
                latch.countDown();
            }
        }

        void reset(int i) {
            latch = new CountDownLatch(i);
        }

        void await() throws Exception {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                Assert.fail("Timed out waiting for registration/unregistration");
            }
        }
    }

}
