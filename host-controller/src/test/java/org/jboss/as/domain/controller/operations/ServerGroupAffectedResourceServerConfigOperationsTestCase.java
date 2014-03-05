/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ControlledProcessState.State;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.MasterDomainControllerClient;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerRestartRequiredServerConfigWriteAttributeHandler;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartException;
import org.jboss.threads.AsyncFuture;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupAffectedResourceServerConfigOperationsTestCase extends AbstractOperationTestCase {

    @Test
    public void testAddServerConfigMaster() throws Exception {
        testAddServerConfig(true, false);
    }

    @Test
    public void testAddServerConfigSlave() throws Exception {
        testAddServerConfig(false, false);
    }

    @Test
    public void testAddServerConfigMasterRollback() throws Exception {
        testAddServerConfig(true, true);
    }

    @Test
    public void testAddServerConfigSlaveRollback() throws Exception {
        testAddServerConfig(false, true);
    }

    private void testAddServerConfig(boolean master, boolean rollback) throws Exception {
        testAddServerConfigBadInfo(master, rollback, false, SocketBindingGroupOverrideType.GOOD);
    }

    @Test
    public void testAddServerConfigNoSocketBindingGroupOverrideMaster() throws Exception {
        testAddServerConfigNoSocketBindingGroupOverride(true, false);
    }

    @Test
    public void testAddServerConfigNoSocketBindingGroupOverrideSlave() throws Exception {
        testAddServerConfigNoSocketBindingGroupOverride(false, false);
    }

    @Test
    public void testAddServerConfigNoSocketBindingGroupOverrideMasterRollback() throws Exception {
        testAddServerConfigNoSocketBindingGroupOverride(true, true);
    }

    @Test
    public void testAddServerConfigNoSocketBindingGroupOverrideSlaveRollback() throws Exception {
        testAddServerConfigNoSocketBindingGroupOverride(false, true);
    }

    private void testAddServerConfigNoSocketBindingGroupOverride(boolean master, boolean rollback) throws Exception {
        testAddServerConfigBadInfo(master, rollback, false, SocketBindingGroupOverrideType.NONE);
    }


    @Test(expected=OperationFailedException.class)
    public void testAddServerConfigBadServerGroupeMaster() throws Exception {
        testAddServerConfigBadInfo(true, false, true, SocketBindingGroupOverrideType.GOOD);
    }

    @Test
    public void testAddServerConfigBadServerGroupSlave() throws Exception {
        testAddServerConfigBadInfo(false, false, true, SocketBindingGroupOverrideType.GOOD);
    }

    @Test(expected=OperationFailedException.class)
    public void testAddServerConfigBadServerGroupMasterRollback() throws Exception {
        //This won't actually get to the rollback part
        testAddServerConfigBadInfo(true, true, true, SocketBindingGroupOverrideType.GOOD);
    }

    @Test
    public void testAddServerConfigBadServerGroupSlaveRollback() throws Exception {
        testAddServerConfigBadInfo(false, true, true, SocketBindingGroupOverrideType.GOOD);
    }

    @Test(expected=OperationFailedException.class)
    public void testAddServerConfigBadSocketBindingGroupOverrideMaster() throws Exception {
        testAddServerConfigBadInfo(true, false, false, SocketBindingGroupOverrideType.BAD);
    }

    @Test
    public void testAddServerConfigBadSocketBindingGroupOverrideSlave() throws Exception {
        testAddServerConfigBadInfo(false, false, false, SocketBindingGroupOverrideType.BAD);
    }

    @Test(expected=OperationFailedException.class)
    public void testAddServerConfigBadSocketBindingGroupOverrideMasterRollback() throws Exception {
        //This won't actually get to the rollback part
        testAddServerConfigBadInfo(true, true, false, SocketBindingGroupOverrideType.BAD);
    }

    @Test
    public void testAddServerConfigBadSocketBindingGroupOverrideSlaveRollback() throws Exception {
        testAddServerConfigBadInfo(false, true, false, SocketBindingGroupOverrideType.BAD);
    }

    private void testAddServerConfigBadInfo(boolean master, boolean rollback, boolean badServerGroup, SocketBindingGroupOverrideType socketBindingGroupOverride) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_GROUP, "server-four"));
        final MockOperationContext operationContext = getOperationContext(rollback, pa);

        String serverGroupName = badServerGroup ? "bad-server-group" : "group-one";
        String socketBindingGroupName;
        if (socketBindingGroupOverride == SocketBindingGroupOverrideType.GOOD) {
            socketBindingGroupName = "binding-two";
        } else if (socketBindingGroupOverride == SocketBindingGroupOverrideType.BAD) {
            socketBindingGroupName = "bad-socket-binding-group";
        } else {
            socketBindingGroupName = null;
        }

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(ADD);
        operation.get(GROUP).set(serverGroupName);
        if (socketBindingGroupName != null) {
            operation.get(SOCKET_BINDING_GROUP).set(socketBindingGroupName);
        }

        ServerAddHandler.create(new MockHostControllerInfo(master)).execute(operationContext, operation);

        if (master && (socketBindingGroupOverride == SocketBindingGroupOverrideType.BAD || badServerGroup)) {
            Assert.fail();
        }

        Assert.assertFalse(operationContext.isReloadRequired());
    }


    @Test
    public void testRemoveServerConfigMaster() throws Exception {
        testRemoveServerConfig(true, false);
    }

    @Test
    public void testRemoveServerConfigSlave() throws Exception {
        testRemoveServerConfig(false, false);
    }

    @Test
    public void testRemoveServerConfigMasterRollback() throws Exception {
        testRemoveServerConfig(true, true);
    }

    @Test
    public void testRemoveServerConfigSlaveRollback() throws Exception {
        testRemoveServerConfig(false, true);
    }

    private void testRemoveServerConfig(boolean master, boolean rollback) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(rollback, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(REMOVE);

        ServerRemoveHandler.INSTANCE.execute(operationContext, operation);

        Assert.assertFalse(operationContext.isReloadRequired());
    }

    @Test
    public void testUpdateServerConfigServerGroupMaster() throws Exception {
        testUpdateServerConfigServerGroup(true, false, false);
    }

    @Test
    public void testUpdateServerConfigServerGroupSlave() throws Exception {
        testUpdateServerConfigServerGroup(false, false, false);

    }

    @Test
    public void testUpdateServerConfigServerGroupMasterRollback() throws Exception {
        testUpdateServerConfigServerGroup(true, true, false);

    }

    @Test
    public void testUpdateServerConfigServerGroupSlaveRollback() throws Exception {
        testUpdateServerConfigServerGroup(false, true, false);
    }


    @Test(expected=OperationFailedException.class)
    public void testUpdateServerConfigBadServerGroupMaster() throws Exception {
        testUpdateServerConfigServerGroup(true, false, true);
    }

    @Test
    public void testUpdateServerConfigBadServerGroupSlave() throws Exception {
        testUpdateServerConfigServerGroup(false, false, true);
    }

    @Test(expected=OperationFailedException.class)
    public void testUpdateServerConfigBadServerGroupMasterRollback() throws Exception {
        testUpdateServerConfigServerGroup(true, true, true);
    }

    @Test
    public void testUpdateServerConfigBadServerGroupSlaveRollback() throws Exception {
        testUpdateServerConfigServerGroup(false, true, true);
    }

    private void testUpdateServerConfigServerGroup(boolean master, boolean rollback, boolean badGroup) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(rollback, pa);

        String groupName = badGroup ? "bad-group" : "group-two";

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set(groupName);

        ServerRestartRequiredServerConfigWriteAttributeHandler.createGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);

        if (master && badGroup) {
            //master will throw an exception
            Assert.fail();
        }

        Assert.assertFalse(operationContext.isReloadRequired());
    }

    @Test
    public void testUpdateServerConfigSocketBindingGroupMaster() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, false, SocketBindingGroupOverrideType.GOOD);
    }

    @Test
    public void testUpdateServerConfigSocketBindingGroupSlave() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, false, SocketBindingGroupOverrideType.GOOD);

    }

    @Test
    public void testUpdateServerConfigSocketBindingGroupMasterRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, true, SocketBindingGroupOverrideType.GOOD);

    }

    @Test
    public void testUpdateServerConfigSocketBindingGroupSlaveRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, true, SocketBindingGroupOverrideType.GOOD);
    }


    @Test(expected=OperationFailedException.class)
    public void testUpdateServerConfigBadSocketBindingGroupMaster() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, false, SocketBindingGroupOverrideType.BAD);
    }

    @Test
    public void testUpdateServerConfigBadSocketBindingGroupSlave() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, false, SocketBindingGroupOverrideType.BAD);
    }

    @Test(expected=OperationFailedException.class)
    public void testUpdateServerConfigBadSocketBindingGroupMasterRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, true, SocketBindingGroupOverrideType.BAD);
    }

    @Test
    public void testUpdateServerConfigBadSocketBindingGroupSlaveRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, true, SocketBindingGroupOverrideType.BAD);
    }


    public void testUpdateServerConfigNoSocketBindingGroupMaster() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, false, SocketBindingGroupOverrideType.NONE);
    }

    @Test
    public void testUpdateServerConfigNoSocketBindingGroupSlave() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, false, SocketBindingGroupOverrideType.NONE);
    }

    public void testUpdateServerConfigNoSocketBindingGroupMasterRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(true, true, SocketBindingGroupOverrideType.NONE);
    }

    @Test
    public void testUpdateServerConfigNoSocketBindingGroupSlaveRollback() throws Exception {
        testUpdateServerConfigSocketBindingGroup(false, true, SocketBindingGroupOverrideType.NONE);
    }

    private void testUpdateServerConfigSocketBindingGroup(boolean master, boolean rollback, SocketBindingGroupOverrideType socketBindingGroupOverride) throws Exception {

        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(rollback, pa);

        String socketBindingGroupName;
        if (socketBindingGroupOverride == SocketBindingGroupOverrideType.GOOD) {
            socketBindingGroupName = "binding-two";
        } else if (socketBindingGroupOverride == SocketBindingGroupOverrideType.BAD) {
            socketBindingGroupName = "bad-socket-binding-group";
        } else {
            socketBindingGroupName = null;
        }

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set(socketBindingGroupName != null ? new ModelNode(socketBindingGroupName) : new ModelNode());

        ServerRestartRequiredServerConfigWriteAttributeHandler.createSocketBindingGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);

        if (master && socketBindingGroupOverride == SocketBindingGroupOverrideType.BAD) {
            //master will throw an exception
            Assert.fail();
        }

        Assert.assertFalse(operationContext.isReloadRequired());
    }

    MockOperationContext getOperationContext(final boolean rollback, final PathAddress operationAddress) {
        final Resource root = createRootResource();
        return new MockOperationContext(root, false, operationAddress, rollback);
    }

    private static class MockHostControllerInfo implements LocalHostControllerInfo {
        private final boolean master;
        public MockHostControllerInfo(boolean master) {
            this.master = master;
        }

        @Override
        public String getLocalHostName() {
            return null;
        }

        @Override
        public boolean isMasterDomainController() {
            return master;
        }

        @Override
        public String getNativeManagementInterface() {
            return null;
        }

        @Override
        public int getNativeManagementPort() {
            return 0;
        }

        @Override
        public String getNativeManagementSecurityRealm() {
            return null;
        }

        @Override
        public String getHttpManagementInterface() {
            return null;
        }

        @Override
        public int getHttpManagementPort() {
            return 0;
        }

        @Override
        public String getHttpManagementSecureInterface() {
            return null;
        }

        @Override
        public int getHttpManagementSecurePort() {
            return 0;
        }

        @Override
        public String getHttpManagementSecurityRealm() {
            return null;
        }

        @Override
        public String getRemoteDomainControllerUsername() {
            return null;
        }

        @Override
        public List<DiscoveryOption> getRemoteDomainControllerDiscoveryOptions() {
            return null;
        }

        @Override
        public State getProcessState() {
            return null;
        }

        @Override
        public boolean isRemoteDomainControllerIgnoreUnaffectedConfiguration() {
            return true;
        }
    }

    private class MockOperationContext extends AbstractOperationTestCase.MockOperationContext {
        private boolean reloadRequired;
        private boolean rollback;
        private OperationStepHandler nextStep;

        protected MockOperationContext(final Resource root, final boolean booting, final PathAddress operationAddress, final boolean rollback) {
            super(root, booting, operationAddress);
            this.rollback = rollback;
        }

        public void completeStep(ResultHandler resultHandler) {
            if (nextStep != null) {
                stepCompleted();
            } else if (rollback) {
                resultHandler.handleResult(ResultAction.ROLLBACK, this, null);
            }
        }

        public void stepCompleted() {
            if (nextStep != null) {
                try {
                    OperationStepHandler step = nextStep;
                    nextStep = null;
                    step.execute(this, null);
                } catch (OperationFailedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void reloadRequired() {
            reloadRequired = true;
        }

        public boolean isReloadRequired() {
            return reloadRequired;
        }

        public void revertReloadRequired() {
            reloadRequired = false;
        }

        public void addStep(OperationStepHandler step, OperationContext.Stage stage) throws IllegalArgumentException {
            nextStep = step;
        }

        public void addStep(ModelNode operation, OperationStepHandler step, OperationContext.Stage stage) throws IllegalArgumentException {
            if (operation.get(OP).asString().equals("verify-running-server")) {
                return;
            }
            super.addStep(operation, step, stage);
        }

        @Override
        public boolean isBooting() {
            return false;
        }

        @Override
        public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
            return new ServiceRegistry() {

                @Override
                public List<ServiceName> getServiceNames() {
                    return null;
                }

                @Override
                public ServiceController<?> getService(ServiceName name) {
                    return null;
                }

                @Override
                public ServiceController<?> getRequiredService(ServiceName name) throws ServiceNotFoundException {
                    if (name.equals(MasterDomainControllerClient.SERVICE_NAME)) {
                        return new ServiceController<MasterDomainControllerClient>() {

                            @Override
                            public void addListener(ServiceListener<? super MasterDomainControllerClient> arg0) {
                            }

                            @Override
                            public MasterDomainControllerClient awaitValue() throws IllegalStateException, InterruptedException {
                                return null;
                            }

                            @Override
                            public MasterDomainControllerClient awaitValue(long arg0, TimeUnit arg1)
                                    throws IllegalStateException, InterruptedException, TimeoutException {
                                return null;
                            }

                            @Override
                            public boolean compareAndSetMode(org.jboss.msc.service.ServiceController.Mode arg0,
                                    org.jboss.msc.service.ServiceController.Mode arg1) {
                                return false;
                            }

                            @Override
                            public ServiceName[] getAliases() {
                                return null;
                            }

                            @Override
                            public Set<ServiceName> getImmediateUnavailableDependencies() {
                                return null;
                            }

                            @Override
                            public org.jboss.msc.service.ServiceController.Mode getMode() {
                                return null;
                            }

                            @Override
                            public ServiceName getName() {
                                return null;
                            }

                            @Override
                            public ServiceController<?> getParent() {
                                return null;
                            }

                            @Override
                            public Service<MasterDomainControllerClient> getService() throws IllegalStateException {
                                return null;
                            }

                            @Override
                            public ServiceContainer getServiceContainer() {
                                return null;
                            }

                            @Override
                            public StartException getStartException() {
                                return null;
                            }

                            @Override
                            public org.jboss.msc.service.ServiceController.State getState() {
                                return null;
                            }

                            @Override
                            public org.jboss.msc.service.ServiceController.Substate getSubstate() {
                                return null;
                            }

                            @Override
                            public MasterDomainControllerClient getValue() throws IllegalStateException {
                                return new MasterDomainControllerClient() {

                                    @Override
                                    public void close() throws IOException {
                                    }

                                    @Override
                                    public AsyncFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
                                        return null;
                                    }

                                    @Override
                                    public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
                                        return null;
                                    }

                                    @Override
                                    public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
                                        return null;
                                    }

                                    @Override
                                    public ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException {
                                        return null;
                                    }

                                    @Override
                                    public ModelNode execute(Operation operation) throws IOException {
                                        return null;
                                    }

                                    @Override
                                    public ModelNode execute(ModelNode operation) throws IOException {
                                        return null;
                                    }

                                    @Override
                                    public void unregister() {
                                    }

                                    @Override
                                    public void fetchDomainWideConfiguration() {
                                    }

                                    @Override
                                    public void register() throws IOException {
                                    }

                                    @Override
                                    public HostFileRepository getRemoteFileRepository() {
                                        return null;
                                    }

                                    @Override
                                    public void pullDownDataForUpdatedServerConfigAndApplyToModel(OperationContext context,
                                            String serverName, String serverGroupName, String socketBindingGroupName)
                                            throws OperationFailedException {
                                        if (root.getChild(PathElement.pathElement(SERVER_GROUP, serverGroupName)) == null) {
                                            root.registerChild(PathElement.pathElement(SERVER_GROUP, serverGroupName), Resource.Factory.create());
                                        }
                                        if (socketBindingGroupName != null) {
                                            if (root.getChild(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroupName)) == null) {
                                                root.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroupName), Resource.Factory.create());
                                            }
                                        }
                                    }
                                };
                            }

                            @Override
                            public void removeListener(ServiceListener<? super MasterDomainControllerClient> arg0) {
                            }

                            @Override
                            public void retry() {
                            }

                            @Override
                            public void setMode(org.jboss.msc.service.ServiceController.Mode arg0) {
                            }

                        };
                    }
                    throw new ServiceNotFoundException();
                }
            };
        }
    }

    private enum SocketBindingGroupOverrideType {
        GOOD,
        BAD,
        NONE
    }
}
