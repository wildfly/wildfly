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

import static org.jboss.as.connector.subsystems.jca.ArchiveValidationAdd.ArchiveValidationParameters;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION;
import static org.jboss.as.connector.subsystems.jca.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

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
            subsystem.get(NAMESPACE).set(Namespace.JCA_1_1.getUriString());

            subsystem.get(CHILDREN, ARCHIVE_VALIDATION , DESCRIPTION).set(bundle.getString("jca." + ARCHIVE_VALIDATION));
            subsystem.get(CHILDREN, BEAN_VALIDATION , DESCRIPTION).set(bundle.getString("jca." + BEAN_VALIDATION));
            subsystem.get(CHILDREN, BOOTSTRAP_CONTEXT , DESCRIPTION).set(bundle.getString("jca." + BOOTSTRAP_CONTEXT));
            subsystem.get(CHILDREN, WORKMANAGER , DESCRIPTION).set(bundle.getString("jca." + WORKMANAGER));
            subsystem.get(CHILDREN, CACHED_CONNECTION_MANAGER , DESCRIPTION).set(bundle.getString("jca." + CACHED_CONNECTION_MANAGER));


            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("jca.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("remove");
            operation.get(DESCRIPTION).set(bundle.getString("jca.remove"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }

    };

    static DescriptionProvider ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode configPropertiesNode = new ModelNode();
            configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(DESCRIPTION).set("jca." + ARCHIVE_VALIDATION);

            for (ArchiveValidationParameters parameter : ArchiveValidationParameters.values()) {
                parameter.getAttribute().addResourceAttributeDescription(bundle, "jca.archive-validation", configPropertiesNode);
            }

            return configPropertiesNode;
        }
    };

    static DescriptionProvider ADD_ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("jca.archive-validation.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (ArchiveValidationParameters parameter : ArchiveValidationParameters.values()) {
                parameter.getAttribute().addOperationParameterDescription(bundle, "jca.archive-validation", op);
            }

            return op;
        }
    };

    static DescriptionProvider REMOVE_ARCHIVE_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("jca.archive-validation.remove"));
            return operation;
        }
    };

    static DescriptionProvider BEAN_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode configPropertiesNode = new ModelNode();
            configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(DESCRIPTION).set("jca." + Constants.BEAN_VALIDATION);

            for (BeanValidationAdd.BeanValidationParameters parameter : BeanValidationAdd.BeanValidationParameters.values()) {
                parameter.getAttribute().addResourceAttributeDescription(bundle, "jca.bean-validation", configPropertiesNode);
            }

            return configPropertiesNode;
        }
    };

    static DescriptionProvider ADD_BEAN_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("jca.bean-validation.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (BeanValidationAdd.BeanValidationParameters parameter : BeanValidationAdd.BeanValidationParameters.values()) {
                parameter.getAttribute().addOperationParameterDescription(bundle, "jca.bean-validation", op);
            }

            return op;
        }
    };

    static DescriptionProvider REMOVE_BEAN_VALIDATION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("jca.bean-validation.remove"));
            return operation;
        }
    };

    static DescriptionProvider CACHED_CONNECTION_MANAGER_DESC = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);

                final ModelNode configPropertiesNode = new ModelNode();
                configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
                configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
                configPropertiesNode.get(DESCRIPTION).set("jca." + Constants.CACHED_CONNECTION_MANAGER);

                for (CachedConnectionManagerAdd.CcmParameters parameter : CachedConnectionManagerAdd.CcmParameters.values()) {
                    parameter.getAttribute().addResourceAttributeDescription(bundle, "jca.cached-connection-manager", configPropertiesNode);
                }

                return configPropertiesNode;
            }
        };

        static DescriptionProvider ADD_CACHED_CONNECTION_MANAGER_DESC = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);

                final ModelNode op = new ModelNode();

                op.get(DESCRIPTION).set(bundle.getString("jca.cached-connection-manager.add"));
                op.get(OPERATION_NAME).set(ADD);

                for (CachedConnectionManagerAdd.CcmParameters parameter : CachedConnectionManagerAdd.CcmParameters.values()) {
                    parameter.getAttribute().addOperationParameterDescription(bundle, "jca.cached-connection-manager", op);
                }

                return op;
            }
        };

        static DescriptionProvider REMOVE_CACHED_CONNECTION_MANAGER_DESC = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(final Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                final ModelNode operation = new ModelNode();
                operation.get(OPERATION_NAME).set(REMOVE);
                operation.get(DESCRIPTION).set(bundle.getString("jca.cached-connection-manager.remove"));
                return operation;
            }
        };

    static DescriptionProvider WORKMANAGER_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode configPropertiesNode = new ModelNode();
            configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(DESCRIPTION).set("jca." + Constants.WORKMANAGER);

            for (WorkManagerAdd.WmParameters parameter : WorkManagerAdd.WmParameters.values()) {
                parameter.getAttribute().addResourceAttributeDescription(bundle, "jca.workmanager", configPropertiesNode);
            }
            configPropertiesNode.get(CHILDREN, WORKMANAGER_LONG_RUNNING, DESCRIPTION).set(bundle.getString(WORKMANAGER_LONG_RUNNING));
            configPropertiesNode.get(CHILDREN, WORKMANAGER_SHORT_RUNNING , DESCRIPTION).set(bundle.getString(WORKMANAGER_SHORT_RUNNING));

            return configPropertiesNode;
        }
    };

    static DescriptionProvider ADD_WORKMANAGER_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("jca.workmanager.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (WorkManagerAdd.WmParameters parameter : WorkManagerAdd.WmParameters.values()) {
                parameter.getAttribute().addOperationParameterDescription(bundle, "jca.workmanager", op);
            }

            return op;
        }
    };

    static DescriptionProvider REMOVE_WORKMANAGER_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("jca.workmanager.remove"));
            return operation;
        }
    };

    static DescriptionProvider BOOTSTRAP_CONTEXT_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode configPropertiesNode = new ModelNode();
            configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(DESCRIPTION).set("jca." + Constants.BOOTSTRAP_CONTEXT);

            for (BootstrapContextAdd.BootstrapCtxParameters parameter : BootstrapContextAdd.BootstrapCtxParameters.values()) {
                parameter.getAttribute().addResourceAttributeDescription(bundle, "jca.bootstrap-context", configPropertiesNode);
            }

            return configPropertiesNode;
        }
    };

    static DescriptionProvider ADD_BOOTSTRAP_CONTEXT_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("jca.bootstrap-context.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (BootstrapContextAdd.BootstrapCtxParameters parameter : BootstrapContextAdd.BootstrapCtxParameters.values()) {
                parameter.getAttribute().addOperationParameterDescription(bundle, "jca.bootstrap-context", op);
            }

            return op;
        }
    };

    static DescriptionProvider REMOVE_BOOTSTRAP_CONTEXT_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("jca.bootstrap-context.remove"));
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
