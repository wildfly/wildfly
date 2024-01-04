/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Validates a given parameter is a legal value (-1 or > 0) where -1 means "forever".
 *
 * The validation is done in 2 steps:
 * 1. a ParameterCorrector will correct any negative value to -1
 * 2. a ParameterValidator will check that negative values and > 0 are valid.
 *
 * Note that the ParameterValidator will validate any negative value until WFCORE-3651 is fixed to preserve
 * backwards compatibility (as the parameter correction is not apply before the validator is called during
 * XML parsing).
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public interface InfiniteOrPositiveValidators {

    ModelTypeValidator LONG_INSTANCE = new ModelTypeValidator(ModelType.LONG) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            long val = value.asLong();
            if (!(val <= -1 || (val > 0 && val < Long.MAX_VALUE))) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.illegalValue(value, parameterName));
            }
        }
    };

    ModelTypeValidator INT_INSTANCE = new ModelTypeValidator(ModelType.INT) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            int val = value.asInt();
            if (!(val <= -1 || (val > 0 && val < Integer.MAX_VALUE))) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.illegalValue(value, parameterName));
            }
        }
    };

    /**
     * Correct any negative value to use -1 (interpreted by Artemis as infinite).
     */
    ParameterCorrector NEGATIVE_VALUE_CORRECTOR = new ParameterCorrector() {
        @Override
        public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
            if (newValue.isDefined() && newValue.getType() != ModelType.EXPRESSION) {
                try {
                    if (newValue.asLong() < -1) {
                        return new ModelNode(-1);
                    }
                } catch (Exception e) {
                    // not convertible; let the validator that the caller will invoke later deal with this
                }
            }
            return newValue;
        }
    };
}
