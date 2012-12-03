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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.INCLUDE_ALIASES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.INCLUDE_RUNTIME;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.PROXIES;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE;
import static org.jboss.as.controller.operations.global.GlobalOperationHandlers.RECURSIVE_DEPTH;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} reading a part of the model. The result will only contain the current attributes of a node by default,
 * excluding all addressable children and runtime attributes. Setting the request parameter "recursive" to "true" will recursively include
 * all children and configuration attributes. Non-recursive queries can include runtime attributes by setting the request parameter
 * "include-runtime" to "true".
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadResourceHandler extends GlobalOperationHandlers.AbstractMultiTargetHandler implements OperationStepHandler {

    private static final SimpleAttributeDefinition ATTRIBUTES_ONLY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ATTRIBUTES_ONLY, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_RESOURCE_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(RECURSIVE, RECURSIVE_DEPTH, PROXIES, INCLUDE_RUNTIME, INCLUDE_DEFAULTS, ATTRIBUTES_ONLY, INCLUDE_ALIASES)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .build();

    public static final OperationStepHandler INSTANCE = new ReadResourceHandler();

    private final ParametersValidator validator = new ParametersValidator() {

        @Override
        public void validate(ModelNode operation) throws OperationFailedException {
            super.validate(operation);
            if (operation.hasDefined(ModelDescriptionConstants.ATTRIBUTES_ONLY)) {
                if (operation.hasDefined(ModelDescriptionConstants.RECURSIVE)) {
                    throw MESSAGES.cannotHaveBothParameters(ModelDescriptionConstants.ATTRIBUTES_ONLY, ModelDescriptionConstants.RECURSIVE);
                }
                if (operation.hasDefined(ModelDescriptionConstants.RECURSIVE_DEPTH)) {
                    throw MESSAGES.cannotHaveBothParameters(ModelDescriptionConstants.ATTRIBUTES_ONLY, ModelDescriptionConstants.RECURSIVE_DEPTH);
                }
            }
        }
    };

    public ReadResourceHandler() {
        //todo use AD for validation
        validator.registerValidator(ModelDescriptionConstants.RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.RECURSIVE_DEPTH, new ModelTypeValidator(ModelType.INT, true));
        validator.registerValidator(ModelDescriptionConstants.INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.INCLUDE_DEFAULTS, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.ATTRIBUTES_ONLY, new ModelTypeValidator(ModelType.BOOLEAN, true));
    }


    @Override
    void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);


        final String opName = operation.require(OP).asString();
        final ModelNode opAddr = operation.get(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final int recursiveDepth = operation.get(ModelDescriptionConstants.RECURSIVE_DEPTH).asInt(0);
        final boolean recursive = recursiveDepth > 0 ? true : operation.get(ModelDescriptionConstants.RECURSIVE).asBoolean(false);
        final boolean queryRuntime = operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).asBoolean(false);
        final boolean proxies = operation.get(ModelDescriptionConstants.PROXIES).asBoolean(false);
        final boolean aliases = operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).asBoolean(false);
        final boolean defaults = operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).asBoolean(true);
        final boolean attributesOnly = operation.get(ModelDescriptionConstants.ATTRIBUTES_ONLY).asBoolean(false);

        // Attributes read directly from the model with no special read handler step in the middle
        final Map<String, ModelNode> directAttributes = new HashMap<String, ModelNode>();
        // Children names read directly from the model with no special read handler step in the middle
        final Map<String, ModelNode> directChildren = new HashMap<String, ModelNode>();
        // Attributes of AccessType.METRIC
        final Map<String, ModelNode> metrics = queryRuntime ? new HashMap<String, ModelNode>() : Collections.<String, ModelNode>emptyMap();
        // Non-AccessType.METRIC attributes with a special read handler registered
        final Map<String, ModelNode> otherAttributes = new HashMap<String, ModelNode>();
        // Child resources recursively read
        final Map<PathElement, ModelNode> childResources = recursive ? new LinkedHashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

        // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
        // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

        // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
        final ReadResourceAssemblyHandler assemblyHandler = new ReadResourceAssemblyHandler(directAttributes, metrics, otherAttributes, directChildren, childResources);
        context.addStep(assemblyHandler, queryRuntime ? OperationContext.Stage.VERIFY : OperationContext.Stage.IMMEDIATE, queryRuntime);
        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();

        // Get the model for this resource.
        final Resource resource = nullSafeReadResource(context, registry);

        final Map<String, Set<String>> childrenByType = registry != null ? GlobalOperationHandlers.getChildAddresses(context, address, registry, resource, null) : Collections.<String, Set<String>>emptyMap();
        final ModelNode model = resource.getModel();

        if (model.isDefined()) {
            // Store direct attributes first
            for (String key : model.keys()) {
                // In case someone put some garbage in it
                if (!childrenByType.containsKey(key)) {
                    directAttributes.put(key, model.get(key));
                }
            }
        }

        if (defaults) {
            //get the model description
            final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
            final Locale locale = GlobalOperationHandlers.getLocale(context, operation);
            final ModelNode nodeDescription = descriptionProvider.getModelDescription(locale);

            if (nodeDescription.isDefined() && nodeDescription.hasDefined(ATTRIBUTES)) {
                for (String key : nodeDescription.get(ATTRIBUTES).keys()) {
                    if ((!childrenByType.containsKey(key)) &&
                            (!directAttributes.containsKey(key) || !directAttributes.get(key).isDefined()) &&
                            nodeDescription.get(ATTRIBUTES).hasDefined(key) &&
                            nodeDescription.get(ATTRIBUTES, key).hasDefined(DEFAULT)) {
                        directAttributes.put(key, nodeDescription.get(ATTRIBUTES, key, DEFAULT));
                    }
                }
            }
        }

        if (!attributesOnly) {
            // Next, process child resources
            for (Map.Entry<String, Set<String>> entry : childrenByType.entrySet()) {
                String childType = entry.getKey();
                Set<String> children = entry.getValue();
                if (children.isEmpty()) {
                    // Just treat it like an undefined attribute
                    directAttributes.put(childType, new ModelNode());
                } else {
                    for (String child : children) {
                        if (recursive) {
                            PathElement childPE = PathElement.pathElement(childType, child);
                            PathAddress relativeAddr = PathAddress.pathAddress(childPE);
                            ImmutableManagementResourceRegistration childReg = registry.getSubModel(relativeAddr);
                            if (childReg == null) {
                                throw new OperationFailedException(new ModelNode().set(MESSAGES.noChildRegistry(childType, child)));
                            }
                            // Decide if we want to invoke on this child resource
                            boolean proxy = childReg.isRemote();
                            boolean runtimeResource = childReg.isRuntimeOnly();
                            boolean getChild = !runtimeResource || (queryRuntime && !proxy) || (proxies && proxy);
                            if (!aliases && childReg.isAlias()) {
                                getChild = false;
                            }
                            if (getChild) {
                                final int newDepth = recursiveDepth > 0 ? recursiveDepth - 1 : 0;
                                // Add a step to read the child resource
                                ModelNode rrOp = new ModelNode();
                                rrOp.get(OP).set(opName);
                                rrOp.get(OP_ADDR).set(PathAddress.pathAddress(address, childPE).toModelNode());
                                rrOp.get(ModelDescriptionConstants.RECURSIVE).set(operation.get(ModelDescriptionConstants.RECURSIVE));
                                rrOp.get(ModelDescriptionConstants.RECURSIVE_DEPTH).set(newDepth);
                                rrOp.get(ModelDescriptionConstants.PROXIES).set(proxies);
                                rrOp.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(queryRuntime);
                                rrOp.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(aliases);
                                rrOp.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set(defaults);
                                ModelNode rrRsp = new ModelNode();
                                childResources.put(childPE, rrRsp);

                                OperationStepHandler rrHandler = childReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, opName);
                                context.addStep(rrRsp, rrOp, rrHandler, OperationContext.Stage.IMMEDIATE);
                            }
                        } else {
                            ModelNode childMap = directChildren.get(childType);
                            if (childMap == null) {
                                childMap = new ModelNode();
                                childMap.setEmptyObject();
                                directChildren.put(childType, childMap);
                            }
                            // Add a "child" => undefined
                            childMap.get(child);
                        }
                    }
                }
            }
        }

        // Last, handle attributes with read handlers registered
        final Set<String> attributeNames = registry != null ? registry.getAttributeNames(PathAddress.EMPTY_ADDRESS) : Collections.<String>emptySet();
        for (final String attributeName : attributeNames) {
            final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if (access == null || access.getFlags().contains(AttributeAccess.Flag.ALIAS) && !aliases) {
                continue;
            } else {
                final AttributeAccess.Storage storage = access.getStorageType();

                if (!queryRuntime && storage != AttributeAccess.Storage.CONFIGURATION) {
                    continue;
                }
                final AttributeAccess.AccessType type = access.getAccessType();
                final OperationStepHandler handler = access.getReadHandler();
                if (handler != null) {
                    // Discard any directAttribute map entry for this, as the read handler takes precedence
                    directAttributes.remove(attributeName);
                    // Create the attribute operation
                    final ModelNode attributeOperation = new ModelNode();
                    attributeOperation.get(OP_ADDR).set(opAddr);
                    attributeOperation.get(OP).set(READ_ATTRIBUTE_OPERATION);
                    attributeOperation.get(GlobalOperationHandlers.NAME.getName()).set(attributeName);

                    final ModelNode attrResponse = new ModelNode();
                    if (type == AttributeAccess.AccessType.METRIC) {
                        metrics.put(attributeName, attrResponse);
                    } else {
                        otherAttributes.put(attributeName, attrResponse);
                    }
                    context.addStep(attrResponse, attributeOperation, handler, OperationContext.Stage.IMMEDIATE);
                }
            }
        }
        context.stepCompleted();
    }

    /**
     * Provides a resource for the current step, either from the context, if the context doesn't have one
     * and {@code registry} is runtime-only, it creates a dummy resource.
     */
    private static Resource nullSafeReadResource(final OperationContext context, final ImmutableManagementResourceRegistration registry) {

        Resource result;
        if (registry != null && registry.isRuntimeOnly()) {
            try {
                result = context.readResource(PathAddress.EMPTY_ADDRESS, false);
            } catch (RuntimeException e) {
                result = PlaceholderResource.INSTANCE;
            }
        } else {
            result = context.readResource(PathAddress.EMPTY_ADDRESS, false);
        }
        return result;
    }

    /**
     * Assembles the response to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadResourceAssemblyHandler implements OperationStepHandler {

        private final Map<String, ModelNode> directAttributes;
        private final Map<String, ModelNode> directChildren;
        private final Map<String, ModelNode> metrics;
        private final Map<String, ModelNode> otherAttributes;
        private final Map<PathElement, ModelNode> childResources;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param directAttributes map of attributes read directly from the model with no special read handler step in the middle
         * @param metrics          map of attributes of AccessType.METRIC. Keys are the attribute names, values are the full
         *                         read-attribute response from invoking the attribute's read handler. Will not be {@code null}
         * @param otherAttributes  map of attributes not of AccessType.METRIC that have a read handler registered. Keys
         *                         are the attribute names, values are the full read-attribute response from invoking the
         *                         attribute's read handler. Will not be {@code null}
         * @param directChildren
         * @param childResources   read-resource response from child resources, where the key is the PathAddress
         *                         relative to the address of the operation this handler is handling and the
         *                         value is the full read-resource response. Will not be {@code null}
         */
        private ReadResourceAssemblyHandler(final Map<String, ModelNode> directAttributes, final Map<String, ModelNode> metrics,
                                            final Map<String, ModelNode> otherAttributes, final Map<String, ModelNode> directChildren,
                                            final Map<PathElement, ModelNode> childResources) {
            this.directAttributes = directAttributes;
            this.metrics = metrics;
            this.otherAttributes = otherAttributes;
            this.directChildren = directChildren;
            this.childResources = childResources;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            Map<String, ModelNode> sortedAttributes = new TreeMap<String, ModelNode>();
            Map<String, ModelNode> sortedChildren = new TreeMap<String, ModelNode>();
            boolean failed = false;
            for (Map.Entry<String, ModelNode> entry : otherAttributes.entrySet()) {
                ModelNode value = entry.getValue();
                if (!value.has(FAILURE_DESCRIPTION)) {
                    sortedAttributes.put(entry.getKey(), value.get(RESULT));
                } else if (!failed && value.hasDefined(FAILURE_DESCRIPTION)) {
                    context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                for (Map.Entry<PathElement, ModelNode> entry : childResources.entrySet()) {
                    PathElement path = entry.getKey();
                    ModelNode value = entry.getValue();
                    if (!value.has(FAILURE_DESCRIPTION)) {
                        ModelNode childTypeNode = sortedChildren.get(path.getKey());
                        if (childTypeNode == null) {
                            childTypeNode = new ModelNode();
                            sortedChildren.put(path.getKey(), childTypeNode);
                        }
                        childTypeNode.get(path.getValue()).set(value.get(RESULT));
                    } else if (!failed && value.hasDefined(FAILURE_DESCRIPTION)) {
                        context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                        failed = true;
                    }
                }
            }
            if (!failed) {
                for (Map.Entry<String, ModelNode> simpleAttribute : directAttributes.entrySet()) {
                    sortedAttributes.put(simpleAttribute.getKey(), simpleAttribute.getValue());
                }
                for (Map.Entry<String, ModelNode> directChild : directChildren.entrySet()) {
                    sortedChildren.put(directChild.getKey(), directChild.getValue());
                }
                for (Map.Entry<String, ModelNode> metric : metrics.entrySet()) {
                    ModelNode value = metric.getValue();
                    if (!value.has(FAILURE_DESCRIPTION)) {
                        sortedAttributes.put(metric.getKey(), value.get(RESULT));
                    }
                    // we ignore metric failures
                    // TODO how to prevent the metric failure screwing up the overall context?
                }

                final ModelNode result = context.getResult();
                result.setEmptyObject();
                for (Map.Entry<String, ModelNode> entry : sortedAttributes.entrySet()) {
                    result.get(entry.getKey()).set(entry.getValue());
                }

                for (Map.Entry<String, ModelNode> entry : sortedChildren.entrySet()) {
                    result.get(entry.getKey()).set(entry.getValue());
                }
            }

            context.stepCompleted();
        }
    }
}
