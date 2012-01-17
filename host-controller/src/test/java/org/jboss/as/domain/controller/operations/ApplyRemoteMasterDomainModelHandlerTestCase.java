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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResource;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ApplyRemoteMasterDomainModelHandlerTestCase {

    private final ApplyRemoteMasterDomainModelHandler handler =
            new ApplyRemoteMasterDomainModelHandler(
                    new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL)),
                    null, null, HOST_INFO) {
        protected void initializeExtension(String module) {
        }
    };

    @Test
    public void testNoChanges() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final OperationContext operationContext = getOperationContext();
        handler.execute(operationContext, operation);
    }

    @Test
    public void testBooting() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final OperationContext operationContext = getOperationContext(true);
        handler.execute(operationContext, operation);
    }

    @Test
    public void testExtensionAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(EXTENSION, "org.jboss.extension")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testExtensionRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(EXTENSION, "org.jboss.extension"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testPathAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(PATH, "some-path")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testPathRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(PATH, "some-path"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testPathChange() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(PATH, "some-path")).toModelNode());
        final ModelNode path = new ModelNode();
        path.set("some path");
        change.get("domain-resource-model").set(path);
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(PATH, "some-path"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSystemPropertyAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, "some-property")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-two")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSystemPropertyRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), Resource.Factory.create());
        operationContext.root.registerChild(PathElement.pathElement(SYSTEM_PROPERTY, "some-property"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSystemPropertyChange() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, "some-property")).toModelNode());
        final ModelNode property = new ModelNode();
        property.set("some property");
        change.get("domain-resource-model").set(property);
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-two"), Resource.Factory.create());
        operationContext.root.registerChild(PathElement.pathElement(SYSTEM_PROPERTY, "some-property"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testProfileAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, "some-profile")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-two")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testProfileRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), Resource.Factory.create());
        final Resource serverGroupResource = Resource.Factory.create();
        serverGroupResource.getModel().get(PROFILE).set("some-profile");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverGroupResource);
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testProfileChange() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, "some-profile")).toModelNode());
        final ModelNode property = new ModelNode();
        property.set("some profile");
        change.get("domain-resource-model").set(property);
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), Resource.Factory.create());
        final Resource serverGroupResource = Resource.Factory.create();
        serverGroupResource.getModel().get(PROFILE).set("some-profile");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-two"), serverGroupResource);
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testInterfaceAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(INTERFACE, "some-interface")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testInterfaceAddWithServerOverride() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(INTERFACE, "some-interface")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).registerChild(PathElement.pathElement(INTERFACE, "some-interface"), Resource.Factory.create());

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testInterfaceRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(INTERFACE, "some-interface"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testInterfaceChange() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(INTERFACE, "some-interface")).toModelNode());
        final ModelNode property = new ModelNode();
        property.set("some interface");
        change.get("domain-resource-model").set(property);
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(INTERFACE, "some-interface"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSocketBindingAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "some-binding")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        final ModelNode serverConfig = new ModelNode();
        serverConfig.get(SOCKET_BINDING_GROUP).set("some-binding");
        change.get("domain-resource-model").set(serverConfig);
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSocketBindingChangeWithServerOverride() throws Exception {
        final ModelNode operation = new ModelNode();
        ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "some-binding")).toModelNode());
        final ModelNode newBinding = new ModelNode();
        newBinding.set("blahh");
        change.get("domain-resource-model").set(newBinding);
        operation.get(DOMAIN_MODEL).add(change);

        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        ModelNode serverConfig = new ModelNode();
        serverConfig.get(SOCKET_BINDING_GROUP).set("some-binding");
        change.get("domain-resource-model").set(serverConfig);
        operation.get(DOMAIN_MODEL).add(change);

        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-two")).toModelNode());
        serverConfig = new ModelNode();
        serverConfig.get(SOCKET_BINDING_GROUP).set("some-binding");
        change.get("domain-resource-model").set(serverConfig);
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        final Resource groupOneResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), groupOneResource);
        groupOneResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");
        final Resource groupTwoResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-two"), groupTwoResource);
        groupTwoResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");

        operationContext.root.getChild(PathElement.pathElement(HOST, "localhost")).getChild(PathElement.pathElement(SERVER_CONFIG, "server-one")).getModel().get(SOCKET_BINDING_GROUP).set("other-binding");
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSocketBindingRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();

        final MockOperationContext operationContext = getOperationContext();
        final Resource groupOneResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), groupOneResource);
        groupOneResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");
        final Resource groupTwoResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-two"), groupTwoResource);
        groupTwoResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testSocketBindingChange() throws Exception {
        final ModelNode operation = new ModelNode();
        ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "some-binding")).toModelNode());
        final ModelNode newBinding = new ModelNode();
        newBinding.set("blahh");
        change.get("domain-resource-model").set(newBinding);
        operation.get(DOMAIN_MODEL).add(change);

        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        ModelNode serverConfig = new ModelNode();
        serverConfig.get(SOCKET_BINDING_GROUP).set("some-binding");
        change.get("domain-resource-model").set(serverConfig);
        operation.get(DOMAIN_MODEL).add(change);

        change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-two")).toModelNode());
        serverConfig = new ModelNode();
        serverConfig.get(SOCKET_BINDING_GROUP).set("some-binding");
        change.get("domain-resource-model").set(serverConfig);
        operation.get(DOMAIN_MODEL).add(change);


        final MockOperationContext operationContext = getOperationContext();
        final Resource groupOneResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), groupOneResource);
        groupOneResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");
        final Resource groupTwoResource = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-two"), groupTwoResource);
        groupTwoResource.getModel().get(SOCKET_BINDING_GROUP).set("some-binding");

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-three")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

     @Test
    public void testServerGroupAdd() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testServerGroupRemove() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(DOMAIN_MODEL).setEmptyList();
        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testServerGroupChange() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        change.get("domain-resource-address").set(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one")).toModelNode());
        final ModelNode group = new ModelNode();
        group.get("Some prop").set("some value");
        change.get("domain-resource-model").set(group);
        operation.get(DOMAIN_MODEL).add(change);

        final MockOperationContext operationContext = getOperationContext();
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), Resource.Factory.create());
        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));
        handler.execute(operationContext, operation);
        operationContext.verify();
    }

    @Test
    public void testRolloutPlans() throws Exception {
        final ModelNode operation = new ModelNode();
        final ModelNode change = new ModelNode();
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS));
        change.get("domain-resource-address").set(pa.toModelNode());
        change.get("domain-resource-model").set(new ModelNode());
        operation.get(DOMAIN_MODEL).add(change);
        final MockOperationContext operationContext = getOperationContext();
        handler.execute(operationContext, operation);
        Resource r = operationContext.root.navigate(pa);
        assertTrue(r instanceof ManagedDMRContentTypeResource);
    }

    private static final LocalHostControllerInfo HOST_INFO = new LocalHostControllerInfo() {
        public String getLocalHostName() {
            return "localhost";
        }

        public boolean isMasterDomainController() {
            return false;
        }

        public String getNativeManagementInterface() {
            return null;
        }

        public int getNativeManagementPort() {
            return 0;
        }

        public String getNativeManagementSecurityRealm() {
            return null;
        }

        public String getHttpManagementInterface() {
            return null;
        }

        public int getHttpManagementPort() {
            return 0;
        }

        public int getHttpManagementSecurePort() {
            return 0;
        }

        public String getHttpManagementSecurityRealm() {
            return null;
        }

        public String getRemoteDomainControllerHost() {
            return null;
        }

        public int getRemoteDomainControllertPort() {
            return 0;
        }

        public NetworkInterfaceBinding getNetworkInterfaceBinding(String name) throws SocketException, UnknownHostException {
            return null;
        }

        public ContentRepository getContentRepository() {
            return null;
        }

        public ControlledProcessState.State getProcessState() {
            return null;
        }
    };

    private MockOperationContext getOperationContext() {
        return getOperationContext(false);
    }


    private MockOperationContext getOperationContext(final boolean booting) {
        final Resource root = createRootResource();
        return new MockOperationContext(root, booting);
    }

    private class MockOperationContext implements OperationContext {
        private final Resource root;
        private final boolean booting;
        private Set<PathAddress> expectedSteps = new HashSet<PathAddress>();

        private MockOperationContext(final Resource root, final boolean booting) {
            this.root = root;
            this.booting = booting;
        }

        public void expectStep(final PathAddress address) {
            this.expectedSteps.add(address);
        }

        public void verify() {
            if (!expectedSteps.isEmpty()) {
                System.out.println("Missing: " + expectedSteps);
                fail("Not all the expected steps were added. " + expectedSteps);
            }
        }

        public void addStep(OperationStepHandler step, OperationContext.Stage stage) throws IllegalArgumentException {
            fail("Should not have added step");
        }

        @Override
        public void addStep(OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            addStep(step, stage);
        }

        public void addStep(ModelNode operation, OperationStepHandler step, OperationContext.Stage stage) throws IllegalArgumentException {
            final PathAddress opAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            if (!expectedSteps.contains(opAddress)) {
                fail("Should not have added step for: " + opAddress);
            }
            expectedSteps.remove(opAddress);
        }

        @Override
        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            addStep(operation, step, stage);
        }

        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, OperationContext.Stage stage) throws IllegalArgumentException {
            fail("Should not have added step");
        }

        public InputStream getAttachmentStream(int index) {
            return null;
        }

        public int getAttachmentStreamCount() {
            return 0;
        }

        public ModelNode getResult() {
            return null;
        }

        public boolean hasResult() {
            return false;
        }

        public OperationContext.ResultAction completeStep() {
            return null;
        }

        public void completeStep(OperationContext.RollbackHandler rollbackHandler) {

        }

        public ModelNode getFailureDescription() {
            return null;
        }

        public boolean hasFailureDescription() {
            return false;
        }

        @Override
        public ProcessType getProcessType() {
            return null;
        }

        @Override
        public RunningMode getRunningMode() {
            return null;
        }

        public OperationContext.Type getType() {
            return null;
        }

        public boolean isBooting() {
            return booting;
        }

        public boolean isRollbackOnly() {
            return false;
        }

        public void setRollbackOnly() {
        }

        public boolean isRollbackOnRuntimeFailure() {
            return false;
        }

        public boolean isResourceServiceRestartAllowed() {
            return false;
        }

        public void reloadRequired() {
        }

        public void restartRequired() {
        }

        public void revertReloadRequired() {
        }

        public void revertRestartRequired() {
        }

        public void runtimeUpdateSkipped() {
        }

        public ImmutableManagementResourceRegistration getResourceRegistration() {
            return getResourceRegistrationForUpdate();
        }

        public ManagementResourceRegistration getResourceRegistrationForUpdate() {
            return RESOURCE_REGISTRATION;
        }

        public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
            return null;
        }

        public ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException {
            return null;
        }

        public void removeService(ServiceController<?> controller) throws UnsupportedOperationException {
        }

        public ServiceTarget getServiceTarget() throws UnsupportedOperationException {
            return null;
        }

        public ModelNode readModel(PathAddress address) {
            return null;
        }

        public ModelNode readModelForUpdate(PathAddress address) {
            return null;
        }

        public void acquireControllerLock() {
        }

        public Resource createResource(PathAddress relativeAddress) {
            final Resource toAdd = Resource.Factory.create();
            addResource(relativeAddress, toAdd);
            return toAdd;
        }

        public void addResource(PathAddress relativeAddress, Resource toAdd) {
            Resource model = root;
            final Iterator<PathElement> i = relativeAddress.iterator();
            while (i.hasNext()) {
                final PathElement element = i.next();
                if (element.isMultiTarget()) {
                    throw MESSAGES.cannotWriteTo("*");
                }
                if (!i.hasNext()) {
                    if (model.hasChild(element)) {
                        throw MESSAGES.duplicateResourceAddress(relativeAddress);
                    } else {
                        model.registerChild(element, toAdd);
                        model = toAdd;
                    }
                } else {
                    model = model.getChild(element);
                    if (model == null) {
                        PathAddress ancestor = PathAddress.EMPTY_ADDRESS;
                        for (PathElement pe : relativeAddress) {
                            ancestor = ancestor.append(pe);
                            if (element.equals(pe)) {
                                break;
                            }
                        }
                        throw MESSAGES.resourceNotFound(ancestor, relativeAddress);
                    }
                }
            }
        }

        public Resource readResource(PathAddress address) {
            if (address == PathAddress.EMPTY_ADDRESS) {
                return root;
            }
            Resource resource = root;
            for (PathElement element : address) {
                resource = resource.getChild(element);
            }
            return resource;
        }

        public Resource readResourceForUpdate(PathAddress address) {
            return readResource(address);
        }

        public Resource removeResource(PathAddress address) throws UnsupportedOperationException {
            return null;
        }

        public Resource getRootResource() {
            return root;
        }

        public Resource getOriginalRootResource() {
            return root;
        }

        public boolean isModelAffected() {
            return false;
        }

        public boolean isResourceRegistryAffected() {
            return false;
        }

        public boolean isRuntimeAffected() {
            return false;
        }

        public OperationContext.Stage getCurrentStage() {
            return null;
        }

        public void report(MessageSeverity severity, String message) {
        }

        public boolean markResourceRestarted(PathAddress resource, Object owner) {
            return false;
        }

        public boolean revertResourceRestarted(PathAddress resource, Object owner) {
            return false;
        }

        public ModelNode resolveExpressions(ModelNode node) {
            return null;
        }

        @Override
         public <V> V getAttachment(final AttachmentKey<V> key) {
            return null;
        }

        @Override
        public <V> V attach(final AttachmentKey<V> key, final V value) {
            return null;
        }

        @Override
        public <V> V attachIfAbsent(final AttachmentKey<V> key, final V value) {
            return null;
        }

        @Override
        public <V> V detach(final AttachmentKey<V> key) {
            return null;
        }
    }

    private Resource createRootResource() {
        final Resource rootResource = Resource.Factory.create();
        final Resource host = Resource.Factory.create();
        final Resource serverOneConfig = Resource.Factory.create();
        final ModelNode serverOneModel = new ModelNode();
        serverOneModel.get(GROUP).set("group-one");
        serverOneConfig.writeModel(serverOneModel);
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-one"), serverOneConfig);

        final Resource serverTwoConfig = Resource.Factory.create();
        final ModelNode serverTwoModel = new ModelNode();
        serverTwoModel.get(GROUP).set("group-one");
        serverTwoConfig.writeModel(serverTwoModel);
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-two"), serverTwoConfig);

        final Resource serverThreeConfig = Resource.Factory.create();
        final ModelNode serverThreeModel = new ModelNode();
        serverThreeModel.get(GROUP).set("group-two");
        serverThreeConfig.writeModel(serverThreeModel);
        host.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-three"), serverThreeConfig);

        rootResource.registerChild(PathElement.pathElement(HOST, "localhost"), host);
        hack(rootResource, EXTENSION);
        hack(rootResource, PATH);
        hack(rootResource, SYSTEM_PROPERTY);
        hack(rootResource, PROFILE);
        hack(rootResource, INTERFACE);
        hack(rootResource, SOCKET_BINDING_GROUP);
        hack(rootResource, DEPLOYMENT);
        hack(rootResource, SERVER_GROUP);
        return rootResource;
    }

    private void hack(final Resource rootResource, final String type) {
        rootResource.registerChild(PathElement.pathElement(type, "hack"), Resource.Factory.create());
        for (Resource.ResourceEntry entry : rootResource.getChildren(type)) {
            rootResource.removeChild(entry.getPathElement());
        }
    }


    private static final ManagementResourceRegistration RESOURCE_REGISTRATION = new ManagementResourceRegistration() {
        @Override
        public ManagementResourceRegistration getOverrideModel(String name) {
            return null;
        }

        public ManagementResourceRegistration getSubModel(PathAddress address) {
            return this;
        }

        public ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider) {
            return null;
        }

        public ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition) {
            return null;
        }

        public void unregisterSubModel(PathElement address) {
        }

        @Override
        public boolean isAllowsOverride() {
            return false;
        }

        @Override
        public void setRuntimeOnly(boolean runtimeOnly) {

        }

        @Override
        public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
            return null;
        }

        @Override
        public void unregisterOverrideModel(String name) {
        }

        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {

        }

        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags) {

        }

        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited) {

        }

        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType) {

        }

        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {

        }

        @Override
        public void unregisterOperationHandler(String operationName) {

        }

        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage) {

        }

        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {

        }

        public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {

        }

        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage) {

        }

        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {

        }

        public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {

        }

        public void registerMetric(String attributeName, OperationStepHandler metricHandler) {

        }

        public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {

        }

        public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {

        }

        @Override
        public void unregisterAttribute(String attributeName) {

        }

        public void registerProxyController(PathElement address, ProxyController proxyController) {

        }

        public void unregisterProxyController(PathElement address) {

        }

        public boolean isRuntimeOnly() {
            return false;
        }

        public boolean isRemote() {
            return false;
        }

        public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
            return null;
        }

        public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
            return null;
        }

        public Set<OperationEntry.Flag> getOperationFlags(PathAddress address, String operationName) {
            return null;
        }

        public OperationEntry getOperationEntry(PathAddress address, String operationName) {
            return null;
        }

        public Set<String> getAttributeNames(PathAddress address) {
            return null;
        }

        public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
            return null;
        }

        public Set<String> getChildNames(PathAddress address) {
            return null;
        }

        public Set<PathElement> getChildAddresses(PathAddress address) {
            return null;
        }

        public DescriptionProvider getModelDescription(PathAddress address) {
            return null;
        }

        public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
            return null;
        }

        public ProxyController getProxyController(PathAddress address) {
            if (address.getLastElement().getKey().equals(SERVER) && !address.getLastElement().getValue().equals("server-two")) {
                return new ProxyController() {
                    public PathAddress getProxyNodeAddress() {
                        return null;
                    }

                    public void execute(ModelNode operation, OperationMessageHandler handler, ProxyOperationControl control, OperationAttachments attachments) {
                    }
                };
            }
            return null;
        }

        public Set<ProxyController> getProxyControllers(PathAddress address) {
            return null;
        }
    };
}
