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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCALE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalOperationHandlers {

    static final String[] NO_LOCATION = new String[0];

    public static final ModelQueryOperationHandler READ_RESOURCE = new ModelQueryOperationHandler() {
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                final ModelNode result;
                if (operation.require(REQUEST_PROPERTIES).require(RECURSIVE).asBoolean()) {
                    result = context.getSubModel().clone();
                } else {
                    result = new ModelNode();

                    Set<String> childNames = context.getRegistry().getChildNames(PathAddress.pathAddress(operation.require(ADDRESS)));

                    ModelNode subModel = context.getSubModel().clone();
                    for (String key : subModel.keys()) {
                        ModelNode child = subModel.get(key);
                        if (childNames.contains(key)) {
                            //Prune the value for this child
                            if (subModel.get(key).isDefined()) {
                                for (String childKey : child.keys()) {
                                    subModel.get(key, childKey).set(new ModelNode());
                                }
                            }

                            result.get(key).set(child);
                        } else {
                            result.get(key).set(child);
                        }
                    }
                }

                resultHandler.handleResultFragment(NO_LOCATION, result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }
    };

    public static final ModelQueryOperationHandler READ_ATTRIBUTE = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

            try {
                String attributeName = operation.require(REQUEST_PROPERTIES).require(NAME).asString();
                if (!context.getRegistry().getAttributeNames(PathAddress.pathAddress(operation.require(ADDRESS))).contains(attributeName)) {
                    resultHandler.handleFailed(new ModelNode().set("No known attribute called " + attributeName)); // TODO i18n
                } else {
                    ModelNode result = context.getSubModel().get(attributeName).clone();
                    resultHandler.handleResultFragment(new String[0], result);
                    resultHandler.handleResultComplete(null);
                }
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }
    };


    public static final ModelQueryOperationHandler READ_CHILDREN_NAMES = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                String childName = operation.require(REQUEST_PROPERTIES).require(CHILD_TYPE).asString();

                ModelNode subModel = context.getSubModel().clone();
                if (!subModel.isDefined()) {
                    ModelNode result = new ModelNode();
                    result.setEmptyList();
                    resultHandler.handleResultFragment(new String[0], result);
                    resultHandler.handleResultComplete(null);
                } else {

                    Set<String> childNames = context.getRegistry().getChildNames(PathAddress.pathAddress(operation.require(ADDRESS)));

                    if (!childNames.contains(childName)) {
                        resultHandler.handleFailed(new ModelNode().set("No known child called " + childName)); //TODO i18n
                    } else {
                        ModelNode result = new ModelNode();
                        subModel = subModel.get(childName);
                        if (!subModel.isDefined()) {
                            result.setEmptyList();
                        } else {
                            for (String key : subModel.keys()) {
                                ModelNode node = new ModelNode();
                                node.set(key);
                                result.add(node);
                            }
                        }

                        resultHandler.handleResultFragment(new String[0], result);
                        resultHandler.handleResultComplete(null);
                    }
                }

            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }
    };

    public static final ModelQueryOperationHandler READ_OPERATION_NAMES = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                ModelNodeRegistration registry = context.getRegistry();
                Map<String, DescriptionProvider> descriptionProviders = registry.getOperationDescriptions(PathAddress.pathAddress(operation.require(ADDRESS)));

                ModelNode result = new ModelNode();
                if (descriptionProviders.size() > 0) {
                    for (String s : descriptionProviders.keySet()) {
                        result.add(s);
                    }
                } else {
                    result.setEmptyList();
                }

                resultHandler.handleResultFragment(new String[0], result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }
    };

    public static final ModelQueryOperationHandler READ_OPERATION_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                String operationName = operation.require(REQUEST_PROPERTIES).require(NAME).asString();

                ModelNodeRegistration registry = context.getRegistry();
                DescriptionProvider descriptionProvider = registry.getOperationDescription(PathAddress.pathAddress(operation.require(ADDRESS)), operationName);

                ModelNode result = descriptionProvider == null ? new ModelNode() : descriptionProvider.getModelDescription(getLocale(operation));

                resultHandler.handleResultFragment(new String[0], result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }
    };

    public static final ModelQueryOperationHandler READ_RESOURCE_DESCRIPTION = new ModelQueryOperationHandler() {

        @Override
        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                final boolean operations = operation.get(REQUEST_PROPERTIES, OPERATIONS).isDefined() ? operation.get(REQUEST_PROPERTIES, OPERATIONS).asBoolean() : false;
                final boolean recursive = operation.get(REQUEST_PROPERTIES, RECURSIVE).isDefined() ? operation.get(REQUEST_PROPERTIES, RECURSIVE).asBoolean() : false;

                final ModelNodeRegistration registry = context.getRegistry();
                final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS));
                final DescriptionProvider descriptionProvider = registry.getModelDescription(address);
                final Locale locale = getLocale(operation);
                final ModelNode result = descriptionProvider.getModelDescription(getLocale(operation));

                addDescription(result, recursive, operations, registry, address, locale);

                resultHandler.handleResultFragment(new String[0], result);
                resultHandler.handleResultComplete(null);
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return Cancellable.NULL;
        }

        private void addDescription(ModelNode result, boolean recursive, boolean operations, ModelNodeRegistration registry, PathAddress address, Locale locale) {

            if (operations) {
                Map<String, DescriptionProvider> ops = registry.getOperationDescriptions(address);
                if (ops.size() > 0) {

                    for (Map.Entry<String, DescriptionProvider> entry : ops.entrySet()) {
                        result.get(OPERATIONS, entry.getKey()).set(entry.getValue().getModelDescription(locale));
                    }

                } else {
                    result.get(OPERATIONS).setEmptyList();
                }
            }

            if (recursive && result.has(CHILDREN)) {
                for (PathElement element : registry.getChildAddresses(address)) {
                    PathAddress childAddress = address.append(element);
                    ModelNode child = registry.getModelDescription(childAddress).getModelDescription(locale);
                    addDescription(child, recursive, operations, registry, childAddress, locale);
                    result.get(CHILDREN, element.getKey(),MODEL_DESCRIPTION, element.getValue()).set(child);
                }
            }
        }
    };



    private static Locale getLocale(ModelNode operation) {
        if (!operation.has(REQUEST_PROPERTIES)) {
            return null;
        }
        if (!operation.get(REQUEST_PROPERTIES).has(LOCALE)) {
            return null;
        }
        return new Locale(operation.get(REQUEST_PROPERTIES, LOCALE).asString());
    }


}
