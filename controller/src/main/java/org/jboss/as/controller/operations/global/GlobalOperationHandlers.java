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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyStepHandler;
import org.jboss.as.controller.client.NewOperation;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Global {@code OperationHanlder}s.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GlobalOperationHandlers {

    public static final NewStepHandler READ_RESOURCE = new ReadResourceHandler();
    public static final NewStepHandler READ_ATTRIBUTE = new ReadAttributeHandler();
    public static final NewStepHandler READ_CHILDREN_NAMES = new ReadChildrenNamesOperationHandler();
    public static final NewStepHandler READ_CHILDREN_RESOURCES = new ReadChildrenResourcesOperationHandler();
    public static final NewStepHandler WRITE_ATTRIBUTE = new WriteAttributeHandler();
    public static final ResolveAddressOperationHandler RESOLVE = new ResolveAddressOperationHandler();

    private GlobalOperationHandlers() {
        //
    }

    /**
     * {@link OperationHandler} reading a part of the model. The result will only contain the current attributes of a node by default,
     * excluding all addressable children and runtime attributes. Setting the request parameter "recursive" to "true" will recursively include
     * all children and configuration attributes. Non-recursive queries can include runtime attributes by setting the request parameter
     * "include-runtime" to "true".
     */
    public static class ReadResourceHandler implements NewStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadResourceHandler() {
            validator.registerValidator(RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);

            final String opName = operation.require(OP).asString();
            final ModelNode opAddr = operation.get(OP_ADDR);
            final PathAddress address = PathAddress.pathAddress(opAddr);
            final boolean recursive = operation.get(RECURSIVE).asBoolean(false);
            final boolean queryRuntime = !recursive && operation.get(INCLUDE_RUNTIME).asBoolean(false);
            final boolean proxies = operation.get(PROXIES).asBoolean(false);

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
            context.addStep(assemblyHandler, NewOperationContext.Stage.IMMEDIATE);

            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            final ModelNode model = safeReadModel(context);

            final Map<String, Set<String>> childrenByType = registry != null ? getChildAddresses(registry, model, null): Collections.<String, Set<String>>emptyMap();

            // Store direct attributes first
            for (String key : model.keys()) {
                if (!childrenByType.containsKey(key)) {
                    directAttributes.put(key, model.get(key));
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
                            if (!proxies && registry.getProxyController(relativeAddr) != null) {  // TODO what about runtime resources and INCLUDE_RUNTIME?
                                storeDirect = true;
                            } else {
                                // Add a step to read the child resource
                                ModelNode rrOp = new ModelNode();
                                rrOp.get(OP).set(opName);
                                rrOp.get(OP_ADDR).set(PathAddress.pathAddress(address, childPE).toModelNode());
                                rrOp.get(RECURSIVE).set(true);
                                rrOp.get(PROXIES).set(proxies);
                                rrOp.get(INCLUDE_RUNTIME).set(queryRuntime);
                                ModelNode rrRsp = new ModelNode();
                                childResources.put(childPE, rrRsp);

                                NewStepHandler rrHandler = registry.getOperationHandler(relativeAddr, opName);
                                context.addStep(rrRsp, rrOp, rrHandler, NewOperationContext.Stage.IMMEDIATE);
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
            for(final String attributeName : attributeNames) {
                final AttributeAccess access = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
                if(access == null) {
                    continue;
                } else {
                    final AttributeAccess.Storage storage = access.getStorageType();
                    if(!queryRuntime && storage != AttributeAccess.Storage.CONFIGURATION) {
                        continue;
                    }
                    final AccessType type = access.getAccessType();
                    final NewStepHandler handler = access.getReadHandler();
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

                        context.addStep(attrResponse, attributeOperation, handler, NewOperationContext.Stage.IMMEDIATE);
                    }
                }
            }
            context.completeStep();
        }
    };

    /**
     * Assembles the resonse to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadResourceAssemblyHandler implements NewStepHandler {

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
         * @param metrics map of attributes of AccessType.METRIC. Keys are the attribute names, values are the full
         *                read-attribute response from invoking the attribute's read handler. Will not be {@code null}
         * @param otherAttributes map of attributes not of AccessType.METRIC that have a read handler registered. Keys
*                        are the attribute names, values are the full read-attribute response from invoking the
*                        attribute's read handler. Will not be {@code null}
         * @param directChildren
         * @param childResources read-resource response from child resources, where the key is the PathAddress
*                       relative to the address of the operation this handler is handling and the
*                       value is the full read-resource response. Will not be {@code null}
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
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

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

            context.completeStep();
        }
    }

    /**
     * {@link OperationHandler} reading a single attribute at the given operation address. The required request parameter "name" represents the attribute name.
     */
    public static class ReadAttributeHandler implements NewStepHandler {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(NAME).asString();
            final ModelNode subModel = safeReadModel(context);
            final AttributeAccess attributeAccess = context.getModelNodeRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if (attributeAccess == null) {
                final Set<String> children = context.getModelNodeRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
                if(children.contains(attributeName)) {
                    throw new OperationFailedException(new ModelNode().set(String.format("'%s' is a registered child of resource (%s)", attributeName, operation.get(OP_ADDR)))); // TODO i18n
                } else if(subModel.has(attributeName)) {
                    final ModelNode result = subModel.get(attributeName);
                    context.getResult().set(result);
                    context.completeStep();
                } else {
                    throw new OperationFailedException(new ModelNode().set(String.format("No known attribute %s", attributeName))); // TODO i18n
                }
            } else if (attributeAccess.getReadHandler() == null) {
                final ModelNode result = subModel.get(attributeName);
                context.getResult().set(result);
                context.completeStep();
            } else {
                attributeAccess.getReadHandler().execute(context, operation);
            }
        }
    };

    /**
     * {@link OperationHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
     */
    public static class WriteAttributeHandler implements NewStepHandler {
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            final String attributeName = operation.require(NAME).asString();
            final AttributeAccess attributeAccess = context.getModelNodeRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if (attributeAccess == null) {
                throw new OperationFailedException(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
            } else if (attributeAccess.getAccessType() != AccessType.READ_WRITE) {
                throw new OperationFailedException(new ModelNode().set("Attribute " + attributeName + " is not writeable")); // TODO i18n
            } else {
                attributeAccess.getWriteHandler().execute(context, operation);
            }
        }
    };

    /**
     * {@link OperationHandler} querying the children names of a given "child-type".
     */
    public static class ReadChildrenNamesOperationHandler implements NewStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadChildrenNamesOperationHandler() {
            validator.registerValidator(CHILD_TYPE, new StringLengthValidator(1));
        }

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);
            final String childType = operation.require(CHILD_TYPE).asString();
            ModelNode subModel = safeReadModel(context);
            ModelNodeRegistration registry = context.getModelNodeRegistration();
            Map<String, Set<String>> childAddresses = getChildAddresses(registry, subModel, childType);
            Set<String> childNames = childAddresses.get(childType);
            if (childNames == null) {
                throw new OperationFailedException(new ModelNode().set(String.format("No known child type named %s", childType))); //TODO i18n
            }
            ModelNode result = context.getResult();
            result.setEmptyList();
            for (String childName : childNames) {
                result.add(childName);
            }
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} querying the children resources of a given "child-type".
     */
    public static class ReadChildrenResourcesOperationHandler implements NewStepHandler {

        private final ParametersValidator validator = new ParametersValidator();

        public ReadChildrenResourcesOperationHandler() {
            validator.registerValidator(CHILD_TYPE, new StringLengthValidator(1));
            validator.registerValidator(RECURSIVE, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(INCLUDE_RUNTIME, new ModelTypeValidator(ModelType.BOOLEAN, true));
            validator.registerValidator(PROXIES, new ModelTypeValidator(ModelType.BOOLEAN, true));
        }

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            validator.validate(operation);
            final String childType = operation.require(CHILD_TYPE).asString();

            final Set<String> childNames = context.getModelNodeRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
            if (!childNames.contains(childType)) {
                throw new OperationFailedException(new ModelNode().set(String.format("No known child type named %s", childType))); //TODO i18n
            }
            final Map<PathElement, ModelNode> resources = new HashMap<PathElement, ModelNode>();

            ModelNode subModel = safeReadModel(context);
            if (!subModel.hasDefined(childType)) {
                context.getResult().setEmptyObject();
            } else {
                // We're going to add a bunch of steps that should immediately follow this one. We are going to add them
                // in reverse order of how they should execute, as that is the way adding a Stage.IMMEDIATE step works

                // Last to execute is the handler that assembles the overall response from the pieces created by all the other steps
                final ReadChildrenResourcesAssemblyHandler assemblyHandler = new ReadChildrenResourcesAssemblyHandler(resources);
                context.addStep(assemblyHandler, NewOperationContext.Stage.IMMEDIATE);
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                for (final String key : subModel.get(childType).keys()) {
                    final PathElement childPath =  PathElement.pathElement(childType, key);
                    final PathAddress childAddress = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(childType, key));

                    final ModelNode readOp = new ModelNode();
                    readOp.get(OP).set(READ_RESOURCE_OPERATION);
                    readOp.get(OP_ADDR).set(PathAddress.pathAddress(address, childPath).toModelNode());

                    if(operation.hasDefined(INCLUDE_RUNTIME)) {
                        readOp.get(INCLUDE_RUNTIME).set(operation.get(INCLUDE_RUNTIME));
                    }
                    if (operation.hasDefined(RECURSIVE)) {
                        readOp.get(RECURSIVE).set(operation.get(RECURSIVE));
                    }
                    if (operation.hasDefined(PROXIES)) {
                        readOp.get(PROXIES).set(operation.get(PROXIES));
                    }
                    final NewStepHandler handler = context.getModelNodeRegistration().getOperationHandler(childAddress, READ_RESOURCE_OPERATION);
                    if(handler == null) {
                        throw new OperationFailedException(new ModelNode().set("no operation handler"));
                    }
                    ModelNode rrRsp = new ModelNode();
                    resources.put(childPath, rrRsp);
                    context.addStep(rrRsp, readOp, handler, NewOperationContext.Stage.IMMEDIATE);
                }
            }

            context.completeStep();
        }
    };

    /**
     * Assembles the resonse to a read-resource request from the components gathered by earlier steps.
     */
    private static class ReadChildrenResourcesAssemblyHandler implements NewStepHandler {

        private final Map<PathElement, ModelNode> resources;

        /**
         * Creates a ReadResourceAssemblyHandler that will assemble the response using the contents
         * of the given maps.
         *
         * @param resources read-resource response from child resources, where the key is the path of the resource
*                       relative to the address of the operation this handler is handling and the
*                       value is the full read-resource response. Will not be {@code null}
         */
        private ReadChildrenResourcesAssemblyHandler(final Map<PathElement, ModelNode> resources) {
            this.resources = resources;
        }

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

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

            context.completeStep();
        }
    }

    /**
     * {@link OperationHandler} querying the child types of a given node.
     */
    public static final NewStepHandler READ_CHILDREN_TYPES = new NewStepHandler() {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            Set<String> childTypes = registry.getChildNames(PathAddress.EMPTY_ADDRESS);
            final ModelNode result = context.getResult();
            result.setEmptyList();
            for (final String key : childTypes) {
                result.add(key);
            }
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} returning the names of the defined operations at a given model address.
     */
    public static final NewStepHandler READ_OPERATION_NAMES = new NewStepHandler() {

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            final Map<String, OperationEntry> operations = registry.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, true);

            final ModelNode result = new ModelNode();
            if (operations.size() > 0) {
                for(final Entry<String, OperationEntry> entry : operations.entrySet()) {
                    if(entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                        result.add(entry.getKey());
                    }
                }
            } else {
                result.setEmptyList();
            }
            context.getResult().set(result);
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} returning the type description of a single operation description.
     */
    public static final NewStepHandler READ_OPERATION_DESCRIPTION = new NewStepHandler() {

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            String operationName = operation.require(NAME).asString();

            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            final DescriptionProvider descriptionProvider = registry.getOperationDescription(PathAddress.EMPTY_ADDRESS, operationName);

            final ModelNode result = descriptionProvider == null ? new ModelNode() : descriptionProvider.getModelDescription(getLocale(operation));

            context.getResult().set(result);
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} querying the complete type description of a given model node.
     */
    public static final NewStepHandler READ_RESOURCE_DESCRIPTION = new NewStepHandler() {

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final boolean operations = operation.get(OPERATIONS).isDefined() ? operation.get(OPERATIONS).asBoolean() : false;
            final boolean recursive = operation.get(RECURSIVE).isDefined() ? operation.get(RECURSIVE).asBoolean() : false;
            final boolean inheritedOps = operation.get(INHERITED).isDefined() ? operation.get(INHERITED).asBoolean() : true;


            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
            final Locale locale = getLocale(operation);
            final ModelNode result = descriptionProvider.getModelDescription(locale);

            addDescription(context, result, recursive, operations, inheritedOps, registry, PathAddress.EMPTY_ADDRESS, locale);

            context.getResult().set(result);
            context.completeStep();
        }

        private void addDescription(final NewOperationContext context, final ModelNode result, final boolean recursive, final boolean operations, final boolean inheritedOps, final ModelNodeRegistration registry, final PathAddress address, final Locale locale) throws OperationFailedException {

            if (operations) {
                final Map<String, OperationEntry> ops = registry.getOperationDescriptions(address, inheritedOps);
                if (ops.size() > 0) {

                    for (final Map.Entry<String, OperationEntry> entry : ops.entrySet()) {
                        if(entry.getValue().getType() == OperationEntry.EntryType.PUBLIC) {
                            final DescriptionProvider provider = entry.getValue().getDescriptionProvider();
                            result.get(OPERATIONS, entry.getKey()).set(provider.getModelDescription(locale));
                        }
                    }

                } else {
                    result.get(OPERATIONS).setEmptyList();
                }
            }

            if (result.hasDefined(ATTRIBUTES)) {
                for (final String attr : result.require(ATTRIBUTES).keys()) {
                     final AttributeAccess access = registry.getAttributeAccess(address, attr);
                     // If there is metadata for an attribute but no AttributeAccess, assume RO. Can't
                     // be writable without a registered handler. This opens the possibility that out-of-date metadata
                     // for attribute "foo" can lead to a read of non-existent-in-model "foo" with
                     // an unexpected undefined value returned. But it removes the possibility of a
                     // dev forgetting to call registry.registerReadOnlyAttribute("foo", null) resulting
                     // in the valid attribute "foo" not being readable
                     final AccessType accessType = access == null ? AccessType.READ_ONLY : access.getAccessType();
                     final Storage storage = access == null ? Storage.CONFIGURATION : access.getStorageType();
                     result.get(ATTRIBUTES, attr, ACCESS_TYPE).set(accessType.toString()); //TODO i18n
                     result.get(ATTRIBUTES, attr, STORAGE).set(storage.toString());
                }
            }
            if (recursive && result.hasDefined(CHILDREN)) {
                for (final PathElement element : registry.getChildAddresses(address)) {
                    final PathAddress childAddress = address.append(element);
                    final DescriptionProvider provider = registry.getModelDescription(childAddress);
                    final ModelNode child;
                    if (provider == null) {
                        //It is probably a proxy
                        Set<ProxyController> proxyControllers = registry.getProxyControllers(childAddress);
                        if (proxyControllers.size() != 1) {
                            throw new IllegalStateException("No description provider found for " + childAddress +
                                    ". Tried to search for proxies, expected to find 1 proxy controller, found: " + proxyControllers.size());
                        }

                        final ModelNode operation = new ModelNode();
                        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
                        operation.get(OP_ADDR).set(new ModelNode());
                        operation.get(RECURSIVE).set(true);
                        operation.get(OPERATIONS).set(operations);
                        if (locale != null) {
                            operation.get(LOCALE).set(locale.toString());
                        }
                        // TODO
                        child = new ModelNode();

                    } else {
                        child = provider.getModelDescription(locale);
                        addDescription(context, child, recursive, operations, inheritedOps, registry, childAddress, locale);
                    }
                    result.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue()).set(child);
                }
            }
        }
    };


    public static final class ResolveAddressOperationHandler implements NewStepHandler, DescriptionProvider {

        public static final String OPERATION_NAME = "resolve-address";
        public static final String ADDRESS_PARAM = "address-to-resolve";
        public static final String ORIGINAL_OPERATION = "original-operation";

        /** {@inheritDoc} */
        @Override
        public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS_PARAM));
            final String operationName = operation.require(ORIGINAL_OPERATION).asString();
            // First check if the address is handled by a proxy
            final Collection<ProxyController> proxies = context.getModelNodeRegistration().getProxyControllers(PathAddress.EMPTY_ADDRESS);
            final int size = proxies.size();
            if(size > 0) {
                final Map<NewProxyController, ModelNode> proxyResponses = new HashMap<NewProxyController, ModelNode>();
                for(final ProxyController proxy : proxies) {
                    final PathAddress proxyAddress = proxy.getProxyNodeAddress();
                    final ModelNode newOperation = operation.clone();
                    newOperation.get(OP_ADDR).set(address.subAddress(proxyAddress.size()).toModelNode());
                    NewProxyController newProxyController = (NewProxyController) proxy;
                    ProxyStepHandler proxyHandler = new ProxyStepHandler(newProxyController);
                    ModelNode proxyResponse = new ModelNode();
                    proxyResponses.put(newProxyController, proxyResponse);
                    context.addStep(proxyResponse, newOperation, proxyHandler, NewOperationContext.Stage.IMMEDIATE);
                }

                context.addStep(new NewStepHandler() {
                    @Override
                    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                        ModelNode failures = new ModelNode().setEmptyObject();
                        ModelNode combined = new ModelNode().setEmptyList();
                        for (Map.Entry<NewProxyController, ModelNode> entry : proxyResponses.entrySet()) {
                            ModelNode rsp = entry.getValue();
                            if (!SUCCESS.equals(rsp.get(OUTCOME))) {
                               failures.get(entry.getKey().getProxyNodeAddress().toString()).set(rsp.get(FAILURE_DESCRIPTION));
                            } else {
                                ModelNode result = rsp.get(RESULT);
                                if (result.isDefined()) {
                                    for (ModelNode item : result.asList()) {
                                        combined.add(item);
                                    }
                                }
                            }
                            if (failures.asInt() > 0) {
                                ModelNode failMsg = new ModelNode();
                                failMsg.add("Controllers for one or more addresses failed");
                                failMsg.add(failures);
                                context.getFailureDescription().set(failMsg);

                            } else {
                                context.getResult().set(combined);
                            }
                        }
                        context.completeStep();
                    }
                }, NewOperationContext.Stage.MODEL);

            } else {
                // Deal with it directly
                final NewStepHandler operationHandler = context.getModelNodeRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, operationName);
                if(operationHandler == null) {
                    context.getFailureDescription().set("No operation handler" + operationName);
                } else {
                    final Collection<PathAddress> resolved;
                    // FIXME we don't have this metadata with NewStepHandler
                    if(operationHandler instanceof ModelQueryOperationHandler) {
                        resolved = PathAddress.resolve(address, safeReadModel(context), operationHandler instanceof ModelAddOperationHandler);
                    } else {
                        resolved = context.getModelNodeRegistration().resolveAddress(address);
                    }
                    if(! resolved.isEmpty()) {
                        ModelNode list = context.getResult().setEmptyList();
                        for(PathAddress a : resolved) {
                            list.add(a.toModelNode());
                        }
                    }
                }
            }
            context.completeStep();
        }

        /** {@inheritDoc} */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            // This is a private operation, so don't bother
            return new ModelNode();
        }

    }

    private static ModelNode safeReadModel(final NewOperationContext context) {
        try {
            return context.readModel(PathAddress.EMPTY_ADDRESS);
        } catch (Exception e) {
            return new ModelNode().setEmptyObject();
        }
    }


    private static Map<String,Set<String>> getChildAddresses(final ModelNodeRegistration registry, final ModelNode model, final String validChildType) {

        Set<PathElement> elements = registry.getChildAddresses(PathAddress.EMPTY_ADDRESS);
        Map<String,Set<String>> result = new HashMap<String, Set<String>>();
        for (PathElement element : elements) {
            String childType = element.getKey();
            if (validChildType != null && !validChildType.equals(childType)) {
                continue;
            }
            Set<String> set = result.get(childType);
            if (set == null) {
                set = new HashSet<String>();
                result.put(childType, set);
            }
            if (element.isWildcard()) {
                if (model.hasDefined(childType)) {
                    set.addAll(model.get(childType).keys());
                }
                // TODO we assume that no proxy controller will registered under a wildcard path element whereby
                // we'd need to somehow as the PC for all the children names. We should probably formalize this
                // by explicitly stating in the ModelNodeRegistration API that such PC's are illegal
            } else {
                set.add(element.getValue());
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
