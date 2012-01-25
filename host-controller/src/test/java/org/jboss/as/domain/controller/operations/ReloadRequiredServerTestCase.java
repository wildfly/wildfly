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

import static junit.framework.Assert.assertEquals;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.ServerOperationResolver;
import org.jboss.as.host.controller.operations.ServerRestartRequiredServerConfigWriteAttributeHandler;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReloadRequiredServerTestCase extends AbstractOperationTestCase {

    @Test
    public void testChangeServerGroupProfile() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("old");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final Resource profileConfig = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), profileConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("some-profile");

        ServerGroupProfileWriteAttributeHandler.INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }

    @Test
    public void testChangeServerGroupProfileNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("some-profile");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final Resource profileConfig = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), profileConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("some-profile");

        ServerGroupProfileWriteAttributeHandler.INSTANCE.execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerGroupInvalidProfile() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("old");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("does-not-exist");

        ServerGroupProfileWriteAttributeHandler.INSTANCE.execute(operationContext, operation);

        operationContext.verify();
    }

    @Test
    public void testChangeServerConfigGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "new-group"), serverConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("new-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.GROUP_INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigGroupNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("group-one");

        ServerRestartRequiredServerConfigWriteAttributeHandler.GROUP_INSTANCE.execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigGroupBadGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("bad-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.GROUP_INSTANCE.execute(operationContext, operation);
    }

    @Test
    public void testChangeServerConfigSocketBindingGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "new-group"), serverConfig);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_GROUP).set("old-group");

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("new-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_GROUP_INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigSocketBindingGroupNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "old-group"), serverConfig);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_GROUP).set("old-group");
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("old-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_GROUP_INSTANCE.execute(operationContext, operation);
        Assert.assertTrue(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT).contains(operation));
        checkServerOperationResolver(operationContext, operation, pa, false);
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigSocketBindingGroupBadGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "new-group"), serverConfig);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_GROUP).set("old-group");

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_GROUP);
        operation.get(VALUE).set("bad-group");

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_GROUP_INSTANCE.execute(operationContext, operation);
    }

    @Test
    public void testChangeServerConfigSocketBindingPortOffset() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(10);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
        Assert.assertNull(operationContext.getAttachment(ServerOperationResolver.DONT_PROPAGATE_TO_SERVERS_ATTACHMENT));
        checkServerOperationResolver(operationContext, operation, pa, true);
    }


    @Test
    public void testChangeServerConfigSocketBindingPortOffsetNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

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

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigSocketBindingPortOffsetBadPort() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_PORT_OFFSET).set(10);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(SOCKET_BINDING_PORT_OFFSET);
        operation.get(VALUE).set(-1);

        ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE.execute(operationContext, operation);
    }

    private void checkServerOperationResolver(OperationContext context, ModelNode operation, PathAddress address, boolean expectServerOps) {
        Map<String, ProxyController> serverProxies = new HashMap<String, ProxyController>();
        serverProxies.put("server-one", new MockServerProxy());
        serverProxies.put("server-two", new MockServerProxy());
        serverProxies.put("server-three", new MockServerProxy());
        ServerOperationResolver resolver = new ServerOperationResolver("localhost", serverProxies);

        ModelNode domain = new ModelNode();
        ModelNode host = new ModelNode();
        host.get(SERVER_CONFIG, "server-one", GROUP).set("group-one");
        host.get(SERVER_CONFIG, "server-two", GROUP).set("nope");
        host.get(SERVER_CONFIG, "server-three", GROUP).set("nope");


        Map<Set<ServerIdentity>, ModelNode> serverOps = resolver.getServerOperations(context, operation, address, domain, host);
        if (expectServerOps) {
            Assert.assertEquals(1, serverOps.size());
            Set<ServerIdentity> ids = serverOps.entrySet().iterator().next().getKey();
            Assert.assertEquals(1, ids.size());

            ServerIdentity expected = new ServerIdentity("localhost", "group-one","server-one");
            assertEquals(expected, ids.iterator().next());

            ModelNode expectedOp = new ModelNode();

            expectedOp.get(OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
            expectedOp.get(OP_ADDR).setEmptyList();
            Assert.assertEquals(expectedOp, serverOps.get(ids));
        } else {
            Assert.assertEquals(0, serverOps.size());
        }
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

}
