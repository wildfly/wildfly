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

package org.jboss.as.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.logging.CommonAttributes.HANDLER_TYPE;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class LoggerHandlerAdd implements ModelAddOperationHandler {

    static final LoggerHandlerAdd INSTANCE = new LoggerHandlerAdd();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final String handlerType = operation.require(HANDLER_TYPE).asString();
        final LoggerHandlerType type = LoggerHandlerType.valueOf(handlerType);
        switch(type) {
            case ASYNC_HANDLER: {
                return AsyncHandlerAdd.INSTANCE.execute(context, operation, resultHandler);
            } case CONSOLE_HANDLER: {
                return ConsoleHandlerAdd.INSTANCE.execute(context, operation, resultHandler);
            } case FILE_HANDLER: {
                return FileHandlerAdd.INSTANCE.execute(context, operation, resultHandler);
            }case PERIODIC_ROTATING_FILE_HANDLER: {
                return PeriodicRotatingFileHandlerAdd.INSTANCE.execute(context, operation, resultHandler);
            }case SIZE_ROTATING_FILE_HANDLER: {
                return SizeRotatingFileHandlerAdd.INSTANCE.execute(context, operation, resultHandler);
            } default: {
                throw new OperationFailedException(new ModelNode().set("unknown log handler type"));
            }
        }
    }

}
