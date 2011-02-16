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
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Global {@code OperationHanlder}s.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GlobalOperationHandlers {

    public static final OperationHandler READ_RESOURCE = new ReadResourceHandler();
    public static final OperationHandler READ_ATTRIBUTE = new ReadAttributeHandler();
    public static final OperationHandler WRITE_ATTRIBUTE = new WriteAttributeHandler();

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
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            try {
                final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS));
                final ModelNode result;
                if (operation.get(RECURSIVE).asBoolean(false)) {
                    // FIXME security checks JBAS-8842
                    result = context.getSubModel().clone();
                    addProxyNodes(address, result, context.getRegistry());

                } else {
                    result = new ModelNode();

                    final Set<String> childNames = context.getRegistry().getChildNames(address);

                    final ModelNode subModel = context.getSubModel().clone();
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
                    final boolean queryRuntime = operation.get(INCLUDE_RUNTIME).asBoolean(false);
                    final Set<String> attributeNames = context.getRegistry().getAttributeNames(address);
                    for(final String attributeName : attributeNames) {
                        final AttributeAccess access = context.getRegistry().getAttributeAccess(address, attributeName);
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
                                final ModelNode attributeOperation = operation.clone();
                                attributeOperation.get(NAME).set(attributeName);
                                handler.execute(context, attributeOperation, new ResultHandler() {
                                    public void handleResultFragment(final String[] location, final ModelNode attributeResult) {
                                        result.get(attributeName).set(attributeResult);
                                    }
                                    public void handleResultComplete() {
                                        // TODO
                                    }
                                    public void handleFailed(ModelNode failureDescription) {
                                        if(type != AccessType.METRIC) {
                                            resultHandler.handleFailed(failureDescription);
                                        }
                                    }
                                    public void handleCancellation() {
                                        resultHandler.handleCancellation();
                                    }
                                });
                            }
                        }
                    }
                }
                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return new BasicOperationResult();
        }

        void addProxyNodes(final PathAddress address, final ModelNode result, final ModelNodeRegistration registry) throws Exception {
            Set<ProxyController> proxyControllers = registry.getProxyControllers(address);
            if (proxyControllers.size() > 0) {
                final ModelNode operation = new ModelNode();
                operation.get(OP).set(READ_RESOURCE_OPERATION);
                operation.get(RECURSIVE).set(true);
                operation.get(ADDRESS).set(new ModelNode());

                for (ProxyController proxyController : proxyControllers) {
                    final ModelNode proxyResult = proxyController.execute(operation);
                    addProxyResultToMainResult(proxyController.getProxyNodeAddress(), result, proxyResult);
                }
            }
        }

        void addProxyResultToMainResult(final PathAddress address, final ModelNode mainResult, final ModelNode proxyResult) {
            ModelNode resultNode = mainResult;
            for (Iterator<PathElement> it = address.iterator() ; it.hasNext() ; ) {
                PathElement element = it.next();
                resultNode = resultNode.require(element.getKey()).require(element.getValue());
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
            OperationResult handlerResult = null;
            try {
                final String attributeName = operation.require(NAME).asString();
                final AttributeAccess attributeAccess = context.getRegistry().getAttributeAccess(PathAddress.pathAddress(operation.require(ADDRESS)), attributeName);
                if (attributeAccess == null) {
                    resultHandler.handleFailed(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
                } else if (attributeAccess.getReadHandler() == null) {
                    final ModelNode result = context.getSubModel().get(attributeName).clone();
                    resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                    resultHandler.handleResultComplete();
                    handlerResult = new BasicOperationResult();
                } else {
                    handlerResult = attributeAccess.getReadHandler().execute(context, operation, resultHandler);
                }

            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
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
            try {
                final String attributeName = operation.require(NAME).asString();
                final AttributeAccess attributeAccess = context.getRegistry().getAttributeAccess(PathAddress.pathAddress(operation.require(ADDRESS)), attributeName);
                if (attributeAccess == null) {
                    throw new OperationFailedException(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
                } else if (attributeAccess.getAccessType() != AccessType.READ_WRITE) {
                    throw new OperationFailedException(new ModelNode().set("Attribute " + attributeName + " is not writeable")); // TODO i18n
                } else {
                    handlerResult = attributeAccess.getWriteHandler().execute(context, operation, resultHandler);
                }

            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return handlerResult;
        }
    };

    /**
     * {@link OperationHandler} querying the children names of a given "child-type".
     */
    public static final ModelQueryOperationHandler READ_CHILDREN_NAMES = new ModelQueryOperationHandler() {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            try {
                String childName = operation.require(CHILD_TYPE).asString();

                ModelNode subModel = context.getSubModel().clone();
                if (!subModel.isDefined()) {
                    final ModelNode result = new ModelNode();
                    result.setEmptyList();
                    resultHandler.handleResultFragment(new String[0], result);
                    resultHandler.handleResultComplete();
                } else {

                    final Set<String> childNames = context.getRegistry().getChildNames(PathAddress.pathAddress(operation.require(ADDRESS)));

                    if (!childNames.contains(childName)) {
                        resultHandler.handleFailed(new ModelNode().set("No known child called " + childName)); //TODO i18n
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
                        resultHandler.handleResultComplete();
                    }
                }

            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} returning the names of the defined operations at a given model address.
     */
    public static final ModelQueryOperationHandler READ_OPERATION_NAMES = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            try {
                final ModelNodeRegistration registry = context.getRegistry();
                final Map<String, DescriptionProvider> descriptionProviders = registry.getOperationDescriptions(PathAddress.pathAddress(operation.require(ADDRESS)));

                final ModelNode result = new ModelNode();
                if (descriptionProviders.size() > 0) {
                    for (final String s : descriptionProviders.keySet()) {
                        result.add(s);
                    }
                } else {
                    result.setEmptyList();
                }
                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} returning the type description of a single operation description.
     */
    public static final ModelQueryOperationHandler READ_OPERATION_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            try {
                String operationName = operation.require(NAME).asString();

                final ModelNodeRegistration registry = context.getRegistry();
                final DescriptionProvider descriptionProvider = registry.getOperationDescription(PathAddress.pathAddress(operation.require(ADDRESS)), operationName);

                final ModelNode result = descriptionProvider == null ? new ModelNode() : descriptionProvider.getModelDescription(getLocale(operation));

                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return new BasicOperationResult();
        }
    };

    /**
     * {@link OperationHandler} querying the complete type description of a given model node.
     */
    public static final ModelQueryOperationHandler READ_RESOURCE_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            try {
                final boolean operations = operation.get(OPERATIONS).isDefined() ? operation.get(OPERATIONS).asBoolean() : false;
                final boolean recursive = operation.get(RECURSIVE).isDefined() ? operation.get(RECURSIVE).asBoolean() : false;

                final ModelNodeRegistration registry = context.getRegistry();
                final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS));
                final DescriptionProvider descriptionProvider = registry.getModelDescription(address);
                final Locale locale = getLocale(operation);
                final ModelNode result = descriptionProvider.getModelDescription(locale);

                addDescription(result, recursive, operations, registry, address, locale);

//                if (recursive) {
//                    addProxyNodes(address, result, operations, locale, context.getRegistry());
//                }

                resultHandler.handleResultFragment(Util.NO_LOCATION, result);
                resultHandler.handleResultComplete();
            } catch (final Exception e) {
                throw new OperationFailedException(Util.createErrorResult(e));
            }
            return new BasicOperationResult();
        }

//        void addProxyNodes(final PathAddress address, final ModelNode result, final boolean operations, final Locale locale, final ModelNodeRegistration registry) throws Exception {
//            Set<ProxyController> proxyControllers = registry.getProxyControllers(address);
//            if (proxyControllers.size() > 0) {
//                final ModelNode operation = new ModelNode();
//                operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
//                operation.get(RECURSIVE).set(true);
//                operation.get(OPERATIONS).set(operations);
//                if (locale != null) {
//                    operation.get(OPERATIONS).set(locale.toString());
//                }
//                operation.get(ADDRESS).set(new ModelNode());
//
//                for (ProxyController proxyController : proxyControllers) {
//                    final ModelNode proxyResult = proxyController.execute(operation);
//                    addProxyResultToMainResult(proxyController.getProxyNodeAddress(), result, proxyResult);
//                }
//            }
//        }
//
//        void addProxyResultToMainResult(final PathAddress address, final ModelNode mainResult, final ModelNode proxyResult) {
//            ModelNode resultNode = mainResult;
//            for (Iterator<PathElement> it = address.iterator() ; it.hasNext() ; ) {
//                PathElement element = it.next();
//                resultNode = resultNode.require(CHILDREN).require(element.getKey()).require(MODEL_DESCRIPTION).get(element.getValue());
//            }
//            resultNode.set(proxyResult.clone());
//        }


        private void addDescription(final ModelNode result, final boolean recursive, final boolean operations, final ModelNodeRegistration registry, final PathAddress address, final Locale locale) throws OperationFailedException {

            if (operations) {
                final Map<String, DescriptionProvider> ops = registry.getOperationDescriptions(address);
                if (ops.size() > 0) {

                    for (final Map.Entry<String, DescriptionProvider> entry : ops.entrySet()) {
                        result.get(OPERATIONS, entry.getKey()).set(entry.getValue().getModelDescription(locale));
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
                        operation.get(ADDRESS).set(new ModelNode());
                        operation.get(RECURSIVE).set(true);
                        operation.get(OPERATIONS).set(operations);
                        if (locale != null) {
                            operation.get(OPERATIONS).set(locale.toString());
                        }
                        child = proxyControllers.iterator().next().execute(operation).get(RESULT);

                    } else {
                        child = provider.getModelDescription(locale);
                        addDescription(child, recursive, operations, registry, childAddress, locale);
                    }
                    result.get(CHILDREN, element.getKey(),MODEL_DESCRIPTION, element.getValue()).set(child);
                }
            }
        }
    };

    private static Locale getLocale(final ModelNode operation) {
        if (!operation.has(LOCALE)) {
            return null;
        }
        return new Locale(operation.get(LOCALE).asString());
    }


}
