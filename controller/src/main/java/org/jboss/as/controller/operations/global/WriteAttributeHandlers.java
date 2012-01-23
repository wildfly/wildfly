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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.InetAddressValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class WriteAttributeHandlers {

    public static class WriteAttributeOperationHandler implements OperationStepHandler {
        public static WriteAttributeOperationHandler INSTANCE = new WriteAttributeOperationHandler();

        final ParametersValidator nameValidator = new ParametersValidator();
        final ParameterValidator valueValidator;

        /**
         * Creates a WriteAttributeOperationHandler that doesn't validate values.
         */
        protected WriteAttributeOperationHandler() {
            this(null);
        }

        /**
         * Creates a WriteAttributeOperationHandler that users the given {@code valueValidator}
         * to validate values before applying them to the model.
         */
        protected WriteAttributeOperationHandler(ParameterValidator valueValidator) {
            this.nameValidator.registerValidator(NAME, new StringLengthValidator(1));
            this.valueValidator = valueValidator;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            nameValidator.validate(operation);
            final String name = operation.require(NAME).asString();
            // Don't require VALUE. Let validateValue decide if it's bothered
            // by an undefined value
            final ModelNode value = operation.get(VALUE);

            validateValue(name, value);

            final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            final ModelNode currentValue = submodel.get(name).clone();

            submodel.get(name).set(value);

            modelChanged(context, operation, name, value, currentValue);
        }

        /**
         * If a validator was passed to the constructor, uses it to validate the value.
         * Subclasses can alter this behavior.
         *
         * @param name the name of the attribute
         * @param value the value to validate
         */
        protected void validateValue(String name, ModelNode value) throws OperationFailedException {
            if (valueValidator != null) {
                valueValidator.validateParameter(name, value);
            }
        }

        /**
         * Notification that the model has been changed. Subclasses can override
         * to apply additional processing. Any subclass that overrides MUST ensure
         * that either {@link org.jboss.as.controller.OperationContext#completeStep()} is invoked
         * or {@link OperationFailedException} is thrown.
         *
         * @param context the context of the operation
         * @param operation the operation
         * @param attributeName the name of the attribute being modified
         * @param newValue the new value for the attribute
         * @param currentValue the existing value for the attribute
         *
         * @throws OperationFailedException
         */
        protected void modelChanged(final OperationContext context, final ModelNode operation, final String attributeName,
                                    final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

            context.completeStep();
        }
    }

    /**
     * WriteAttributeOperationHandler that uses a ModelTypeValidator to validate the operation's
     * value attribute. The parameters in the constructors are passed to the equivalent
     * constructor in {@link ModelTypeValidator}.
     */
    public static class ModelTypeValidatingHandler extends WriteAttributeOperationHandler {

        public ModelTypeValidatingHandler(final ModelType type) {
            this(false, false, false, type);
        }

        public ModelTypeValidatingHandler(final ModelType type, final boolean nullable) {
            this(nullable, false, false, type);
        }

        public ModelTypeValidatingHandler(final ModelType type, final boolean nullable, final boolean allowExpressions) {
            this(nullable, allowExpressions, false, type);
        }

        public ModelTypeValidatingHandler(final ModelType type, final boolean nullable, final boolean allowExpressions, final boolean strict) {
            this(nullable, allowExpressions, strict, type);
        }

        public ModelTypeValidatingHandler(final boolean nullable, final boolean allowExpressions, final boolean strict, ModelType firstValidType, ModelType... otherValidTypes) {
            super(new ModelTypeValidator(nullable, allowExpressions, strict, firstValidType, otherValidTypes));
        }
    }

    public static class StringLengthValidatingHandler extends WriteAttributeOperationHandler {

        public StringLengthValidatingHandler(final int min) {
            this(min, Integer.MAX_VALUE, false, true);
        }

        public StringLengthValidatingHandler(final int min, final boolean nullable) {
            this(min, Integer.MAX_VALUE, nullable, true);
        }

        public StringLengthValidatingHandler(final int min, final boolean nullable, final boolean allowExpressions) {
            this(min, Integer.MAX_VALUE, nullable, allowExpressions);
        }

        public StringLengthValidatingHandler(final int min, final int max, final boolean nullable, final boolean allowExpressions) {
            super(new StringLengthValidator(min, max, nullable, allowExpressions));
        }
    }

    public static class IntRangeValidatingHandler extends WriteAttributeOperationHandler {

        public IntRangeValidatingHandler(final int min) {
            this(min, Integer.MAX_VALUE, false, true);
        }

        public IntRangeValidatingHandler(final int min, final boolean nullable) {
            this(min, Integer.MAX_VALUE, nullable, true);
        }

        public IntRangeValidatingHandler(final int min, final int max, final boolean nullable, final boolean allowExpressions) {
            super(new IntRangeValidator(min, max, nullable, allowExpressions));
        }
    }

    public static class InetAddressValidatingHandler extends WriteAttributeOperationHandler {
        public InetAddressValidatingHandler(final boolean nullable, final boolean allowExpressions) {
            super(new InetAddressValidator(nullable, allowExpressions));
        }
    }

    public static class ListValidatatingHandler extends WriteAttributeOperationHandler {

        public ListValidatatingHandler(ParameterValidator elementValidator) {
            this(elementValidator, false, 1, Integer.MAX_VALUE);
        }

        public ListValidatatingHandler(ParameterValidator elementValidator, boolean nullable) {
            this(elementValidator, nullable, 1, Integer.MAX_VALUE);
        }

        public ListValidatatingHandler(ParameterValidator elementValidator, boolean nullable, int minSize, int maxSize) {
            super(new ListValidator(elementValidator, nullable, minSize, maxSize));
        }
    }

    public static class AttributeDefinitionValidatingHandler extends WriteAttributeOperationHandler {

        public AttributeDefinitionValidatingHandler(AttributeDefinition definition) {
            super(definition.getValidator());
        }
    }


}
