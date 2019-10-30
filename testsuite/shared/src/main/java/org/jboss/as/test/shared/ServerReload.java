/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.shared;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.xnio.IoUtils;

/**
 * @author Stuart Douglas
 */
public class ServerReload {

    private static final Logger log = Logger.getLogger(ServerReload.class);

    public static final int TIMEOUT = 100000;

    public static void executeReloadAndWaitForCompletion(ModelControllerClient client) {
        executeReloadAndWaitForCompletion(client, TIMEOUT);
    }

    public static void executeReloadAndWaitForCompletion(ModelControllerClient client, String serverConfig) {
        executeReloadAndWaitForCompletion(client, TIMEOUT, false, null, -1, serverConfig);
    }
    public static void executeReloadAndWaitForCompletion(ModelControllerClient client, boolean adminOnly) {
        executeReloadAndWaitForCompletion(client, TIMEOUT, adminOnly, null, -1);
    }

    public static void executeReloadAndWaitForCompletion(ModelControllerClient client, int timeout) {
        executeReloadAndWaitForCompletion(client, timeout, false, null, -1);
    }

    public static void executeReloadAndWaitForCompletion(ManagementClient managementClient) {
        executeReloadAndWaitForCompletion(managementClient.getControllerClient(), TIMEOUT, false, managementClient.getMgmtAddress(), managementClient.getMgmtPort());
    }

    /**
     *
     * @param client
     * @param timeout
     * @param adminOnly if {@code true}, the server will be reloaded in admin-only mode
     * @param serverAddress if {@code null}, use {@code TestSuiteEnvironment.getServerAddress()} to create the ModelControllerClient
     * @param serverPort if {@code -1}, use {@code TestSuiteEnvironment.getServerPort()} to create the ModelControllerClient
     */
    public static void executeReloadAndWaitForCompletion(ModelControllerClient client, int timeout, boolean adminOnly, String serverAddress, int serverPort) {
        executeReload(client, adminOnly, null);
        waitForLiveServerToReload(timeout,
                serverAddress != null ? serverAddress : TestSuiteEnvironment.getServerAddress(),
                serverPort != -1 ? serverPort : TestSuiteEnvironment.getServerPort());
    }
    /**
     *
     * @param client
     * @param timeout
     * @param adminOnly if {@code true}, the server will be reloaded in admin-only mode
     * @param serverAddress if {@code null}, use {@code TestSuiteEnvironment.getServerAddress()} to create the ModelControllerClient
     * @param serverPort if {@code -1}, use {@code TestSuiteEnvironment.getServerPort()} to create the ModelControllerClient
     */
    public static void executeReloadAndWaitForCompletion(ModelControllerClient client, int timeout, boolean adminOnly, String serverAddress, int serverPort, String serverConfig) {
        executeReload(client, adminOnly, serverConfig);
        waitForLiveServerToReload(timeout,
                serverAddress != null ? serverAddress : TestSuiteEnvironment.getServerAddress(),
                serverPort != -1 ? serverPort : TestSuiteEnvironment.getServerPort());
    }

    private static void executeReload(ModelControllerClient client, boolean adminOnly, String serverConfig) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        operation.get("admin-only").set(adminOnly);
        if(serverConfig != null) {
            operation.get("server-config").set(serverConfig);
        }
        try {
            ModelNode result = client.execute(operation);
            if (!"success".equals(result.get(ClientConstants.OUTCOME).asString())) {
                fail("Reload operation didn't finish successfully: " + result.asString());
            }
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw new RuntimeException(e);
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
    }

    private static void waitForLiveServerToReload(int timeout, String serverAddress, int serverPort) {
        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < timeout) {
            //do the sleep before we check, as the attribute state may not change instantly
            //also reload generally takes longer than 100ms anyway
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            try {
                ModelControllerClient liveClient = ModelControllerClient.Factory.create(
                        serverAddress, serverPort);
                try {
                    ModelNode result = liveClient.execute(operation);
                    if ("running" .equals(result.get(RESULT).asString())) {
                        return;
                    }
                } catch (IOException e) {
                } finally {
                    IoUtils.safeClose(liveClient);
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        fail("Live Server did not reload in the imparted time.");
    }

    public static String getContainerRunningState(ManagementClient managementClient) throws IOException {
        return getContainerRunningState(managementClient.getControllerClient());
    }

    public static String getContainerRunningState(ModelControllerClient modelControllerClient) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        ModelNode rsp = modelControllerClient.execute(operation);
        return SUCCESS.equals(rsp.get(OUTCOME).asString()) ? rsp.get(RESULT).asString() : FAILED;
    }


    /**
     * Checks if the container status is "reload-required" and if it's the case executes reload and waits for completion.
     * Otherwise
     */
    public static void reloadIfRequired(final ModelControllerClient controllerClient) throws Exception {
        String runningState = getContainerRunningState(controllerClient);
        if ("reload-required".equalsIgnoreCase(runningState)) {
            log.trace("Server reload is required. The reload will be executed.");
            executeReloadAndWaitForCompletion(controllerClient);
        } else {
            log.debugf("Server reload is not required; server-state is %s", runningState);
            Assert.assertEquals("Server state 'running' is expected", "running", runningState);
        }
    }

    public static void reloadIfRequired(final ManagementClient managementClient) throws Exception {
        String runningState = getContainerRunningState(managementClient);
        if ("reload-required".equalsIgnoreCase(runningState)) {
            log.trace("Server reload is required. The reload will be executed.");
            executeReloadAndWaitForCompletion(managementClient);
        } else {
            log.debugf("Server reload is not required; server-state is %s", runningState);
            Assert.assertEquals("Server state 'running' is expected", "running", runningState);
        }
    }

    /**
     * {@link ServerSetupTask} that if necessary calls
     * {@link #executeReloadAndWaitForCompletion(ModelControllerClient)} in the {@code setup} method
     */
    public static class BeforeSetupTask extends SetupTask {

        public static final BeforeSetupTask INSTANCE = new BeforeSetupTask();

        public BeforeSetupTask() {
            super(true, false);
        }
    }

    /**
     * {@link ServerSetupTask} that if necessary calls
     * {@link #executeReloadAndWaitForCompletion(ModelControllerClient)} in the {@code tearDown} method
     */
    public static class AfterSetupTask extends SetupTask {

        public static final AfterSetupTask INSTANCE = new AfterSetupTask();

        public AfterSetupTask() {
            super(false, true);
        }
    }

    private static class SetupTask implements ServerSetupTask {

        private final boolean before;
        private final boolean after;

        private SetupTask(boolean before, boolean after) {
            this.before = before;
            this.after = after;
        }

        /**
         * If required, calls {@link #executeReloadAndWaitForCompletion(ModelControllerClient)}.
         *
         * {@inheritDoc}
         */
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            if (before) {
                reloadIfRequired(managementClient.getControllerClient());
            }
        }

        /**
         * If required, calls {@link #executeReloadAndWaitForCompletion(ModelControllerClient)}.
         *
         * {@inheritDoc}
         */
        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            if (after) {
                reloadIfRequired(managementClient.getControllerClient());
            }
        }
    }
}
