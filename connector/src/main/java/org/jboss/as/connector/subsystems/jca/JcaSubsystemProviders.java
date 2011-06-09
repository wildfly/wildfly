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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_DEBUG;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.THREAD_POOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class JcaSubsystemProviders {

    static final String RESOURCE_NAME = JcaSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("jca"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.JCA_1_0.getUriString());

            subsystem.get(ATTRIBUTES, BEAN_VALIDATION_ENABLED, DESCRIPTION).set(bundle.getString("bean-validation.enabled"));
            subsystem.get(ATTRIBUTES, BEAN_VALIDATION_ENABLED, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, BEAN_VALIDATION_ENABLED, REQUIRED).set(true);

            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_ENABLED, DESCRIPTION).set(
                    bundle.getString("archive-validation.enabled"));
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_ENABLED, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_ENABLED, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_ENABLED, DEFAULT).set(true);

            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, DESCRIPTION).set(
                    bundle.getString("archive-validation.fail-on-error"));
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, DEFAULT).set(true);

            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_WARN, DESCRIPTION).set(
                    bundle.getString("archive-validation.fail-on-warn"));
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_WARN, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_WARN, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ARCHIVE_VALIDATION_FAIL_ON_WARN, DEFAULT).set(false);

            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_DEBUG, DESCRIPTION).set(
                    bundle.getString("cached-connection-manager.debug"));
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_DEBUG, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_DEBUG, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_DEBUG, DEFAULT).set(false);

            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_ERROR, DESCRIPTION).set(
                    bundle.getString("cached-connection-manager.error"));
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_ERROR, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_ERROR, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, CACHED_CONNECTION_MANAGER_ERROR, DEFAULT).set(false);

            subsystem.get(CHILDREN, THREAD_POOL, DESCRIPTION).set(bundle.getString("threadpool"));
            subsystem.get(CHILDREN, THREAD_POOL, REQUIRED).set(false);

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("connector.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_ENABLED, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.enabled"));
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_ENABLED, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_ENABLED, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_ENABLED, DEFAULT).set(false);

            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.fail-on-error"));
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_ERROR, DEFAULT).set(true);

            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_WARN, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.fail-on-warn"));
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_WARN, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_WARN, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, ARCHIVE_VALIDATION_FAIL_ON_WARN, DEFAULT).set(false);

            operation.get(REQUEST_PROPERTIES, BEAN_VALIDATION_ENABLED, DESCRIPTION).set(
                    bundle.getString("connector.archive-validation.fail-on-warn"));
            operation.get(REQUEST_PROPERTIES, BEAN_VALIDATION_ENABLED, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, BEAN_VALIDATION_ENABLED, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_DEBUG, DESCRIPTION).set(
                    bundle.getString("cached-connection-manager.debug"));
            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_DEBUG, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_DEBUG, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_ERROR, DESCRIPTION).set(
                    bundle.getString("cached-connection-manager.error"));
            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_ERROR, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, CACHED_CONNECTION_MANAGER_ERROR, REQUIRED).set(false);

            return operation;
        }
    };

    static DescriptionProvider SUBSYSTEM_REMOVE_DESC = new DescriptionProvider() {

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

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
