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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class ResourceAdaptersSubsystemProviders {

    static final String[] RESOURCEADAPTER_ATTRIBUTE = new String[]{ARCHIVE.getName(), TRANSACTIONSUPPORT.getName(), BOOTSTRAPCONTEXT.getName(),
            CONFIG_PROPERTIES.getName(), BEANVALIDATIONGROUPS.getName(), CONNECTIONDEFINITIONS_NAME, ADMIN_OBJECTS_NAME};
    static final SimpleAttributeDefinition[] CONNECTIONDEFINITIONS_NODEATTRIBUTE = new SimpleAttributeDefinition[]{
            CLASS_NAME, JNDINAME,
            USE_JAVA_CONTEXT,
            ENABLED, MAX_POOL_SIZE,
            MIN_POOL_SIZE,
            POOL_USE_STRICT_MIN,
            POOL_FLUSH_STRATEGY,
            SECURITY_DOMAIN_AND_APPLICATION,
            APPLICATION,
            SECURITY_DOMAIN,
            ALLOCATION_RETRY,
            ALLOCATION_RETRY_WAIT_MILLIS,
            BLOCKING_TIMEOUT_WAIT_MILLIS,
            IDLETIMEOUTMINUTES,
            XA_RESOURCE_TIMEOUT,
            USETRYLOCK,
            BACKGROUNDVALIDATIONMILLIS,
            BACKGROUNDVALIDATION,
            USE_FAST_FAIL, USE_CCM};

    static final SimpleAttributeDefinition[] ADMIN_OBJECTS_NODEATTRIBUTE = new SimpleAttributeDefinition[]{
            CLASS_NAME, JNDINAME,
            USE_JAVA_CONTEXT, ENABLED};

    public static final String RESOURCE_NAME = ResourceAdaptersSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("resource-adapters"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.RESOURCEADAPTERS_1_0.getUriString());
            // Should this be an attribute instead

            subsystem.get(CHILDREN, Constants.RESOURCEADAPTER_NAME, DESCRIPTION).set(bundle.getString(Constants.RESOURCEADAPTER_NAME));
            //subsystem.get(CHILDREN, Constants.RESOURCEADAPTER_NAME, MODEL_DESCRIPTION);

            return subsystem;

        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("resource-adapters.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }

    };


    static DescriptionProvider CONFIG_PROPERTIES_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode configPropertiesNode = new ModelNode();
            configPropertiesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            configPropertiesNode.get(DESCRIPTION).set(CONFIG_PROPERTIES.getName());


            CONFIG_PROPERTY_VALUE.addResourceAttributeDescription(bundle, "config-properties", configPropertiesNode);

            return configPropertiesNode;
        }
    };

    static DescriptionProvider CONNECTION_DEFINITION_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode connectionDefinitionNode = new ModelNode();
            connectionDefinitionNode.get(HEAD_COMMENT_ALLOWED).set(true);
            connectionDefinitionNode.get(TAIL_COMMENT_ALLOWED).set(true);
            connectionDefinitionNode.get(DESCRIPTION).set(CONNECTIONDEFINITIONS_NAME);


            for (SimpleAttributeDefinition attribute : CONNECTIONDEFINITIONS_NODEATTRIBUTE) {
                attribute.addResourceAttributeDescription(bundle, null, connectionDefinitionNode);
            }

            connectionDefinitionNode.get(CHILDREN, Constants.CONFIG_PROPERTIES.getName(), DESCRIPTION).set(bundle.getString(Constants.CONFIG_PROPERTIES.getName()));

            return connectionDefinitionNode;
        }
    };

    static DescriptionProvider ADMIN_OBJECT_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode adminObjectNode = new ModelNode();
            adminObjectNode.get(HEAD_COMMENT_ALLOWED).set(true);
            adminObjectNode.get(TAIL_COMMENT_ALLOWED).set(true);
            adminObjectNode.get(DESCRIPTION).set(ADMIN_OBJECTS_NAME);


            for (SimpleAttributeDefinition attribute : ADMIN_OBJECTS_NODEATTRIBUTE) {
                attribute.addResourceAttributeDescription(bundle, null, adminObjectNode);
            }

            adminObjectNode.get(CHILDREN, Constants.CONFIG_PROPERTIES.getName(), DESCRIPTION).set(bundle.getString(Constants.CONFIG_PROPERTIES.getName()));

            return adminObjectNode;
        }
    };


     static final DescriptionProvider RESOURCEADAPTER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode raNode = new ModelNode();
            raNode.get(HEAD_COMMENT_ALLOWED).set(true);
            raNode.get(TAIL_COMMENT_ALLOWED).set(true);
            raNode.get(DESCRIPTION).set(RESOURCEADAPTER_NAME);

            raNode.get(ATTRIBUTES, ARCHIVE.getName(), DESCRIPTION).set(bundle.getString(ARCHIVE.getName()));
            raNode.get(ATTRIBUTES, ARCHIVE.getName(), TYPE).set(ModelType.STRING);
            raNode.get(ATTRIBUTES, ARCHIVE.getName(), REQUIRED).set(true);
            raNode.get(ATTRIBUTES, TRANSACTIONSUPPORT.getName(), DESCRIPTION).set(bundle.getString(TRANSACTIONSUPPORT.getName()));
            raNode.get(ATTRIBUTES, TRANSACTIONSUPPORT.getName(), TYPE).set(ModelType.STRING);
            raNode.get(ATTRIBUTES, TRANSACTIONSUPPORT.getName(), REQUIRED).set(true);

            raNode.get(CHILDREN, Constants.CONNECTIONDEFINITIONS_NAME, DESCRIPTION).set(bundle.getString(Constants.CONNECTIONDEFINITIONS_NAME));
            raNode.get(CHILDREN, Constants.ADMIN_OBJECTS_NAME, DESCRIPTION).set(bundle.getString(Constants.ADMIN_OBJECTS_NAME));
            raNode.get(CHILDREN, Constants.CONFIG_PROPERTIES.getName(), DESCRIPTION).set(bundle.getString(Constants.CONFIG_PROPERTIES.getName()));


            return raNode;
        }

    };

    static final DescriptionProvider ADD_RESOURCEADAPTER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("resource-adapter.add"));


            final ModelNode adminObjectNode = new ModelNode();
            adminObjectNode.get(DESCRIPTION).set(ADMIN_OBJECTS_NAME);


            operation.get(REQUEST_PROPERTIES, ARCHIVE.getName(), DESCRIPTION).set(bundle.getString(ARCHIVE.getName()));
            operation.get(REQUEST_PROPERTIES, ARCHIVE.getName(), TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ARCHIVE.getName(), REQUIRED).set(true);
            operation.get(REQUEST_PROPERTIES, TRANSACTIONSUPPORT.getName(), DESCRIPTION).set(bundle.getString(TRANSACTIONSUPPORT.getName()));
            operation.get(REQUEST_PROPERTIES, TRANSACTIONSUPPORT.getName(), TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, TRANSACTIONSUPPORT.getName(), REQUIRED).set(true);

            return operation;
        }

    };

    static DescriptionProvider ADD_CONFIG_PROPERTIES_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("config-properties.add"));
            op.get(OPERATION_NAME).set(ADD);


            CONFIG_PROPERTY_VALUE.addOperationParameterDescription(bundle, "config-properties", op);

            return op;
        }
    };


     static DescriptionProvider ADD_CONNECTION_DEFINITION_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("connection-definition.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (SimpleAttributeDefinition attribute : CONNECTIONDEFINITIONS_NODEATTRIBUTE) {
                attribute.addOperationParameterDescription(bundle, null, op);
            }

            return op;
        }
    };

    static DescriptionProvider ADD_ADMIN_OBJECT_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("admin-object.add"));
            op.get(OPERATION_NAME).set(ADD);

            for (SimpleAttributeDefinition attribute : ADMIN_OBJECTS_NODEATTRIBUTE) {
                attribute.addOperationParameterDescription(bundle, null, op);
            }

            return op;
        }
    };

    static DescriptionProvider REMOVE_RESOURCEADAPTER_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("resourceadapter.remove"));
            return operation;
        }
    };

    static DescriptionProvider REMOVE_CONFIG_PROPERTIES_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("config-properties.remove"));
            return operation;
        }
    };

    static DescriptionProvider REMOVE_CONNECTION_DEFINITION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("connection-definition.remove"));
            return operation;
        }
    };

    static DescriptionProvider REMOVE_ADMIN_OBJECT_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("admin-object.remove"));
            return operation;
        }
    };


    static DescriptionProvider FLUSH_IDLE_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("flush-idle-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("resourceadapter.flush-idle-connection-in-pool"));
            return operation;
        }
    };

    static DescriptionProvider FLUSH_ALL_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("flush-all-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("resourceadapter.flush-all-connection-in-pool"));
            return operation;
        }
    };

    static DescriptionProvider TEST_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("test-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("resourceadapter.test-connection-in-pool"));
            return operation;
        }
    };

    public static DescriptionProvider CLEAR_STATISTICS_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("clear-statistics");
            operation.get(DESCRIPTION).set(bundle.getString("resourceadapter.clear-statistics"));
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
