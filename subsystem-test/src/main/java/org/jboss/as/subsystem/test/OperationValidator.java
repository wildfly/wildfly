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
package org.jboss.as.subsystem.test;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates operations against the model controllers descripton providers
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class OperationValidator {

    private final ManagementResourceRegistration root;

    OperationValidator(final ManagementResourceRegistration root) {
        this.root = root;
    }

    void validateOperations(final List<ModelNode> operations) {
        if (operations == null) {
            return;
        }
        for (ModelNode operation : operations) {
            validateOperation(operation);
        }
    }

    // TODO enable once AS7-2421 is complete
    void validateRemoveOperations() {
        final ModelNode missing = new ModelNode().setEmptyList();
        validateRemoveOperations(PathAddress.EMPTY_ADDRESS, root, missing);
        if(missing.asInt() > 0) {
            Assert.fail("following resources are missing a remove operation " + missing);
        }
    }

    void validateOperation(final ModelNode operation) {
        if (operation == null) {
            return;
        }
        final DescriptionProvider provider = getDescriptionProvider(operation);
        final ModelNode description = provider.getModelDescription(null);

        final Map<String, ModelNode> describedProperties = getDescribedRequestProperties(description);
        final Map<String, ModelNode> actualParams = getActualRequestProperties(operation);

        checkActualOperationParamsAreDescribed(description, operation, describedProperties, actualParams);
        checkAllRequiredPropertiesArePresent(description, operation, describedProperties, actualParams);
        checkParameterTypes(description, operation, describedProperties, actualParams);

        //TODO check ranges
    }

    private Map<String, ModelNode> getDescribedRequestProperties(final ModelNode description){
        final Map<String, ModelNode> requestProperties = new HashMap<String, ModelNode>();
        if (description.hasDefined(REQUEST_PROPERTIES)) {
            for (String key : description.get(REQUEST_PROPERTIES).keys()) {
                ModelNode desc = description.get(REQUEST_PROPERTIES, key);
                Assert.assertTrue("Undefined request property '" + key + "' in " + description, desc.isDefined());
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
                Assert.fail("Operation " + operation + " contains a parameter '" + paramName + "' which does not appear in the description " + description);
            }
        }
    }

    private void checkAllRequiredPropertiesArePresent(final ModelNode description, final ModelNode operation, final Map<String, ModelNode> describedProperties, final Map<String, ModelNode> actualParams) {
        for (String paramName : describedProperties.keySet()) {
            final ModelNode described = describedProperties.get(paramName);
            final boolean required;
            if (described.hasDefined(REQUIRED)) {
                Assert.assertEquals("'" + REQUIRED + "' for '" + paramName + "' must be a boolean in " + description, ModelType.BOOLEAN, described.get(REQUIRED).getType());
                required = described.get(REQUIRED).asBoolean();
            } else {
                required = true;
            }
            Collection<ModelNode> alternatives = null;
            if(described.hasDefined(ALTERNATIVES)) {
                alternatives = described.get(ALTERNATIVES).asList();
            }
            final boolean exist = actualParams.containsKey(paramName);
            if (required) {
                if(! exist && ! hasAlternative(actualParams.keySet(), alternatives)) {
                    Assert.fail("Required parameter '" + paramName + "' is not present in " + operation);
                }
            }
            if(exist && hasAlternative(actualParams.keySet(), alternatives)) {
                Assert.fail("Alternative parameter for '" + paramName + "' is present in " + operation);
            }
        }
    }

    private void checkParameterTypes(final ModelNode description, final ModelNode operation, final Map<String, ModelNode> describedProperties, final Map<String, ModelNode> actualParams) {
        for (String paramName : actualParams.keySet()) {
            ModelNode value = actualParams.get(paramName);
            if(!value.isDefined()) {
                continue;
            }
            if(value.getType() == ModelType.OBJECT && value.keys().isEmpty()) {
                return;
            }
            ModelNode typeNode = describedProperties.get(paramName).get(TYPE);
            Assert.assertTrue("No type for param '" + paramName + "' in " + description, typeNode.isDefined());
            final ModelType modelType;
            try {
                modelType = Enum.valueOf(ModelType.class, typeNode.asString());
            } catch (Exception e) {
                Assert.fail("Could not determine type for param '" + paramName + "' in " + description);
                return;
            }

            try {
                checkType(modelType, value);
            } catch (IllegalArgumentException e) {
                Assert.fail("Could not convert '" + paramName + "' to a " + modelType + " in " + operation);
            }
            checkRange(operation, paramName, modelType, describedProperties.get(paramName), value);
            checkList(operation, paramName, modelType, describedProperties.get(paramName), value);
        }
    }

    private void checkRange(final ModelNode operation, final String paramName, final ModelType modelType, final ModelNode describedProperty, final ModelNode value) {
        if (!value.isDefined()) {
            return;
        }
        if (describedProperty.hasDefined(MIN)) {
            switch (modelType) {
            case BIG_DECIMAL:
                Assert.assertFalse(paramName + ":" + value.asBigDecimal() + " is smaller than min " + describedProperty.get(MIN).asBigDecimal() + " for " + operation,
                        value.asBigDecimal().compareTo(describedProperty.get(MIN).asBigDecimal()) == -1);
                break;
            case BIG_INTEGER:
                Assert.assertFalse(paramName + ":" + value.asBigInteger() + " is smaller than min " + describedProperty.get(MIN).asBigInteger() + " for " + operation,
                        value.asBigInteger().compareTo(describedProperty.get(MIN).asBigInteger()) == -1);
                break;
            case DOUBLE:
                Assert.assertFalse(paramName + ":" + value.asDouble() + " is smaller than min " + describedProperty.get(MIN).asDouble() + " for " + operation,
                        value.asDouble() < describedProperty.get(MIN).asDouble());
                break;
            case INT:
                Assert.assertFalse(paramName + ":" + value.asInt() + " is smaller than min " + describedProperty.get(MIN).asInt() + " for " + operation,
                        value.asInt() < describedProperty.get(MIN).asInt());
                break;
            case LONG:
                Assert.assertFalse(paramName + ":" + value.asLong() + " is smaller than min " + describedProperty.get(MIN).asLong() + " for " + operation,
                        value.asLong() < describedProperty.get(MIN).asLong());
                break;
            }
        }
        if (describedProperty.hasDefined(MAX)) {
            switch (modelType) {
            case BIG_DECIMAL:
                Assert.assertFalse(paramName + ":" + value.asBigDecimal() + " is bigger than max " + describedProperty.get(MAX).asBigDecimal() + " for " + operation,
                        value.asBigDecimal().compareTo(describedProperty.get(MAX).asBigDecimal()) == 1);
                break;
            case BIG_INTEGER:
                Assert.assertFalse(paramName + ":" + value.asBigInteger() + " is bigger than max " + describedProperty.get(MAX).asBigInteger() + " for " + operation,
                        value.asBigInteger().compareTo(describedProperty.get(MAX).asBigInteger()) == 1);
                break;
            case DOUBLE:
                Assert.assertFalse(paramName + ":" + value.asDouble() + " is bigger than max " + describedProperty.get(MAX).asDouble() + " for " + operation,
                        value.asDouble() > describedProperty.get(MAX).asDouble());
                break;
            case INT:
                Assert.assertFalse(paramName + ":" + value.asInt() + " is bigger than max " + describedProperty.get(MAX).asInt() + " for " + operation,
                        value.asInt() > describedProperty.get(MAX).asInt());
                break;
            case LONG:
                Assert.assertFalse(paramName + ":" + value.asLong() + " is bigger than max " + describedProperty.get(MAX).asLong() + " for " + operation,
                        value.asLong() > describedProperty.get(MAX).asLong());
                break;
            }
        }
        if (describedProperty.hasDefined(MIN_LENGTH)) {
            int minLength = describedProperty.get(MIN_LENGTH).asInt();
            switch (modelType) {
            case LIST:
                Assert.assertTrue(paramName + ":" + value.asList().size() + " is shorter than min-length " + minLength + " for " + operation,
                        value.asList().size() >= minLength);
                break;
            case BYTES:
                Assert.assertTrue(paramName + ":" + value.asBytes().length + " is shorter than min-length " + minLength + " for " + operation,
                        value.asBytes().length >= minLength);
                break;
            case STRING:
                Assert.assertTrue(paramName + ":" + value.asString().length() + " is shorter than min-length " + minLength + " for " + operation,
                        value.asString().length() >= minLength);
                break;
            }
        }
        if (describedProperty.hasDefined(MAX_LENGTH)) {
            int minLength = describedProperty.get(MAX_LENGTH).asInt();
            switch (modelType) {
            case LIST:
                Assert.assertTrue(paramName + ":" + value.asList().size() + " is longer than max-length " + minLength + " for " + operation,
                        value.asList().size() <= minLength);
                break;
            case BYTES:
                Assert.assertTrue(paramName + ":" + value.asBytes().length + " is longer than max-length " + minLength + " for " + operation,
                        value.asBytes().length <= minLength);
                break;
            case STRING:
                Assert.assertTrue(paramName + ":" + value.asString().length() + " is longer than max-length " + minLength + " for " + operation,
                        value.asString().length() <= minLength);
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
                        Assert.fail(paramName + " is expected to be a list of " + elementType + " " + operation);
                    }
                }
            }
        }
    }

    /**
     * Check that all resources registering an add operation also provide a remove operation.
     *
     * @param current the current path address
     * @param registration the MNR
     * @param missing the missing remove operations info
     */
    private void validateRemoveOperations(final PathAddress current, final ImmutableManagementResourceRegistration registration, final ModelNode missing) {
        final OperationStepHandler addHandler = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.ADD);
        if(addHandler != null) {
            final OperationStepHandler remove = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, ModelDescriptionConstants.REMOVE);
            if(remove == null) {
                missing.add(current.toModelNode());
            }
        }
        final Set<PathElement> children = registration.getChildAddresses(PathAddress.EMPTY_ADDRESS);
        for(final PathElement child : children) {
            final ImmutableManagementResourceRegistration childReg = registration.getSubModel(PathAddress.EMPTY_ADDRESS.append(child));
            if(childReg != null) {
                validateRemoveOperations(current.append(child), childReg, missing);
            }
        }
    }

    DescriptionProvider getDescriptionProvider(final ModelNode operation) {
        Assert.assertTrue("Operation has no " + OP + " field", operation.hasDefined(OP));
        Assert.assertTrue("Operation has no " + OP_ADDR + " field", operation.hasDefined(OP_ADDR));

        final String name = operation.get(OP).asString();
        Assert.assertNotNull("Null operation name", name);
        Assert.assertTrue("Empty operation name " + name, name.trim().length() > 0);

        final PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));

        final DescriptionProvider provider = root.getOperationDescription(addr, name);
        Assert.assertNotNull("No operation called '" + name + "' for " + addr, provider);
        return provider;
    }

    static boolean hasAlternative(final Set<String> keys, Collection<ModelNode> alternatives) {
        if(alternatives == null || alternatives.isEmpty()) {
            return false;
        }
        for(final ModelNode alternative : alternatives) {
            if(keys.contains(alternative.asString())) {
                return true;
            }
        }
        return false;
    }
}
