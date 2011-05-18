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
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
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

            node.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));
            node.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, TYPE).set(ModelType.LIST);
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, VALUE_TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, DESCRIPTION).set(bundle.getString("logger.handlers"));
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, REQUIRED).set(true);

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

            operation.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));
            operation.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

            return operation;
        }
    };

    static final DescriptionProvider LOGGER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("logger"));
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

            node.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));
            node.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, TYPE).set(ModelType.LIST);
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, VALUE_TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, DESCRIPTION).set(bundle.getString("logger.handlers"));
            node.get(REQUEST_PROPERTIES, CommonAttributes.HANDLERS, REQUIRED).set(false);

            node.get(REQUEST_PROPERTIES, ENCODING, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ENCODING, DESCRIPTION).set(bundle.getString("logger.level"));
            node.get(REQUEST_PROPERTIES, ENCODING, REQUIRED).set(true);

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

            node.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));
            node.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

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


    private static void addCommonHandlerAttributes(final ModelNode modelNode, final ResourceBundle bundle) {
        modelNode.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        modelNode.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("handler.name"));

        modelNode.get(ATTRIBUTES, LEVEL, TYPE).set(ModelType.STRING);
        modelNode.get(ATTRIBUTES, LEVEL, DESCRIPTION).set(bundle.getString("handler.level"));

        modelNode.get(ATTRIBUTES, ENCODING, TYPE).set(ModelType.STRING);
        modelNode.get(ATTRIBUTES, ENCODING, DESCRIPTION).set(bundle.getString("handler.encoding"));

        modelNode.get(ATTRIBUTES, FILTER, TYPE).set(ModelType.STRING);
        modelNode.get(ATTRIBUTES, FILTER, DESCRIPTION).set(bundle.getString("handler.filter"));

        modelNode.get(ATTRIBUTES, FORMATTER, TYPE).set(ModelType.STRING);
        modelNode.get(ATTRIBUTES, FORMATTER, DESCRIPTION).set(bundle.getString("handler.formatter"));

        modelNode.get(ATTRIBUTES, AUTOFLUSH, TYPE).set(ModelType.BOOLEAN);
        modelNode.get(ATTRIBUTES, AUTOFLUSH, DESCRIPTION).set(bundle.getString("handler.autoflush"));
    }

    private static void addCommonHandlerRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        modelNode.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("handler.name"));
        modelNode.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("handler.level"));
        modelNode.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, ENCODING, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, ENCODING, DESCRIPTION).set(bundle.getString("handler.encoding"));
        modelNode.get(REQUEST_PROPERTIES, ENCODING, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, FILTER, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, FILTER, DESCRIPTION).set(bundle.getString("handler.filter"));
        modelNode.get(REQUEST_PROPERTIES, FILTER, REQUIRED).set(false);

        modelNode.get(REQUEST_PROPERTIES, FORMATTER, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, FORMATTER, DESCRIPTION).set(bundle.getString("handler.formatter"));
        modelNode.get(REQUEST_PROPERTIES, FORMATTER, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, TYPE).set(ModelType.BOOLEAN);
        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, DESCRIPTION).set(bundle.getString("handler.autoflush"));
        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, REQUIRED).set(false);
    }

    private static void addCommonHandlerUpdateRequestProperties(final ModelNode modelNode, final ResourceBundle bundle) {
        modelNode.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("handler.level"));
        modelNode.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, ENCODING, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, ENCODING, DESCRIPTION).set(bundle.getString("handler.encoding"));
        modelNode.get(REQUEST_PROPERTIES, ENCODING, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, FILTER, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, FILTER, DESCRIPTION).set(bundle.getString("handler.filter"));
        modelNode.get(REQUEST_PROPERTIES, FILTER, REQUIRED).set(false);

        modelNode.get(REQUEST_PROPERTIES, FORMATTER, TYPE).set(ModelType.STRING);
        modelNode.get(REQUEST_PROPERTIES, FORMATTER, DESCRIPTION).set(bundle.getString("handler.formatter"));
        modelNode.get(REQUEST_PROPERTIES, FORMATTER, REQUIRED).set(true);

        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, TYPE).set(ModelType.BOOLEAN);
        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, DESCRIPTION).set(bundle.getString("handler.autoflush"));
        modelNode.get(REQUEST_PROPERTIES, AUTOFLUSH, REQUIRED).set(false);
    }


    static final DescriptionProvider ASYNC_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("async.handler"));

            addCommonHandlerAttributes(node, bundle);

            node.get(ATTRIBUTES, HANDLER, TYPE).set(ModelType.LIST);
            node.get(ATTRIBUTES, HANDLER, VALUE_TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, HANDLER, DESCRIPTION).set(bundle.getString("logger.handlers"));

            node.get(ATTRIBUTES, QUEUE_LENGTH, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, QUEUE_LENGTH, DESCRIPTION).set(bundle.getString("async.queue-length"));

            node.get(ATTRIBUTES, OVERFLOW_ACTION, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, OVERFLOW_ACTION, DESCRIPTION).set(bundle.getString("async.overflow-action"));

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

            addCommonHandlerRequestProperties(operation, bundle);

            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, DESCRIPTION).set(bundle.getString("async.queue-length"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, REQUIRED).set(true);

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

            addCommonHandlerUpdateRequestProperties(operation, bundle);

            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, DESCRIPTION).set(bundle.getString("async.queue-length"));
            operation.get(REQUEST_PROPERTIES, QUEUE_LENGTH, REQUIRED).set(true);

            return operation;
        }
    };

    static final DescriptionProvider CONSOLE_HANDLER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("console.handler"));

            addCommonHandlerAttributes(node, bundle);

            node.get(ATTRIBUTES, TARGET, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TARGET, DESCRIPTION).set(bundle.getString("console.handler.target"));

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

            addCommonHandlerRequestProperties(operation, bundle);

            operation.get(REQUEST_PROPERTIES, TARGET, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, TARGET, DESCRIPTION).set(bundle.getString("console.handler.target"));
            operation.get(REQUEST_PROPERTIES, TARGET, REQUIRED).set(true);

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

            addCommonHandlerUpdateRequestProperties(operation, bundle);

            operation.get(REQUEST_PROPERTIES, TARGET, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, TARGET, DESCRIPTION).set(bundle.getString("console.handler.target"));
            operation.get(REQUEST_PROPERTIES, TARGET, REQUIRED).set(true);

            return operation;
        }
    };

    private static void addCommonFileHandlerAttributes(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerAttributes(model, bundle);
        model.get(ATTRIBUTES, APPEND, TYPE).set(ModelType.BOOLEAN);
        model.get(ATTRIBUTES, APPEND, DESCRIPTION).set(bundle.getString("file.handler.append"));

        model.get(ATTRIBUTES, FILE, RELATIVE_TO, TYPE).set(ModelType.STRING);
        model.get(ATTRIBUTES, FILE, RELATIVE_TO, DESCRIPTION).set(bundle.getString("file.handler.relative-to"));

        model.get(ATTRIBUTES, FILE, PATH, TYPE).set(ModelType.STRING);
        model.get(ATTRIBUTES, FILE, PATH, DESCRIPTION).set(bundle.getString("file.handler.path"));
    }

    private static void addCommonFileHandlerRequestProperties(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerRequestProperties(model, bundle);
        model.get(REQUEST_PROPERTIES, APPEND, TYPE).set(ModelType.BOOLEAN);
        model.get(REQUEST_PROPERTIES, APPEND, DESCRIPTION).set(bundle.getString("file.handler.append"));
        model.get(REQUEST_PROPERTIES, APPEND, REQUIRED).set(false);

        model.get(REQUEST_PROPERTIES, FILE, RELATIVE_TO, TYPE).set(ModelType.STRING);
        model.get(REQUEST_PROPERTIES, FILE, RELATIVE_TO, DESCRIPTION).set(bundle.getString("file.handler.relative-to"));
        model.get(REQUEST_PROPERTIES, FILE, RELATIVE_TO, REQUIRED).set(false);

        model.get(REQUEST_PROPERTIES, FILE, PATH, TYPE).set(ModelType.STRING);
        model.get(REQUEST_PROPERTIES, FILE, PATH, DESCRIPTION).set(bundle.getString("file.handler.path"));
        model.get(REQUEST_PROPERTIES, FILE, PATH, REQUIRED).set(true);
    }

    private static void addCommonFileHandlerUpdateRequestProperties(final ModelNode model, final ResourceBundle bundle) {
        addCommonHandlerUpdateRequestProperties(model, bundle);
        model.get(REQUEST_PROPERTIES, APPEND, TYPE).set(ModelType.BOOLEAN);
        model.get(REQUEST_PROPERTIES, APPEND, DESCRIPTION).set(bundle.getString("file.handler.append"));
        model.get(REQUEST_PROPERTIES, APPEND, REQUIRED).set(false);
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

            node.get(ATTRIBUTES, SUFFIX, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SUFFIX, DESCRIPTION).set(bundle.getString("periodic.handler.suffix"));

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

            operation.get(REQUEST_PROPERTIES, SUFFIX, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, SUFFIX, DESCRIPTION).set(bundle.getString("periodic.handler.suffix"));
            operation.get(REQUEST_PROPERTIES, SUFFIX, REQUIRED).set(true);

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

            operation.get(REQUEST_PROPERTIES, SUFFIX, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, SUFFIX, DESCRIPTION).set(bundle.getString("periodic.handler.suffix"));
            operation.get(REQUEST_PROPERTIES, SUFFIX, REQUIRED).set(true);

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

            node.get(ATTRIBUTES, ROTATE_SIZE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ROTATE_SIZE, DESCRIPTION).set(bundle.getString("size.periodic.handler.rotate-size"));

            node.get(ATTRIBUTES, MAX_BACKUP_INDEX, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, MAX_BACKUP_INDEX, DESCRIPTION).set(bundle.getString("size.periodic.handler.max-backup"));

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

            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, DESCRIPTION).set(bundle.getString("size.periodic.handler.rotate-size"));
            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, DESCRIPTION).set(bundle.getString("size.periodic.handler.max-backup"));
            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, REQUIRED).set(true);

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

            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, DESCRIPTION).set(bundle.getString("size.periodic.handler.rotate-size"));
            operation.get(REQUEST_PROPERTIES, ROTATE_SIZE, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, TYPE).set(ModelType.INT);
            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, DESCRIPTION).set(bundle.getString("size.periodic.handler.max-backup"));
            operation.get(REQUEST_PROPERTIES, MAX_BACKUP_INDEX, REQUIRED).set(true);

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

            node.get(REQUEST_PROPERTIES, LEVEL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));
            node.get(REQUEST_PROPERTIES, LEVEL, REQUIRED).set(true);

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

            node.get(REQUEST_PROPERTIES, PATH, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PATH, DESCRIPTION).set(bundle.getString("file.handler.path"));
            node.get(REQUEST_PROPERTIES, PATH, REQUIRED).set(true);

            node.get(REQUEST_PROPERTIES, RELATIVE_TO, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("file.handler.relative-to"));
            node.get(REQUEST_PROPERTIES, RELATIVE_TO, REQUIRED).set(false);

            return node;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
