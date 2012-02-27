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
package org.jboss.as.test.smoke.mgmt.servermodule;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.smoke.mgmt.servermodule.archive.sar.Simple;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * Tests deployment to a standalone server, both via the client API and by the
 * filesystem scanner.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServerInModuleDeploymentTestCase {

    //need for ArquillianResource injection to work correctly
    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @ArquillianResource
    private ManagementClient managementClient;

    @Test
    public void testDeploymentStreamApi() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final ServerDeploymentManager manager = ServerDeploymentManager.Factory
                .create(InetAddress.getByName(managementClient.getMgmtAddress()), managementClient.getMgmtPort(), getCallbackHandler());
        final ModelControllerClient client = managementClient.getControllerClient();
        testDeployments(client, new DeploymentExecutor() {

            @Override
            public void initialDeploy() {
                final InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
                try {
                    Future<?> future = manager.execute(manager.newDeploymentPlan()
                            .add("test-deployment.sar", is).deploy("test-deployment.sar").build());
                    awaitDeploymentExecution(future);
                } finally {
                    if (is != null) try {
                        is.close();
                    } catch (IOException ignore) {
                        //
                    }
                }
            }

            @Override
            public void fullReplace() {
                final InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
                try {
                    Future<?> future = manager.execute(manager.newDeploymentPlan()
                            .replace("test-deployment.sar", is).build());
                    awaitDeploymentExecution(future);
                } finally {
                    if (is != null) try {
                        is.close();
                    } catch (IOException ignore) {
                        //
                    }
                }
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
                .create(InetAddress.getByName(managementClient.getMgmtAddress()), managementClient.getMgmtPort(), getCallbackHandler());
        final ModelControllerClient client = managementClient.getControllerClient();
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ZipExporter.class).exportTo(file, true);

        testDeployments(client, new DeploymentExecutor() {

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
    public void testFilesystemScannerRegistration() throws Exception {
        final File deployDir = createDeploymentDir("dummy");

        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        final String scannerName = "dummy";
        addDeploymentScanner(deployDir, client, scannerName, false);
        removeDeploymentScanner(client, scannerName);
        addDeploymentScanner(deployDir, client, scannerName, false);
        removeDeploymentScanner(client, scannerName);
    }

    @Test
    public void testFilesystemDeployment_Marker() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ZipExporter.class).exportTo(file, true);

        final File deployDir = createDeploymentDir("marker-deployments");

        ModelControllerClient client = managementClient.getControllerClient();
        final String scannerName = "markerZips";
        addDeploymentScanner(deployDir, client, scannerName, false);

        final File target = new File(deployDir, "test-deployment.sar");
        final File deployed = new File(deployDir, "test-deployment.sar.deployed");
        Assert.assertFalse(target.exists());

        testDeployments(client, new DeploymentExecutor() {
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
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!dodeploy.exists() && !isdeploying.exists()) {
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
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!dodeploy.exists() && !isdeploying.exists() && deployed.exists()) {
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

    }

    @Test
    public void testFilesystemDeployment_Auto() throws Exception {
        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final File dir = new File("target/archives");
        dir.mkdirs();
        final File file = new File(dir, "test-deployment.sar");
        archive.as(ZipExporter.class).exportTo(file, true);

        final File deployDir = createDeploymentDir("auto-deployments");

        ModelControllerClient client = managementClient.getControllerClient();
        final String scannerName = "autoZips";
        addDeploymentScanner(deployDir, client, scannerName, true);

        final File target = new File(deployDir, "test-deployment.sar");
        final File deployed = new File(deployDir, "test-deployment.sar.deployed");
        Assert.assertFalse(target.exists());

        testDeployments(client, new DeploymentExecutor() {
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

                Assert.assertTrue(file.exists());
            }

            @Override
            public void fullReplace() throws IOException {
                // The test is going to call this as soon as the deployment
                // sends a notification
                // but often before the scanner has completed the process
                // and deleted the
                // .isdeploying put down by deployment scanner. So pause a bit to
                // let that complete
                // so we don't end up having our own file deleted
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!isdeploying.exists()) {
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

                if (isdeploying.exists()) {
                    Assert.fail("initialDeploy step did not complete in a reasonably timely fashion");
                }

                // Copy file to deploy directory again
                initialDeploy();
            }

            @Override
            public void undeploy() {
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!isdeploying.exists() && deployed.exists()) {
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
                if (!deployed.exists()) {
                    Assert.fail("fullReplace step did not complete in a reasonably timely fashion");
                }

                // Delete file from deploy directory
                target.delete();
            }
        });
    }

    @Test
    public void testExplodedFilesystemDeployment() throws Exception {

        final File deployDir = createDeploymentDir("exploded-deployments");

        ModelControllerClient client = managementClient.getControllerClient();
        final String scannerName = "exploded";
        addDeploymentScanner(deployDir, client, scannerName, false);

        final JavaArchive archive = ShrinkWrapUtils.createJavaArchive("servermodule/test-deployment.sar",
                Simple.class.getPackage());
        final File dir = new File("target/archives");
        dir.mkdirs();
        archive.as(ExplodedExporter.class).exportExploded(deployDir);

        final File deployed = new File(deployDir, "test-deployment.sar.deployed");
        Assert.assertFalse(deployed.exists());

        testDeployments(client, new DeploymentExecutor() {
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
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!dodeploy.exists() && !isdeploying.exists()) {
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
                final File isdeploying = new File(deployDir, "test-deployment.sar.isdeploying");
                for (int i = 0; i < 500; i++) {
                    if (!dodeploy.exists() && !isdeploying.exists() && deployed.exists()) {
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
    }

    private ModelNode addDeploymentScanner(final File deployDir, final ModelControllerClient client, final String scannerName, final boolean autoDeployZipped)
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
        if (autoDeployZipped == false) {
            add.get("auto-deploy-zipped").set(false);
        }

        ModelNode result = client.execute(add);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.require(ModelDescriptionConstants.OUTCOME).asString());
        return result;
    }

    private void removeDeploymentScanner(final ModelControllerClient client, final String scannerName) throws IOException {

        ModelNode addr = new ModelNode();
        addr.add("subsystem", "deployment-scanner");
        addr.add("scanner", scannerName);
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).set(addr);
        client.execute(op);
    }

    private File createDeploymentDir(String dir) {
        final File deployDir = new File("target", dir);
        cleanFile(deployDir);
        deployDir.mkdirs();
        Assert.assertTrue(deployDir.exists());
        return deployDir;
    }

    private void testDeployments(ModelControllerClient client, DeploymentExecutor deploymentExecutor) throws Exception {
        final MBeanServerConnection mbeanServer = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL()).getMBeanServerConnection();
        final ObjectName name = new ObjectName("jboss.test:service=testdeployments");

        // NOTE: Use polling until we have jmx over remoting
        // final TestNotificationListener listener = new TestNotificationListener(name);
        // mbeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, null, null);
        try {
            // Initial deploy
            deploymentExecutor.initialDeploy();
            PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, name));

            //listener.await();
            Assert.assertNotNull(mbeanServer.getMBeanInfo(name));

            // Full replace
            // listener.reset(2);
            deploymentExecutor.fullReplace();
            PollingUtils.retryWithTimeout(10000, new PollingUtils.WaitForMBeanTask(mbeanServer, name));

            // listener.await();
            Assert.assertNotNull(mbeanServer.getMBeanInfo(name));

            // Undeploy
            // listener.reset(1);
            deploymentExecutor.undeploy();
            // listener.await();
            try {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 10000) {
                    mbeanServer.getMBeanInfo(name);
                    Thread.sleep(100);
                }
                Assert.fail("Should not have found MBean");
            } catch (Exception expected) {
            }
        } finally {
            //mbeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
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

}
