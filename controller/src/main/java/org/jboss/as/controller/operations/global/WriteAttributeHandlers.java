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

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class WriteAttributeHandlers {
    public abstract static class AbstractWriteAttributeOperationHandler implements ModelUpdateOperationHandler {

        @Override
        public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            Cancellable cancellable = Cancellable.NULL;
            try {
                final String name = operation.require(NAME).asString();
                // Don't require VALUE. Let validateValue decide if it's bothered
                // by and undefined value
                final ModelNode value = operation.get(VALUE);

                String error = validateValue(name, value);
                if (error != null) {
                    resultHandler.handleFailed(new ModelNode().set(error));
                } else {
                    context.getSubModel().require(name).set(value);
                    // FIXME there should be a compensating operation generated
                    resultHandler.handleResultComplete(null);
                }

            } catch (final Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return cancellable;
        }

        protected abstract String validateValue(String name, ModelNode value);
    }

    public static class ValidatingWriteAttributeOperationHandler extends AbstractWriteAttributeOperationHandler implements ModelUpdateOperationHandler {
        protected final ModelType type;
        protected final boolean nullable;
        protected final boolean allowExpressions;

        public ValidatingWriteAttributeOperationHandler(ModelType type) {
            this(type, false, true);
        }

        public ValidatingWriteAttributeOperationHandler(ModelType type, boolean nullable) {
            this(type, nullable, true);
        }

        public ValidatingWriteAttributeOperationHandler(ModelType type, boolean nullable, boolean allowExpressions) {
            this.type = type;
            this.nullable = nullable;
            this.allowExpressions = allowExpressions;
        }

        @Override
        protected String validateValue(final String name, final ModelNode value) {
            if (!value.isDefined()) {
                if (!nullable) {
                    return "Attribute " + name + " may not be null "; //TODO i18n
                }
            } else {
                if (value.getType() != type && (!allowExpressions || value.getType() != ModelType.EXPRESSION)) {
                    return "Wrong type for " + name + ". Expected " + type + " but was " + value.getType(); //TODO i18n
                }
            }
            return null;
        }
    }

    public static class StringLengthValidatingHandler extends ValidatingWriteAttributeOperationHandler {
        protected final int min;
        protected final int max;

        public StringLengthValidatingHandler(final int min) {
            this(min, Integer.MAX_VALUE, false, true);
        }

        public StringLengthValidatingHandler(final int min, final boolean nullable) {
            this(min, Integer.MAX_VALUE, nullable, true);
        }

        public StringLengthValidatingHandler(final int min, final int max, final boolean nullable, final boolean allowExpressions) {
            super(ModelType.STRING, nullable, allowExpressions);
            this.min = min;
            this.max = max;
        }

        @Override
        protected String validateValue(String name, ModelNode value) {
            String result = super.validateValue(name, value);
            if (result == null && value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                String str = value.asString();
                if (str.length() < min) {
                    result = "\"" + str + "\" is an invalid value for attribute " + name + ". Values must have a minimum length of " + min + " characters";
                }
                else if (str.length() > max) {
                    result = "\"" + str + "\" is an invalid value for attribute " + name + ". Values must have a maximum length of " + max + " characters";
                }
            }
            return result;
        }
    }

    public static class IntRangeValidatingHandler extends ValidatingWriteAttributeOperationHandler {
        protected final int min;
        protected final int max;

        public IntRangeValidatingHandler(final int min) {
            this(min, Integer.MAX_VALUE, false, true);
        }

        public IntRangeValidatingHandler(final int min, final boolean nullable) {
            this(min, Integer.MAX_VALUE, nullable, true);
        }

        public IntRangeValidatingHandler(final int min, final int max, final boolean nullable, final boolean allowExpressions) {
            super(ModelType.INT, nullable, allowExpressions);
            this.min = min;
            this.max = max;
        }

        @Override
        protected String validateValue(String name, ModelNode value) {
            String result = super.validateValue(name, value);
            if (result == null && value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                int val = value.asInt();
                if (val < min) {
                    result = val + " is an invalid value for attribute " + name + ". A minimum value of " + min + " is required";
                }
                else if (val > max) {
                    result = val + " is an invalid value for attribute " + name + ". A maximum length of " + max + " is required";
                }
            }
            return result;
        }


    }

}
