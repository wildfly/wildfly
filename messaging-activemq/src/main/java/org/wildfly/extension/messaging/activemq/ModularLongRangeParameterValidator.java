/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
