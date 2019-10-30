/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms.bridge;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates a given parameter is a legal value (-1 or > 0) where -1 means "forever"
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public interface InfiniteOrPositiveValidators {

    ModelTypeValidator LONG_INSTANCE = new ModelTypeValidator(ModelType.LONG) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            long val = value.asLong();
            if (!(val == -1 || (val > 0 && val < Long.MAX_VALUE))) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.illegalValue(value, parameterName));
            }
        }
    };

    ModelTypeValidator INT_INSTANCE = new ModelTypeValidator(ModelType.INT) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            int val = value.asInt();
            if (!(val == -1 || (val > 0 && val < Integer.MAX_VALUE))) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.illegalValue(value, parameterName));
            }
        }
    };

}
