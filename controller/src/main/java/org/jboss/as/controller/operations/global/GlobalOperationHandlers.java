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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyStepHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

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

        static final String PROXIES = "proxies";

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNode result = readModel(context, operation, PathAddress.EMPTY_ADDRESS);
            context.getResult().set(result);
            context.completeStep();
        }

        protected ModelNode readModel(final NewOperationContext context, final ModelNode readOperation, final PathAddress address) throws OperationFailedException {
            final ModelNodeRegistration registry = context.getModelNodeRegistration();
            final ModelNode model = context.readModel(address);
            final ModelNode result;
            if (readOperation.get(RECURSIVE).asBoolean(false)) {
                    // FIXME security checks JBAS-8842
                    result = model.clone();
                    // FIXME deal with proxies
            } else {
                    result = new ModelNode();

                    final Set<String> childNames = registry != null ? registry.getChildNames(address) : Collections.<String>emptySet();

                    final ModelNode subModel = model.clone();
                    for (final String key : subModel.keys()) {
                        final ModelNode child = subModel.get(key);
                        if (childNames.contains(key)) {
                            //Prune the value for this child
                            if (subModel.get(key).isDefined()) {
                                for (final String childKey : child.keys()) {
                                    subModel.get(key, childKey).set(new ModelNode());
                                }
                            }

                            result.get(key).set(child);
                        } else {
                            result.get(key).set(child);
                        }
                    }
                    // Handle attributes
                    final boolean queryRuntime = readOperation.get(INCLUDE_RUNTIME).asBoolean(false);
                    final Set<String> attributeNames = registry != null ? registry.getAttributeNames(address) : Collections.<String>emptySet();
                    for(final String attributeName : attributeNames) {
                        final AttributeAccess access = registry.getAttributeAccess(address, attributeName);
                        if(access == null) {
                            continue;
                        } else {
                            final AttributeAccess.Storage storage = access.getStorageType();
                            if(! queryRuntime && storage != AttributeAccess.Storage.CONFIGURATION) {
                                continue;
                            }
                            final AccessType type = access.getAccessType();
                            // FIXME incorrect cast just to compile
                            final NewStepHandler handler = access.getReadHandler();
                            if(handler != null) {
                                // Create the attribute operation
                                final ModelNode attributeOperation = readOperation.clone();
                                attributeOperation.get(NAME).set(attributeName);
                                context.addStep(result.get(attributeName), attributeOperation, handler, NewOperationContext.Stage.MODEL);
                            }
                        }
                    }
                // FIXME deal with proxies
            }
            return result;
        }

        protected void addProxyNodes(NewOperationContext context, PathAddress address, ModelNode originalOperation, ModelNode result, ModelNodeRegistration registry) {
            // FIXME implement
            throw new UnsupportedOperationException();
        }
        protected void handleNonRecursiveProxyEntries(final NewOperationContext context, final PathAddress address, final ModelNode originalOperation, final ModelNode result, final ModelNodeRegistration registry) {
            // FIXME implement
            throw new UnsupportedOperationException();
        }
        protected void addProxyResultToMainResult(final PathAddress address, final ModelNode mainResult, final ModelNode proxyResult) {
            // FIXME implement
            throw new UnsupportedOperationException();
        }
    };

    /**
     * {@link OperationHandler} reading a single attribute at the given operation address. The required request parameter "name" represents the attribute name.
     */
    public static class ReadAttributeHandler implements NewStepHandler {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(NAME).asString();
            final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);
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
                    throw new OperationFailedException(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
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
                attributeAccess.getReadHandler().execute(context, operation);
            }
        }
    };

    /**
     * {@link OperationHandler} querying the children names of a given "child-type".
     */
    public static class ReadChildrenNamesOperationHandler implements NewStepHandler {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            final String childName = operation.require(CHILD_TYPE).asString();
            ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

            if (!subModel.isDefined()) {
                final ModelNode result = new ModelNode();
                context.getResult().setEmptyList();
            } else {
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                final Set<String> childNames = context.getModelNodeRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
                if (!childNames.contains(childName)) {
                    throw new OperationFailedException(new ModelNode().set("No known child called " + childName)); //TODO i18n
                } else {
                    final ModelNode result = new ModelNode();
                    subModel = subModel.get(childName);
                    if (!subModel.isDefined()) {
                        result.setEmptyList();
                    } else {
                        for (final String key : subModel.keys()) {
                            final ModelNode node = new ModelNode();
                            node.set(key);
                            result.add(node);
                        }
                    }
                    context.getResult().set(result);
                }
            }
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} querying the children resources of a given "child-type".
     */
    public static class ReadChildrenResourcesOperationHandler implements NewStepHandler {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            final String childName = operation.require(CHILD_TYPE).asString();

            ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);
            if (!subModel.isDefined()) {
                context.getResult().setEmptyList();
            } else {
                final Set<String> childNames = context.getModelNodeRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
                if (!childNames.contains(childName)) {
                    throw new OperationFailedException(new ModelNode().set("No known child called " + childName)); //TODO i18n
                } else {
                    final ModelNode result = context.getResult();
                    subModel = subModel.get(childName);
                    if (!subModel.isDefined()) {
                        result.setEmptyList();
                    } else {
                        for (final String key : subModel.keys()) {
                            final PathAddress childAddress = PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement(childName, key));

                            final ModelNode readOp = new ModelNode();
                            readOp.get(OP).set(READ_RESOURCE_OPERATION);
                            readOp.get(OP_ADDR).set(childAddress.toModelNode());

                            if(operation.hasDefined(INCLUDE_RUNTIME)) {
                                readOp.get(INCLUDE_RUNTIME).set(operation.get(INCLUDE_RUNTIME).asBoolean());
                            }
                            final NewStepHandler handler = context.getModelNodeRegistration().getOperationHandler(childAddress, READ_RESOURCE_OPERATION);
                            if(handler == null) {
                                throw new OperationFailedException(new ModelNode().set("no operation handler"));
                            }
                            context.addStep(result.get(key), readOp, handler, NewOperationContext.Stage.MODEL);
                        }
                    }
                }
            }
            context.completeStep();
        }
    };

    /**
     * {@link OperationHandler} querying the child types of a given node.
     */
    public static final NewStepHandler READ_CHILDREN_TYPES = new NewStepHandler() {
        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

            ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);
            if (!subModel.isDefined()) {
                context.getResult().setEmptyList();
            } else {
                Set<String> childTypes = context.getModelNodeRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
                final ModelNode result = new ModelNode();
                for (final String key : childTypes) {
                    final ModelNode node = new ModelNode();
                    node.set(key);
                    result.add(node);
                }
                context.getResult().set(result);
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
                            operation.get(OPERATIONS).set(locale.toString());
                        }
                        // TODO
                        child = new ModelNode();

                    } else {
                        child = provider.getModelDescription(locale);
                        addDescription(context, child, recursive, operations, inheritedOps, registry, childAddress, locale);
                    }
                    result.get(CHILDREN, element.getKey(),MODEL_DESCRIPTION, element.getValue()).set(child);
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
                        resolved = PathAddress.resolve(address, context.readModel(PathAddress.EMPTY_ADDRESS), operationHandler instanceof ModelAddOperationHandler);
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
            return new ModelNode();
        }

    }

    private static Locale getLocale(final ModelNode operation) {
        if (!operation.hasDefined(LOCALE)) {
            return null;
        }
        return new Locale(operation.get(LOCALE).asString());
    }


}
