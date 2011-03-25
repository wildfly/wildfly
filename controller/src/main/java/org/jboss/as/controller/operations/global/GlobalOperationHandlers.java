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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
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

/**
 * Global {@code OperationHanlder}s.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GlobalOperationHandlers {

    public static final OperationHandler READ_RESOURCE = new ReadResourceHandler();
    public static final OperationHandler READ_ATTRIBUTE = new ReadAttributeHandler();
    public static final OperationHandler READ_CHILDREN_NAMES = new ReadChildrenNamesOperationHandler();
    public static final OperationHandler READ_CHILDREN_RESOURCES = new ReadChildrenResourcesOperationHandler();
    public static final OperationHandler WRITE_ATTRIBUTE = new WriteAttributeHandler();
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
    public static class ReadResourceHandler implements ModelQueryOperationHandler {

        static final String PROXIES = "proxies";

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final ModelNode result = readModel(context, operation, resultHandler, address);
            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        protected ModelNode readModel(final OperationContext context, final ModelNode readOperation,
                final ResultHandler resultHandler, final PathAddress address) throws OperationFailedException {
            final ModelNodeRegistration registry = context.getRegistry();
            final ModelNode model = context.getSubModel(address);
            final ModelNode result;
                if (readOperation.get(RECURSIVE).asBoolean(false)) {
                    // FIXME security checks JBAS-8842
                    result = model.clone();
                    if(readOperation.get(PROXIES).asBoolean(true)) {
                        addProxyNodes(context, address, readOperation, result, registry);
                    }
                } else {
                    result = new ModelNode();

                    final Set<String> childNames = registry.getChildNames(address);

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
                    final Set<String> attributeNames = registry.getAttributeNames(address);
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
                            final OperationHandler handler = access.getReadHandler();
                            if(handler != null) {
                                // Create the attribute operation
                                final ModelNode attributeOperation = readOperation.clone();
                                attributeOperation.get(NAME).set(attributeName);
                                // TODO this assumes the ResultHandler is invoked synchronously
                                handler.execute(context, attributeOperation, new ResultHandler() {
                                    @Override
                                    public void handleResultFragment(final String[] location, final ModelNode attributeResult) {
                                        result.get(attributeName).set(attributeResult);
                                    }
                                    @Override
                                    public void handleResultComplete() {
                                        // TODO
                                    }
                                    @Override
                                    public void handleFailed(ModelNode failureDescription) {
                                        if(type != AccessType.METRIC) {
                                            resultHandler.handleFailed(failureDescription);
                                        }
                                    }
                                    @Override
                                    public void handleCancellation() {
                                        resultHandler.handleCancellation();
                                    }
                                });
                            }
                        }
                    }
                }
            return result;
        }

        protected void addProxyNodes(final OperationContext context, final PathAddress address, final ModelNode originalOperation, final ModelNode result, final ModelNodeRegistration registry) {
            Set<ProxyController> proxyControllers = registry.getProxyControllers(address);
            if (proxyControllers.size() > 0) {
                final ModelNode operation = new ModelNode();
                operation.get(OP).set(READ_RESOURCE_OPERATION);
                operation.get(RECURSIVE).set(true);
                operation.get(OP_ADDR).set(new ModelNode());

                for (ProxyController proxyController : proxyControllers) {
                    final ModelNode proxyResult = proxyController.execute(OperationBuilder.Factory.copy(context, operation).build());

                    //Trim the address to not include the host=>hostB part if this is a slave controller
                    PathAddress proxyAddress = proxyController.getProxyNodeAddress();
//                    if (proxyAddress.size() > 1) {
//                        if (proxyAddress.getElement(0).getKey().equals(HOST)) {
//                            proxyAddress = proxyAddress.subAddress(1);
//                            proxyResult =
//                        }
//                    }

                    addProxyResultToMainResult(proxyAddress, result, proxyResult);
                }
            }
        }

        protected void addProxyResultToMainResult(final PathAddress address, final ModelNode mainResult, final ModelNode proxyResult) {
            ModelNode resultNode = mainResult;
            for (Iterator<PathElement> it = address.iterator() ; it.hasNext() ; ) {
                PathElement element = it.next();
                resultNode = resultNode.get(element.getKey()).get(element.getValue());
            }
            resultNode.set(proxyResult.get(RESULT).clone());
        }

    };

    /**
     * {@link OperationHandler} reading a single attribute at the given operation address. The required request parameter "name" represents the attribute name.
     */
    public static class ReadAttributeHandler implements ModelQueryOperationHandler {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            OperationResult handlerResult = new BasicOperationResult();

            final String attributeName = operation.require(NAME).asString();
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final AttributeAccess attributeAccess = context.getRegistry().getAttributeAccess(address, attributeName);
            if (attributeAccess == null) {
                final Set<String> children = context.getRegistry().getChildNames(address);
                if(children.contains(attributeName)) {
                    throw new OperationFailedException(new ModelNode().set(String.format("'%s' is a registered child of resource (%s)", attributeName, address))); // TODO i18n
                } else if(context.getSubModel().has(attributeName)) {
                    final ModelNode result = context.getSubModel().get(attributeName).clone();
                    resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                    resultHandler.handleResultComplete();
                } else {
                    throw new OperationFailedException(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
                }
            } else if (attributeAccess.getReadHandler() == null) {
                final ModelNode result = context.getSubModel().get(attributeName).clone();
                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
            } else {
                handlerResult = attributeAccess.getReadHandler().execute(context, operation, resultHandler);
            }

            return handlerResult;
        }
    };

    /**
     * {@link OperationHandler} writing a single attribute. The required request parameter "name" represents the attribute name.
     */
    public static class WriteAttributeHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            OperationResult handlerResult = null;

            final String attributeName = operation.require(NAME).asString();
            final AttributeAccess attributeAccess = context.getRegistry().getAttributeAccess(PathAddress.pathAddress(operation.get(OP_ADDR)), attributeName);
            if (attributeAccess == null) {
                throw new OperationFailedException(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
            } else if (attributeAccess.getAccessType() != AccessType.READ_WRITE) {
                throw new OperationFailedException(new ModelNode().set("Attribute " + attributeName + " is not writeable")); // TODO i18n
            } else {
                handlerResult = attributeAccess.getWriteHandler().execute(context, operation, resultHandler);
            }

            return handlerResult;
        }
    };

    /**
     * {@link OperationHandler} querying the children names of a given "child-type".
     */
    public static class ReadChildrenNamesOperationHandler implements ModelQueryOperationHandler {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            String childName = operation.require(CHILD_TYPE).asString();

            ModelNode subModel = context.getSubModel().clone();
            if (!subModel.isDefined()) {
                final ModelNode result = new ModelNode();
                result.setEmptyList();
                resultHandler.handleResultFragment(new String[0], result);
            } else {
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                final Set<String> childNames = context.getRegistry().getChildNames(address);
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
                    resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                }
            }

            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} querying the children resources of a given "child-type".
     */
    public static class ReadChildrenResourcesOperationHandler extends ReadResourceHandler {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            String childName = operation.require(CHILD_TYPE).asString();

            ModelNode subModel = context.getSubModel().clone();
            if (!subModel.isDefined()) {
                final ModelNode result = new ModelNode();
                result.setEmptyList();
                resultHandler.handleResultFragment(new String[0], result);
                resultHandler.handleResultComplete();
            } else {
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                final Set<String> childNames = context.getRegistry().getChildNames(address);
                if (!childNames.contains(childName)) {
                    throw new OperationFailedException(new ModelNode().set("No known child called " + childName)); //TODO i18n
                } else {
                    final AtomicBoolean ok = new AtomicBoolean(true);
                    final ModelNode result = new ModelNode();
                    subModel = subModel.get(childName);
                    if (!subModel.isDefined()) {
                        result.setEmptyList();
                    } else {

                        for (final String key : subModel.keys()) {
                            if (!ok.get()) {
                                break;
                            }
                            final PathAddress childAddress = address.append(PathElement.pathElement(childName, key));

                            final ModelNode readOp = operation.clone();
                            readOp.get(OP_ADDR).set(childAddress.toModelNode());

                            if(operation.has(INCLUDE_RUNTIME))
                                readOp.get(INCLUDE_RUNTIME).set(operation.get(INCLUDE_RUNTIME).asBoolean());

                            final ModelNode readResult = readModel(context, readOp, new ResultHandler() {
                                public void handleResultFragment(String[] location, ModelNode result) {
                                    // TODO
                                }
                                public void handleResultComplete() {
                                    // TODO
                                }
                                public void handleFailed(ModelNode failureDescription) {
                                    ok.set(false);
                                    resultHandler.handleFailed(failureDescription);
                                }
                                public void handleCancellation() {
                                    ok.set(false);
                                    resultHandler.handleCancellation();
                                }
                            }, childAddress);
                            result.add(key, readResult);
                        }
                    }

                    if (ok.get()) {
                        resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                        resultHandler.handleResultComplete();
                    }
                }
            }

            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} querying the child types of a given node.
     */
    public static final ModelQueryOperationHandler READ_CHILDREN_TYPES = new ModelQueryOperationHandler() {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            ModelNode subModel = context.getSubModel().clone();
            if (!subModel.isDefined()) {
                final ModelNode result = new ModelNode();
                result.setEmptyList();
                resultHandler.handleResultFragment(new String[0], result);
            } else {

                Set<String> childTypes = context.getRegistry().getChildNames(PathAddress.pathAddress(operation.get(OP_ADDR)));
                final ModelNode result = new ModelNode();
                for (final String key : childTypes) {
                    final ModelNode node = new ModelNode();
                    node.set(key);
                    result.add(node);
                }

                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            }

            resultHandler.handleResultComplete();

            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} returning the names of the defined operations at a given model address.
     */
    public static final ModelQueryOperationHandler READ_OPERATION_NAMES = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            final ModelNodeRegistration registry = context.getRegistry();
            final Map<String, OperationEntry> operations = registry.getOperationDescriptions(PathAddress.pathAddress(operation.get(OP_ADDR)), true);

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
            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();

            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} returning the type description of a single operation description.
     */
    public static final ModelQueryOperationHandler READ_OPERATION_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            String operationName = operation.require(NAME).asString();

            final ModelNodeRegistration registry = context.getRegistry();
            final DescriptionProvider descriptionProvider = registry.getOperationDescription(PathAddress.pathAddress(operation.get(OP_ADDR)), operationName);

            final ModelNode result = descriptionProvider == null ? new ModelNode() : descriptionProvider.getModelDescription(getLocale(operation));

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();

            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} querying the complete type description of a given model node.
     */
    public static final ModelQueryOperationHandler READ_RESOURCE_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

            final boolean operations = operation.get(OPERATIONS).isDefined() ? operation.get(OPERATIONS).asBoolean() : false;
            final boolean recursive = operation.get(RECURSIVE).isDefined() ? operation.get(RECURSIVE).asBoolean() : false;

            final ModelNodeRegistration registry = context.getRegistry();
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final DescriptionProvider descriptionProvider = registry.getModelDescription(address);
            final Locale locale = getLocale(operation);
            final ModelNode result = descriptionProvider.getModelDescription(locale);

            addDescription(context, result, recursive, operations, registry, address, locale);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();

            return new BasicOperationResult();
        }

        private void addDescription(final OperationContext context, final ModelNode result, final boolean recursive, final boolean operations, final ModelNodeRegistration registry, final PathAddress address, final Locale locale) throws OperationFailedException {

            if (operations) {
                final Map<String, OperationEntry> ops = registry.getOperationDescriptions(address, true);
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

            if (result.has(ATTRIBUTES)) {
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
            if (recursive && result.has(CHILDREN)) {
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
                        child = proxyControllers.iterator().next().execute(OperationBuilder.Factory.copy(context, operation).build()).get(RESULT);

                    } else {
                        child = provider.getModelDescription(locale);
                        addDescription(context, child, recursive, operations, registry, childAddress, locale);
                    }
                    result.get(CHILDREN, element.getKey(),MODEL_DESCRIPTION, element.getValue()).set(child);
                }
            }
        }
    };


    public static final class ResolveAddressOperationHandler implements ModelQueryOperationHandler, DescriptionProvider {

        public static final String OPERATION_NAME = "resolve-address";
        public static final String ADDRESS_PARAM = "address-to-resolve";
        public static final String ORIGINAL_OPERATION = "original-operation";

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS_PARAM));
            final String operationName = operation.require(ORIGINAL_OPERATION).asString();
            // First check if the address is handled by a proxy
            final Collection<ProxyController> proxies = context.getRegistry().getProxyControllers(address);
            final int size = proxies.size();
            if(size > 0) {
                final AtomicInteger count = new AtomicInteger(size);
                final AtomicInteger status = new AtomicInteger();
                final ModelNode failureResult = new ModelNode();
                for(final ProxyController proxy : proxies) {
                    final PathAddress proxyAddress = proxy.getProxyNodeAddress();
                    final ModelNode newOperation = operation.clone();
                    newOperation.get(OP_ADDR).set(address.subAddress(proxyAddress.size()).toModelNode());
                    final Operation operationContext = OperationBuilder.Factory.create(newOperation).build();
                    proxy.execute(operationContext, new ResultHandler() {
                        @Override
                        public void handleResultFragment(String[] location, ModelNode result) {
                            synchronized(failureResult) {
                                if(status.get() == 0) {
                                    // Addresses are aggregated as list by the controller
                                    final PathAddress resolved = PathAddress.pathAddress(result);
                                    resultHandler.handleResultFragment(Util.NO_LOCATION, proxyAddress.append(resolved).toModelNode());
                                }
                            }
                        }
                        @Override
                        public void handleResultComplete() {
                            synchronized(failureResult) {
                                status.compareAndSet(0, 1);
                                if(count.decrementAndGet() == 0) {
                                    handleComplete();
                                }
                            }
                        }
                        @Override
                        public void handleFailed(ModelNode failureDescription) {
                            synchronized(failureResult) {
                                if(failureDescription != null)  {
                                    failureResult.add(failureDescription);
                                }
                                status.compareAndSet(0, 2);
                                if(count.decrementAndGet() == 0) {
                                    handleComplete();
                                }
                            }
                        }
                        @Override
                        public void handleCancellation() {
                            synchronized(failureResult) {
                                status.compareAndSet(0, 3);
                                if(count.decrementAndGet() == 0) {
                                    handleComplete();
                                }
                            }
                        }
                        private void handleComplete() {
                            final int s = status.get();
                            switch(s) {
                                case 1: resultHandler.handleResultComplete(); break;
                                case 2: resultHandler.handleFailed(new ModelNode()); break;
                                case 3: resultHandler.handleCancellation(); break;
                                default : throw new IllegalStateException();
                            }
                        }
                    });
                    return new BasicOperationResult();
                }
            }
            final OperationHandler operationHandler = context.getRegistry().getOperationHandler(address, operationName);
            if(operationHandler == null) {
                resultHandler.handleFailed(new ModelNode().set("no operation handler" + operationName));
                return new BasicOperationResult();
            }
            final Collection<PathAddress> resolved;
            if(operationHandler instanceof ModelQueryOperationHandler) {
                resolved = PathAddress.resolve(address, context.getSubModel(), operationHandler instanceof ModelAddOperationHandler);
            } else {
                resolved = context.getRegistry().resolveAddress(address);
            }
            if(! resolved.isEmpty()) {
                for(PathAddress a : resolved) {
                    // Addresses are aggregated as list by the controller
                    resultHandler.handleResultFragment(Util.NO_LOCATION, a.toModelNode());
                }
            }
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        /** {@inheritDoc} */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }

    }

    private static Locale getLocale(final ModelNode operation) {
        if (!operation.has(LOCALE)) {
            return null;
        }
        return new Locale(operation.get(LOCALE).asString());
    }


}
