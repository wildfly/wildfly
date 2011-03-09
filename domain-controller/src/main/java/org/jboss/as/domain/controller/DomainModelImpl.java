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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.BasicTransactionalModelController;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.descriptions.common.ExtensionDescription;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingRemoveHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.ProfileAddHandler;
import org.jboss.as.domain.controller.operations.ProfileDescribeHandler;
import org.jboss.as.domain.controller.operations.ProfileRemoveHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.ExtensionRemoveHandler;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelImpl extends BasicTransactionalModelController implements DomainModel {

    private final ExtensionContext extensionContext;
    private final HostControllerProxy hostController;
    private final ServerOperationResolver serverOperationResolver;
    private final String localHostName;

    /** Constructor for a master DC. */
    protected DomainModelImpl(final ExtensibleConfigurationPersister configurationPersister, final HostControllerProxy localHostProxy) {
        super(createCoreModel(), configurationPersister, DomainDescriptionProviders.ROOT_PROVIDER);
        ModelNodeRegistration registry = getRegistry();
        this.extensionContext = initialize(registry, configurationPersister, null);
        this.hostController = localHostProxy;
        this.localHostName = hostController.getName();
        this.serverOperationResolver = new ServerOperationResolver(localHostName);
        registry.registerProxyController(localHostProxy.getProxyNodeAddress().getLastElement(), localHostProxy);
    }

    /** Constructor for a slave DC. */
    protected DomainModelImpl(final ModelNode model, final ExtensibleConfigurationPersister configurationPersister, final HostControllerProxy localHostProxy) {
        super(model, configurationPersister, DomainDescriptionProviders.ROOT_PROVIDER);
        ModelNodeRegistration registry = getRegistry();
        this.extensionContext = initialize(registry, configurationPersister, model);
        this.hostController = localHostProxy;
        this.localHostName = hostController.getName();
        this.serverOperationResolver = new ServerOperationResolver(localHostName);
        registry.registerProxyController(localHostProxy.getProxyNodeAddress().getLastElement(), localHostProxy);
    }

    private static ModelNode createCoreModel() {
        // Create roots
        final ModelNode rootModel = new ModelNode();
        rootModel.get(NAMESPACES).setEmptyList();
        rootModel.get(SCHEMA_LOCATIONS).setEmptyList();
        rootModel.get(EXTENSION);
        rootModel.get(INTERFACE);
        rootModel.get(PROFILE);
        rootModel.get(SERVER_GROUP);
        rootModel.get(SOCKET_BINDING_GROUP);
        rootModel.get(PATH);
        rootModel.get(HOST);
        return rootModel;
    }

    @Override
    public ModelNode getDomainModel() {
        return super.getModel().clone();
    }

    protected ExtensionContext initialize(final ModelNodeRegistration root, final ExtensibleConfigurationPersister configurationPersister, final ModelNode model) {
        // Global operations

        root.registerOperationHandler(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME, GlobalOperationHandlers.RESOLVE, GlobalOperationHandlers.RESOLVE, false, OperationEntry.EntryType.PRIVATE);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE, SystemPropertyAddHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);

        final ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.NAMED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(ADD, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);
        interfaces.registerOperationHandler(REMOVE, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);

        final ModelNodeRegistration profile = root.registerSubModel(PathElement.pathElement(PROFILE), DomainDescriptionProviders.PROFILE);
        profile.registerOperationHandler(ADD, ProfileAddHandler.INSTANCE, DomainDescriptionProviders.PROFILE_ADD, false);
        profile.registerOperationHandler(REMOVE, ProfileRemoveHandler.INSTANCE, DomainDescriptionProviders.PROFILE_REMOVE, false);
        profile.registerOperationHandler(DESCRIBE, ProfileDescribeHandler.INSTANCE, DomainDescriptionProviders.PROFILE_DESCRIBE, false);

        final ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), DomainDescriptionProviders.PATH_DESCRIPTION);
        paths.registerOperationHandler(ADD, PathAddHandler.NAMED_INSTANCE, DomainDescriptionProviders.PATH_ADD, false);
        paths.registerOperationHandler(REMOVE, PathRemoveHandler.INSTANCE, DomainDescriptionProviders.PATH_REMOVE, false);

        final ModelNodeRegistration socketBindingGroup = root.registerSubModel(PathElement.pathElement(SOCKET_BINDING_GROUP), DomainDescriptionProviders.SOCKET_BINDING_GROUP);
        socketBindingGroup.registerOperationHandler(ADD, SocketBindingGroupAddHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_GROUP, false);
        // TODO remove
        final ModelNodeRegistration socketBindings = socketBindingGroup.registerSubModel(PathElement.pathElement(SOCKET_BINDING), DomainDescriptionProviders.SOCKET_BINDING);
        socketBindings.registerOperationHandler(ADD, SocketBindingAddHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_ADD, false);
        socketBindings.registerOperationHandler(REMOVE, SocketBindingRemoveHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_REMOVE, false);

        final ModelNodeRegistration serverGroups = root.registerSubModel(PathElement.pathElement(SERVER_GROUP), DomainDescriptionProviders.SERVER_GROUP);
        serverGroups.registerOperationHandler(ADD, ServerGroupAddHandler.INSTANCE, DomainDescriptionProviders.SERVER_GROUP_ADD, false);
        serverGroups.registerOperationHandler(REMOVE, ServerGroupRemoveHandler.INSTANCE, DomainDescriptionProviders.SERVER_GROUP_REMOVE, false);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        final ModelNodeRegistration groupVMs = serverGroups.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(groupVMs);

        // Deployments
        final ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);

        // Extensions
        final ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        final ExtensionContext extensionContext = new ExtensionContextImpl(profile, deployments, configurationPersister);
        final ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        initializeExtensions(model);

        registerInternalOperations();

        return extensionContext;
    }

    private void initializeExtensions(ModelNode model) {
        // If we were provided a model, we're a slave and need to initialize all extensions
        if (model != null && model.hasDefined(EXTENSION)) {
            for (Property prop : model.get(EXTENSION).asPropertyList()) {
                try {
                    String module = prop.getValue().get(ExtensionDescription.MODULE).asString();
                    for (Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                        extension.initialize(extensionContext);
                    }
                } catch (ModuleLoadException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    void setInitialDomainModel(ModelNode domainModel) {
        getModel().set(domainModel);
        initializeExtensions(domainModel);
    }



    @Override
    public ModelNode execute(ExecutionContext executionContext, ControllerTransactionContext transaction) {
        ModelNode op = executionContext.getOperation();
        if (HostControllerClient.EXECUTE_ON_DOMAIN.equals(op.require(OP).asString())) {
            if (!op.hasDefined(OP_ADDR) || op.get(OP_ADDR).asInt() == 0) {
                ModelNode onDomain = op.require(HostControllerClient.DOMAIN_OP);
                return executeOnDomain(executionContext.clone(onDomain), transaction);
            }
        }
        return super.execute(executionContext, transaction);
    }

    private ModelNode executeOnDomain(final ExecutionContext executionContext, final ControllerTransactionContext transaction) {
        ModelNode operation = executionContext.getOperation();
        ParsedOp parsedOp = parseOperation(operation, 0);
        ModelNode domainOp = parsedOp.getDomainOperation();
        ModelNode overallResult = null;
        if (domainOp != null) {
            ModelNode opResult = super.execute(executionContext.clone(domainOp), transaction);
            overallResult = createOverallResult(opResult, parsedOp);
        }
        else {
            overallResult = new ModelNode();
            overallResult.get(OUTCOME).set(IGNORED);
        }
        return overallResult;
    }

    private ParsedOp parseOperation(ModelNode operation, int index) {
        String targetHost = null;
        String runningServerTarget = null;
        ModelNode runningServerOp = null;
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (address.size() > 0) {
            PathElement first = address.getElement(0);
            if (HOST.equals(first.getKey())) {
                targetHost = first.getValue();
                if (address.size() > 1 && RUNNING_SERVER.equals(address.getElement(1).getKey())) {
                    runningServerTarget = address.getElement(1).getValue();
                    ModelNode relativeAddress = new ModelNode();
                    for (int i = 2; i < address.size(); i++) {
                        PathElement element = address.getElement(i);
                        relativeAddress.add(element.getKey(), element.getValue());
                    }
                    runningServerOp = operation.clone();
                    runningServerOp.get(OP_ADDR).set(relativeAddress);
                }
            }
        }

        ParsedOp result = null;

        if (targetHost != null && !localHostName.equals(targetHost)) {
            result = new SimpleParsedOp(index);
        }
        else if (runningServerTarget != null) {
            result = new SimpleParsedOp(index, runningServerTarget, runningServerOp);
        }
        else if (COMPOSITE.equals(operation.require(OP).asString())) {
            // Recurse into the steps to see what's required
            if (operation.hasDefined(STEPS)) {
                int stepNum = 0;
                List<ParsedOp> parsedSteps = new ArrayList<ParsedOp>();
                for (ModelNode step : operation.get(STEPS).asList()) {
                    parsedSteps.add(parseOperation(step, stepNum++));
                }
                result = new ParsedMultiStepOp(index, parsedSteps);
            }
            else {
                // Will fail later
                result = new SimpleParsedOp(index, operation, address);
            }
        }
        else {
            result = new SimpleParsedOp(index, operation, address);
        }

        return result;
    }

    private ModelNode createOverallResult(ModelNode opResult, ParsedOp parsedOp) {
        if (!SUCCESS.equals(opResult.get(OUTCOME).asString())) {
            return opResult;
        }
        ModelNode resultNode = opResult.get(RESULT);

        ModelNode overallResult = new ModelNode();
        overallResult.get(OUTCOME).set(SUCCESS);
        ModelNode domainResult = parsedOp.getFormattedDomainResult(resultNode);
        overallResult.get(RESULT, DOMAIN_RESULTS).set(domainResult);
        Map<Set<ServerIdentity>, ModelNode> serverOps = parsedOp.getServerOps(getDomainModel(), getHostModel());
        ModelNode serverOpsNode = overallResult.get(RESULT, SERVER_OPERATIONS);
        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : serverOps.entrySet()) {
            ModelNode setNode = serverOpsNode.add();
            ModelNode serverNode = setNode.get("servers");
            for (ServerIdentity server : entry.getKey()) {
                serverNode.add(server.getServerName(), server.getServerGroupName());
            }
            setNode.get(OP).set(entry.getValue());
        }
        ModelNode compOp = opResult.has(COMPENSATING_OPERATION) ? opResult.get(COMPENSATING_OPERATION) : null;
        if (compOp != null) {
            if (compOp.isDefined()) {
                compOp = parsedOp.getFormattedDomainCompensatingOp(compOp);
            }
            overallResult.get(COMPENSATING_OPERATION).set(compOp);
        }
        return overallResult;
    }

    private String getServerGroup(String serverName) {
        return hostController.getServerGroupName(serverName);
    }

    private ModelNode getHostModel() {
        return hostController.getHostModel();
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerOperations(ModelNode domainOp, PathAddress domainOpAddress, ModelNode domainModel, ModelNode hostModel) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        if (domainOpAddress.size() > 0 && HOST.equals(domainOpAddress.getElement(0).getKey())) {
            OperationHandler handler = hostController.getRegistry().getOperationHandler(domainOpAddress, domainOp.require(OP).asString());
            if (!(handler instanceof ModelUpdateOperationHandler)) {
                result = Collections.emptyMap();
            }
        }
        else {
            OperationHandler handler = getRegistry().getOperationHandler(domainOpAddress, domainOp.require(OP).asString());
            if (!(handler instanceof ModelUpdateOperationHandler)) {
                result = Collections.emptyMap();
            }
        }
        if (result == null) {
            result = serverOperationResolver.getServerOperations(domainOp, domainOpAddress, domainModel, hostModel);
        }
        return result;
    }

    private interface ParsedOp {
        ModelNode getDomainOperation();
        Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel);
        ModelNode getFormattedDomainResult(ModelNode resultNode);
        ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted);
    }

    private class SimpleParsedOp implements ParsedOp {
        private final String domainStep;
        private final ModelNode domainOp;
        private final PathAddress domainOpAddress;
        private Map<Set<ServerIdentity>, ModelNode> serverOps;

        private SimpleParsedOp(int index) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = null;
            this.domainOpAddress = null;
            this.serverOps = Collections.emptyMap();
        }

        private SimpleParsedOp(int index, ModelNode domainOp, final PathAddress domainOpAddress) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = domainOp;
            this.domainOpAddress = domainOpAddress;
            this.serverOps = null;
        }

        private SimpleParsedOp(int index, String serverName, ModelNode serverOp) {
            this.domainStep = "step-" + (index + 1);
            this.domainOp = null;
            this.domainOpAddress = null;
            final ServerIdentity serverIdentity = new ServerIdentity(localHostName, getServerGroup(serverName), serverName);
            this.serverOps = Collections.singletonMap(Collections.singleton(serverIdentity), serverOp);
        }

        @Override
        public Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel) {
            Map<Set<ServerIdentity>, ModelNode> result = serverOps;
            if (serverOps == null) {
                result = DomainModelImpl.this.getServerOperations(domainOp, domainOpAddress, domainModel, hostModel);
            }
            return result;
        }

        @Override
        public ModelNode getDomainOperation() {
            return domainOp;
        }

        @Override
        public ModelNode getFormattedDomainResult(ModelNode resultNode) {
            ModelNode formatted = new ModelNode();
            formatted.get(domainStep).set(resultNode);
            return formatted;
        }

        @Override
        public ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted) {
            return unformatted;
        }
    }

    private class ParsedMultiStepOp implements ParsedOp {

        private final String domainStep;
        private final List<ParsedOp> steps;

        private ParsedMultiStepOp(final int index, final List<ParsedOp> steps) {
            this.domainStep = "step-" + (index + 1);
            this.steps = steps;
        }

        @Override
        public Map<Set<ServerIdentity>, ModelNode> getServerOps(ModelNode domainModel, ModelNode hostModel) {
            Map<Set<ServerIdentity>, List<ModelNode>> buildingBlocks = new HashMap<Set<ServerIdentity>, List<ModelNode>>();
            for (ParsedOp step : steps) {
                Map<Set<ServerIdentity>, ModelNode> stepResult = step.getServerOps(domainModel, hostModel);
                if (stepResult.size() == 0) {
                    continue;
                }
                else if (buildingBlocks.size() == 0) {
                    for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                        List<ModelNode> list = new ArrayList<ModelNode>();
                        list.add(entry.getValue());
                        buildingBlocks.put(entry.getKey(), list);
                    }
                }
                else {
                    for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : stepResult.entrySet()) {
                        List<ModelNode> existingOp = buildingBlocks.get(entry.getKey());
                        if (existingOp != null) {
                            existingOp.add(entry.getValue());
                        }
                        else {
                            Set<ServerIdentity> newSet = new HashSet<ServerIdentity>(entry.getKey());
                            Set<Set<ServerIdentity>> existingSets = new HashSet<Set<ServerIdentity>>(buildingBlocks.keySet());
                            for (Set<ServerIdentity> existing : existingSets) {
                                Set<ServerIdentity> copy = new HashSet<ServerIdentity>(existing);
                                copy.retainAll(newSet);
                                if (copy.size() > 0) {
                                    if (copy.size() == existing.size()) {
                                        // Just add the new step and store back
                                        buildingBlocks.get(existing).add(entry.getValue());
                                    }
                                    else {
                                        // Add the new step to the intersection; store the old set of steps
                                        // under a key that includes the remainder
                                        List<ModelNode> existingSteps = buildingBlocks.remove(existing);
                                        List<ModelNode> newSteps = new ArrayList<ModelNode>(existingSteps);
                                        buildingBlocks.put(copy, newSteps);
                                        existing.removeAll(copy);
                                        buildingBlocks.put(existing, existingSteps);
                                    }

                                    // no longer track the servers we've stored
                                    newSet.removeAll(copy);
                                }
                            }

                            // Any servers not stored above get their own entry
                            if (newSet.size() > 0) {
                                List<ModelNode> toAdd = new ArrayList<ModelNode>();
                                toAdd.add(entry.getValue());
                                buildingBlocks.put(newSet, toAdd);
                            }
                        }
                    }
                }
            }

            Map<Set<ServerIdentity>, ModelNode> result = null;
            if (buildingBlocks.size() > 0) {
                result = new HashMap<Set<ServerIdentity>, ModelNode>();
                for (Map.Entry<Set<ServerIdentity>, List<ModelNode>> entry : buildingBlocks.entrySet()) {
                    List<ModelNode> ops = entry.getValue();
                    if (ops.size() == 1) {
                        result.put(entry.getKey(), ops.get(0));
                    }
                    else {
                        ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                        ModelNode steps = composite.get(STEPS);
                        for (ModelNode step : entry.getValue()) {
                            steps.add(step);
                        }
                        result.put(entry.getKey(), composite);
                    }
                }
            }
            else {
                result = Collections.emptyMap();
            }
            return result;
        }

        @Override
        public ModelNode getDomainOperation() {
            ModelNode result = null;
            List<ModelNode> domainSteps = new ArrayList<ModelNode>();
            for (ParsedOp step : steps) {
                ModelNode stepNode = step.getDomainOperation();
                if (stepNode != null) {
                    domainSteps.add(stepNode);
                }
            }
            if (domainSteps.size() == 1) {
                result = domainSteps.get(0);
            }
            else if (domainSteps.size() > 1) {
                ModelNode stepsParam = new ModelNode();
                for (ModelNode stepNode : domainSteps) {
                    stepsParam.add(stepNode);
                }
                result = Util.getEmptyOperation(COMPOSITE, new ModelNode());
                result.get(STEPS).set(stepsParam);
            }
            return result;
        }

        @Override
        public ModelNode getFormattedDomainResult(ModelNode resultNode) {
            ModelNode formatted = new ModelNode();
            int resultStep = 0;
            for (int i = 0; i < steps.size(); i++) {
                ParsedOp po = steps.get(i);
                if (po.getDomainOperation() != null) {
                    String label = "step-" + (++resultStep);
                    ModelNode stepResultNode = resultNode.get(label);
                    ModelNode formattedStepResultNode = po.getFormattedDomainResult(stepResultNode);
                    formatted.get("step-" + (i+1)).set(formattedStepResultNode);
                }
                else {
                    formatted.get("step-" + (i+1), OUTCOME).set(IGNORED);
                }
            }
            return formatted;
        }

        @Override
        public ModelNode getFormattedDomainCompensatingOp(ModelNode unformatted) {
            ModelNode formatted = new ModelNode();
            int resultStep = 0;
            for (int i = 0; i < steps.size(); i++) {
                ParsedOp po = steps.get(i);
                if (po.getDomainOperation() != null) {
                    String label = "step-" + (++resultStep);
                    ModelNode stepResultNode = unformatted.get(label);
                    formatted.get("step-" + (i+1)).set(stepResultNode);
                }
                else {
                    formatted.get("step-" + (i+1)).set(IGNORED);
                }
            }
            return formatted;
        }
    }
}
