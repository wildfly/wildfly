/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

class LogStoreProviders {

    static final String RESOURCE_NAME = LogStoreProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final SimpleAttributeDefinition[] LOG_STORE_ATTRIBUTE = new SimpleAttributeDefinition[] {
            LogStoreConstants.LOG_STORE_TYPE };
    static final SimpleAttributeDefinition[] TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition[] {
            LogStoreConstants.JMX_NAME, LogStoreConstants.TRANSACTION_ID,
            LogStoreConstants.TRANSACTION_AGE,
            LogStoreConstants.RECORD_TYPE};
    static final SimpleAttributeDefinition[] PARTECIPANT_RW_ATTRIBUTE = new SimpleAttributeDefinition[] {
            };
    static final SimpleAttributeDefinition[] PARTECIPANT_ATTRIBUTE = new SimpleAttributeDefinition[] {
            LogStoreConstants.JMX_NAME, LogStoreConstants.PARTECIPANT_JNDI_NAME,
            LogStoreConstants.PARTECIPANT_STATUS, LogStoreConstants.RECORD_TYPE,
            LogStoreConstants.EIS_NAME, LogStoreConstants.EIS_VERSION };

    public static DescriptionProvider LOG_STORE_MODEL_CHILD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("log-store"));

            for (SimpleAttributeDefinition propertyType : LOG_STORE_ATTRIBUTE) {
                node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store." + propertyType.getName()));
                node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                node.get(ATTRIBUTES, propertyType.getName(), ACCESS_TYPE, READ_ONLY).set(true);
            }

            node.get(CHILDREN, LogStoreConstants.TRANSACTIONS, DESCRIPTION).set(bundle.getString("log-store.transaction"));

            return node;
        }
    };

    static DescriptionProvider ADD_LOG_STORE_MODEL_CHILD = new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(final Locale locale) {
                    final ResourceBundle bundle = getResourceBundle(locale);

                    final ModelNode operation = new ModelNode();
                    operation.get(OPERATION_NAME).set(ADD);
                    operation.get(DESCRIPTION).set(bundle.getString("log-store.add"));
 //             operation.get(LogStoreConstants.LOG_STORE_TYPE.getName());

                    for (SimpleAttributeDefinition propertyType : LOG_STORE_ATTRIBUTE) {
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                                bundle.getString("log-store." + propertyType.getName()));
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getType());
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                        if (propertyType.getDefaultValue() != null)
                            operation.get(REQUEST_PROPERTIES, propertyType.getName(), DEFAULT).set(propertyType.getDefaultValue().toString());
                    }

                    return operation;
                }
        };

    static DescriptionProvider REMOVE_LOG_STORE_MODEL_CHILD = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(final Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                final ModelNode operation = new ModelNode();
                operation.get(OPERATION_NAME).set(REMOVE);
                operation.get(DESCRIPTION).set(bundle.getString("log-store.remove"));
                return operation;
            }
    };

    static DescriptionProvider PROBE_OPERATION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(LogStoreConstants.PROBE);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.probe"));
            return operation;
        }
    };

    static DescriptionProvider RECOVER_OPERATION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(LogStoreConstants.RECOVER);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.participant.recover"));
            return operation;
        }
    };

    static DescriptionProvider REFRESH_OPERATION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(LogStoreConstants.REFRESH);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.participant.refresh"));
            return operation;
        }
    };

    static DescriptionProvider DELETE_OPERATION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(LogStoreConstants.DELETE);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.transaction.delete"));
            return operation;
        }
    };

    public static DescriptionProvider TRANSACTION_CHILD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("log-store.transaction"));
            for (SimpleAttributeDefinition propertyType : TRANSACTION_ATTRIBUTE) {
                node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store.transaction." + propertyType.getName()));
                node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                node.get(ATTRIBUTES, propertyType.getName(), ACCESS_TYPE, READ_ONLY).set(true);
            }
            node.get(CHILDREN, LogStoreConstants.PARTICIPANTS, DESCRIPTION).set(bundle.getString("log-store.participant"));

            return node;
        }
    };

    static DescriptionProvider ADD_TRANSACTION_CHILD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.transaction.add"));
            for (SimpleAttributeDefinition propertyType : TRANSACTION_ATTRIBUTE) {
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                        bundle.getString("log-store.transaction." + propertyType.getName()));
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getType());
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                if (propertyType.getDefaultValue() != null)
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), DEFAULT).set(propertyType.getDefaultValue().toString());
            }
            return operation;
        }
    };

    static DescriptionProvider REMOVE_TRANSACTION_CHILD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("log-store.transaction.remove"));
            return operation;
        }
    };


    public static DescriptionProvider PARTECIPANT_CHILD = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set(bundle.getString("log-store.participant"));
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_ATTRIBUTE) {
                    node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store.participant." + propertyType.getName()));
                    node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                    node.get(ATTRIBUTES, propertyType.getName(), ACCESS_TYPE, READ_ONLY).set(true);
                }
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_RW_ATTRIBUTE) {
                    node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store.participant." + propertyType.getName()));
                    node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                    node.get(ATTRIBUTES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                }
                return node;
            }
        };

        static DescriptionProvider ADD_PARTECIPANT_CHILD = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(final Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                final ModelNode operation = new ModelNode();
                operation.get(OPERATION_NAME).set(ADD);
                operation.get(DESCRIPTION).set(bundle.getString("log-store.participant.add"));
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_ATTRIBUTE) {
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                            bundle.getString("log-store.participant." + propertyType.getName()));
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getType());
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                    if (propertyType.getDefaultValue() != null)
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), DEFAULT).set(propertyType.getDefaultValue().toString());
                }
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_RW_ATTRIBUTE) {
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                            bundle.getString("log-store.participant." + propertyType.getName()));
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getType());
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                    if (propertyType.getDefaultValue() != null)
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), DEFAULT).set(propertyType.getDefaultValue().toString());
                }
                return operation;
            }
        };

        static DescriptionProvider REMOVE_PARTECIPANT_CHILD = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(final Locale locale) {
                final ResourceBundle bundle = getResourceBundle(locale);
                final ModelNode operation = new ModelNode();
                operation.get(OPERATION_NAME).set(REMOVE);
                operation.get(DESCRIPTION).set(bundle.getString("log-store.participant.remove"));
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
