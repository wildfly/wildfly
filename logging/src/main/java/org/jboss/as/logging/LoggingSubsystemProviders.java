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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.CommonAttributes.VALUE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.logging.handlers.HandlerLevelChange;
import org.jboss.as.logging.handlers.async.AsyncHandlerAssignSubhandler;
import org.jboss.as.logging.handlers.async.AsyncHandlerUnassignSubhandler;
import org.jboss.as.logging.handlers.async.AsyncHandlerUpdateProperties;
import org.jboss.as.logging.handlers.console.ConsoleHandlerUpdateProperties;
import org.jboss.as.logging.handlers.custom.CustomHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.FileHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.HandlerFileChange;
import org.jboss.as.logging.handlers.file.PeriodicHandlerUpdateProperties;
import org.jboss.as.logging.handlers.file.SizeRotatingHandlerUpdateProperties;
import org.jboss.as.logging.loggers.LoggerAssignHandler;
import org.jboss.as.logging.loggers.LoggerLevelChange;
import org.jboss.as.logging.loggers.LoggerUnassignHandler;
import org.jboss.as.logging.loggers.RootLoggerAdd;
import org.jboss.as.logging.loggers.RootLoggerAssignHandler;
import org.jboss.as.logging.loggers.RootLoggerLevelChange;
import org.jboss.as.logging.loggers.RootLoggerRemove;
import org.jboss.as.logging.loggers.RootLoggerUnassignHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
class LoggingSubsystemProviders {

