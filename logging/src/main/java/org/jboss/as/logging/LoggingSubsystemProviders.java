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
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.HANDLER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;

import java.util.Locale;
import java.util.ResourceBundle;

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
            subsystem.get(CHILDREN, CommonAttributes.HANDLER, DESCRIPTION).set(bundle.getString("handler"));

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

    static final DescriptionProvider HANDLERS = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("handler"));

            node.get(ATTRIBUTES, LEVEL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, LEVEL, DESCRIPTION).set(bundle.getString("logger.level"));

            node.get(ATTRIBUTES, HANDLER, TYPE).set(ModelType.LIST);
            node.get(ATTRIBUTES, HANDLER, VALUE_TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, HANDLER, DESCRIPTION).set(bundle.getString("logger.handlers"));

            node.get(ATTRIBUTES, ENCODING, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENCODING, DESCRIPTION).set(bundle.getString("logger.level"));

            return node;
        }
    };

    static final DescriptionProvider HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("handler.add"));
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

    static final ModelNode getAsyncModelDescription(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("async.handler"));
        return node;
    }

    static final DescriptionProvider CONSOLE_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("console.handler"));
            return node;
        }
    };

    static final DescriptionProvider FILE_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("file.handler"));
            return node;
        }
    };

    static final DescriptionProvider PERIODIC_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("periodic.handler"));
            return node;
        }
    };

    static final DescriptionProvider SIZE_PERIODIC_HANDLER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("size.periodic.handler"));
            return node;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static void main(String[] args) {
        System.out.println(SUBSYSTEM.getModelDescription(null));
        System.out.println(SUBSYSTEM_ADD.getModelDescription(null));
        System.out.println(CONSOLE_HANDLER_ADD.getModelDescription(null));
        System.out.println(FILE_HANDLER_ADD.getModelDescription(null));
        System.out.println(HANDLER_ADD.getModelDescription(null));
        System.out.println(HANDLER_REMOVE.getModelDescription(null));
        System.out.println(HANDLERS.getModelDescription(null));
        System.out.println(LOGGER.getModelDescription(null));
        System.out.println(LOGGER_ADD.getModelDescription(null));
        System.out.println(LOGGER_REMOVE.getModelDescription(null));
        System.out.println(PERIODIC_HANDLER_ADD.getModelDescription(null));
        System.out.println(REMOVE_ROOT_LOGGER.getModelDescription(null));
        System.out.println(SET_ROOT_LOGGER.getModelDescription(null));
        System.out.println(SIZE_PERIODIC_HANDLER_ADD.getModelDescription(null));
    }

}
