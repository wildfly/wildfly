/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author wangc
 *
 */
public class ModularLongRangeParameterValidator extends LongRangeValidator {

    private final long moduleSize;

    public ModularLongRangeParameterValidator(final long moduleSize, final long min) {
        super(min);
        this.moduleSize = moduleSize;
    }

    public ModularLongRangeParameterValidator(final long moduleSize, final long min, final boolean nullable) {
        super(min, nullable);
        this.moduleSize = moduleSize;
    }

    public ModularLongRangeParameterValidator(final long moduleSize, final long min, final long max, final boolean nullable,
            final boolean allowExpressions) {
        super(min, max, nullable, allowExpressions);
        this.moduleSize = moduleSize;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            long val = value.asLong();
            if (val % moduleSize != 0)
                throw MessagingLogger.ROOT_LOGGER.invalidModularParameterValue(val, parameterName, moduleSize);
        }
    }
}
