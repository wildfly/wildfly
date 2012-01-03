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
package org.jboss.as.controller.operations.validation;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALTERNATIVES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates operations against the model controllers descripton providers
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OperationValidator {

    private final ImmutableManagementResourceRegistration root;
    private final boolean validateDescriptions;
    private final boolean includeOperation;

    public OperationValidator(final ImmutableManagementResourceRegistration root) {
        this(root, true, true);
    }

    public OperationValidator(final ImmutableManagementResourceRegistration root, boolean validateDescriptions, boolean includeOperationAndDescription) {
        this.root = root;
        this.validateDescriptions = validateDescriptions;
        this.includeOperation = includeOperationAndDescription;
    }

    /**
     * Validates operations against their description providers
     *
     * @param operation The operation to validate
     * @throws IllegalArgumentException if any operation is not valid
     */
    public void validateOperations(final List<ModelNode> operations) {
        if (operations == null) {
            return;
        }

        for (ModelNode operation : operations) {
            validateOperation(operation);
        }
    }

    /**
     * Validates an operation against its description provider
     *
     * @param operation The operation to validate
     * @throws IllegalArgumentException if the operation is not valid
     */
    public void validateOperation(final ModelNode operation) {
        if (operation == null) {
            return;
        }
        final DescriptionProvider provider = getDescriptionProvider(operation);
        final ModelNode description = provider.getModelDescription(null);

        final Map<String, ModelNode> describedProperties = getDescribedRequestProperties(operation, description);
        final Map<String, ModelNode> actualParams = getActualRequestProperties(operation);

        checkActualOperationParamsAreDescribed(description, operation, describedProperties, actualParams);
        checkAllRequiredPropertiesArePresent(description, operation, describedProperties, actualParams);
        checkParameterTypes(description, operation, describedProperties, actualParams);

        //TODO check ranges
    }

    private Map<String, ModelNode> getDescribedRequestProperties(final ModelNode operation, final ModelNode description){
        final Map<String, ModelNode> requestProperties = new HashMap<String, ModelNode>();
        if (description.hasDefined(REQUEST_PROPERTIES)) {
            for (String key : description.get(REQUEST_PROPERTIES).keys()) {
                ModelNode desc = description.get(REQUEST_PROPERTIES, key);
                if (!desc.isDefined()) {
                    throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionUndefinedRequestProperty(key, getPathAddress(operation), desc));
                }
                requestProperties.put(key, desc);
            }
        }
        return requestProperties;
    }

    private Map<String, ModelNode> getActualRequestProperties(final ModelNode operation) {
        final Map<String, ModelNode> requestProperties = new HashMap<String, ModelNode>();
        for (String key : operation.keys()) {
            if (key.equals(OP) || key.equals(OP_ADDR) || key.equals(OPERATION_HEADERS)) {
                continue;
            }
            requestProperties.put(key, operation.get(key));
        }
        return requestProperties;
    }

    private void checkActualOperationParamsAreDescribed(final ModelNode description, final ModelNode operation, final Map<String, ModelNode> describedProperties, final Map<String, ModelNode> actualParams) {
        for (String paramName : actualParams.keySet()) {
            final ModelNode param = actualParams.get(paramName);
            if(! param.isDefined()) {
                continue;
            }
            if(param.getType() == ModelType.OBJECT && param.keys().isEmpty()) {
                return;
            }
            if (!describedProperties.containsKey(paramName)) {
                throw MESSAGES.validationFailedActualParameterNotDescribed(paramName, describedProperties.keySet(), formatOperationForMessage(operation));
            }
        }
    }

    private void checkAllRequiredPropertiesArePresent(final ModelNode description, final ModelNode operation, final Map<String, ModelNode> describedProperties, final Map<String, ModelNode> actualParams) {
        for (String paramName : describedProperties.keySet()) {
            final ModelNode described = describedProperties.get(paramName);
            final boolean required;
            if (described.hasDefined(REQUIRED)) {
                if (ModelType.BOOLEAN != described.get(REQUIRED).getType()) {
                    throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionRequiredFlagIsNotABoolean(paramName, getPathAddress(operation), description));
                    required = false;
                } else {
                    required = described.get(REQUIRED).asBoolean();
                }
            } else {
                required = true;
            }
            Collection<ModelNode> alternatives = null;
            if(described.hasDefined(ALTERNATIVES)) {
                alternatives = described.get(ALTERNATIVES).asList();
            }
            final boolean exist = actualParams.containsKey(paramName);
            final String alternative = hasAlternative(actualParams.keySet(), alternatives);
            if (required) {
                if(!exist && alternative == null) {
                    throw MESSAGES.validationFailedRequiredParameterNotPresent(paramName, formatOperationForMessage(operation));
                }
            }
            if(exist && alternative != null) {
                throw MESSAGES.validationFailedRequiredParameterPresentAsWellAsAlternative(alternative, paramName, formatOperationForMessage(operation));
            }
        }
    }

    private void checkParameterTypes(final ModelNode description, final ModelNode operation, final Map<String, ModelNode> describedProperties, final Map<String, ModelNode> actualParams) {
        for (String paramName : actualParams.keySet()) {
            final ModelNode value = actualParams.get(paramName);
            if(!value.isDefined()) {
                continue;
            }
            if(value.getType() == ModelType.OBJECT && value.keys().isEmpty()) {
                return;
            }
            final ModelNode typeNode = describedProperties.get(paramName).get(TYPE);
            if (!typeNode.isDefined()) {
                throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionNoParamTypeInDescription(paramName, getPathAddress(operation), description));
                return;
            }
            final ModelType modelType;
            try {
                modelType = Enum.valueOf(ModelType.class, typeNode.asString());
            } catch (Exception e) {
                throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionInvalidParamTypeInDescription(paramName, getPathAddress(operation), description));
                return;
            }

            try {
                checkType(modelType, value);
            } catch (IllegalArgumentException e) {
                throw MESSAGES.validationFailedCouldNotConvertParamToType(paramName, modelType, formatOperationForMessage(operation));
            }
            checkRange(operation, description, paramName, modelType, describedProperties.get(paramName), value);
            checkList(operation, paramName, modelType, describedProperties.get(paramName), value);
        }
    }

    private void checkRange(final ModelNode operation, final ModelNode description, final String paramName, final ModelType modelType, final ModelNode describedProperty, final ModelNode value) {
        if (!value.isDefined()) {
            return;
        }
        if (describedProperty.hasDefined(MIN)) {
            switch (modelType) {
                case BIG_DECIMAL: {
                    final BigDecimal min;
                    try {
                        min = describedProperty.get(MIN).asBigDecimal();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MIN, paramName, ModelType.BIG_DECIMAL, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asBigDecimal().compareTo(min) == -1) {
                        throw MESSAGES.validationFailedValueIsSmallerThanMin(value.asBigDecimal(), paramName, min, formatOperationForMessage(operation));
                    }
                }
                break;
                case BIG_INTEGER: {
                    final BigInteger min;
                    try {
                        min = describedProperty.get(MIN).asBigInteger();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MIN, paramName, ModelType.BIG_INTEGER, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asBigInteger().compareTo(min) == -1) {
                        throw MESSAGES.validationFailedValueIsSmallerThanMin(value.asBigInteger(), paramName, min, formatOperationForMessage(operation));
                    }
                }
                break;
                case DOUBLE: {
                    final double min;
                    try {
                        min = describedProperty.get(MIN).asDouble();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MIN, paramName, ModelType.DOUBLE, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asDouble() < min) {
                        throw MESSAGES.validationFailedValueIsSmallerThanMin(value.asDouble(), paramName, min, formatOperationForMessage(operation));
                    }
                }
                break;
                case INT: {
                    final int min;
                    try {
                        min = describedProperty.get(MIN).asInt();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MIN, paramName, ModelType.INT, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asInt() < min) {
                        throw MESSAGES.validationFailedValueIsSmallerThanMin(value.asInt(), paramName, min, formatOperationForMessage(operation));
                    }
                }
                break;
                case LONG: {
                    final long min;
                    try {
                        min = describedProperty.get(MIN).asLong();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MIN, paramName, ModelType.LONG, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asLong() < describedProperty.get(MIN).asLong()) {
                        throw MESSAGES.validationFailedValueIsSmallerThanMin(value.asLong(), paramName, min, formatOperationForMessage(operation));
                    }
                }
                break;
            }
        }
        if (describedProperty.hasDefined(MAX)) {
            switch (modelType) {
                case BIG_DECIMAL: {
                    final BigDecimal max;
                    try {
                        max = describedProperty.get(MAX).asBigDecimal();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MAX, paramName, ModelType.BIG_DECIMAL, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asBigDecimal().compareTo(max) == 1) {
                        throw MESSAGES.validationFailedValueIsGreaterThanMax(value.asBigDecimal(), paramName, max, formatOperationForMessage(operation));
                    }
                }
                break;
                case BIG_INTEGER: {
                    final BigInteger max;
                    try {
                        max = describedProperty.get(MAX).asBigInteger();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MAX, paramName, ModelType.BIG_INTEGER, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asBigInteger().compareTo(max) == 1) {
                        throw MESSAGES.validationFailedValueIsGreaterThanMax(value.asBigInteger(), paramName, max, formatOperationForMessage(operation));
                    }
                }
                break;
                case DOUBLE: {
                    final double max;
                    try {
                        max = describedProperty.get(MAX).asDouble();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MAX, paramName, ModelType.DOUBLE, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asDouble() > max) {
                        throw MESSAGES.validationFailedValueIsGreaterThanMax(value.asDouble(), paramName, max, formatOperationForMessage(operation));
                    }
                }
                break;
                case INT: {
                    final int max;
                    try {
                        max = describedProperty.get(MAX).asInt();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MAX, paramName, ModelType.INT, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asInt() > max) {
                        throw MESSAGES.validationFailedValueIsGreaterThanMax(value.asInt(), paramName, max, formatOperationForMessage(operation));
                    }
                }
                break;
                case LONG: {
                    final Long max;
                    try {
                        max = describedProperty.get(MAX).asLong();
                    } catch (IllegalArgumentException e) {
                        throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxForParameterHasWrongType(MAX, paramName, ModelType.LONG, getPathAddress(operation), description));
                        return;
                    }
                    if (value.asLong() > describedProperty.get(MAX).asLong()) {
                        throw MESSAGES.validationFailedValueIsGreaterThanMax(value.asLong(), paramName, max, formatOperationForMessage(operation));
                    }
                }
                break;
            }
        }
        if (describedProperty.hasDefined(MIN_LENGTH)) {
            final int minLength;
            try {
                minLength = describedProperty.get(MIN_LENGTH).asInt();
            } catch (IllegalArgumentException e) {
                throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxLengthForParameterHasWrongType(MIN_LENGTH, paramName, getPathAddress(operation), description));
                return;
            }
            switch (modelType) {
            case LIST:
                if (value.asList().size() < minLength) {
                    throw MESSAGES.validationFailedValueIsShorterThanMinLength(value.asList().size(), paramName, minLength, formatOperationForMessage(operation));
                }
                break;
            case BYTES:
                if (value.asBytes().length < minLength) {
                    throw MESSAGES.validationFailedValueIsShorterThanMinLength(value.asBytes().length, paramName, minLength, formatOperationForMessage(operation));
                }
                break;
            case STRING:
                if (value.asString().length() < minLength) {
                    throw MESSAGES.validationFailedValueIsShorterThanMinLength(value.asString().length(), paramName, minLength, formatOperationForMessage(operation));
                }
                break;
            }
        }
        if (describedProperty.hasDefined(MAX_LENGTH)) {
            final int maxLength;
            try {
                maxLength = describedProperty.get(MAX_LENGTH).asInt();
            } catch (IllegalArgumentException e) {
                throwOrWarnAboutDescriptorProblem(MESSAGES.invalidDescriptionMinMaxLengthForParameterHasWrongType(MAX_LENGTH, paramName, getPathAddress(operation), description));
                return;
            }
            switch (modelType) {
            case LIST:
                if (value.asList().size() > maxLength) {
                    throw MESSAGES.validationFailedValueIsLongerThanMaxLength(value.asList().size(), paramName, maxLength, formatOperationForMessage(operation));
                }
                break;
            case BYTES:
                if (value.asBytes().length > maxLength) {
                    throw MESSAGES.validationFailedValueIsLongerThanMaxLength(value.asBytes().length, paramName, maxLength, formatOperationForMessage(operation));
                }
                break;
            case STRING:
                if (value.asString().length() > maxLength) {
                    throw MESSAGES.validationFailedValueIsLongerThanMaxLength(value.asString().length(), paramName, maxLength, formatOperationForMessage(operation));
                }
                break;
            }
        }
    }


    private void checkType(final ModelType modelType, final ModelNode value) {
        switch (modelType) {
            case BIG_DECIMAL:
                value.asBigDecimal();
                break;
            case BIG_INTEGER:
                value.asBigInteger();
                break;
            case BOOLEAN:
                value.asBoolean();
                break;
            case BYTES:
                value.asBytes();
                break;
            case DOUBLE:
                value.asDouble();
                break;
            case EXPRESSION:
                value.asString();
                break;
            case INT:
                value.asInt();
                break;
            case LIST:
                value.asList();
                break;
            case LONG:
                value.asLong();
                break;
            case OBJECT:
                value.asObject();
                break;
            case PROPERTY:
                value.asProperty();
                break;
            case STRING:
                value.asString();
                break;
            case TYPE:
                value.asType();
                break;
        }
    }

    private void checkList(final ModelNode operation, final String paramName, final ModelType modelType, final ModelNode describedProperty, final ModelNode value) {
        if (describedProperty.get(TYPE).asType() == ModelType.LIST) {
            if (describedProperty.hasDefined(VALUE_TYPE) && describedProperty.get(VALUE_TYPE).getType() == ModelType.TYPE) {
                ModelType elementType = describedProperty.get(VALUE_TYPE).asType();
                for (ModelNode element : value.asList()) {
                    try {
                        checkType(elementType, element);
                    } catch (IllegalArgumentException e) {
                        throw MESSAGES.validationFailedInvalidElementType(paramName, elementType, formatOperationForMessage(operation));
                    }
                }
            }
        }
    }

    private DescriptionProvider getDescriptionProvider(final ModelNode operation) {
        if (!operation.hasDefined(OP)) {
            throw MESSAGES.validationFailedOperationHasNoField(OP, formatOperationForMessage(operation));
        }
        if (!operation.hasDefined(OP_ADDR)) {
            throw MESSAGES.validationFailedOperationHasNoField(OP_ADDR, formatOperationForMessage(operation));
        }
        final String name = operation.get(OP).asString();
        if (name == null || name.trim().length() == 0) {
            throw MESSAGES.validationFailedOperationHasANullOrEmptyName(formatOperationForMessage(operation));
        }

        final PathAddress addr = getPathAddress(operation);

        final DescriptionProvider provider = root.getOperationDescription(addr, name);
        if (provider == null) {
            throw MESSAGES.validationFailedNoOperationFound(name, addr, formatOperationForMessage(operation));
        }
        return provider;
    }

    private String hasAlternative(final Set<String> keys, Collection<ModelNode> alternatives) {
        if(alternatives == null || alternatives.isEmpty()) {
            return null;
        }
        for(final ModelNode alternative : alternatives) {
            if(keys.contains(alternative.asString())) {
                return alternative.asString();
            }
        }
        return null;
    }

    private PathAddress getPathAddress(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }

    /**
     * Throws an exception or logs a message
     *
     * @param message The message for the exception or the log message. Must be internationalized
     */
    private void throwOrWarnAboutDescriptorProblem(String message) {
        if (validateDescriptions) {
            throw new IllegalArgumentException(message);
        }
        ControllerLogger.ROOT_LOGGER.warn(message);
    }

    private String formatOperationForMessage(ModelNode operation) {
        if (includeOperation) {
            return operation.asString();
        }
        return "";
    }

