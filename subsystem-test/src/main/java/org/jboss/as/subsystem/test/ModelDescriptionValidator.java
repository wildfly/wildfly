/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALTERNATIVES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates the types and entries of the of the description providers in the model, as read by
 * {@code /some/path:read-resource-description(recursive=true,inherited=false,operations=true)}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelDescriptionValidator {

    private static final Map<String, ArbitraryDescriptorValidator> VALID_RESOURCE_KEYS;
    private static final Map<String, ArbitraryDescriptorValidator> VALID_CHILD_TYPE_KEYS;
    private static final Map<String, AttributeOrParameterArbitraryDescriptorValidator> VALID_ATTRIBUTE_KEYS;
    private static final Map<String, ArbitraryDescriptorValidator> VALID_OPERATION_KEYS;
    private static final Map<String, AttributeOrParameterArbitraryDescriptorValidator> VALID_PARAMETER_KEYS;
    static {
        Map<String, ArbitraryDescriptorValidator> validResourceKeys = new HashMap<String, ArbitraryDescriptorValidator>();
        validResourceKeys.put(DESCRIPTION, NullDescriptorValidator.INSTANCE);
        validResourceKeys.put(HEAD_COMMENT_ALLOWED, BooleanDescriptorValidator.INSTANCE);
        validResourceKeys.put(TAIL_COMMENT_ALLOWED, BooleanDescriptorValidator.INSTANCE);
        validResourceKeys.put(NAMESPACE, NullDescriptorValidator.INSTANCE);
        validResourceKeys.put(ATTRIBUTES, NullDescriptorValidator.INSTANCE);
        validResourceKeys.put(OPERATIONS, NullDescriptorValidator.INSTANCE);
        validResourceKeys.put(CHILDREN, NullDescriptorValidator.INSTANCE);
        VALID_RESOURCE_KEYS = Collections.unmodifiableMap(validResourceKeys);

        Map<String, ArbitraryDescriptorValidator> validChildTypeKeys = new HashMap<String, ModelDescriptionValidator.ArbitraryDescriptorValidator>();
        validChildTypeKeys.put(DESCRIPTION, NullDescriptorValidator.INSTANCE);
        validChildTypeKeys.put(MODEL_DESCRIPTION, NullDescriptorValidator.INSTANCE);
        validChildTypeKeys.put(MIN_OCCURS, SimpleIntDescriptorValidator.INSTANCE);
        validChildTypeKeys.put(MAX_OCCURS, SimpleIntDescriptorValidator.INSTANCE);
        validChildTypeKeys.put(ALLOWED, StringListValidator.INSTANCE);
        VALID_CHILD_TYPE_KEYS = Collections.unmodifiableMap(validChildTypeKeys);


        Map<String, AttributeOrParameterArbitraryDescriptorValidator> paramAndAttributeKeys = new HashMap<String, AttributeOrParameterArbitraryDescriptorValidator>();
        //Normal
        paramAndAttributeKeys.put(DESCRIPTION, NullDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(REQUIRED, BooleanDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(NILLABLE, BooleanDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(VALUE_TYPE, NullDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(TYPE, NullDescriptorValidator.INSTANCE);
        //Arbitrary
        paramAndAttributeKeys.put(ALTERNATIVES, StringListValidator.INSTANCE);
        paramAndAttributeKeys.put(REQUIRES, StringListValidator.INSTANCE);
        paramAndAttributeKeys.put(MIN, NumericDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(MAX, NumericDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(MIN_LENGTH, LengthDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(MAX_LENGTH, LengthDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(DEFAULT, NullDescriptorValidator.INSTANCE); //TODO Validate same type as target type
        paramAndAttributeKeys.put(ALLOWED, StringListValidator.INSTANCE);
        paramAndAttributeKeys.put(UNIT, NumericDescriptorValidator.INSTANCE);
        paramAndAttributeKeys.put(EXPRESSIONS_ALLOWED, BooleanDescriptorValidator.INSTANCE);

        Map<String, AttributeOrParameterArbitraryDescriptorValidator> validAttributeKeys = new HashMap<String, AttributeOrParameterArbitraryDescriptorValidator>();
        validAttributeKeys.putAll(paramAndAttributeKeys);
        validAttributeKeys.put(ACCESS_TYPE, AccessTypeDescriptorValidator.INSTANCE);
        validAttributeKeys.put(HEAD_COMMENT_ALLOWED, BooleanDescriptorValidator.INSTANCE);
        validAttributeKeys.put(TAIL_COMMENT_ALLOWED, BooleanDescriptorValidator.INSTANCE);
        validAttributeKeys.put(STORAGE, StorageDescriptorValidator.INSTANCE);
        validAttributeKeys.put(RESTART_REQUIRED, NullDescriptorValidator.INSTANCE); //TODO some more validation here
        VALID_ATTRIBUTE_KEYS = Collections.unmodifiableMap(validAttributeKeys);

        Map<String, ArbitraryDescriptorValidator> validOperationKeys = new HashMap<String, ArbitraryDescriptorValidator>();
        validOperationKeys.put(DESCRIPTION, NullDescriptorValidator.INSTANCE);
        validOperationKeys.put(OPERATION_NAME, NullDescriptorValidator.INSTANCE);
        validOperationKeys.put(REQUEST_PROPERTIES, NullDescriptorValidator.INSTANCE);
        validOperationKeys.put(REPLY_PROPERTIES, NullDescriptorValidator.INSTANCE);
        VALID_OPERATION_KEYS = Collections.unmodifiableMap(validOperationKeys);

        Map<String, AttributeOrParameterArbitraryDescriptorValidator> validParameterKeys = new HashMap<String, AttributeOrParameterArbitraryDescriptorValidator>();
        validParameterKeys.putAll(paramAndAttributeKeys);
        VALID_PARAMETER_KEYS = Collections.unmodifiableMap(validParameterKeys);
    }

    private final List<ValidationFailure> errors;
    private final ModelNode address;
    private final ModelNode description;
    private final ValidationConfiguration validationConfiguration;

    /**
     * Constructor
     *
     * @param address the address of the model
     * @param description the description of the model
     * @param validationConfiguration extra configuration
     */
    public ModelDescriptionValidator(ModelNode address, ModelNode description, ValidationConfiguration validationConfiguration) {
        this(new ArrayList<ValidationFailure>(), address, description, validationConfiguration);
    }

    private ModelDescriptionValidator(List<ValidationFailure> errors, ModelNode address, ModelNode description, ValidationConfiguration validationConfiguration) {
        if (address == null) {
            throw new IllegalArgumentException("Null address");
        }
        if (description == null) {
            throw new IllegalArgumentException("Null address");
        }
        this.errors = errors;
        this.address = address;
        this.description = description;
        this.validationConfiguration = validationConfiguration == null ? new ValidationConfiguration() : validationConfiguration;
    }

    public List<ValidationFailure> validateResource() {
        if (!description.isDefined()) {
            errors.add(new ValidationFailure("No model", address));
            return errors;
        }
        for (String key : description.keys()) {
            ArbitraryDescriptorValidator validator = VALID_RESOURCE_KEYS.get(key);
            if (validator == null) {
                errors.add(new ValidationFailure("Invalid key '" + key + "'", address));
            } else {
                if (description.hasDefined(key)) {
                    String error = validator.validate(description, key);
                    if (error != null) {
                        errors.add(new ValidationFailure(error, address));
                    }
                } else {
                    if (!key.equals(OPERATIONS) && !key.equals(CHILDREN)) {
                        errors.add(new ValidationFailure("Empty key '" + key + "'", address));
                    }
                }

            }
        }
        if (!description.hasDefined(DESCRIPTION)) {
            errors.add(new ValidationFailure("Missing description", address));
        }
        if (description.hasDefined("attributes")) {
            validateAttributes(description.get("attributes"));
        }
        if (description.hasDefined("operations")) {
            validateOperations(description.get("operations"));
        }
        if (description.hasDefined("children")) {
            validateChildren(description.get("children"));
        }

        return errors;
    }

    private void validateAttributes(ModelNode attributes) {
        for (String key : attributes.keys()) {
            ModelNode attribute = attributes.get(key);
            AttributeValidationElement attributeValidationElement = new AttributeValidationElement(key);
            validateAttributeOrParameter(attributeValidationElement, attribute);
        }
    }

    private void validateOperations(ModelNode operations) {
        for (String key : operations.keys()) {
            OperationValidationElement operation = new OperationValidationElement(key);
            ModelNode op = operations.get(key);
            operation.validateKeys(op);

            if (!op.hasDefined(DESCRIPTION)) {
                errors.add(operation.createValidationFailure("Missing description"));
            }
            if (!key.equals(op.get(OPERATION_NAME).asString())) {
                errors.add(operation.createValidationFailure("Expected operation-name '" + key + "'"));
            }
            if (op.hasDefined(REQUEST_PROPERTIES)) {
                for (String param : op.get(REQUEST_PROPERTIES).keys()) {
                    ModelNode paramNode = op.get(REQUEST_PROPERTIES, param);
                    OperationParameterValidationElement parameterValidationElement = new OperationParameterValidationElement(operation, param);
                    validateAttributeOrParameter(parameterValidationElement, paramNode);
                }
            }
            if (op.hasDefined(REPLY_PROPERTIES)) {
                ModelNode reply = op.get(REPLY_PROPERTIES);
                OperationParameterValidationElement parameterValidationElement = new OperationParameterValidationElement(operation, REPLY_PROPERTIES);
                validateAttributeOrParameter(parameterValidationElement, reply);
            }
        }
    }

    private void validateChildren(ModelNode children) {
        for (String type : children.keys()) {
            ModelNode typeNode = children.get(type);
            for (String key : typeNode.keys()) {
                ArbitraryDescriptorValidator validator = VALID_CHILD_TYPE_KEYS.get(key);
                if (validator == null) {
                    errors.add(new ValidationFailure("Invalid key '" + key + "' found for child type '" + type + "'", address));
                } else {
                    if (typeNode.hasDefined(key)) {
                        String error = validator.validate(typeNode, key);
                        if (error != null) {
                            errors.add(new ValidationFailure(error, address));
                        }
                    } else {
                        if (!key.equals(MODEL_DESCRIPTION)) {
                            errors.add(new ValidationFailure("Empty key '" + key + "' found for child type '" + type + "'", address));
                        }
                    }
                }
            }

            if (!typeNode.hasDefined(DESCRIPTION)) {
                errors.add(new ValidationFailure("Missing description for child type '" + type + "' on ModelNode '" + typeNode.toString()+"'", address));
            }
            ModelNode childType = typeNode.get(MODEL_DESCRIPTION);
            if (!childType.isDefined()) {
                errors.add(new ValidationFailure("No model description for child '" + type + "'", address));
                continue;
            }

            for (String child : childType.keys()) {
                ModelNode childAddress = address.clone();
                childAddress.add(type, child);
                ModelDescriptionValidator childValidator = new ModelDescriptionValidator(errors, childAddress, childType.get(child), validationConfiguration);
                childValidator.validateResource();
            }
        }
    }

    private void validateAttributeOrParameter(AttributeValidationElement validationElement, ModelNode rawDescription) {
        boolean reply = validationElement.name.equals(REPLY_PROPERTIES);
        if (!rawDescription.hasDefined(DESCRIPTION) && !reply) {
            errors.add(validationElement.createValidationFailure("Missing description"));
        }
        if (reply && rawDescription.isDefined()) {
          if (rawDescription.keys().size() == 0) {
              return;
          }
        }
        validateAttributeOrParameter(validationElement, rawDescription, rawDescription);
    }

    private void validateAttributeOrParameter(AttributeValidationElement validationElement, ModelNode rawDescription, ModelNode currentDescription) {

        if (!currentDescription.isDefined()) {
            errors.add(validationElement.createValidationFailure("Undefined description " + currentDescription.asString()));
        }


        ModelNode typeNode = currentDescription.hasDefined(TYPE) ? currentDescription.get(TYPE) : null;
        ModelNode valueTypeNode = currentDescription.hasDefined(VALUE_TYPE) ? currentDescription.get(VALUE_TYPE) : null;

        ModelType type;
        try {
            type = typeNode.asType();
        } catch (Exception e) {
            errors.add(validationElement.createValidationFailure("Invalid type " + currentDescription.asString()));
            return;
        }

        if (type == ModelType.OBJECT || type == ModelType.LIST) {
            if (valueTypeNode == null && !validationElement.allowNullValueTypeForObject()) {
                errors.add(validationElement.createValidationFailure("No value-type for type=" + type + " " + currentDescription.asString()));
                return;
            }
        } else {
            if (valueTypeNode != null) {
                errors.add(validationElement.createValidationFailure("Unnecessary value-type for type=" + type + " " + currentDescription.asString()));
            }
        }

        if (valueTypeNode != null) {
            try {
                valueTypeNode.asType();
            } catch (Exception e) {
                //Complex type

                Set<String> keys;
                try {
                    keys = valueTypeNode.keys();
                } catch (Exception e1) {
                    errors.add(validationElement.createValidationFailure("Could not get keys for value-type for type=" + type + " " + currentDescription.asString()));
                    return;
                }
                for (String key : keys) {
                    validateAttributeOrParameter(validationElement, rawDescription, valueTypeNode.get(key));
                }
            }
        }
        validationElement.validateKeys(type, currentDescription);
    }

    /**
     * Validate an attribute or parameter descriptor
     */
    public interface AttributeOrParameterArbitraryDescriptorValidator {
        /**
         * Validate an attribute or parameter descriptor
         *
         * @param currentType the type of the parameter/attribute
         * @param currentNode the current attribute or parameter
         * @param descriptor the current descriptor being validated
         * @return {@code null} if it passed validation, or an error message describing the problem
         */
        String validate(ModelType currentType, ModelNode currentNode, String descriptor);
    }

    /**
     * Validate a resource or operation descriptor
     */
    public interface ArbitraryDescriptorValidator {
        /**
         * Validate a resource or operation arbitrary descriptor
         *
         * @param currentNode the current attribute or parameter
         * @param descriptor the current descriptor being validated
         * @return {@code null} if it passed validation, or an error message describing the problem
         */
        String validate(ModelNode currentNode, String descriptor);
    }

    private static class SimpleIntDescriptorValidator implements ArbitraryDescriptorValidator {
        static final SimpleIntDescriptorValidator INSTANCE = new SimpleIntDescriptorValidator();
        @Override
        public String validate(ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                try {
                    currentNode.get(descriptor).asInt();
                } catch (Exception e) {
                    return "'" + descriptor + "' is not an int";
                }

            }
            return null;
        }
    }

    private static class NumericDescriptorValidator implements AttributeOrParameterArbitraryDescriptorValidator {

        static final NumericDescriptorValidator INSTANCE = new NumericDescriptorValidator();

        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                if (currentType != ModelType.BIG_DECIMAL && currentType != ModelType.BIG_INTEGER &&
                        currentType != ModelType.DOUBLE && currentType != ModelType.INT && currentType != ModelType.LONG) {
                    return "Unnecessary '" + descriptor + "' for non-numeric type=" + currentType;
                }
                if (!descriptor.equals(UNIT)) {
                    try {
                        if (currentType == ModelType.BIG_DECIMAL) {
                            currentNode.get(descriptor).asBigDecimal();
                        } else if (currentType == ModelType.BIG_INTEGER) {
                            currentNode.get(descriptor).asBigInteger();
                        } else if (currentType == ModelType.DOUBLE) {
                            currentNode.get(descriptor).asDouble();
                        } else if (currentType == ModelType.INT) {
                            currentNode.get(descriptor).asInt();
                        } else if (currentType == ModelType.LONG) {
                            currentNode.get(descriptor).asLong();
                        }
                    } catch (Exception e) {
                        return "'" + descriptor + "' is not a " + currentType;

                    }
                }
            }
            return null;
        }
    }

    private static class LengthDescriptorValidator implements AttributeOrParameterArbitraryDescriptorValidator {

        static final LengthDescriptorValidator INSTANCE = new LengthDescriptorValidator();

        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                if (currentType != ModelType.LIST && currentType != ModelType.STRING &&
                        currentType != ModelType.BYTES) {
                    return "Unnecessary '" + descriptor + "' for non-numeric type=" + currentType;
                }
                try {
                    currentNode.get(descriptor).asLong();
                } catch (Exception e) {
                    return "'" + descriptor + "' is not a int/long";
                }
            }
            return null;
        }
    }

    private static class BooleanDescriptorValidator implements ArbitraryDescriptorValidator, AttributeOrParameterArbitraryDescriptorValidator {
        static final BooleanDescriptorValidator INSTANCE = new BooleanDescriptorValidator();

        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                try {
                    currentNode.get(descriptor).asBoolean();
                } catch (Exception e) {
                    return "'" + descriptor + "' is not a boolean";
                }
            }
            return null;
        }

        @Override
        public String validate(ModelNode currentNode, String descriptor) {
            return validate(null, currentNode, descriptor);
        }
    }

    private static class AccessTypeDescriptorValidator implements AttributeOrParameterArbitraryDescriptorValidator {
        static final AccessTypeDescriptorValidator INSTANCE = new AccessTypeDescriptorValidator();

        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                try {
                    AccessType.valueOf(currentNode.get(descriptor).asString().toUpperCase().replace('-', '_'));
                } catch (Exception e) {
                    return "'" + descriptor + "=" + currentNode.get(descriptor) + "' is not an access type";
                }
            }
            return null;
        }
    }

    private static class StorageDescriptorValidator implements AttributeOrParameterArbitraryDescriptorValidator {
        static final StorageDescriptorValidator INSTANCE = new StorageDescriptorValidator();

        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                try {
                    AttributeAccess.Storage.valueOf(currentNode.get(descriptor).asString().toUpperCase());
                } catch (Exception e) {
                    return "'" + descriptor + "' is not a storage type";
                }
            }
            return null;
        }
    }


    private static class NullDescriptorValidator implements ArbitraryDescriptorValidator, AttributeOrParameterArbitraryDescriptorValidator {
        static final NullDescriptorValidator INSTANCE = new NullDescriptorValidator();

        @Override
        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            return null;
        }

        @Override
        public String validate(ModelNode currentNode, String descriptor) {
            return null;
        }
    }

    private static class StringListValidator implements ArbitraryDescriptorValidator, AttributeOrParameterArbitraryDescriptorValidator {
        static final StringListValidator INSTANCE = new StringListValidator();

        @Override
        public String validate(ModelType currentType, ModelNode currentNode, String descriptor) {
            return validate(currentNode, descriptor);
        }

        @Override
        public String validate(ModelNode currentNode, String descriptor) {
            if (currentNode.hasDefined(descriptor)) {
                List<ModelNode> list;
                try {
                    list = currentNode.asList();
                } catch (Exception e) {
                    return "'" + descriptor + "' is not a list";
                }
                for (ModelNode entry : list) {
                    try {
                        String s = entry.asString();
                    } catch (Exception e) {
                        return "'" + descriptor + "' is not a string";
                    }

                }
            }
            return null;
        }
    }

    /**
     * Allows extra configuration of the validation
     */
    public static class ValidationConfiguration {
        private Map<ModelNode, Set<String>> nullValueTypeAttributes = new HashMap<ModelNode, Set<String>>();
        private Map<ModelNode, Map<String, Set<String>>> nullValueTypeOperations = new HashMap<ModelNode, Map<String, Set<String>>>();
        private Map<ModelNode, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>> attributeDescriptors = new HashMap<ModelNode, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>>();
        private Map<ModelNode, Map<String, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>>> operationParameterDescriptors = new HashMap<ModelNode, Map<String, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>>>();

        /**
         * Allow undefined value-types for a OBJECT or LIST type attribute. This should be used
         * sparingly since an undefined value-type is normally a problem with your model.
         *
         * @param address the address of the attribute
         * @param name the name of the attribute
         */
        public void allowNullValueTypeForAttribute(ModelNode address, String name) {
            Set<String> names = nullValueTypeAttributes.get(address);
            if (names == null) {
                names = new HashSet<String>();
                nullValueTypeAttributes.put(address, names);
            }
            names.add(name);
        }

        /**
         * Allow undefined value-types for OBJECT or LIST type for all parameters and return types for an operation.
         * This should be used sparingly since an undefined value-type is normally a problem with your model.
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         */
        public void allowNullValueTypeForOperation(ModelNode address, String operation) {
            allowNullValueTypeForOperationParameter(address, operation, "*");
        }

        /**
         * Allow undefined value-types for OBJECT or LIST type for an operation's reply-properties.
         * This should be used sparingly since an undefined value-type is normally a problem with your model.
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         */
        public void allowNullValueTypeForOperationReplyProperties(ModelNode address, String operation) {
            allowNullValueTypeForOperationParameter(address, operation, REPLY_PROPERTIES);
        }

        /**
         * Allow undefined value-types for OBJECT or LIST type for an operation's parameter.
         * This should be used sparingly since an undefined value-type is normally a problem with your model.
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         * @param param the name of the parameter
         */
        public void allowNullValueTypeForOperationParameter(ModelNode address, String operation, String param) {
            Map<String, Set<String>> names = nullValueTypeOperations.get(address);
            if (names == null) {
                names = new HashMap<String, Set<String>>();
                nullValueTypeOperations.put(address, names);
            }
            Set<String> params = names.get(operation);
            if (params == null) {
                params = new HashSet<String>();
                names.put(operation, params);
            }
            params.add(param);
        }

        /**
         * Register an additional arbitrary descriptor for an attribute
         *
         * @param address the address of the attribute
         * @param name the name of the attribute
         * @param descriptor the arbitrary descriptor to register for the attribute
         * @param validator an implementation of a validator, may be {@code null}
         */
        public void registerAttributeArbitraryDescriptor(ModelNode address, String name, String descriptor, AttributeOrParameterArbitraryDescriptorValidator validator) {
            Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>> byName = attributeDescriptors.get(address);
            if (byName == null) {
                byName = new HashMap<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>();
                attributeDescriptors.put(address, byName);
            }
            Map<String, AttributeOrParameterArbitraryDescriptorValidator> descriptors = byName.get(name);
            if (descriptors == null) {
                descriptors = new HashMap<String, AttributeOrParameterArbitraryDescriptorValidator>();
                byName.put(name, descriptors);
            }
            descriptors.put(descriptor, validator == null ? NullDescriptorValidator.INSTANCE : validator);
        }

        /**
         * Register an additional arbitrary descriptor for all of an operation's parameters and reply properties
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         * @param descriptor the arbitrary descriptor to register for the operation's parameters
         * @param validator an implementation of a validator, may be {@code null}
         */
        public void registerArbitraryDescriptorForOperation(ModelNode address, String operation, String descriptor, AttributeOrParameterArbitraryDescriptorValidator validator) {
            registerArbitraryDescriptorForOperationParameter(address, operation, "*", descriptor, validator);
        }

        /**
         * Register an additional arbitrary descriptor for an operation's reply properties
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         * @param descriptor the arbitrary descriptor to register for the operation's parameters
         * @param validator an implementation of a validator, may be {@code null}
         */
        public void registerArbitraryDescriptorForOperationReplyProperties(ModelNode address, String operation, String descriptor, AttributeOrParameterArbitraryDescriptorValidator validator) {
            registerArbitraryDescriptorForOperationParameter(address, operation, REPLY_PROPERTIES, descriptor, validator);
        }

        /**
         * Register an additional arbitrary descriptor for an operation's parameter
         *
         * @param address the address of the operation
         * @param operation the name of the operation
         * @param parameter the name of the parameter
         * @param descriptor the arbitrary descriptor to register for the operation's parameters
         * @param validator an implementation of a validator, may be {@code null}
         */
        public void registerArbitraryDescriptorForOperationParameter(ModelNode address, String operation, String parameter, String descriptor, AttributeOrParameterArbitraryDescriptorValidator validator) {
            Map<String, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>> byName = operationParameterDescriptors.get(address);
            if (byName == null) {
                byName = new HashMap<String, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>>();
                operationParameterDescriptors.put(address, byName);
            }
            Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>> params = byName.get(operation);
            if (params == null) {
                params = new HashMap<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>();
                byName.put(operation, params);
            }
            Map<String, AttributeOrParameterArbitraryDescriptorValidator> descriptors = params.get(parameter);
            if (descriptors == null) {
                descriptors = new HashMap<String, ModelDescriptionValidator.AttributeOrParameterArbitraryDescriptorValidator>();
                params.put(parameter, descriptors);
            }
            descriptors.put(descriptor, validator == null ? NullDescriptorValidator.INSTANCE : validator);
        }


        private AttributeOrParameterArbitraryDescriptorValidator getAttributeValidator(ModelNode address, String name, String descriptor) {
            Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>  byName = attributeDescriptors.get(address);
            if (byName == null) {
                return null;
            }
            Map<String, AttributeOrParameterArbitraryDescriptorValidator> descriptors = byName.get(name);
            if (descriptors == null) {
                return null;
            }
            return descriptors.get(descriptor);
        }

        private AttributeOrParameterArbitraryDescriptorValidator getOperationParameterValidator(ModelNode address, String operation, String parameter, String descriptor) {
            Map<String, Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>>>  byName = operationParameterDescriptors.get(address);
            if (byName == null) {
                return null;
            }
            Map<String, Map<String, AttributeOrParameterArbitraryDescriptorValidator>> params = byName.get(operation);
            if (params == null) {
                return null;
            }
            Map<String, AttributeOrParameterArbitraryDescriptorValidator> descriptors = params.get(parameter);
            if (descriptors != null) {
                AttributeOrParameterArbitraryDescriptorValidator validator = descriptors.get(descriptor);
                if (validator != null) {
                    return validator;
                }
            }
            descriptors = params.get("*");
            if (descriptors == null) {
                return null;
            }
            return descriptors.get(descriptor);
        }


        private boolean isAllowNullValueTypeForOperationParameter(ModelNode address, String operation, String param) {
            Map<String, Set<String>> operations = nullValueTypeOperations.get(address);
            if (operations == null) {
                return false;
            }
            Set<String> params = operations.get(operation);
            if (params == null) {
                return false;
            }
            return params.contains(param) || params.contains("*");
        }


        private boolean isAllowNullValueTypeForAttribute(ModelNode address, String name) {
            Set<String> names = nullValueTypeAttributes.get(address);
            if (names == null) {
                return false;
            }
            return names.contains(name);
        }
    }

    /**
     * Contains a validation error
     */
    public static class ValidationFailure {
        protected final String message;
        protected final ModelNode address;
        ValidationFailure(String message, ModelNode address){
            this.message = message;
            this.address = address;
        }

        /**
         * Formats the error string for printing
         *
         * @return the formatted error string
         */
        public String toString() {
            return message + " @" + address;
        }

        /**
         * Gets the address the validation error happened at
         *
         * @return the address
         */
        public final ModelNode getAddress() {
            return address;
        }

        /**
         * Gets the name of the operation failing validation
         *
         * @return the operation name or {@code null} if it was not an operation
         */
        public String getOperationName() {
            return null;
        }

        /**
         * Gets the name of the operation parameter failing validation
         *
         * @return the operation parameter name or {@code null} if it was not an operation parameter
         */
        public String getOperationParameterName() {
            return null;
        }


        /**
         * Gets the name of the attribute failing validation
         *
         * @return the attribute name or {@code null} if it was not an attribute
         */
        public String getAttributeName() {
            return null;
        }
    }

    private static class OperationValidationFailure extends ValidationFailure {
        protected final String operationName;
        OperationValidationFailure(String message, ModelNode address, String operationName) {
            super(message, address);
            this.operationName = operationName;
        }

        public String getOperationName() {
            return operationName;
        }

        public String toString() {
            return message + " for operation '" + operationName +
                    "' @" + address;
        }

    }

    private static class OperationParameterValidationFailure extends OperationValidationFailure {
        private final String parameterName;
        OperationParameterValidationFailure(String message, ModelNode address, String operationName, String parameterName) {
            super(message, address, operationName);
            this.parameterName = parameterName;
        }

        public String getOperationParameterName() {
            return parameterName;
        }

        public String toString() {
            return message + " for operation parameter '" + operationName + "." + parameterName +
                        "' @" + address;
        }
    }

    private static class AttributeValidationFailure extends ValidationFailure {
        private final String attributeName;
        private AttributeValidationFailure(String message, ModelNode address, String attributeName) {
            super(message, address);
            this.attributeName = attributeName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String toString() {
            return message + " for attribute '" + attributeName +
                        "'" + address;
        }

    }

    private abstract class ValidationElement {
        protected final String name;

        public ValidationElement(String name) {
            this.name = name;
        }

        abstract ValidationFailure createValidationFailure(String description);
    }

    private class OperationValidationElement extends ValidationElement {
        OperationValidationElement(String name) {
            super(name);
        }

        String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Operation '" + name + "' @" + address;
        }

        void validateKeys(ModelNode description) {
            if (!description.isDefined()) {
                errors.add(createValidationFailure("Missing description for operation"));
                return;
            }
            for (String opKey : description.keys()) {
                ArbitraryDescriptorValidator validator = VALID_OPERATION_KEYS.get(opKey);
                if (validator == null) {
                    errors.add(createValidationFailure("Invalid key '" + opKey + "'"));
                } else {
                    if (description.hasDefined(opKey)) {
                        String error = validator.validate(description, opKey);
                        if (error != null) {
                            errors.add(createValidationFailure(error));

                        }
                    } else {
                        errors.add(createValidationFailure("Empty key '" + opKey + "'"));
                    }
                }
            }
        }

        ValidationFailure createValidationFailure(String message) {
            return new OperationValidationFailure(message, address, name);
        }
    }

    private class AttributeValidationElement extends ValidationElement {
        final Map<String, AttributeOrParameterArbitraryDescriptorValidator> standardValidators;
        AttributeValidationElement(String name) {
            this(name, VALID_ATTRIBUTE_KEYS);
        }

        public AttributeValidationElement(String name, Map<String, AttributeOrParameterArbitraryDescriptorValidator> standardValidators) {
            super(name);
            this.standardValidators = standardValidators;
        }

        @Override
        public String toString() {
            return "Attribute '" + name + "' @" + address;
        }

        void validateKeys(ModelType currentType, ModelNode description) {
            for (String attrKey : description.keys()) {
                AttributeOrParameterArbitraryDescriptorValidator validator = standardValidators.get(attrKey);
                if (validator == null) {
                    validator = getExtraValidator(attrKey);
                    if (validator == null) {
                        errors.add(createValidationFailure("Invalid key '" + attrKey + "'"));
                    }
                } else {
                    if (description.hasDefined(attrKey)) {
                        String error = validator.validate(currentType, description, attrKey);
                        if (error != null) {
                            errors.add(createValidationFailure(error));
                        }
                    } else {
                        errors.add(createValidationFailure("Empty key '" + attrKey + "'"));
                    }
                }
            }
        }

        ValidationFailure createValidationFailure(String message) {
            return new AttributeValidationFailure(message, address, name);
        }

        AttributeOrParameterArbitraryDescriptorValidator getExtraValidator(String key) {
            return validationConfiguration.getAttributeValidator(address, name, key);
        }

        boolean allowNullValueTypeForObject() {
            return validationConfiguration.isAllowNullValueTypeForAttribute(address, name);
        }
    }

    private class OperationParameterValidationElement extends AttributeValidationElement {
        private final OperationValidationElement operation;

        OperationParameterValidationElement(OperationValidationElement operation, String name) {
            super(name, VALID_PARAMETER_KEYS);
            this.operation = operation;
        }

        @Override
        public String toString() {
            return "Parameter '" + name + "' in operation " + operation.getName() + "' @" + address;
        }

        ValidationFailure createValidationFailure(String message) {
            return new OperationParameterValidationFailure(message, address, operation.getName(), name);
        }

        AttributeOrParameterArbitraryDescriptorValidator getExtraValidator(String key) {
            return validationConfiguration.getOperationParameterValidator(address, operation.getName(), name, key);
        }


        boolean allowNullValueTypeForObject() {
            return validationConfiguration.isAllowNullValueTypeForOperationParameter(address, operation.getName(), name);
        }
    }
}
