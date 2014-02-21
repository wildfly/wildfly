/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControlledProcessState.State;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.ServerOperationResolver;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.operations.ServerRestartRequiredServerConfigWriteAttributeHandler;
import org.jboss.as.server.operations.ServerProcessStateHandler;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReloadRequiredServerTestCase extends AbstractOperationTestCase {

    @Test
    public void testChangeServerGroupProfileMaster() throws Exception {
        testChangeServerGroupProfile(true);
    }

    @Test
    public void testChangeServerGroupProfileSlave() throws Exception {
        testChangeServerGroupProfile(false);
    }

    private void testChangeServerGroupProfile(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("profile-two");

        new ServerGroupProfileWriteAttributeHandler(master, null).execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }

    @Test
    public void testChangeServerGroupProfileNoChangeMaster() throws Exception {
        testChangeServerGroupProfileNoChange(true);
    }

    @Test
    public void testChangeServerGroupProfileNoChangeSlave() throws Exception {
        testChangeServerGroupProfileNoChange(false);
    }

    private void testChangeServerGroupProfileNoChange(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("profile-one");

        new ServerGroupProfileWriteAttributeHandler(master, null).execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerGroupInvalidProfileMaster() throws Exception {
        testChangeServerGroupInvalidProfile(true);
    }

    @Test
    public void testChangeServerGroupInvalidProfileSlave() throws Exception {
        testChangeServerGroupInvalidProfile(false);
    }

    private void testChangeServerGroupInvalidProfile(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("does-not-exist");

        new ServerGroupProfileWriteAttributeHandler(master, null).execute(operationContext, operation);

        operationContext.verify();

        if (!master) {
            Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
            Assert.assertTrue(operationContext.isReloadRequired());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testChangeServerConfigGroupMaster() throws Exception {
        testChangeServerConfigGroup(true);
    }

    @Test
    public void testChangeServerConfigGroupSlave() throws Exception {
        testChangeServerConfigGroup(false);
    }


    public void testChangeServerConfigGroup(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("group-two");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigGroupNoChangeMaster() throws Exception {
        testChangeServerConfigGroupNoChange(true);
    }

    @Test
    public void testChangeServerConfigGroupNoChangeSlave() throws Exception {
        testChangeServerConfigGroupNoChange(true);
    }

    public void testChangeServerConfigGroupNoChange(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("group-one");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigGroupBadGroupMaster() throws Exception {
        testChangeServerConfigGroupBadGroup(true);
    }

    @Test
    public void testChangeServerConfigGroupBadGroupSlave() throws Exception {
        testChangeServerConfigGroupBadGroup(false);
    }

    private void testChangeServerConfigGroupBadGroup(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("bad-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);

        operationContext.verify();

        if (!master) {
            Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testChangeServerConfigSocketBindingGroupMaster() throws Exception {
        testChangeServerConfigSocketBindingGroup(true);
    }

    @Test
    public void testChangeServerConfigSocketBindingGroupSlave() throws Exception {
        testChangeServerConfigSocketBindingGroup(false);
    }

    private void testChangeServerConfigSocketBindingGroup(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("binding-two");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createSocketBindingGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigSocketBindingGroupNoChangeMaster() throws Exception {
        testChangeServerConfigSocketBindingGroupNoChange(true);
    }

    @Test
    public void testChangeServerConfigSocketBindingGroupNoChangeSlave() throws Exception {
        testChangeServerConfigSocketBindingGroupNoChange(false);
    }

    private void testChangeServerConfigSocketBindingGroupNoChange(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("binding-one");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createSocketBindingGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);

    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigSocketBindingGroupBadGroupMaster() throws Exception {
        testChangeServerConfigSocketBindingGroupBadGroup(true);
    }

    @Test
    public void testChangeServerConfigSocketBindingGroupBadGroupSlave() throws Exception {
        testChangeServerConfigSocketBindingGroupBadGroup(false);
    }

    private void testChangeServerConfigSocketBindingGroupBadGroup(boolean master) throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("bad-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.createSocketBindingGroupInstance(new MockHostControllerInfo(master)).execute(operationContext, operation);

        operationContext.verify();

        if (!master) {
            Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testChangeServerConfigSocketBindingPortOffset() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(65535);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigSocketBindingPortOffsetNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_PORT_OFFSET).set(10);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(10);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }


    @Test
    public void testChangeServerConfigSocketBindingPortNegativeValue() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_PORT_OFFSET).set(10);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(-65535);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigSocketBindingPortOffsetBadPort() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(false, pa);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_PORT_OFFSET).set(10);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(65536);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
    }

    MockOperationContext getOperationContext(boolean serversOnly, final PathAddress operationAddress) {
        final Resource root = createRootResource();
        return new MockOperationContext(root, false, operationAddress);
    }

    private void checkServerOperationResolver(MockOperationContext context, ModelNode operation, PathAddress address, boolean expectServerOps) {
        Map<String, ProxyController> serverProxies = new HashMap<String, ProxyController>();
        serverProxies.put("server-one", new MockServerProxy());
        serverProxies.put("server-two", new MockServerProxy());
        serverProxies.put("server-three", new MockServerProxy());
        ServerOperationResolver resolver = new ServerOperationResolver("localhost", serverProxies);

        final Resource backup = context.root;
        context.root = getServerResolutionResource();
        try {
            Map<Set<ServerIdentity>, ModelNode> serverOps = resolver.getServerOperations(context, operation, address);
            if (expectServerOps) {
                Assert.assertEquals(1, serverOps.size());
                Set<ServerIdentity> ids = serverOps.entrySet().iterator().next().getKey();
                Assert.assertEquals(1, ids.size());

                ServerIdentity expected = new ServerIdentity("localhost", "group-one","server-one");
                assertEquals(expected, ids.iterator().next());

                ModelNode expectedOp = new ModelNode();

                expectedOp.get(OP).set(ServerProcessStateHandler.REQUIRE_RELOAD_OPERATION);
                expectedOp.get(OP_ADDR).setEmptyList();
                Assert.assertEquals(expectedOp, serverOps.get(ids));
            } else {
                Assert.assertEquals(0, serverOps.size());
            }
        } finally {
            context.root = backup;
        }
    }

    private Resource getServerResolutionResource() {

        final Resource result = Resource.Factory.create();
        final Resource host =  Resource.Factory.create();
        result.registerChild(PathElement.pathElement(HOST, "localhost"), host);
        final Resource serverOne = Resource.Factory.create();
        serverOne.getModel().get(GROUP).set("group-one");
        serverOne.getModel().get(SOCKET_BINDING_GROUP).set("group-one");
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-one"), serverOne);
        final Resource serverTwo = Resource.Factory.create();
        serverTwo.getModel().get(GROUP).set("nope");
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-two"), serverTwo);
        final Resource serverThree = Resource.Factory.create();
        serverThree.getModel().get(GROUP).set("nope");
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-three"), serverThree);

        return result;
    }

    private class MockServerProxy implements ProxyController {

        @Override
        public PathAddress getProxyNodeAddress() {
            return null;
        }

        @Override
        public void execute(ModelNode operation, OperationMessageHandler handler, ProxyOperationControl control,
                OperationAttachments attachments) {
        }

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
        protected MockOperationContext(final Resource root, final boolean booting, final PathAddress operationAddress) {
            super(root, booting, operationAddress);
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
    }
}