//  // TODO enable once AS7-2421 is complete
//  void validateRemoveOperations() {
//      final ModelNode missing = new ModelNode().setEmptyList();
//      validateRemoveOperations(PathAddress.EMPTY_ADDRESS, root, missing);
//      if(missing.asInt() > 0) {
//          Assert.fail("following resources are missing a remove operation " + missing);
//      }
//  }
//  /**
//   * Check that all resources registering an add operation also provide a remove operation.
//   *
//   * @param current the current path address
//   * @param registration the MNR
//   * @param missing the missing remove operations info
//   */
//  private void validateRemoveOperations(final PathAddress current, final ImmutableManagementResourceRegistration registration, final ModelNode missing) {
//      final OperationStepHandler addHandler = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD);
//      if(addHandler != null) {
//          final OperationStepHandler remove = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE);
//          if(remove == null) {
//              missing.add(current.toModelNode());
//          }
//      }
//      final Set<PathElement> children = registration.getChildAddresses(PathAddress.EMPTY_ADDRESS);
//      for(final PathElement child : children) {
//          final ImmutableManagementResourceRegistration childReg = registration.getSubModel(PathAddress.EMPTY_ADDRESS.append(child));
//          if(childReg != null) {
//              validateRemoveOperations(current.append(child), childReg, missing);
//          }
//      }
//  }
}
