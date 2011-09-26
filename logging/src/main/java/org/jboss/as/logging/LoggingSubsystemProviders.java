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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
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

        static final DescriptionProvider SET_ROOT_LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set("set-root-logger");
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

            operation.get(OPERATION_NAME).set("remove-root-logger");
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

            addRequestProperties(operation, LEVEL, ModelType.STRING, bundle.getString("logger.level"), true);

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
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

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
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

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

            addAttribute(node, USE_PARENT_HANDLERS, ModelType.BOOLEAN, bundle.getString("logger.use.parent.handlers"));
            addAttribute(node, CATEGORY, ModelType.STRING, bundle.getString("logger.category"));

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
            addRequestProperties(node, USE_PARENT_HANDLERS, ModelType.BOOLEAN, bundle.getString("logger.use.parent.handlers"), false);
            addRequestProperties(node, CATEGORY, ModelType.STRING, bundle.getString("logger.category"), true);

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

            addRequestProperties(node, LEVEL, ModelType.STRING, bundle.getString("logger.level"), true);

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
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

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
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

            return node;
        }
    };


    static final DescriptionProvider HANDLER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("handler.remove"));
            return node;
        }
    };


    private static void addCommonLoggerAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        addAttribute(modelNode, LEVEL, ModelType.STRING, bundle.getString("handler.level"));
        addAttribute(modelNode, FILTER, ModelType.STRING, bundle.getString("handler.filter"));
        addAttribute(modelNode, HANDLERS, ModelType.LIST, bundle.getString("logger.handlers"));
    }


    private static void addCommonLoggerRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addRequestProperties(modelNode, LEVEL, ModelType.STRING, bundle.getString("handler.level"), true);
        addRequestProperties(modelNode, FILTER, ModelType.STRING, bundle.getString("handler.filter"), false);
        addRequestProperties(modelNode, HANDLERS, ModelType.LIST, ModelType.STRING, bundle.getString("logger.handlers"), false);
    }

    private static void addCommonHandlerAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        addAttribute(modelNode, NAME, ModelType.STRING, bundle.getString("handler.name"));
        addAttribute(modelNode, ENCODING, ModelType.STRING, bundle.getString("handler.encoding"));
        addAttribute(modelNode, LEVEL, ModelType.STRING, bundle.getString("handler.level"));
        addAttribute(modelNode, FILTER, ModelType.STRING, bundle.getString("handler.filter"));
        addAttribute(modelNode, FORMATTER, ModelType.STRING, bundle.getString("handler.formatter"));
    }

    private static void addCommonHandlerRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addRequestProperties(modelNode, NAME, ModelType.STRING, bundle.getString("handler.name"), true);
        addRequestProperties(modelNode, ENCODING, ModelType.STRING, bundle.getString("handler.encoding"), true);
        addRequestProperties(modelNode, LEVEL, ModelType.STRING, bundle.getString("handler.level"), true);
        addRequestProperties(modelNode, FILTER, ModelType.STRING, bundle.getString("handler.filter"), false);
        addRequestProperties(modelNode, FORMATTER, ModelType.STRING, bundle.getString("handler.formatter"), true);
    }

    private static void addCommonHandlerUpdateRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addRequestProperties(modelNode, NAME, ModelType.STRING, bundle.getString("handler.name"), true);
        addRequestProperties(modelNode, ENCODING, ModelType.STRING, bundle.getString("handler.encoding"), true);
        addRequestProperties(modelNode, LEVEL, ModelType.STRING, bundle.getString("handler.level"), true);
        addRequestProperties(modelNode, FILTER, ModelType.STRING, bundle.getString("handler.filter"), false);
        addRequestProperties(modelNode, FORMATTER, ModelType.STRING, bundle.getString("handler.formatter"), true);
    }

    private static void addCommonHandlerOutputStreamAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        addCommonHandlerAttributes(modelNode, bundle);
        addAttribute(modelNode, AUTOFLUSH, ModelType.BOOLEAN, bundle.getString("handler.autoflush"));
    }

    private static void addCommonHandlerOutputStreamRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addCommonHandlerRequestProperties(modelNode, bundle);
        addRequestProperties(modelNode, AUTOFLUSH, ModelType.BOOLEAN, bundle.getString("handler.autoflush"), false);
    }

    private static void addCommonHandlerOutputStreamUpdateRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        addCommonHandlerUpdateRequestProperties(modelNode, bundle);
        addRequestProperties(modelNode, AUTOFLUSH, ModelType.BOOLEAN, bundle.getString("handler.autoflush"), false);
    }

    static final DescriptionProvider ASYNC_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("async.handler"));

            addAttribute(node, NAME, ModelType.STRING, bundle.getString("handler.name"));
            addAttribute(node, LEVEL, ModelType.STRING, bundle.getString("handler.level"));
            addAttribute(node, FILTER, ModelType.STRING, bundle.getString("handler.filter"));
            addAttribute(node, QUEUE_LENGTH, ModelType.INT, bundle.getString("async.queue-length"));
            addAttribute(node, OVERFLOW_ACTION, ModelType.STRING, bundle.getString("async.overflow-action"));
            addAttribute(node, SUBHANDLERS, ModelType.LIST, ModelType.STRING, bundle.getString("logger.handlers"));

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

            addRequestProperties(operation, NAME, ModelType.STRING, bundle.getString("handler.name"), true);
            addRequestProperties(operation, LEVEL, ModelType.STRING, bundle.getString("handler.level"), true);
            addRequestProperties(operation, FILTER, ModelType.STRING, bundle.getString("handler.filter"), false);
            addRequestProperties(operation, QUEUE_LENGTH, ModelType.INT, bundle.getString("async.queue-length"), false);
            addRequestProperties(operation, OVERFLOW_ACTION, ModelType.STRING, bundle.getString("async.overflow-action"), false);
            addRequestProperties(operation, SUBHANDLERS, ModelType.LIST, ModelType.STRING, bundle.getString("logger.handlers"), false);

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

            addRequestProperties(operation, NAME, ModelType.STRING, bundle.getString("handler.name"), true);
            addRequestProperties(operation, LEVEL, ModelType.STRING, bundle.getString("handler.level"), true);
            addRequestProperties(operation, FILTER, ModelType.STRING, bundle.getString("handler.filter"), false);
            addRequestProperties(operation, QUEUE_LENGTH, ModelType.INT, bundle.getString("async.queue-length"), false);
            addRequestProperties(operation, OVERFLOW_ACTION, ModelType.STRING, bundle.getString("async.overflow-action"), false);
            addRequestProperties(operation, SUBHANDLERS, ModelType.LIST, ModelType.STRING, bundle.getString("logger.handlers"), false);

            return operation;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_ASSIGN_SUBHANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(LoggerAssignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("async.handler.assign-subhandler"));
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

            return node;
        }
    };

    static final DescriptionProvider ASYNC_HANDLER_UNASSIGN_SUBHANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(LoggerUnassignHandler.OPERATION_NAME);
            node.get(DESCRIPTION).set(bundle.getString("async.handler.unassign-subhandler"));
            addRequestProperties(node, NAME, ModelType.STRING, bundle.getString("handler.name"), true);

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
            addAttribute(node, TARGET, ModelType.STRING, bundle.getString("console.handler.target"));

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
            addRequestProperties(operation, TARGET, ModelType.STRING, bundle.getString("console.handler.target"), true);

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

            addCommonHandlerOutputStreamUpdateRequestProperties(operation, bundle);
            addRequestProperties(operation, TARGET, ModelType.STRING, bundle.getString("console.handler.target"), true);

            return operation;
        }
    };

    private static void addCommonFileHandlerAttributes(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerOutputStreamAttributes(model, bundle);
        addAttribute(model, APPEND, ModelType.BOOLEAN, bundle.getString("file.handler.append"));
        addAttribute(model.get(FILE.getName()), RELATIVE_TO, ModelType.STRING, bundle.getString("file.handler.relative-to"));
        addAttribute(model.get(FILE.getName()), PATH, ModelType.STRING, bundle.getString("file.handler.path"));
    }

    private static void addCommonFileHandlerRequestProperties(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerOutputStreamRequestProperties(model, bundle);
        addRequestProperties(model, APPEND, ModelType.BOOLEAN, bundle.getString("file.handler.append"), false);
        addRequestProperties(model.get(FILE.getName()), RELATIVE_TO, ModelType.STRING, bundle.getString("file.handler.relative-to"), false);
        addRequestProperties(model.get(FILE.getName()), PATH, ModelType.STRING, bundle.getString("file.handler.path"), true);
    }

    private static void addCommonFileHandlerUpdateRequestProperties(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerOutputStreamUpdateRequestProperties(model, bundle);
        addRequestProperties(model, APPEND, ModelType.BOOLEAN, bundle.getString("file.handler.append"), false);
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

            addCommonFileHandlerUpdateRequestProperties(operation, bundle);

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
            addAttribute(node, SUFFIX, ModelType.STRING, bundle.getString("periodic.handler.suffix"));

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
            addRequestProperties(operation, SUFFIX, ModelType.STRING, bundle.getString("periodic.handler.suffix"), true);

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

            addCommonFileHandlerUpdateRequestProperties(operation, bundle);
            addRequestProperties(operation, SUFFIX, ModelType.STRING, bundle.getString("periodic.handler.suffix"), true);

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
            addAttribute(node, ROTATE_SIZE, ModelType.STRING, bundle.getString("size.periodic.handler.rotate-size"));
            addAttribute(node, MAX_BACKUP_INDEX, ModelType.INT, bundle.getString("size.periodic.handler.max-backup"));

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
            addRequestProperties(operation, ROTATE_SIZE, ModelType.STRING, bundle.getString("size.periodic.handler.rotate-size"), true);
            addRequestProperties(operation, MAX_BACKUP_INDEX, ModelType.INT, bundle.getString("size.periodic.handler.max-backup"), true);

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

            addCommonFileHandlerUpdateRequestProperties(operation, bundle);
            addRequestProperties(operation, ROTATE_SIZE, ModelType.STRING, bundle.getString("size.periodic.handler.rotate-size"), true);
            addRequestProperties(operation, MAX_BACKUP_INDEX, ModelType.INT, bundle.getString("size.periodic.handler.max-backup"), true);

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
            addAttribute(node, CLASS, ModelType.STRING, bundle.getString("custom.handler.class"));
            addAttribute(node, MODULE, ModelType.STRING, bundle.getString("custom.handler.module"));
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
            addRequestProperties(node, CLASS, ModelType.STRING, bundle.getString("custom.handler.class"), true);
            addRequestProperties(node, MODULE, ModelType.STRING, bundle.getString("custom.handler.module"), true);
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
            node.get(DESCRIPTION).set(bundle.getString("handler.enable"));
            return node;
        }
    };

    static final DescriptionProvider HANDLER_DISABLE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
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
            addRequestProperties(node, LEVEL, ModelType.STRING, bundle.getString("logger.level"), true);

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
            addRequestProperties(node, PATH, ModelType.STRING, bundle.getString("file.handler.path"), true);
            addRequestProperties(node, RELATIVE_TO, ModelType.STRING, bundle.getString("file.handler.relative-to"), true);

            return node;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static void addAttribute(final ModelNode node, final AttributeDefinition definition, final ModelType type, final String description) {
        addAttribute(node, definition, type, null, description);
    }

    private static void addAttribute(final ModelNode node, final AttributeDefinition definition, final ModelType type, final ModelType valueType, final String description) {
        addAttribute(node, definition.getName(), type, valueType, description);
    }

    private static void addAttribute(final ModelNode node, final String name, final ModelType type, final String description) {
        addAttribute(node, name, type, null, description);
    }

    private static void addAttribute(final ModelNode node, final String name, final ModelType type, final ModelType valueType, final String description) {
        node.get(ATTRIBUTES, name, TYPE).set(type);
        node.get(ATTRIBUTES, name, DESCRIPTION).set(description);
        if (valueType != null) {
            node.get(ATTRIBUTES, name, VALUE_TYPE).set(valueType);
        }
    }

    private static void addRequestProperties(final ModelNode node, final AttributeDefinition definition, final ModelType type, final String description, final boolean isRequired) {
        addRequestProperties(node, definition, type, null, description, isRequired);
    }

    private static void addRequestProperties(final ModelNode node, final AttributeDefinition definition, final ModelType type, final ModelType valueType, final String description, final boolean isRequired) {
        addRequestProperties(node, definition.getName(), type, valueType, description, isRequired);
    }

    private static void addRequestProperties(final ModelNode node, final String name, final ModelType type, final String description, final boolean isRequired) {
        addRequestProperties(node, name, type, null, description, isRequired);
    }

    private static void addRequestProperties(final ModelNode node, final String name, final ModelType type, final ModelType valueType, final String description, final boolean isRequired) {
        node.get(REQUEST_PROPERTIES, name, TYPE).set(type);
        node.get(REQUEST_PROPERTIES, name, DESCRIPTION).set(description);
        node.get(REQUEST_PROPERTIES, name, REQUIRED).set(isRequired);
        if (valueType != null) {
            node.get(REQUEST_PROPERTIES, name, VALUE_TYPE).set(valueType);
        }
    }

    private static void addHandlerPropertiesAttributes(final ModelNode node, final ResourceBundle bundle) {
        final String properties = PROPERTIES.getName();
        final String name = NAME.getName();
        final String value = VALUE.getName();
        node.get(ATTRIBUTES, properties, TYPE).set(ModelType.LIST);
        node.get(ATTRIBUTES, properties, DESCRIPTION).set(bundle.getString("handler.properties"));
        node.get(ATTRIBUTES, properties, VALUE_TYPE, name, DESCRIPTION).set(bundle.getString("handler.properties.name"));
        node.get(ATTRIBUTES, properties, VALUE_TYPE, name, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, properties, VALUE_TYPE, value, DESCRIPTION).set(bundle.getString("handler.properties.value"));
        node.get(ATTRIBUTES, properties, VALUE_TYPE, value, TYPE).set(ModelType.STRING);
    }

    private static void addHandlerPropertiesRequestProperties(final ModelNode node, final ResourceBundle bundle) {
        final String properties = PROPERTIES.getName();
        final String name = NAME.getName();
        final String value = VALUE.getName();
        node.get(REQUEST_PROPERTIES, properties, TYPE).set(ModelType.LIST);
        node.get(REQUEST_PROPERTIES, properties, DESCRIPTION).set(bundle.getString("handler.properties"));
        node.get(REQUEST_PROPERTIES, properties, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, name, DESCRIPTION).set(bundle.getString("handler.properties.name"));
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, name, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, name, REQUIRED).set(true);
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, value, DESCRIPTION).set(bundle.getString("handler.properties.value"));
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, value, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, properties, VALUE_TYPE, value, REQUIRED).set(true);
    }
}
