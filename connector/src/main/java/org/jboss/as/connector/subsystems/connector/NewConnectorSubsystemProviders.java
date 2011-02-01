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
package org.jboss.as.connector.subsystems.connector;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.connector.subsystems.connector.Constants.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class NewConnectorSubsystemProviders {

    static final String RESOURCE_NAME = NewConnectorSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("connector"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CONNECTOR_1_0.getUriString());

            return subsystem;
        }
    };

    static final DescriptionProvider ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("archive-validation"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, ENABLED, DESCRIPTION).set(bundle.getString("archive-validation.enabled"));
            node.get(ATTRIBUTES, ENABLED, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, ENABLED, REQUIRED).set(false);

            node.get(ATTRIBUTES, FAIL_ON_ERROR, DESCRIPTION).set(bundle.getString("archive-validation.fail-on-error"));
            node.get(ATTRIBUTES, FAIL_ON_ERROR, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, FAIL_ON_ERROR, REQUIRED).set(false);

            node.get(ATTRIBUTES, FAIL_ON_WARN, DESCRIPTION).set(bundle.getString("archive-validation.fail-on-warn"));
            node.get(ATTRIBUTES, FAIL_ON_WARN, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, FAIL_ON_WARN, REQUIRED).set(false);

            return node;
        }
    };

    static final DescriptionProvider BEAN_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("bean-validation"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, ENABLED, DESCRIPTION).set(bundle.getString("bean-validation.enabled"));
            node.get(ATTRIBUTES, ENABLED, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, ENABLED, REQUIRED).set(true);

            return node;
        }
    };

    static final DescriptionProvider DEFAULT_WORKMANAGER_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("default-workmanager"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, SHORT_RUNNING_THREAD_POOL, DESCRIPTION).set(
                    bundle.getString("default-workmanager.short-running-thread-pool"));
            node.get(ATTRIBUTES, SHORT_RUNNING_THREAD_POOL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SHORT_RUNNING_THREAD_POOL, REQUIRED).set(true);

            node.get(ATTRIBUTES, LONG_RUNNING_THREAD_POOL, DESCRIPTION).set(
                    bundle.getString("default-workmanager.long-running-thread-pool"));
            node.get(ATTRIBUTES, LONG_RUNNING_THREAD_POOL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, LONG_RUNNING_THREAD_POOL, REQUIRED).set(true);

            return node;
        }
    };

    // Operations
    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("connector.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            return operation;
        }
    };

    static DescriptionProvider ADD_ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("connector.archive-validation.add"));
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            operation.get(REQUEST_PROPERTIES, ENABLED, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.enabled"));
            operation.get(REQUEST_PROPERTIES, ENABLED, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ENABLED, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, FAIL_ON_ERROR, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.fail-on-error"));
            operation.get(REQUEST_PROPERTIES, FAIL_ON_ERROR, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, FAIL_ON_ERROR, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, FAIL_ON_WARN, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.fail-on-warn"));
            operation.get(REQUEST_PROPERTIES, FAIL_ON_WARN, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, FAIL_ON_WARN, REQUIRED).set(false);

            return operation;
        }
    };

    static DescriptionProvider REMOVE_ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("connector.archive-validation.remove"));
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            return operation;
        }
    };

    static DescriptionProvider ADD_DEFAULT_WORKMANAGED_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("connector.default-workmanager.add"));
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            operation.get(REQUEST_PROPERTIES, SHORT_RUNNING_THREAD_POOL, DESCRIPTION).set(
                    bundle.getString("connector.default-workmanager.short-running-thread-pool"));
            operation.get(REQUEST_PROPERTIES, SHORT_RUNNING_THREAD_POOL, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, SHORT_RUNNING_THREAD_POOL, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, LONG_RUNNING_THREAD_POOL, DESCRIPTION).set(
                    bundle.getString("connector.default-workmanager.long-running-thread-pool"));
            operation.get(REQUEST_PROPERTIES, LONG_RUNNING_THREAD_POOL, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, LONG_RUNNING_THREAD_POOL, REQUIRED).set(false);

            return operation;
        }
    };

    static DescriptionProvider REMOVE_DEFAULT_WORKMANAGED_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("connector.default-workmanager.remove"));
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            return operation;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