    static final String RESOURCE_NAME = LoggingSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("logging"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(OPERATIONS);

            subsystem.get(CHILDREN, CommonAttributes.ROOT_LOGGER, DESCRIPTION).set(bundle.getString("root.logger"));
            subsystem.get(CHILDREN, CommonAttributes.ROOT_LOGGER, MODEL_DESCRIPTION).setEmptyObject();
            subsystem.get(CHILDREN, CommonAttributes.LOGGER, DESCRIPTION).set(bundle.getString("logger"));
            subsystem.get(CHILDREN, CommonAttributes.ASYNC_HANDLER, DESCRIPTION).set(bundle.getString("async.handler"));
            subsystem.get(CHILDREN, CommonAttributes.CONSOLE_HANDLER, DESCRIPTION).set(bundle.getString("console.handler"));
            subsystem.get(CHILDREN, CommonAttributes.FILE_HANDLER, DESCRIPTION).set(bundle.getString("file.handler"));
            subsystem.get(CHILDREN, CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER, DESCRIPTION).set(bundle.getString("periodic.handler"));
            subsystem.get(CHILDREN, CommonAttributes.SIZE_ROTATING_FILE_HANDLER, DESCRIPTION).set(bundle.getString("size.periodic.handler"));
            subsystem.get(CHILDREN, CommonAttributes.CUSTOM_HANDLER, DESCRIPTION).set(bundle.getString("custom.handler"));

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("logging.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("logging.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("root.logger"));

            addCommonLoggerAttributes(node, bundle);

            return node;
        }
    };

    static final DescriptionProvider ADD_ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(DESCRIPTION).set(bundle.getString("root.logger.set"));

            addCommonLoggerRequestProperties(node, bundle);

            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };

    static final DescriptionProvider LEGACY_ADD_ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(RootLoggerAdd.LEGACY_OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("root.logger.set"));

            addCommonLoggerRequestProperties(node, bundle);

            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };

    static final DescriptionProvider REMOVE_ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(ModelDescriptionConstants.REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("root.logger.remove"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };

    static final DescriptionProvider LEGACY_REMOVE_ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(RootLoggerRemove.LEGACY_OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("root.logger.remove"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };


    static final DescriptionProvider ROOT_LOGGER_CHANGE_LEVEL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(RootLoggerLevelChange.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("root.logger.change-level"));
            LEVEL.addOperationParameterDescription(bundle, "logger", operation);

            return operation;
        }
    };

    static final DescriptionProvider ROOT_LOGGER_ASSIGN_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();

            node.get(OPERATION_NAME).set(RootLoggerAssignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("root.logger.assign-handler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };

    static final DescriptionProvider ROOT_LOGGER_UNASSIGN_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();

            node.get(OPERATION_NAME).set(RootLoggerUnassignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("root.logger.unassign-handler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };

    static final DescriptionProvider LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("logger"));

            addCommonLoggerAttributes(node, bundle);
            USE_PARENT_HANDLERS.addResourceAttributeDescription(bundle, "logger", node);
            CATEGORY.addResourceAttributeDescription(bundle, "logger", node);

            return node;
        }
    };

    static final DescriptionProvider LOGGER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("logger.add"));

            addCommonLoggerRequestProperties(node, bundle);
            USE_PARENT_HANDLERS.addOperationParameterDescription(bundle, "logger", node);
            CATEGORY.addOperationParameterDescription(bundle, "logger", node);

            return node;
        }
    };

    static final DescriptionProvider LOGGER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(REMOVE);
            node.get(DESCRIPTION).set(bundle.getString("logger.remove"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };

    static final DescriptionProvider LOGGER_CHANGE_LEVEL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(LoggerLevelChange.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("logger.change-level"));
            LEVEL.addOperationParameterDescription(bundle, "logger", node);

            return node;
        }
    };

    static final DescriptionProvider LOGGER_ASSIGN_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(LoggerAssignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("logger.assign-handler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };

    static final DescriptionProvider LOGGER_UNASSIGN_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(LoggerUnassignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("logger.unassign-handler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };


    static final DescriptionProvider HANDLER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(REMOVE);
            node.get(DESCRIPTION).set(bundle.getString("handler.remove"));
            return node;
        }
    };


    private static void addCommonLoggerAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        LEVEL.addResourceAttributeDescription(bundle, "handler", modelNode);
        FILTER.addResourceAttributeDescription(bundle, "handler", modelNode);
        HANDLERS.addResourceAttributeDescription(bundle, "logger", modelNode);
    }


    private static void addCommonLoggerRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        LEVEL.addOperationParameterDescription(bundle, "handler", modelNode);
        FILTER.addOperationParameterDescription(bundle, "handler", modelNode);
        HANDLERS.addOperationParameterDescription(bundle, "logger", modelNode);
    }

    private static void addCommonHandlerAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        NAME.addResourceAttributeDescription(bundle, "handler", modelNode);
        ENCODING.addResourceAttributeDescription(bundle, "handler", modelNode);
        LEVEL.addResourceAttributeDescription(bundle, "handler", modelNode);
        FILTER.addResourceAttributeDescription(bundle, "handler", modelNode);
        FORMATTER.addResourceAttributeDescription(bundle, "handler", modelNode);
    }

    private static void addCommonHandlerRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        NAME.addOperationParameterDescription(bundle, "handler", modelNode);
        ENCODING.addOperationParameterDescription(bundle, "handler", modelNode);
        LEVEL.addOperationParameterDescription(bundle, "handler", modelNode);
        FILTER.addOperationParameterDescription(bundle, "handler", modelNode);
        FORMATTER.addOperationParameterDescription(bundle, "handler", modelNode);
    }

    private static void addCommonHandlerOutputStreamAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        addCommonHandlerAttributes(modelNode, bundle);
        AUTOFLUSH.addResourceAttributeDescription(bundle, "handler", modelNode);
    }

    private static void addCommonHandlerOutputStreamRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addCommonHandlerRequestProperties(modelNode, bundle);
        AUTOFLUSH.addOperationParameterDescription(bundle, "handler", modelNode);
    }

    static final DescriptionProvider ASYNC_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("async.handler"));

            NAME.addResourceAttributeDescription(bundle, "handler", node);
            LEVEL.addResourceAttributeDescription(bundle, "handler", node);
            FILTER.addResourceAttributeDescription(bundle, "handler", node);
            FORMATTER.addResourceAttributeDescription(bundle, "handler", node);
            QUEUE_LENGTH.addResourceAttributeDescription(bundle, "async", node);
            OVERFLOW_ACTION.addResourceAttributeDescription(bundle, "async", node);
            SUBHANDLERS.addResourceAttributeDescription(bundle, "async.handler", node);

            return node;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("async.handler.add"));

            NAME.addOperationParameterDescription(bundle, "handler", operation);
            LEVEL.addOperationParameterDescription(bundle, "handler", operation);
            FILTER.addOperationParameterDescription(bundle, "handler", operation);
            FORMATTER.addOperationParameterDescription(bundle, "handler", operation);
            QUEUE_LENGTH.addOperationParameterDescription(bundle, "async", operation);
            OVERFLOW_ACTION.addOperationParameterDescription(bundle, "async", operation);
            SUBHANDLERS.addOperationParameterDescription(bundle, "async.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(AsyncHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("async.handler.update"));

            NAME.addOperationParameterDescription(bundle, "handler", operation);
            LEVEL.addOperationParameterDescription(bundle, "handler", operation);
            FILTER.addOperationParameterDescription(bundle, "handler", operation);
            FORMATTER.addOperationParameterDescription(bundle, "handler", operation);
            QUEUE_LENGTH.addOperationParameterDescription(bundle, "async", operation);
            OVERFLOW_ACTION.addOperationParameterDescription(bundle, "async", operation);
            SUBHANDLERS.addOperationParameterDescription(bundle, "async.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_ASSIGN_SUBHANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(AsyncHandlerAssignSubhandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("async.handler.assign-subhandler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_UNASSIGN_SUBHANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(AsyncHandlerUnassignSubhandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("async.handler.unassign-subhandler"));
            NAME.addOperationParameterDescription(bundle, "handler", node);

            return node;
        }
    };

    static final DescriptionProvider CONSOLE_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("console.handler"));

            addCommonHandlerOutputStreamAttributes(node, bundle);
            TARGET.addResourceAttributeDescription(bundle, "console.handler", node);

            return node;
        }
    };

    static final DescriptionProvider CONSOLE_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("console.handler"));

            addCommonHandlerOutputStreamRequestProperties(operation, bundle);
            TARGET.addOperationParameterDescription(bundle, "console.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider CONSOLE_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ConsoleHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("console.handler.update"));

            addCommonHandlerOutputStreamRequestProperties(operation, bundle);
            TARGET.addOperationParameterDescription(bundle, "console.handler", operation);

            return operation;
        }
    };

    private static void addCommonFileHandlerAttributes(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerOutputStreamAttributes(model, bundle);
        APPEND.addResourceAttributeDescription(bundle, "file.handler", model);
        final ModelNode file = FILE.addResourceAttributeDescription(bundle, "file.handler", model);
        addAttributeValueType(file, PATH, bundle.getString("file.handler.path"));
        addAttributeValueType(file, RELATIVE_TO, bundle.getString("file.handler.relative-to"));
    }

    private static void addCommonFileHandlerRequestProperties(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerOutputStreamRequestProperties(model, bundle);
        APPEND.addOperationParameterDescription(bundle, "file.handler", model);
        final ModelNode file = FILE.addOperationParameterDescription(bundle, "file.handler", model);
        addRequestPropertiesValueType(file, PATH, bundle.getString("file.handler.path"));
        addRequestPropertiesValueType(file, RELATIVE_TO, bundle.getString("file.handler.relative-to"));
    }

    static final DescriptionProvider FILE_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("file.handler"));

            addCommonFileHandlerAttributes(node, bundle);

            return node;
        }
    };

    static final DescriptionProvider FILE_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("file.handler"));

            addCommonFileHandlerRequestProperties(operation, bundle);

            return operation;
        }
    };

    static final DescriptionProvider FILE_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(FileHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("file.handler.update"));

            addCommonFileHandlerRequestProperties(operation, bundle);

            return operation;
        }
    };

    static final DescriptionProvider PERIODIC_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("periodic.handler"));

            addCommonFileHandlerAttributes(node, bundle);
            SUFFIX.addResourceAttributeDescription(bundle, "periodic.handler", node);

            return node;
        }
    };

    static final DescriptionProvider PERIODIC_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("periodic.handler"));

            addCommonFileHandlerRequestProperties(operation, bundle);
            SUFFIX.addOperationParameterDescription(bundle, "periodic.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider PERIODIC_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(PeriodicHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("periodic.handler.update"));

            addCommonFileHandlerRequestProperties(operation, bundle);
            SUFFIX.addOperationParameterDescription(bundle, "periodic.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider SIZE_PERIODIC_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("size.periodic.handler"));

            addCommonFileHandlerAttributes(node, bundle);
            ROTATE_SIZE.addResourceAttributeDescription(bundle, "size.periodic.handler", node);
            MAX_BACKUP_INDEX.addResourceAttributeDescription(bundle, "size.periodic.handler", node);

            return node;
        }
    };

    static final DescriptionProvider SIZE_PERIODIC_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("size.periodic.handler.add"));

            addCommonFileHandlerRequestProperties(operation, bundle);
            ROTATE_SIZE.addOperationParameterDescription(bundle, "size.periodic.handler", operation);
            MAX_BACKUP_INDEX.addOperationParameterDescription(bundle, "size.periodic.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider SIZE_PERIODIC_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(SizeRotatingHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("size.periodic.handler.update"));

            addCommonFileHandlerRequestProperties(operation, bundle);
            ROTATE_SIZE.addOperationParameterDescription(bundle, "size.periodic.handler", operation);
            MAX_BACKUP_INDEX.addOperationParameterDescription(bundle, "size.periodic.handler", operation);

            return operation;
        }
    };

    static final DescriptionProvider CUSTOM_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("custom.handler"));

            addCommonHandlerAttributes(node, bundle);
            CLASS.addResourceAttributeDescription(bundle, "custom.handler", node);
            MODULE.addResourceAttributeDescription(bundle, "custom.handler", node);
            addHandlerPropertiesAttributes(node, bundle);

            return node;
        }
    };

    static final DescriptionProvider CUSTOM_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("custom.handler"));

            addCommonHandlerRequestProperties(node, bundle);
            CLASS.addOperationParameterDescription(bundle, "custom.handler", node);
            MODULE.addOperationParameterDescription(bundle, "custom.handler", node);
            addHandlerPropertiesRequestProperties(node, bundle);

            return node;
        }
    };

    static final DescriptionProvider CUSTOM_HANDLER_UPDATE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(CustomHandlerUpdateProperties.OPERATION_NAME);
            operation.get(DESCRIPTION).set(bundle.getString("custom.handler.update"));

            addCommonHandlerRequestProperties(operation, bundle);
            addHandlerPropertiesRequestProperties(operation, bundle);

            return operation;
        }
    };

    static final DescriptionProvider HANDLER_ENABLE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ENABLE);
            node.get(DESCRIPTION).set(bundle.getString("handler.enable"));
            return node;
        }
    };

    static final DescriptionProvider HANDLER_DISABLE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(DISABLE);
            node.get(DESCRIPTION).set(bundle.getString("handler.disable"));
            return node;
        }
    };

    static final DescriptionProvider HANDLER_CHANGE_LEVEL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(HandlerLevelChange.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("handler.change-level"));
            LEVEL.addOperationParameterDescription(bundle, "logger", node);

            return node;
        }
    };

    static final DescriptionProvider HANDLER_CHANGE_FILE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(HandlerFileChange.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("handler.change-file"));
            final ModelNode file = FILE.addOperationParameterDescription(bundle, "file.handler", node);
            addRequestPropertiesValueType(file, PATH, bundle.getString("file.handler.path"));
            addRequestPropertiesValueType(file, RELATIVE_TO, bundle.getString("file.handler.relative-to"));

            return node;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static ModelNode addAttributeValueType(final ModelNode node, final AttributeDefinition definition, final String description) {
        final ModelNode valueType = node.get(VALUE_TYPE, definition.getName());
        valueType.get(DESCRIPTION).set(description);
        valueType.get(TYPE).set(definition.getType());
        return valueType;
    }

    private static ModelNode addRequestPropertiesValueType(final ModelNode node, final AttributeDefinition definition, final String description) {
        final ModelNode valueType = node.get(VALUE_TYPE, definition.getName());
        valueType.get(DESCRIPTION).set(description);
        valueType.get(TYPE).set(definition.getType());
        valueType.get(REQUIRED).set(!definition.isAllowNull());
        return valueType;
    }

    private static ModelNode addHandlerPropertiesAttributes(final ModelNode node, final ResourceBundle bundle) {
        final ModelNode property = node.get(ATTRIBUTES, PROPERTIES);
        property.get(TYPE).set(ModelType.LIST);
        property.get(DESCRIPTION).set(bundle.getString("handler.properties"));
        addAttributeValueType(property, NAME, bundle.getString("handler.properties.name"));
        addAttributeValueType(property, VALUE, bundle.getString("handler.properties.value"));
        return property;
    }

    private static ModelNode addHandlerPropertiesRequestProperties(final ModelNode node, final ResourceBundle bundle) {
        final ModelNode property = node.get(REQUEST_PROPERTIES, PROPERTIES);
        property.get(TYPE).set(ModelType.LIST);
        property.get(DESCRIPTION).set(bundle.getString("handler.properties"));
        addRequestPropertiesValueType(property, NAME, bundle.getString("handler.properties.name"));
        addRequestPropertiesValueType(property, VALUE, bundle.getString("handler.properties.value"));
        return property;
    }
}
