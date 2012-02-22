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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.logging.loggers.RootLoggerAdd;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.ASYNC_HANDLER;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.CONSOLE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LOGGER;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_NAME;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SIZE_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class LoggingDescribeHandler implements OperationStepHandler, DescriptionProvider {

    static final LoggingDescribeHandler INSTANCE = new LoggingDescribeHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode result = context.getResult();
        result.add(LoggingSubsystemAdd.createOperation(rootAddress.toModelNode()));
        if (model.hasDefined(ROOT_LOGGER)) {
            final ModelNode add = Util.getEmptyOperation(RootLoggerAdd.OPERATION_NAME, rootAddress.append(LoggingExtension.rootLoggerPath).toModelNode());
            final ModelNode rootLogger = model.get(ROOT_LOGGER, ROOT_LOGGER_NAME);
            copy(LEVEL, rootLogger, add);
            copy(FILTER, rootLogger, add);
            copy(HANDLERS, rootLogger, add);
            result.add(add);
        }
        if (model.hasDefined(LOGGER)) {
            for (Property prop : model.get(LOGGER).asPropertyList()) {
                ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(LOGGER, prop.getName())).toModelNode());
                copy(NAME, prop.getValue(), add);
                copy(CATEGORY, prop.getValue(), add);
                copy(USE_PARENT_HANDLERS, prop.getValue(), add);
                copy(HANDLERS, prop.getValue(), add);
                copy(LEVEL, prop.getValue(), add);
                result.add(add);
            }
        }
        if (model.hasDefined(ASYNC_HANDLER)) {
            for (Property prop : model.get(ASYNC_HANDLER).asPropertyList()) {
                result.add(defineAsynchHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        if (model.hasDefined(CONSOLE_HANDLER)) {
            for (Property prop : model.get(CONSOLE_HANDLER).asPropertyList()) {
                result.add(defineConsoleHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        if (model.hasDefined(FILE_HANDLER)) {
            for (Property prop : model.get(FILE_HANDLER).asPropertyList()) {
                result.add(defineFileHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        if (model.hasDefined(CUSTOM_HANDLER)) {
            for (Property prop : model.get(CUSTOM_HANDLER).asPropertyList()) {
                result.add(defineCustomHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        if (model.hasDefined(PERIODIC_ROTATING_FILE_HANDLER)) {
            for (Property prop : model.get(PERIODIC_ROTATING_FILE_HANDLER).asPropertyList()) {
                result.add(definePeriodicRotatingFileHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        if (model.hasDefined(SIZE_ROTATING_FILE_HANDLER)) {
            for (Property prop : model.get(SIZE_ROTATING_FILE_HANDLER).asPropertyList()) {
                result.add(defineSizeRotatingFileHandler(prop.getName(), prop.getValue(), rootAddress));
            }
        }
        context.completeStep();
    }

    private ModelNode defineAsynchHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(ASYNC_HANDLER, name)).toModelNode());

        copy(NAME, handler, add);
        copy(LEVEL, handler, add);
        copy(FILTER, handler, add);
        copy(QUEUE_LENGTH, handler, add);
        copy(SUBHANDLERS, handler, add);
        copy(OVERFLOW_ACTION, handler, add);

        return add;
    }


    private ModelNode defineConsoleHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(CONSOLE_HANDLER, name)).toModelNode());

        copyCommonFlushingHandlerAttributes(handler, add);
        copy(TARGET, handler, add);

        return add;
    }

    private ModelNode defineFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(FILE_HANDLER, name)).toModelNode());

        copyCommonFlushingHandlerAttributes(handler, add);
        copy(APPEND, handler, add);
        copy(FILE, handler, add);

        return add;
    }


    private ModelNode defineCustomHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(CUSTOM_HANDLER, name)).toModelNode());

        copyCommonHandlerAttributes(handler, add);
        copy(CLASS, handler, add);
        copy(MODULE, handler, add);
        copy(PROPERTIES, handler, add);

        return add;
    }

    private ModelNode definePeriodicRotatingFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(PERIODIC_ROTATING_FILE_HANDLER, name)).toModelNode());

        copyCommonFlushingHandlerAttributes(handler, add);
        copy(FILE, handler, add);
        copy(APPEND, handler, add);
        copy(SUFFIX, handler, add);

        return add;
    }


    private ModelNode defineSizeRotatingFileHandler(final String name, final ModelNode handler, final PathAddress rootAddress) throws OperationFailedException {
        ModelNode add = Util.getEmptyOperation(ADD, rootAddress.append(PathElement.pathElement(SIZE_ROTATING_FILE_HANDLER, name)).toModelNode());

        copyCommonFlushingHandlerAttributes(handler, add);
        copy(FILE, handler, add);
        copy(APPEND, handler, add);
        copy(MAX_BACKUP_INDEX, handler, add);
        copy(ROTATE_SIZE, handler, add);

        return add;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getSubsystemDescribeOperation(locale);
    }

    private void copyCommonHandlerAttributes(final ModelNode from, final ModelNode to) throws OperationFailedException {
        copy(NAME, from, to);
        copy(ENCODING, from, to);
        copy(FORMATTER, from, to);
        copy(LEVEL, from, to);
        copy(FILTER, from, to);
    }

    private void copyCommonFlushingHandlerAttributes(final ModelNode from, final ModelNode to) throws OperationFailedException {
        copyCommonHandlerAttributes(from, to);
        copy(AUTOFLUSH, from, to);
    }

    private void copy(final AttributeDefinition definition, final ModelNode from, final ModelNode to) throws OperationFailedException {
        copy(definition.getName(), from, to);
    }

    private void copy(final String name, final ModelNode from, final ModelNode to) {
        to.get(name).set(from.get(name));
    }
}
