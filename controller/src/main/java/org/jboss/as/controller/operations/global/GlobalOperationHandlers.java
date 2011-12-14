/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE_DEPTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Global {@code OperationHandler}s.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GlobalOperationHandlers {

    public static final OperationStepHandler READ_RESOURCE = new ReadResourceHandler();
    public static final OperationStepHandler READ_ATTRIBUTE = new ReadAttributeHandler();
    public static final OperationStepHandler READ_CHILDREN_NAMES = new ReadChildrenNamesOperationHandler();
    public static final OperationStepHandler READ_CHILDREN_RESOURCES = new ReadChildrenResourcesOperationHandler();
    public static final OperationStepHandler UNDEFINE_ATTRIBUTE = new UndefineAttributeHandler();
    public static final OperationStepHandler WRITE_ATTRIBUTE = new WriteAttributeHandler();
    public static final OperationStepHandler VALIDATE_ADDRESS = new OperationStepHandler() {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            try {
                context.readResource(PathAddress.EMPTY_ADDRESS);
            } catch (Exception e) {
                context.getFailureDescription().set(new ModelNode().set(MESSAGES.resourceNotFound(operation.get(OP_ADDR))));
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    };


    public static final String VALIDATE_ADDRESS_OPERATION_NAME = "validate-address";

    private GlobalOperationHandlers() {
        //
    }

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} reading a part of the model. The result will only contain the current attributes of a node by default,
     * excluding all addressable children and runtime attributes. Setting the request parameter "recursive" to "true" will recursively include
     * all children and configuration attributes. Non-recursive queries can include runtime attributes by setting the request parameter
     * "include-runtime" to "true".
     */
    public static class ReadResourceHandler extends AbstractMultiTargetHandler implements OperationStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadResourceHandler() {
            validator.registerValidator(RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(RECURSIVE_DEPTH, new ModelTypeValidator(ModelType.INT, true));
            validator.registerValidator(INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(INCLUDE_DEFAULTS, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);

            final String opName = operation.require(OP).asString();
            final ModelNode opAddr = operation.get(OP_ADDR);
            final PathAddress address = PathAddress.pathAddress(opAddr);
            final int recursiveDepth = operation.get(RECURSIVE_DEPTH).asInt(0);
            final boolean recursive = recursiveDepth > 0 ? true : operation.get(RECURSIVE).asBoolean(false);
            final boolean queryRuntime = operation.get(INCLUDE_RUNTIME).asBoolean(false);
            final boolean proxies = operation.get(PROXIES).asBoolean(false);
            final boolean defaults = operation.get(INCLUDE_DEFAULTS).asBoolean(true);

            // Attributes read directly from the model with no special read handler step in the middle
            final Map<String, ModelNode> directAttributes = new HashMap<String, ModelNode>();
            // Children names read directly from the model with no special read handler step in the middle
            final Map<String, ModelNode> directChildren = new HashMap<String, ModelNode>();
            // Attributes of AccessType.METRIC
            final Map<String, ModelNode> metrics = queryRuntime ? new HashMap<String, ModelNode>() : Collections.<String, ModelNode>emptyMap();
            // Non-AccessType.METRIC attributes with a special read handler registered
            final Map<String, ModelNode> otherAttributes = new HashMap<String, ModelNode>();
            // Child resources recursively read
            final Map<PathElement, ModelNode> childResources = recursive ? new HashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

            // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
            // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

            // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
            final ReadResourceAssemblyHandler assemblyHandler = new ReadResourceAssemblyHandler(directAttributes, metrics, otherAttributes, directChildren, childResources);
            context.addStep(assemblyHandler, queryRuntime ? OperationContext.Stage.VERIFY : OperationContext.Stage.IMMEDIATE, queryRuntime);
            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();

            // Get the model for this resource.
            final Resource resource = nullSafeReadResource(context, registry);
            final Map<String, Set<String>> childrenByType = registry != null ? getChildAddresses(registry, resource, null) : Collections.<String, Set<String>>emptyMap();
            final ModelNode model = resource.getModel();

            if (model.isDefined()) {
                // Store direct attributes first
                for (String key : model.keys()) {
                    // In case someone put some garbage in it
                    if (!childrenByType.containsKey(key)) {
                        directAttributes.put(key, model.get(key));
                    }
                }
                if (defaults) {
                    //get the model description
                    final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
                    final Locale locale = getLocale(operation);
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
            }


            // Next, process child resources
            for (Map.Entry<String, Set<String>> entry : childrenByType.entrySet()) {
                String childType = entry.getKey();
                Set<String> children = entry.getValue();
                if (children.isEmpty()) {
                    // Just treat it like an undefined attribute
                    directAttributes.put(childType, new ModelNode());
                } else {
                    for (String child : children) {
                        boolean storeDirect = !recursive;
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
                            if (!runtimeResource || (queryRuntime && !proxy)  || (proxies && proxy)) {
                                final int newDepth = recursiveDepth > 0 ? recursiveDepth - 1 : 0;
                                // Add a step to read the child resource
                                ModelNode rrOp = new ModelNode();
                                rrOp.get(OP).set(opName);
                                rrOp.get(OP_ADDR).set(PathAddress.pathAddress(address, childPE).toModelNode());
                                rrOp.get(RECURSIVE).set(true);
                                rrOp.get(PROXIES).set(proxies);
                                rrOp.get(INCLUDE_RUNTIME).set(queryRuntime);
                                ModelNode rrRsp = new ModelNode();
                                childResources.put(childPE, rrRsp);

                                OperationStepHandler rrHandler = childReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, opName);
                                context.addStep(rrRsp, rrOp, rrHandler, OperationContext.Stage.IMMEDIATE);
                            } else {
                                storeDirect = true;
                            }
                        }
                        if (storeDirect) {
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

            // Last, handle attributes with read handlers registered
            final Set<String> attributeNames = registry != null ? registry.getAttributeNames(PathAddress.EMPTY_ADDRESS) : Collections.<String>emptySet();
            for (final String attributeName : attributeNames) {
                final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
                if (access == null) {
                    continue;
                } else {
                    final AttributeAccess.Storage storage = access.getStorageType();

                    if (! queryRuntime && storage != AttributeAccess.Storage.CONFIGURATION) {
                        continue;
                    }
                    final AccessType type = access.getAccessType();
                    final OperationStepHandler handler = access.getReadHandler();
                    if (handler != null) {
                        // Discard any directAttribute map entry for this, as the read handler takes precedence
                        directAttributes.remove(attributeName);
                        // Create the attribute operation
                        final ModelNode attributeOperation = new ModelNode();
                        attributeOperation.get(OP_ADDR).set(opAddr);
                        attributeOperation.get(OP).set(READ_ATTRIBUTE_OPERATION);
                        attributeOperation.get(NAME).set(attributeName);

                        final ModelNode attrResponse = new ModelNode();
                        if (type == AccessType.METRIC) {
                            metrics.put(attributeName, attrResponse);
                        } else {
                            otherAttributes.put(attributeName, attrResponse);
                        }
                        context.addStep(attrResponse, attributeOperation, handler, OperationContext.Stage.IMMEDIATE);
                    }
                }
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        /**
         *  Provides a resource for the current step, either from the context, if the context doesn't have one
         *  and {@code registry} is runtime-only, it creates a dummy resource.
         */
        private static Resource nullSafeReadResource(final OperationContext context, final ImmutableManagementResourceRegistration registry) {

            Resource result;
            if (registry != null && registry.isRuntimeOnly()) {
                try {
                    result = context.readResource(PathAddress.EMPTY_ADDRESS);
                } catch (RuntimeException e) {
                    result = PlaceholderResource.INSTANCE;
                }
            } else {
                result = context.readResource(PathAddress.EMPTY_ADDRESS);
            }
            return result;
        }
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
         * @param directAttributes
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
                                            final Map<String, ModelNode> otherAttributes, Map<String, ModelNode> directChildren, final Map<PathElement, ModelNode> childResources) {
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

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} reading a single attribute at the given operation address. The required request parameter "name" represents the attribute name.
     */
    public static class ReadAttributeHandler extends AbstractMultiTargetHandler implements OperationStepHandler {

        private ParametersValidator validator = new ParametersValidator();

        public ReadAttributeHandler() {
            validator.registerValidator(NAME, new StringLengthValidator(1));
            validator.registerValidator(INCLUDE_DEFAULTS, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {
            validator.validate(operation);
            final String attributeName = operation.require(NAME).asString();
            final boolean defaults = operation.get(INCLUDE_DEFAULTS).asBoolean(true);

            final ModelNode subModel = safeReadModel(context);
            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            final AttributeAccess attributeAccess = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);


            if (attributeAccess == null) {
                final Set<String> children = context.getResourceRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
                if (children.contains(attributeName)) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeRegisteredOnResource(attributeName, operation.get(OP_ADDR))));
                } else if (subModel.hasDefined(attributeName)) {
                    final ModelNode result = subModel.get(attributeName);
                    context.getResult().set(result);
                } else {
                    // No defined value in the model. See if we should reply with a default from the metadata,
                    // reply with undefined, or fail because it's a non-existent attribute name
                    final ModelNode nodeDescription = getNodeDescription(registry, operation);
                    if (defaults && nodeDescription.get(ATTRIBUTES).hasDefined(attributeName) &&
                            nodeDescription.get(ATTRIBUTES, attributeName).hasDefined(DEFAULT)) {
                        final ModelNode result = nodeDescription.get(ATTRIBUTES, attributeName, DEFAULT);
                        context.getResult().set(result);
                    } else if (subModel.has(attributeName) || nodeDescription.get(ATTRIBUTES).has(attributeName)) {
                        // model had no defined value, but we treat its existence in the model or the metadata
                        // as proof that it's a legit attribute name
                        context.getResult(); // this initializes the "result" to ModelType.UNDEFINED
                    } else {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
                    }
                }
                // Complete the step for the unregistered attribute case
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else if (attributeAccess.getReadHandler() == null) {
                // We know the attribute name is legit as it's in the registry, so this case is simpler
                if (subModel.hasDefined(attributeName) || !defaults) {
                    final ModelNode result = subModel.get(attributeName);
                    context.getResult().set(result);
                } else {
                    // It wasn't in the model, but user wants a default value from metadata if there is one
                    final ModelNode nodeDescription = getNodeDescription(registry, operation);
                    if (nodeDescription.get(ATTRIBUTES).hasDefined(attributeName) &&
                            nodeDescription.get(ATTRIBUTES, attributeName).hasDefined(DEFAULT)) {
                        final ModelNode result = nodeDescription.get(ATTRIBUTES, attributeName, DEFAULT);
                        context.getResult().set(result);
                    } else {
                        context.getResult(); // this initializes the "result" to ModelType.UNDEFINED
                    }
                }
                // Complete the step for the "registered attribute but default read handler" case
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                OperationStepHandler handler = attributeAccess.getReadHandler();
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(handler.getClass());
                try {
                    handler.execute(context, operation);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
                // no context.completeStep() here as that's the read handler's job
            }
        }

        private ModelNode getNodeDescription(ImmutableManagementResourceRegistration registry, ModelNode operation) {
            final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
            final Locale locale = getLocale(operation);
            return descriptionProvider.getModelDescription(locale);
        }
    }

    ;

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
     */
    public static class WriteAttributeHandler implements OperationStepHandler {

        private ParametersValidator nameValidator = new ParametersValidator();

        public WriteAttributeHandler() {
            nameValidator.registerValidator(NAME, new StringLengthValidator(1));
        }

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            nameValidator.validate(operation);
            final String attributeName = operation.require(NAME).asString();
            final AttributeAccess attributeAccess = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if (attributeAccess == null) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
            } else if (attributeAccess.getAccessType() != AccessType.READ_WRITE) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeNotWritable(attributeName)));
            } else {
                OperationStepHandler handler = attributeAccess.getWriteHandler();
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(handler.getClass());
                try {
                    handler.execute(context, operation);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        }
    }

    ;

    /**
     * The undefine-attribute handler, writing an undefined value for a single attribute.
     */
    public static class UndefineAttributeHandler extends WriteAttributeHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode original) throws OperationFailedException {
            final ModelNode operation = original.clone();
            operation.get(VALUE).set(new ModelNode());
            super.execute(context, operation);
        }

    }

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the children names of a given "child-type".
     */
    public static class ReadChildrenNamesOperationHandler implements OperationStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadChildrenNamesOperationHandler() {
            validator.registerValidator(CHILD_TYPE, new StringLengthValidator(1));
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);
            final String childType = operation.require(CHILD_TYPE).asString();
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            Map<String, Set<String>> childAddresses = getChildAddresses(registry, resource, childType);
            Set<String> childNames = childAddresses.get(childType);
            if (childNames == null) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownChildType(childType)));
            }
            // Sort the result
            childNames = new TreeSet<String>(childNames);
            ModelNode result = context.getResult();
            result.setEmptyList();
            for (String childName : childNames) {
                result.add(childName);
            }

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the children resources of a given "child-type".
     */
    public static class ReadChildrenResourcesOperationHandler implements OperationStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadChildrenResourcesOperationHandler() {
            validator.registerValidator(CHILD_TYPE, new StringLengthValidator(1));
            validator.registerValidator(RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(RECURSIVE_DEPTH, new ModelTypeValidator(ModelType.INT, true));
            validator.registerValidator(INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(INCLUDE_DEFAULTS, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);
            final String childType = operation.require(CHILD_TYPE).asString();

            final Set<String> childNames = context.getResourceRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
            if (!childNames.contains(childType)) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownChildType(childType)));
            }
            final Map<PathElement, ModelNode> resources = new HashMap<PathElement, ModelNode>();

            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            if (!resource.hasChildren(childType)) {
                context.getResult().setEmptyObject();
            } else {
                // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
                // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

                // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
                final ReadChildrenResourcesAssemblyHandler assemblyHandler = new ReadChildrenResourcesAssemblyHandler(resources);
                context.addStep(assemblyHandler, OperationContext.Stage.IMMEDIATE);

                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                for (final String key : resource.getChildrenNames(childType)) {
                    final PathElement childPath = PathElement.pathElement(childType, key);
                    final PathAddress childAddress = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(childType, key));

                    final ModelNode readOp = new ModelNode();
                    readOp.get(OP).set(READ_RESOURCE_OPERATION);
                    readOp.get(OP_ADDR).set(PathAddress.pathAddress(address, childPath).toModelNode());

                    if (operation.hasDefined(INCLUDE_RUNTIME)) {
                        readOp.get(INCLUDE_RUNTIME).set(operation.get(INCLUDE_RUNTIME));
                    }
                    if (operation.hasDefined(RECURSIVE)) {
                        readOp.get(RECURSIVE).set(operation.get(RECURSIVE));
                    }
                    if(operation.hasDefined(RECURSIVE_DEPTH)) {
                        readOp.get(RECURSIVE_DEPTH).set(operation.get(RECURSIVE_DEPTH));
                    }
                    if (operation.hasDefined(PROXIES)) {
                        readOp.get(PROXIES).set(operation.get(PROXIES));
                    }
                    if (operation.hasDefined(INCLUDE_DEFAULTS)) {
                        readOp.get(INCLUDE_DEFAULTS).set(operation.get(INCLUDE_DEFAULTS));
                    }
                    final OperationStepHandler handler = context.getResourceRegistration().getOperationHandler(childAddress, READ_RESOURCE_OPERATION);
                    if (handler == null) {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.noOperationHandler()));
                    }
                    ModelNode rrRsp = new ModelNode();
                    resources.put(childPath, rrRsp);
                    context.addStep(rrRsp, readOp, handler, OperationContext.Stage.IMMEDIATE);
                }
            }

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    ;

    /**
     * Assembles the response to a read-resource request from the components gathered by earlier steps.
     */
    public static class ReadChildrenResourcesAssemblyHandler implements OperationStepHandler {

        private final Map<PathElement, ModelNode> resources;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param resources read-resource response from child resources, where the key is the path of the resource
         *                  relative to the address of the operation this handler is handling and the
         *                  value is the full read-resource response. Will not be {@code null}
         */
        public ReadChildrenResourcesAssemblyHandler(final Map<PathElement, ModelNode> resources) {
            this.resources = resources;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    Map<String, ModelNode> sortedChildren = new TreeMap<String, ModelNode>();
                    boolean failed = false;
                    for (Map.Entry<PathElement, ModelNode> entry : resources.entrySet()) {
                        PathElement path = entry.getKey();
                        ModelNode value = entry.getValue();
                        if (!value.has(FAILURE_DESCRIPTION)) {
                            sortedChildren.put(path.getValue(), value.get(RESULT));
                        } else if (!failed && value.hasDefined(FAILURE_DESCRIPTION)) {
                            context.getFailureDescription().set(value.get(FAILURE_DESCRIPTION));
                            failed = true;
                        }
                    }
                    if (!failed) {
                        final ModelNode result = context.getResult();
                        result.setEmptyObject();

                        for (Map.Entry<String, ModelNode> entry : sortedChildren.entrySet()) {
                            result.get(entry.getKey()).set(entry.getValue());
                        }
                    }

                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.VERIFY);

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the child types of a given node.
     */
    public static final OperationStepHandler READ_CHILDREN_TYPES = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            Set<String> childTypes = new TreeSet<String>(registry.getChildNames(PathAddress.EMPTY_ADDRESS));
            final ModelNode result = context.getResult();
            result.setEmptyList();
            for (final String key : childTypes) {
                result.add(key);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    };

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} returning the names of the defined operations at a given model address.
     */
    public static final OperationStepHandler READ_OPERATION_NAMES = new OperationStepHandler() {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            final Map<String, OperationEntry> operations = registry.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, true);

            final ModelNode result = new ModelNode();
            if (operations.size() > 0) {
                for (final Entry<String, OperationEntry> entry : operations.entrySet()) {
                    if (entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                        result.add(entry.getKey());
                    }
                }
            } else {
                result.setEmptyList();
            }
            context.getResult().set(result);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    };

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} returning the type description of a single operation description.
     */
    public static final OperationStepHandler READ_OPERATION_DESCRIPTION = new OperationStepHandler() {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            String operationName = operation.require(NAME).asString();

            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            OperationEntry operationEntry = registry.getOperationEntry(PathAddress.EMPTY_ADDRESS, operationName);
            if (operationEntry == null) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.operationNotRegistered(operationName,
                        PathAddress.pathAddress(operation.require(OP_ADDR)))));
            } else {
                final ModelNode result = operationEntry.getDescriptionProvider().getModelDescription(getLocale(operation));
                Set<OperationEntry.Flag> flags = operationEntry.getFlags();
                boolean readOnly = flags.contains(OperationEntry.Flag.READ_ONLY);
                result.get(READ_ONLY).set(readOnly);
                if (!readOnly) {
                    if (flags.contains(OperationEntry.Flag.RESTART_ALL_SERVICES)) {
                        result.get(RESTART_REQUIRED).set("all-services");
                    } else if (flags.contains(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)) {
                        result.get(RESTART_REQUIRED).set("resource-services");
                    } else if (flags.contains(OperationEntry.Flag.RESTART_JVM)) {
                        result.get(RESTART_REQUIRED).set("jvm");
                    }
                }

                context.getResult().set(result);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    };

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the complete type description of a given model node.
     */
    public static final OperationStepHandler READ_RESOURCE_DESCRIPTION = new OperationStepHandler() {
        private final ParametersValidator validator = new ParametersValidator();

        {
            validator.registerValidator(RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(RECURSIVE_DEPTH, new ModelTypeValidator(ModelType.INT, true));
            validator.registerValidator(PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(OPERATIONS, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(INHERITED, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            if (address.isMultiTarget()) {
                // Format wildcard queries as list
                final ModelNode result = context.getResult().setEmptyList();
                context.addStep(new ModelNode(), AbstractMultiTargetHandler.FAKE_OPERATION, new RegistrationAddressResolver(operation, result,
                        new OperationStepHandler() {
                            @Override
                            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                                // step handler bypassing further wildcard resolution
                                doExecute(context, operation);
                            }
                        }), OperationContext.Stage.IMMEDIATE);
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                doExecute(context, operation);
            }
        }

        void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);

            final String opName = operation.require(OP).asString();
            final ModelNode opAddr = operation.get(OP_ADDR);
            final PathAddress address = PathAddress.pathAddress(opAddr);
            final int recursiveDepth = operation.get(RECURSIVE_DEPTH).asInt(0);
            final boolean recursive = recursiveDepth > 0 ? true : operation.get(RECURSIVE).asBoolean(false);
            final boolean proxies = operation.get(PROXIES).asBoolean(false);
            final boolean ops = operation.get(OPERATIONS).asBoolean(false);
            final boolean inheritedOps = operation.get(INHERITED).asBoolean(true);

            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
            final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
            final Locale locale = getLocale(operation);

            final ModelNode nodeDescription = descriptionProvider.getModelDescription(locale);
            final Map<String, ModelNode> operations = new HashMap<String, ModelNode>();
            final Map<PathElement, ModelNode> childResources = recursive ? new HashMap<PathElement, ModelNode>() : Collections.<PathElement, ModelNode>emptyMap();

            // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
            // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

            // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
            final ReadResourceDescriptionAssemblyHandler assemblyHandler = new ReadResourceDescriptionAssemblyHandler(nodeDescription, operations, childResources);
            context.addStep(assemblyHandler, OperationContext.Stage.IMMEDIATE);

            if (ops) {
                for (final Map.Entry<String, OperationEntry> entry : registry.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, inheritedOps).entrySet()) {
                    if (entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                        final DescriptionProvider provider = entry.getValue().getDescriptionProvider();
                        operations.put(entry.getKey(), provider.getModelDescription(locale));
                    }
                }
            }
            if (nodeDescription.hasDefined(ATTRIBUTES)) {
                for (final String attr : nodeDescription.require(ATTRIBUTES).keys()) {
                    final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attr);
                    // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                    // be writable without a registered handler. This opens the possibility that out-of-date metadata
                    // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                    // an unexpected undefined value returned. But it removes the possibility of a
                    // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                    // in the valid attribute "foo" not being readable
                    final AccessType accessType = access == null ? AccessType.READ_ONLY : access.getAccessType();
                    final Storage storage = access == null ? Storage.CONFIGURATION : access.getStorageType();
                    final ModelNode attrNode = nodeDescription.get(ATTRIBUTES, attr);
                    attrNode.get(ACCESS_TYPE).set(accessType.toString());
                    attrNode.get(STORAGE).set(storage.toString());
                    if (accessType == AccessType.READ_WRITE) {
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
                for (final PathElement element : registry.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
                    PathAddress relativeAddr = PathAddress.pathAddress(element);
                    ImmutableManagementResourceRegistration childReg = registry.getSubModel(relativeAddr);

                    boolean readChild = true;
                    if (childReg.isRemote() && !proxies) {
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
                        rrOp.get(RECURSIVE).set(operation.get(RECURSIVE));
                        rrOp.get(RECURSIVE_DEPTH).set(newDepth);
                        rrOp.get(PROXIES).set(proxies);
                        rrOp.get(OPERATIONS).set(ops);
                        rrOp.get(INHERITED).set(inheritedOps);
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
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    };

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
                nodeDescription.get(OPERATIONS, entry.getKey()).set(entry.getValue());
            }

            context.getResult().set(nodeDescription);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    public abstract static class AbstractMultiTargetHandler implements OperationStepHandler {

        public static final ModelNode FAKE_OPERATION;

        static {
            final ModelNode resolve = new ModelNode();
            resolve.get(OP).set("resolve");
            resolve.get(OP_ADDR).setEmptyList();
            resolve.protect();
            FAKE_OPERATION = resolve;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            // In case if it's a multiTarget operation, resolve the address first
            // This only works for model resources, which can be resolved into a concrete addresses
            if (address.isMultiTarget()) {
                // The final result should be a list of executed operations
                final ModelNode result = context.getResult().setEmptyList();
                // Trick the context to give us the model-root
                context.addStep(new ModelNode(), FAKE_OPERATION, new ModelAddressResolver(operation, result, new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        doExecute(context, operation);
                    }
                }), OperationContext.Stage.IMMEDIATE);
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                doExecute(context, operation);
            }
        }

        /**
         * Execute the actual operation if it is not addressed to multiple targets.
         *
         * @param context   the operation context
         * @param operation the original operation
         * @throws OperationFailedException
         */
        abstract void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException;
    }

    public static final class ModelAddressResolver implements OperationStepHandler {

        private final ModelNode operation;
        private final ModelNode result;
        private final OperationStepHandler handler; // handler bypassing further wildcard resolution

        public ModelAddressResolver(final ModelNode operation, final ModelNode result, final OperationStepHandler delegate) {
            this.operation = operation;
            this.result = result;
            this.handler = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            execute(address, PathAddress.EMPTY_ADDRESS, context);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        void execute(final PathAddress address, final PathAddress base, final OperationContext context) {
            final Resource resource = context.readResource(base);
            final PathAddress current = address.subAddress(base.size());
            final Iterator<PathElement> iterator = current.iterator();
            if (iterator.hasNext()) {
                final PathElement element = iterator.next();
                if (element.isMultiTarget()) {
                    final String childType = element.getKey().equals("*") ? null : element.getKey();
                    final ImmutableManagementResourceRegistration registration = context.getResourceRegistration().getSubModel(base);
                    if (registration.isRemote() || registration.isRuntimeOnly()) {
                        // At least for proxies it should use the proxy operation handler
                        throw new IllegalStateException();
                    }
                    final Map<String, Set<String>> resolved = getChildAddresses(registration, resource, childType);
                    for (Map.Entry<String, Set<String>> entry : resolved.entrySet()) {
                        final String key = entry.getKey();
                        final Set<String> children = entry.getValue();
                        if (children.isEmpty()) {
                            continue;
                        }
                        if (element.isWildcard()) {
                            for (final String child : children) {
                                // Double check if the child actually exists
                                if (resource.hasChild(PathElement.pathElement(key, child))) {
                                    execute(address, base.append(PathElement.pathElement(key, child)), context);
                                }
                            }
                        } else {
                            for (final String segment : element.getSegments()) {
                                if (children.contains(segment)) {
                                    // Double check if the child actually exists
                                    if (resource.hasChild(PathElement.pathElement(key, segment))) {
                                        execute(address, base.append(PathElement.pathElement(key, segment)), context);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Double check if the child actually exists
                    if (resource.hasChild(element)) {
                        execute(address, base.append(element), context);
                    }
                }
            } else {
                final String operationName = operation.require(OP).asString();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(base.toModelNode());

                final ModelNode result = this.result.add();
                result.get(OP_ADDR).set(base.toModelNode());
                context.addStep(result, newOp, handler, OperationContext.Stage.IMMEDIATE);
            }
        }

    }

    static class RegistrationAddressResolver implements OperationStepHandler {

        private final ModelNode operation;
        private final ModelNode result;
        private final OperationStepHandler handler; // handler bypassing further wildcard resolution

        RegistrationAddressResolver(final ModelNode operation, final ModelNode result, final OperationStepHandler delegate) {
            this.operation = operation;
            this.result = result;
            this.handler = delegate;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            execute(address, PathAddress.EMPTY_ADDRESS, context);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        void execute(final PathAddress address, PathAddress base, final OperationContext context) {
            final PathAddress current = address.subAddress(base.size());
            final Iterator<PathElement> iterator = current.iterator();
            if (iterator.hasNext()) {
                final PathElement element = iterator.next();
                if (element.isMultiTarget()) {
                    final Set<PathElement> children = context.getResourceRegistration().getChildAddresses(base);
                    if (children == null || children.isEmpty()) {
                        return;
                    }
                    final String childType = element.getKey().equals("*") ? null : element.getKey();
                    for (final PathElement path : children) {
                        if (childType != null && !childType.equals(path.getKey())) {
                            continue;
                        }
                        execute(address, base.append(path), context);
                    }
                } else {
                    execute(address, base.append(element), context);
                }
            } else {
                final String operationName = operation.require(OP).asString();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(base.toModelNode());

                final ModelNode result = this.result.add();
                result.get(OP_ADDR).set(base.toModelNode());
                context.addStep(result, newOp, handler, OperationContext.Stage.IMMEDIATE);
            }
        }
    }

    private static ModelNode safeReadModel(final OperationContext context) {
        try {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode result = resource.getModel();
            if (result.isDefined()) {
                return result;
            }
        } catch (Exception e) {
            // ignore
        }
        return new ModelNode().setEmptyObject();
    }

    /**
     * Gets the addresses of the child resources under the given resource.
     *
     * @param registry       registry entry representing the resource
     * @param resource       the current resource
     * @param validChildType a single child type to which the results should be limited. If {@code null} the result
     *                       should include all child types
     * @return map where the keys are the child types and the values are a set of child names associated with a type
     */
    private static Map<String, Set<String>> getChildAddresses(final ImmutableManagementResourceRegistration registry, Resource resource, final String validChildType) {

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        Set<PathElement> elements = registry.getChildAddresses(PathAddress.EMPTY_ADDRESS);
        for (PathElement element : elements) {
            String childType = element.getKey();
            if (validChildType != null && !validChildType.equals(childType)) {
                continue;
            }
            Set<String> set = result.get(childType);
            if (set == null) {
                set = new HashSet<String>();
                result.put(childType, set);
                if (resource.hasChildren(childType)) {
                    set.addAll(resource.getChildrenNames(childType));
                }
            }
            if (!element.isWildcard()) {
                ImmutableManagementResourceRegistration childReg = registry.getSubModel(PathAddress.pathAddress(element));
                if (childReg != null && childReg.isRuntimeOnly()) {
                    set.add(element.getValue());
                }
            }
        }

        return result;
    }

    private static Locale getLocale(final ModelNode operation) {
        if (!operation.hasDefined(LOCALE)) {
            return null;
        }
        return new Locale(operation.get(LOCALE).asString());
    }


}
