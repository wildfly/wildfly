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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.INCLUDE_ALIASES;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.INCLUDE_RUNTIME;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.PROXIES;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.RECURSIVE;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.RECURSIVE_DEPTH;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.UnauthorizedException;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} reading a part of the model. The result will only contain the current attributes of a node by default,
 * excluding all addressable children and runtime attributes. Setting the request parameter "recursive" to "true" will recursively include
 * all children and configuration attributes. Queries can include runtime attributes by setting the request parameter
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

    private final OperationStepHandler overrideHandler;

    public ReadResourceHandler() {
        this(null, null);
    }

    ReadResourceHandler(final FilteredData filteredData, OperationStepHandler overrideHandler) {
        super(filteredData);
        //todo use AD for validation
        validator.registerValidator(ModelDescriptionConstants.RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.RECURSIVE_DEPTH, new ModelTypeValidator(ModelType.INT, true));
        validator.registerValidator(ModelDescriptionConstants.INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.INCLUDE_DEFAULTS, new ModelTypeValidator(ModelType.BOOLEAN, true));
        validator.registerValidator(ModelDescriptionConstants.ATTRIBUTES_ONLY, new ModelTypeValidator(ModelType.BOOLEAN, true));
        this.overrideHandler = overrideHandler;
    }



    @Override
    void doExecute(OperationContext context, ModelNode operation, FilteredData filteredData) throws OperationFailedException {

        if (filteredData == null) {
            doExecuteInternal(context, operation);
        } else {
            try {
                if (overrideHandler == null) {
                    doExecuteInternal(context, operation);
                } else {
                    overrideHandler.execute(context, operation);
                }
            } catch (NoSuchResourceException nsre) {
                // Just report the failure to the filter and complete normally
                PathAddress pa = PathAddress.pathAddress(operation.get(OP_ADDR));

                filteredData.addAccessRestrictedResource(pa);
                context.getResult().set(new ModelNode());
                context.stepCompleted();
            } catch (UnauthorizedException ue) {
                // Just report the failure to the filter and complete normally
                PathAddress pa = PathAddress.pathAddress(operation.get(OP_ADDR));
                filteredData.addReadRestrictedResource(pa);
                context.getResult().set(new ModelNode());
                context.stepCompleted();
            }
        }
    }

    void doExecuteInternal(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);

        final String opName = operation.require(OP).asString();
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));

        final int recursiveDepth = operation.get(ModelDescriptionConstants.RECURSIVE_DEPTH).asInt(0);
        final boolean recursive = recursiveDepth > 0 || operation.get(ModelDescriptionConstants.RECURSIVE).asBoolean(false);
        final boolean queryRuntime = operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).asBoolean(false);
        final boolean proxies = operation.get(ModelDescriptionConstants.PROXIES).asBoolean(false);
        final boolean aliases = operation.get(ModelDescriptionConstants.INCLUDE_ALIASES).asBoolean(false);
        final boolean defaults = operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).asBoolean(true);
        final boolean attributesOnly = operation.get(ModelDescriptionConstants.ATTRIBUTES_ONLY).asBoolean(false);

        // Child types with no actual children
        final Set<String> nonExistentChildTypes = new HashSet<String>();
        // Children names read directly from the model where we didn't call read-resource to gather data
        // We wouldn't call read-resource if the recursive=false
        final Map<String, ModelNode> directChildren = new HashMap<String, ModelNode>();
        // Attributes of AccessType.METRIC
        final Map<String, ModelNode> metrics = queryRuntime ? new HashMap<String, ModelNode>() : Collections.<String, ModelNode>emptyMap();
        // Non-AccessType.METRIC attributes with a special read handler registered
        final Map<String, ModelNode> otherAttributes = new HashMap<String, ModelNode>();
        // Child resources recursively read
        final Map<PathElement, ModelNode> childResources = recursive ? new LinkedHashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

        final FilteredData localFilteredData = getFilteredData() == null ? new FilteredData(address) : getFilteredData();

        // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
        // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

        // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
        final ReadResourceAssemblyHandler assemblyHandler = new ReadResourceAssemblyHandler(address, metrics,
                otherAttributes, directChildren, childResources, nonExistentChildTypes, localFilteredData);
        context.addStep(assemblyHandler, queryRuntime ? OperationContext.Stage.VERIFY : OperationContext.Stage.MODEL, true);
        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();

        // Get the model for this resource.
        final Resource resource = nullSafeReadResource(context, registry);

        final Map<String, Set<String>> childrenByType = registry != null ? GlobalOperationHandlers.getChildAddresses(context, address, registry, resource, null) : Collections.<String, Set<String>>emptyMap();

        if (!attributesOnly) {
            // Next, process child resources
            for (Map.Entry<String, Set<String>> entry : childrenByType.entrySet()) {

                String childType = entry.getKey();

                // child type has no children until we add one
                nonExistentChildTypes.add(childType);

                for (String child : entry.getValue()) {
                    PathElement childPE = PathElement.pathElement(childType, child);
                    PathAddress absoluteChildAddr = address.append(childPE);

                    ModelNode rrOp = Util.createEmptyOperation(READ_RESOURCE_OPERATION, absoluteChildAddr);
                    PathAddress relativeAddr = PathAddress.pathAddress(childPE);

                    if (recursive) {
                        ImmutableManagementResourceRegistration childReg = registry.getSubModel(relativeAddr);
                        if (childReg == null) {
                            throw new OperationFailedException(new ModelNode().set(MESSAGES.noChildRegistry(childType, child)));
                        }
                        // Decide if we want to invoke on this child resource
                        boolean proxy = childReg.isRemote();
                        boolean runtimeResource = childReg.isRuntimeOnly();
                        boolean getChild = !runtimeResource || (queryRuntime && !proxy) || (proxies && proxy);
                        if (!aliases && childReg.isAlias()) {
                            nonExistentChildTypes.remove(childType);
                            getChild = false;
                        }
                        if (getChild) {
                            nonExistentChildTypes.remove(childType);
                            final int newDepth = recursiveDepth > 0 ? recursiveDepth - 1 : 0;
                            // Add a step to read the child resource
                            rrOp.get(ModelDescriptionConstants.RECURSIVE).set(operation.get(ModelDescriptionConstants.RECURSIVE));
                            rrOp.get(ModelDescriptionConstants.RECURSIVE_DEPTH).set(newDepth);
                            rrOp.get(ModelDescriptionConstants.PROXIES).set(proxies);
                            rrOp.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(queryRuntime);
                            rrOp.get(ModelDescriptionConstants.INCLUDE_ALIASES).set(aliases);
                            rrOp.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set(defaults);
                            ModelNode rrRsp = new ModelNode();
                            childResources.put(childPE, rrRsp);

                            // See if there was an override registered for the standard :read-resource handling (unlikely!!!)
                            OperationStepHandler overrideHandler = childReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, opName);
                            if (overrideHandler != null && overrideHandler.getClass() == getClass()) {
                                // not an override
                                overrideHandler = null;
                            }
                            OperationStepHandler rrHandler = new ReadResourceHandler(localFilteredData, overrideHandler);

                            context.addStep(rrRsp, rrOp, rrHandler, OperationContext.Stage.MODEL, true);
                        }
                    } else {
                        // Non-recursive. Just output the names of the children
                        // But filter inaccessible children
                        AuthorizationResult ar = context.authorize(rrOp, EnumSet.of(Action.ActionEffect.ADDRESS));
                        if (ar.getDecision() == AuthorizationResult.Decision.DENY) {
                            localFilteredData.addAccessRestrictedResource(absoluteChildAddr);
                        } else {
                            ModelNode childMap = directChildren.get(childType);
                            if (childMap == null) {
                                nonExistentChildTypes.remove(childType);
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

        // Handle registered attributes
        final Set<String> attributeNames = registry != null ? registry.getAttributeNames(PathAddress.EMPTY_ADDRESS) : Collections.<String>emptySet();
        for (final String attributeName : attributeNames) {

            final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if ((aliases || !access.getFlags().contains(AttributeAccess.Flag.ALIAS))
                    && (queryRuntime || access.getStorageType() == AttributeAccess.Storage.CONFIGURATION)) {

                Map<String, ModelNode> responseMap = access.getAccessType() == AttributeAccess.AccessType.METRIC ? metrics : otherAttributes;

                addReadAttributeStep(context, address, defaults, localFilteredData, registry, attributeName, responseMap);

            }
        }

        // Any attributes stored in the model but without a registry entry
        final ModelNode model = resource.getModel();
        if (model.isDefined()) {
            for (String key : model.keys()) {
                // Skip children and attributes already handled
                if (!otherAttributes.containsKey(key) && !childrenByType.containsKey(key) && !metrics.containsKey(key)) {
                    addReadAttributeStep(context, address, defaults, localFilteredData, registry, key, otherAttributes);
                }
            }
        }

        // Last, if defaults are desired, look for unregistered attributes also not in the model
        // by checking the resource description
        if (defaults) {
            //get the model description
            final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
            final Locale locale = GlobalOperationHandlers.getLocale(context, operation);
            final ModelNode nodeDescription = descriptionProvider.getModelDescription(locale);

            if (nodeDescription.isDefined() && nodeDescription.hasDefined(ATTRIBUTES)) {
                for (String key : nodeDescription.get(ATTRIBUTES).keys()) {
                    if ((!childrenByType.containsKey(key)) &&
                            !otherAttributes.containsKey(key) &&
                            !metrics.containsKey(key) &&
                            nodeDescription.get(ATTRIBUTES).hasDefined(key) &&
                            nodeDescription.get(ATTRIBUTES, key).hasDefined(DEFAULT)) {
                        addReadAttributeStep(context, address, defaults, localFilteredData, registry, key, otherAttributes);
                    }
                }
            }
        }

        context.stepCompleted();
    }

    private void addReadAttributeStep(OperationContext context, PathAddress address, boolean defaults, FilteredData localFilteredData, ImmutableManagementResourceRegistration registry, String attributeName, Map<String, ModelNode> responseMap) {
        // See if there was an override registered for the standard :read-attribute handling (unlikely!!!)
        OperationStepHandler overrideHandler = registry.getOperationHandler(PathAddress.EMPTY_ADDRESS, READ_ATTRIBUTE_OPERATION);
        if (overrideHandler != null && overrideHandler == ReadAttributeHandler.INSTANCE) {
            // not an override
            overrideHandler = null;
        }

        OperationStepHandler readAttributeHandler = new ReadAttributeHandler(localFilteredData, overrideHandler);

        final ModelNode attributeOperation = Util.getReadAttributeOperation(address, attributeName);
        attributeOperation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).set(defaults);

        final ModelNode attrResponse = new ModelNode();
        responseMap.put(attributeName, attrResponse);

        context.addStep(attrResponse, attributeOperation, readAttributeHandler, OperationContext.Stage.MODEL, true);
    }

    /**
     * Provides a resource for the current step, either from the context, if the context doesn't have one
     * and {@code registry} is runtime-only, it creates a dummy resource.
     */
    private static Resource nullSafeReadResource(final OperationContext context, final ImmutableManagementResourceRegistration registry) {

        Resource result;
        if (registry != null && registry.isRuntimeOnly()) {
            try {
                //TODO check that having changed this from false to true does not break anything
                //If it does, consider adding a Resource.alwaysClone() method that can be used in
                //OperationContextImpl.readResourceFromRoot(final PathAddress address, final boolean recursive)
                //instead of the recursive check
                result = context.readResource(PathAddress.EMPTY_ADDRESS, true);
            } catch (RuntimeException e) {
                result = PlaceholderResource.INSTANCE;
            }
        } else {
            //TODO check that having changed this from false to true does not break anything
            //If it does, consider adding a Resource.alwaysClone() method that can be used in
            //OperationContextImpl.readResourceFromRoot(final PathAddress address, final boolean recursive)
            //instead of the recursive check
            result = context.readResource(PathAddress.EMPTY_ADDRESS, true);
        }
        return result;
    }

    /**
     * Assembles the response to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadResourceAssemblyHandler implements OperationStepHandler {

        private final PathAddress address;
        private final Map<String, ModelNode> directChildren;
        private final Map<String, ModelNode> metrics;
        private final Map<String, ModelNode> otherAttributes;
        private final Map<PathElement, ModelNode> childResources;
        private final Set<String> nonExistentChildTypes;
        private final FilteredData filteredData;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param address          address of the resource
         * @param metrics          map of attributes of AccessType.METRIC. Keys are the attribute names, values are the full
         *                         read-attribute response from invoking the attribute's read handler. Will not be {@code null}
         * @param otherAttributes  map of attributes not of AccessType.METRIC that have a read handler registered. Keys
 *                         are the attribute names, values are the full read-attribute response from invoking the
 *                         attribute's read handler. Will not be {@code null}
         * @param directChildren   Children names read directly from the parent resource where we didn't call read-resource
*                         to gather data. We wouldn't call read-resource if the recursive=false
         * @param childResources   read-resource response from child resources, where the key is the PathAddress
*                         relative to the address of the operation this handler is handling and the
*                         value is the full read-resource response. Will not be {@code null}
         * @param nonExistentChildTypes names of child types where no data is available
         * @param filteredData     information about resources and attributes that were filtered
         */
        private ReadResourceAssemblyHandler(final PathAddress address,
                                            final Map<String, ModelNode> metrics,
                                            final Map<String, ModelNode> otherAttributes, final Map<String, ModelNode> directChildren,
                                            final Map<PathElement, ModelNode> childResources, final Set<String> nonExistentChildTypes, FilteredData filteredData) {
            this.address = address;
            this.metrics = metrics;
            this.otherAttributes = otherAttributes;
            this.directChildren = directChildren;
            this.childResources = childResources;
            this.nonExistentChildTypes = nonExistentChildTypes;
            this.filteredData = filteredData;
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
                } else if (value.hasDefined(FAILURE_DESCRIPTION)) {
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
                for (Map.Entry<String, ModelNode> directChild : directChildren.entrySet()) {
                    sortedChildren.put(directChild.getKey(), directChild.getValue());
                }
                for (String nonExistentChildType : nonExistentChildTypes) {
                    sortedChildren.put(nonExistentChildType, new ModelNode());
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
                    if (!entry.getValue().isDefined()) {
                        result.get(entry.getKey()).set(entry.getValue());
                    } else {
                        ModelNode childTypeNode = new ModelNode();
                        for (Property property : entry.getValue().asPropertyList()) {
                            PathElement pe = PathElement.pathElement(entry.getKey(), property.getName());
                            if (!filteredData.isFilteredResource(address, pe)) {
                                childTypeNode.get(property.getName()).set(property.getValue());
                            }
                        }
                        result.get(entry.getKey()).set(childTypeNode);
                    }
                }

                if (filteredData.hasFilteredData()) {
                    context.getResponseHeaders().get(ACCESS_CONTROL).set(filteredData.toModelNode());
                }
            }

            context.stepCompleted();
        }
    }
}
