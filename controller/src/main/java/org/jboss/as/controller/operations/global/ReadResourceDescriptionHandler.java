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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.INCLUDE_ALIASES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.LOCALE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.PROXIES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE_DEPTH;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} querying the complete type description of a given model node.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ReadResourceDescriptionHandler implements OperationStepHandler {

    private static final SimpleAttributeDefinition INHERITED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INHERITED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(true))
            .build();

    private static final SimpleAttributeDefinition OPERATIONS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.OPERATIONS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_RESOURCE_DESCRIPTION_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(OPERATIONS, INHERITED, RECURSIVE, RECURSIVE_DEPTH, PROXIES, INCLUDE_ALIASES, LOCALE)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .build();

    static final OperationStepHandler INSTANCE = new ReadResourceDescriptionHandler();

    private final ParametersValidator validator = new ParametersValidator();

    {
        validator.registerValidator(RECURSIVE.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(RECURSIVE_DEPTH.getName(), new ModelTypeValidator(ModelType.INT, true));
        validator.registerValidator(PROXIES.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(OPERATIONS.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(INHERITED.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(INCLUDE_ALIASES.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (address.isMultiTarget()) {
            executeMultiTarget(context, operation);
        } else {
            doExecute(context, operation);
        }
    }

    private void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);

        final String opName = operation.require(OP).asString();
        final ModelNode opAddr = operation.get(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final int recursiveDepth = RECURSIVE_DEPTH.resolveModelAttribute(context, operation).asInt();
        final boolean recursive = recursiveDepth > 0 || RECURSIVE.resolveModelAttribute(context, operation).asBoolean();
        final boolean proxies = PROXIES.resolveModelAttribute(context, operation).asBoolean();
        final boolean ops = OPERATIONS.resolveModelAttribute(context, operation).asBoolean();
        final boolean aliases = INCLUDE_ALIASES.resolveModelAttribute(context, operation).asBoolean();
        final boolean inheritedOps = INHERITED.resolveModelAttribute(context, operation).asBoolean();

        //Get hold of the real registry if it was an alias
        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();

        AliasEntry aliasEntry = registry.getAliasEntry();
        final ImmutableManagementResourceRegistration realRegistry = aliasEntry == null ? registry : context.getRootResourceRegistration().getSubModel(aliasEntry.convertToTargetAddress(PathAddress.pathAddress(opAddr)));

        final DescriptionProvider descriptionProvider = realRegistry.getModelDescription(PathAddress.EMPTY_ADDRESS);
        final Locale locale = GlobalOperationHandlers.getLocale(context, operation);

        final ModelNode nodeDescription = descriptionProvider.getModelDescription(locale);
        final Map<String, ModelNode> operations = new HashMap<String, ModelNode>();
        final Map<PathElement, ModelNode> childResources = recursive ? new HashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

        // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
        // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

        // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
        final ReadResourceDescriptionAssemblyHandler assemblyHandler = new ReadResourceDescriptionAssemblyHandler(nodeDescription, operations, childResources);
        context.addStep(assemblyHandler, OperationContext.Stage.IMMEDIATE);

        if (ops) {
            for (final Map.Entry<String, OperationEntry> entry : realRegistry.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, inheritedOps).entrySet()) {
                if (entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                    if (context.getProcessType() != ProcessType.DOMAIN_SERVER || entry.getValue().getFlags().contains(OperationEntry.Flag.RUNTIME_ONLY)) {
                        final DescriptionProvider provider = entry.getValue().getDescriptionProvider();
                        operations.put(entry.getKey(), provider.getModelDescription(locale));
                    }
                }
            }
        }
        if (nodeDescription.hasDefined(ATTRIBUTES)) {
            for (final String attr : nodeDescription.require(ATTRIBUTES).keys()) {
                final AttributeAccess access = realRegistry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attr);
                // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                // be writable without a registered handler. This opens the possibility that out-of-date metadata
                // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                // an unexpected undefined value returned. But it removes the possibility of a
                // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                // in the valid attribute "foo" not being readable
                final AttributeAccess.AccessType accessType = access == null ? AttributeAccess.AccessType.READ_ONLY : access.getAccessType();
                final AttributeAccess.Storage storage = access == null ? AttributeAccess.Storage.CONFIGURATION : access.getStorageType();
                final ModelNode attrNode = nodeDescription.get(ATTRIBUTES, attr);
                //AS7-3085 - For a domain mode server show writable attributes as read-only
                String displayedAccessType =
                        context.getProcessType() == ProcessType.DOMAIN_SERVER && storage == AttributeAccess.Storage.CONFIGURATION ?
                                AttributeAccess.AccessType.READ_ONLY.toString() : accessType.toString();
                attrNode.get(ACCESS_TYPE).set(displayedAccessType);
                attrNode.get(STORAGE).set(storage.toString());
                if (accessType == AttributeAccess.AccessType.READ_WRITE) {
                    Set<AttributeAccess.Flag> flags = access.getFlags();
                    if (flags.contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
                        attrNode.get(RESTART_REQUIRED).set("all-services");
                    } else if (flags.contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)) {
                        attrNode.get(RESTART_REQUIRED).set("resource-services");
                    } else if (flags.contains(AttributeAccess.Flag.RESTART_JVM)) {
                        attrNode.get(RESTART_REQUIRED).set("jvm");
                    } else {
                        attrNode.get(RESTART_REQUIRED).set("no-services");
                    }
                }
            }
        }

        if (recursive) {
            for (final PathElement element : realRegistry.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
                PathAddress relativeAddr = PathAddress.pathAddress(element);
                ImmutableManagementResourceRegistration childReg = realRegistry.getSubModel(relativeAddr);

                boolean readChild = true;
                if (childReg.isRemote() && !proxies) {
                    readChild = false;
                }
                if (childReg.isAlias() && !aliases) {
                    readChild = false;
                }

                if (readChild) {
                    final int newDepth = recursiveDepth > 0 ? recursiveDepth - 1 : 0;
                    ModelNode rrOp = new ModelNode();
                    rrOp.get(OP).set(opName);
                    try {
                        rrOp.get(OP_ADDR).set(PathAddress.pathAddress(address, element).toModelNode());
                    } catch (Exception e) {
                        continue;
                    }
                    rrOp.get(RECURSIVE.getName()).set(operation.get(RECURSIVE.getName()));
                    rrOp.get(RECURSIVE_DEPTH.getName()).set(newDepth);
                    rrOp.get(PROXIES.getName()).set(proxies);
                    rrOp.get(OPERATIONS.getName()).set(ops);
                    rrOp.get(INHERITED.getName()).set(inheritedOps);
                    rrOp.get(LOCALE.getName()).set(operation.get(LOCALE.getName()));
                    rrOp.get(INCLUDE_ALIASES.getName()).set(aliases);
                    ModelNode rrRsp = new ModelNode();
                    childResources.put(element, rrRsp);

                    final OperationStepHandler handler = childReg.isRemote() ? childReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, opName) :
                            new OperationStepHandler() {
                                @Override
                                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                                    doExecute(context, operation);
                                }
                            };
                    context.addStep(rrRsp, rrOp, handler, OperationContext.Stage.IMMEDIATE);
                }
                //Add a "child" => undefined
                nodeDescription.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue());
            }
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {

                if (!context.hasFailureDescription()) {
                    for (final ModelNode value : childResources.values()) {
                        if (value.hasDefined(FAILURE_DESCRIPTION)) {
                            context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                            break;
                        }
                    }
                }
            }
        });
    }

    private void executeMultiTarget(final OperationContext context, final ModelNode operation) {
        // Format wildcard queries as list
        final ModelNode result = context.getResult().setEmptyList();
        context.addStep(new ModelNode(), GlobalOperationHandlers.AbstractMultiTargetHandler.FAKE_OPERATION.clone(),
            new GlobalOperationHandlers.RegistrationAddressResolver(operation, result,
                new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        // step handler bypassing further wildcard resolution
                        doExecute(context, operation);
                    }
                }), OperationContext.Stage.IMMEDIATE);
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (!context.hasFailureDescription()) {
                    String op = operation.require(OP).asString();
                    Map<PathAddress, ModelNode> failures = new HashMap<PathAddress, ModelNode>();
                    for (ModelNode resultItem : result.asList()) {
                        if (resultItem.hasDefined(FAILURE_DESCRIPTION)) {
                            final PathAddress failedAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
                            ModelNode failedDesc = resultItem.get(FAILURE_DESCRIPTION);
                            failures.put(failedAddress, failedDesc);
                        }
                    }

                    if (failures.size() == 1) {
                        Map.Entry<PathAddress, ModelNode> entry = failures.entrySet().iterator().next();
                        if (entry.getValue().getType() == ModelType.STRING) {
                            context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtSingleAddress(op, entry.getKey(), entry.getValue().asString()));
                        } else {
                            context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtSingleAddressWithComplexFailure(op, entry.getKey()));
                        }
                    } else if (failures.size() > 1) {
                        context.getFailureDescription().set(ControllerMessages.MESSAGES.wildcardOperationFailedAtMultipleAddresses(op, failures.keySet()));
                    }
                }
            }
        });

    }

    /**
     * Assembles the response to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadResourceDescriptionAssemblyHandler implements OperationStepHandler {

        private final ModelNode nodeDescription;
        private final Map<String, ModelNode> operations;
        private final Map<PathElement, ModelNode> childResources;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param nodeDescription basic description of the node, of its attributes and of its child types
         * @param operations      descriptions of the resource's operations
         * @param childResources  read-resource-description response from child resources, where the key is the PathAddress
         *                        relative to the address of the operation this handler is handling and the
         *                        value is the full read-resource response. Will not be {@code null}
         */
        private ReadResourceDescriptionAssemblyHandler(final ModelNode nodeDescription, final Map<String, ModelNode> operations, final Map<PathElement, ModelNode> childResources) {
            this.nodeDescription = nodeDescription;
            this.operations = operations;
            this.childResources = childResources;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            for (Map.Entry<PathElement, ModelNode> entry : childResources.entrySet()) {
                final PathElement element = entry.getKey();
                final ModelNode value = entry.getValue();
                if (!value.has(FAILURE_DESCRIPTION)) {
                    nodeDescription.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue()).set(value.get(RESULT));
                } else if (value.hasDefined(FAILURE_DESCRIPTION)) {
                    context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                    break;
                }
            }

            for (Map.Entry<String, ModelNode> entry : operations.entrySet()) {
                nodeDescription.get(OPERATIONS.getName(), entry.getKey()).set(entry.getValue());
            }

            context.getResult().set(nodeDescription);
            context.stepCompleted();
        }
    }
}
