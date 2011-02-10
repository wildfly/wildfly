/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.HANDLER_TYPE;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LOGGER;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class LoggingDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider{

    static final LoggingDescribeHandler INSTANCE = new LoggingDescribeHandler();

    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        final ModelNode model = context.getSubModel();

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode result = new ModelNode();
        result.add(LoggingExtension.NewLoggingSubsystemAdd.createOperation(rootAddress.toModelNode()));
        if (model.hasDefined(ROOT_LOGGER)) {
            ModelNode add = Util.getEmptyOperation(RootLoggerAdd.OPERATION_NAME, rootAddress.toModelNode());
            add.get(LEVEL).set(model.get(ROOT_LOGGER, LEVEL));
            add.get(HANDLERS).set(model.get(ROOT_LOGGER, HANDLERS));
            result.add(add);
        }
        if (model.hasDefined(LOGGER)) {
            for (Property prop : model.get(LOGGER).asPropertyList()) {
                ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(LOGGER, prop.getName())).toModelNode());
                add.get(HANDLERS).set(prop.getValue().get(HANDLERS));
                add.get(LEVEL).set(prop.getValue().get(LEVEL));
                result.add(add);
            }
        }
        if (model.hasDefined(HANDLER)) {
            for (Property prop : model.get(HANDLER).asPropertyList()) {
                final String name = prop.getName();
                final ModelNode handler = prop.getValue();
                final LoggerHandlerType type = Enum.valueOf(LoggerHandlerType.class, handler.get(HANDLER_TYPE).asString());
                switch (type) {
                case ASYNC_HANDLER:
                    result.add(defineAsynchHandler(name, handler, rootAddress));
                    break;
                case CONSOLE_HANDLER:
                    result.add(defineConsoleHandler(name, handler, rootAddress));
                    break;
                case FILE_HANDLER:
                    result.add(defineFileHandler(name, handler, rootAddress));
                    break;
                case PERIODIC_ROTATING_FILE_HANDLER:
                    result.add(definePeriodicRotatingFileHandler(name, handler, rootAddress));
                    break;
                case SIZE_ROTATING_FILE_HANDLER:
                    result.add(defineSizeRotatingFileHandler(name, handler, rootAddress));
                    break;
                default:
                    break;
                }

            }
        }

        resultHandler.handleResultFragment(Util.NO_LOCATION, result);
        resultHandler.handleResultComplete(new ModelNode());
        return Cancellable.NULL;
    }

    private ModelNode defineAsynchHandler(final String name, final ModelNode handler, final PathAddress rootAddress) {
        ModelNode add = Util.getEmptyOperation(AsyncHandlerAdd.OPERATION_NAME, rootAddress.append(PathElement.pathElement(HANDLER, name)).toModelNode());

        add.get(HANDLER_TYPE).set(handler.get(HANDLER_TYPE));
        add.get(QUEUE_LENGTH).set(handler.get(QUEUE_LENGTH));
        add.get(SUBHANDLERS).set(handler.get(SUBHANDLERS));
        add.get(LEVEL).set(handler.get(LEVEL));
        add.get(OVERFLOW_ACTION).set(handler.get(OVERFLOW_ACTION));

        return add;
    }


    private ModelNode defineConsoleHandler(final String name, final ModelNode handler, final PathAddress rootAddress) {
        ModelNode add = Util.getEmptyOperation(ConsoleHandlerAdd.OPERATION_NAME, rootAddress.append(PathElement.pathElement(HANDLER, name)).toModelNode());

        if (handler.hasDefined(AUTOFLUSH)) {
            add.get(AUTOFLUSH).set(handler.get(AUTOFLUSH));
        }
        if (handler.hasDefined(ENCODING)) {
            add.get(ENCODING).set(handler.get(ENCODING));
        }
        if (handler.hasDefined(FORMATTER)) {
            add.get(FORMATTER).set(handler.get(FORMATTER));
        }
        if (handler.hasDefined(HANDLER_TYPE)) {
            add.get(HANDLER_TYPE).set(handler.get(HANDLER_TYPE));
        }
        if (handler.hasDefined(LEVEL)) {
            add.get(LEVEL).set(handler.get(LEVEL));
        }
        if (handler.hasDefined(QUEUE_LENGTH)) {
            add.get(QUEUE_LENGTH).set(handler.get(QUEUE_LENGTH));
        }

        return add;
    }

    private ModelNode defineFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) {
        ModelNode add = Util.getEmptyOperation(FileHandlerAdd.OPERATION_NAME, rootAddress.append(PathElement.pathElement(HANDLER, name)).toModelNode());

        if (handler.hasDefined(AUTOFLUSH)) {
            add.get(AUTOFLUSH).set(handler.get(AUTOFLUSH));
        }
        if (handler.hasDefined(ENCODING)) {
            add.get(ENCODING).set(handler.get(ENCODING));
        }
        if (handler.hasDefined(FORMATTER)) {
            add.get(FORMATTER).set(handler.get(FORMATTER));
        }
        if (handler.hasDefined(LEVEL)) {
            add.get(LEVEL).set(handler.get(LEVEL));
        }
        if (handler.hasDefined(FILE)) {
            add.get(FILE).set(handler.get(FILE));
        }
        if (handler.hasDefined(HANDLER_TYPE)) {
            add.get(HANDLER_TYPE).set(handler.get(HANDLER_TYPE));
        }
        if (handler.hasDefined(QUEUE_LENGTH)) {
            add.get(QUEUE_LENGTH).set(handler.get(QUEUE_LENGTH));
        }

        return add;
    }

    private ModelNode definePeriodicRotatingFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) {
        ModelNode add = Util.getEmptyOperation(PeriodicRotatingFileHandlerAdd.OPERATION_NAME, rootAddress.append(PathElement.pathElement(HANDLER, name)).toModelNode());

        if (handler.hasDefined(AUTOFLUSH)) {
            add.get(AUTOFLUSH).set(handler.get(AUTOFLUSH));
        }
        if (handler.hasDefined(ENCODING)) {
            add.get(ENCODING).set(handler.get(ENCODING));
        }
        if (handler.hasDefined(FORMATTER)) {
            add.get(FORMATTER).set(handler.get(FORMATTER));
        }
        if (handler.hasDefined(HANDLER_TYPE)) {
            add.get(HANDLER_TYPE).set(handler.get(HANDLER_TYPE));
        }
        if (handler.hasDefined(LEVEL)) {
            add.get(LEVEL).set(handler.get(LEVEL));
        }
        if (handler.hasDefined(FILE)) {
            add.get(FILE).set(handler.get(FILE));
        }
        if (handler.hasDefined(QUEUE_LENGTH)) {
            add.get(QUEUE_LENGTH).set(handler.get(QUEUE_LENGTH));
        }
        if (handler.hasDefined(SUFFIX)) {
            add.get(SUFFIX).set(handler.get(SUFFIX));
        }
        return add;
    }

    private ModelNode defineSizeRotatingFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) {
        ModelNode add = Util.getEmptyOperation(SizeRotatingFileHandlerAdd.OPERATION_NAME, rootAddress.append(PathElement.pathElement(HANDLER, name)).toModelNode());

        if (handler.hasDefined(AUTOFLUSH)) {
            add.get(AUTOFLUSH).set(handler.get(AUTOFLUSH));
        }
        if (handler.hasDefined(ENCODING)) {
            add.get(ENCODING).set(handler.get(ENCODING));
        }
        if (handler.hasDefined(FORMATTER)) {
            add.get(FORMATTER).set(handler.get(FORMATTER));
        }
        if (handler.hasDefined(HANDLER_TYPE)) {
            add.get(HANDLER_TYPE).set(handler.get(HANDLER_TYPE));
        }
        if (handler.hasDefined(LEVEL)) {
            add.get(LEVEL).set(handler.get(LEVEL));
        }
        if (handler.hasDefined(FILE)) {
            add.get(FILE).set(handler.get(FILE));
        }
        if (handler.hasDefined(MAX_BACKUP_INDEX)) {
            add.get(MAX_BACKUP_INDEX).set(handler.get(MAX_BACKUP_INDEX));
        }
        if (handler.hasDefined(ROTATE_SIZE)) {
            add.get(ROTATE_SIZE).set(handler.get(ROTATE_SIZE));
        }

        return add;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getSubsystemDescribeOperation(locale);
    }
}
