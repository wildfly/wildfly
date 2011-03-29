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

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class ResourceAdaptersSubsystemProviders {

    static final String[] RESOURCEADAPTER_ATTRIBUTE = new String[] { ARCHIVE, TRANSACTIONSUPPORT, BOOTSTRAPCONTEXT,
            BEANVALIDATIONGROUPS, CONNECTIONDEFINITIONS, ADMIN_OBJECTS };
    static final NodeAttribute[] CONNECTIONDEFINITIONS_NODEATTRIBUTE = new NodeAttribute[] {
            new NodeAttribute(CLASS_NAME, ModelType.STRING, true), new NodeAttribute(JNDINAME, ModelType.STRING, true),
            new NodeAttribute(POOL_NAME, ModelType.STRING, false),
            new NodeAttribute(USE_JAVA_CONTEXT, ModelType.BOOLEAN, false),
            new NodeAttribute(ENABLED, ModelType.BOOLEAN, false), new NodeAttribute(MAX_POOL_SIZE, ModelType.INT, false),
            new NodeAttribute(MIN_POOL_SIZE, ModelType.INT, false),
            new NodeAttribute(POOL_USE_STRICT_MIN, ModelType.BOOLEAN, false),
            new NodeAttribute(SECURITY_DOMAIN_AND_APPLICATION, ModelType.STRING, false),
            new NodeAttribute(APPLICATION, ModelType.STRING, false),
            new NodeAttribute(SECURITY_DOMAIN, ModelType.STRING, false),
            new NodeAttribute(ALLOCATION_RETRY, ModelType.BOOLEAN, false),
            new NodeAttribute(ALLOCATION_RETRY_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(BLOCKING_TIMEOUT_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(IDLETIMEOUTMINUTES, ModelType.INT, false),
            new NodeAttribute(XA_RESOURCE_TIMEOUT, ModelType.LONG, false),
            new NodeAttribute(USETRYLOCK, ModelType.BOOLEAN, false),
            new NodeAttribute(BACKGROUNDVALIDATIONMINUTES, ModelType.INT, false),
            new NodeAttribute(BACKGROUNDVALIDATION, ModelType.BOOLEAN, false),
            new NodeAttribute(USE_FAST_FAIL, ModelType.BOOLEAN, false) };

    static final NodeAttribute[] ADMIN_OBJECTS_NODEATTRIBUTE = new NodeAttribute[] {
            new NodeAttribute(CLASS_NAME, ModelType.STRING, true), new NodeAttribute(JNDINAME, ModelType.STRING, true),
            new NodeAttribute(POOL_NAME, ModelType.STRING, false),
            new NodeAttribute(USE_JAVA_CONTEXT, ModelType.BOOLEAN, false), new NodeAttribute(ENABLED, ModelType.BOOLEAN, false) };

    static final String RESOURCE_NAME = ResourceAdaptersSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("resource-adapters-subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.RESOURCEADAPTERS_1_0.getUriString());
            final ModelNode resourceAdaptersNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(RESOURCEADAPTERS);

            final ModelNode connectionDefinitionsNode = new ModelNode();
            connectionDefinitionsNode.get(HEAD_COMMENT_ALLOWED).set(true);
            connectionDefinitionsNode.get(TAIL_COMMENT_ALLOWED).set(true);
            connectionDefinitionsNode.get(DESCRIPTION).set(CONNECTIONDEFINITIONS);

            for (NodeAttribute attribute : CONNECTIONDEFINITIONS_NODEATTRIBUTE) {
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(
                        bundle.getString(attribute.getName()));
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            resourceAdaptersNode.get(CONNECTIONDEFINITIONS).set(connectionDefinitionsNode);

            subsystem.get(RESOURCEADAPTER).set(resourceAdaptersNode);
            final ModelNode adminObjectNode = new ModelNode();
            adminObjectNode.get(HEAD_COMMENT_ALLOWED).set(true);
            adminObjectNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(ADMIN_OBJECTS);

            for (NodeAttribute attribute : ADMIN_OBJECTS_NODEATTRIBUTE) {
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            resourceAdaptersNode.get(ADMIN_OBJECTS).set(adminObjectNode);

            resourceAdaptersNode.get(ATTRIBUTES, ARCHIVE, DESCRIPTION).set(bundle.getString(ARCHIVE));
            resourceAdaptersNode.get(ATTRIBUTES, ARCHIVE, TYPE).set(ModelType.STRING);
            resourceAdaptersNode.get(ATTRIBUTES, ARCHIVE, REQUIRED).set(true);

            resourceAdaptersNode.get(ATTRIBUTES, TRANSACTIONSUPPORT, DESCRIPTION).set(bundle.getString(TRANSACTIONSUPPORT));
            resourceAdaptersNode.get(ATTRIBUTES, TRANSACTIONSUPPORT, TYPE).set(ModelType.STRING);
            resourceAdaptersNode.get(ATTRIBUTES, TRANSACTIONSUPPORT, REQUIRED).set(false);

            resourceAdaptersNode.get(ATTRIBUTES, CONFIG_PROPERTIES, TYPE).set(ModelType.STRING);
            resourceAdaptersNode.get(ATTRIBUTES, CONFIG_PROPERTIES, REQUIRED).set(false);

            resourceAdaptersNode.get(ATTRIBUTES, BEANVALIDATIONGROUPS, DESCRIPTION).set(bundle.getString(BEANVALIDATIONGROUPS));
            resourceAdaptersNode.get(ATTRIBUTES, BEANVALIDATIONGROUPS, TYPE).set(ModelType.STRING);
            resourceAdaptersNode.get(ATTRIBUTES, BEANVALIDATIONGROUPS, REQUIRED).set(false);

            subsystem.get(RESOURCEADAPTER).set(resourceAdaptersNode);

            return subsystem;

        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("resource-adapters-subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.RESOURCEADAPTERS_1_0.getUriString());
            final ModelNode resourceAdaptersNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(RESOURCEADAPTERS);

            final ModelNode connectionDefinitionsNode = new ModelNode();
            connectionDefinitionsNode.get(HEAD_COMMENT_ALLOWED).set(true);
            connectionDefinitionsNode.get(TAIL_COMMENT_ALLOWED).set(true);
            connectionDefinitionsNode.get(DESCRIPTION).set(CONNECTIONDEFINITIONS);

            for (NodeAttribute attribute : CONNECTIONDEFINITIONS_NODEATTRIBUTE) {
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(
                        bundle.getString(attribute.getName()));
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                connectionDefinitionsNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            resourceAdaptersNode.get(CONNECTIONDEFINITIONS).set(connectionDefinitionsNode);

            subsystem.get(RESOURCEADAPTER).set(resourceAdaptersNode);
            final ModelNode adminObjectNode = new ModelNode();
            adminObjectNode.get(HEAD_COMMENT_ALLOWED).set(true);
            adminObjectNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(ADMIN_OBJECTS);

            for (NodeAttribute attribute : ADMIN_OBJECTS_NODEATTRIBUTE) {
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                adminObjectNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            resourceAdaptersNode.get(ADMIN_OBJECTS).set(adminObjectNode);

            final ModelNode archiveNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(ARCHIVE);
            final ModelNode transactionSupportNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(TRANSACTIONSUPPORT);
            final ModelNode bootstrapSupportNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(BOOTSTRAPCONTEXT);
            final ModelNode configPropertiesNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(CONFIG_PROPERTIES);
            final ModelNode beanValidationNode = new ModelNode();
            resourceAdaptersNode.get(HEAD_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(TAIL_COMMENT_ALLOWED).set(true);
            resourceAdaptersNode.get(DESCRIPTION).set(BEANVALIDATIONGROUPS);

            resourceAdaptersNode.get(ARCHIVE).set(archiveNode);
            resourceAdaptersNode.get(TRANSACTIONSUPPORT).set(transactionSupportNode);
            resourceAdaptersNode.get(BOOTSTRAPCONTEXT).set(bootstrapSupportNode);
            resourceAdaptersNode.get(CONFIG_PROPERTIES).set(configPropertiesNode);
            resourceAdaptersNode.get(BEANVALIDATIONGROUPS).set(beanValidationNode);

            subsystem.get(RESOURCEADAPTER).set(resourceAdaptersNode);

            return subsystem;

        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    static class NodeAttribute {
        private final String name;
        private final ModelType modelType;
        private final boolean required;

        public NodeAttribute(String name, ModelType modelType, boolean required) {
            super();
            this.name = name;
            this.modelType = modelType;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public ModelType getModelType() {
            return modelType;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return "NodeAttribute [name=" + name + ", modelType=" + modelType + ", required=" + required + "]";
        }

    }

}
