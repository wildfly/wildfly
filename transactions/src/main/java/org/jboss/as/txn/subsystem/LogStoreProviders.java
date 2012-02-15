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

    static final SimpleAttributeDefinition[] LOG_STORE_ATTRIBUTE = new SimpleAttributeDefinition[] { LogStoreConstans.LOG_STORE_TYPE };
    static final SimpleAttributeDefinition[] TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition[] { LogStoreConstans.TRANSACTION_ID, LogStoreConstans.TRANSACTION_AGE };
    static final SimpleAttributeDefinition[] PARTECIPANT_RW_ATTRIBUTE = new SimpleAttributeDefinition[] { LogStoreConstans.PARTECIPANT_STATUS };
    static final SimpleAttributeDefinition[] PARTECIPANT_ATTRIBUTE = new SimpleAttributeDefinition[] { LogStoreConstans.PARTECIPANT_JNDI_NAME };


    public static DescriptionProvider LOG_STORE_MODEL_CHILD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("logstore"));

            for (SimpleAttributeDefinition propertyType : LOG_STORE_ATTRIBUTE) {
                node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store." + propertyType.getName()));
                node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                node.get(ATTRIBUTES, propertyType.getName(), ACCESS_TYPE, READ_ONLY).set(true);
            }

            node.get(CHILDREN, LogStoreConstans.TRANSACTIONS, DESCRIPTION).set(bundle.getString("log-store.transaction"));

            return node;
        }
    };

    static DescriptionProvider ADD_LOG_STORE_MODEL_CHILD = new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(final Locale locale) {
                    final ResourceBundle bundle = getResourceBundle(locale);
                    final ModelNode operation = new ModelNode();
                    operation.get(OPERATION_NAME).set(ADD);
                    operation.get(DESCRIPTION).set(bundle.getString("log_store.add"));

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
                operation.get(DESCRIPTION).set(bundle.getString("log_store.remove"));
                return operation;
            }
    };

    static DescriptionProvider PROBE_OPERATION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(LogStoreConstans.PROBE);
            operation.get(DESCRIPTION).set(bundle.getString("log_store.probe"));
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
            node.get(CHILDREN, LogStoreConstans.PARTECIPANTS, DESCRIPTION).set(bundle.getString("log-store.partecipant"));

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
                node.get(DESCRIPTION).set(bundle.getString("log-store.partecipant"));
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_ATTRIBUTE) {
                    node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store.partecipant." + propertyType.getName()));
                    node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getType());
                    node.get(ATTRIBUTES, propertyType.getName(), ACCESS_TYPE, READ_ONLY).set(true);
                }
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_RW_ATTRIBUTE) {
                    node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString("log-store.partecipant." + propertyType.getName()));
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
                operation.get(DESCRIPTION).set(bundle.getString("log-store.partecipant.add"));
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_ATTRIBUTE) {
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                            bundle.getString("log-store.partecipant." + propertyType.getName()));
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getType());
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(!propertyType.isAllowNull());
                    if (propertyType.getDefaultValue() != null)
                        operation.get(REQUEST_PROPERTIES, propertyType.getName(), DEFAULT).set(propertyType.getDefaultValue().toString());
                }
                for (SimpleAttributeDefinition propertyType : PARTECIPANT_RW_ATTRIBUTE) {
                    operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                            bundle.getString("log-store.partecipant." + propertyType.getName()));
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
                operation.get(DESCRIPTION).set(bundle.getString("log-store.partecipant.remove"));
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
