/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATION_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.REPORT_DIRECTORY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_ELYTRON_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_PRINCIPAL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_FROM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_REQUIRED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_TO;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.connector.metadata.api.resourceadapter.WorkManagerSecurity;
import org.jboss.as.connector.metadata.resourceadapter.WorkManagerSecurityImpl;
import org.jboss.as.connector.util.ParserException;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.Activations;
import org.jboss.jca.common.api.metadata.resourceadapter.WorkManager;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.resourceadapter.WorkManagerImpl;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A ResourceAdapterParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ResourceAdapterParser extends CommonIronJacamarParser {
    /**
     * The bundle
     */
    private static final CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    public void parse(final XMLExtendedStreamReader reader, final ModelNode subsystemAddOperation, final List<ModelNode> list, ModelNode parentAddress) throws Exception {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    return;
                case START_ELEMENT: {
                    switch (Tag.forName(reader.getLocalName())) {
                        case RESOURCE_ADAPTERS:
                            parseResourceAdapters(reader, list, parentAddress);
                            break;
                        case REPORT_DIRECTORY:
                            parseReportDirectory(reader, subsystemAddOperation);
                            break;
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void parseResourceAdapters(final XMLExtendedStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Tag.forName(reader.getLocalName()) == Tag.RESOURCE_ADAPTERS) {
                        return;
                    } else {
                        if (Activations.Tag.forName(reader.getLocalName()) == Activations.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Activations.Tag.forName(reader.getLocalName())) {
                        case RESOURCE_ADAPTER: {
                            parseResourceAdapter(reader, list, parentAddress);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseResourceAdapter(final XMLExtendedStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        final ModelNode raAddress = parentAddress.clone();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        String archiveOrModuleName = null;
        HashMap<String, ModelNode> configPropertiesOperations = new HashMap<>();
        HashMap<String, ModelNode> connectionDefinitionsOperations = new HashMap<>();
        HashMap<String, HashMap<String, ModelNode>> cfConfigPropertiesOperations = new HashMap<>();

        HashMap<String, ModelNode> adminObjectsOperations = new HashMap<>();
        HashMap<String, HashMap<String, ModelNode>> aoConfigPropertiesOperations = new HashMap<>();


        boolean archiveOrModuleMatched = false;
        boolean txSupportMatched = false;
        boolean isXa = false;
        String id = null;

        int attributeSize = reader.getAttributeCount();

                for (int i = 0; i < attributeSize; i++) {
                    Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    String value = reader.getAttributeValue(i);
                    switch (attribute) {
                        case ID: {
                            id = value;
                            break;
                        }
                        case STATISTICS_ENABLED:
                            STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                            break;
                        default:
                            break;
                    }
                }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activations.Tag.forName(reader.getLocalName()) == Activations.Tag.RESOURCE_ADAPTER) {
                        if (!archiveOrModuleMatched) {
                            throw new ParserException(bundle.requiredElementMissing(ARCHIVE.getName(), RESOURCEADAPTER_NAME));

                        }

                        if (id != null) {
                            raAddress.add(RESOURCEADAPTER_NAME, id);
                        } else {
                            raAddress.add(RESOURCEADAPTER_NAME, archiveOrModuleName);
                        }

                        raAddress.protect();

                        operation.get(OP_ADDR).set(raAddress);
                        list.add(operation);

                        for (Map.Entry<String, ModelNode> entry : configPropertiesOperations.entrySet()) {
                            final ModelNode env = raAddress.clone();
                            env.add(CONFIG_PROPERTIES.getName(), entry.getKey());
                            env.protect();

                            entry.getValue().get(OP_ADDR).set(env);
                            list.add(entry.getValue());
                        }

                        for (Map.Entry<String, ModelNode> entry : connectionDefinitionsOperations.entrySet()) {
                            final ModelNode env = raAddress.clone();
                            env.add(CONNECTIONDEFINITIONS_NAME, entry.getKey());
                            env.protect();

                            entry.getValue().get(OP_ADDR).set(env);
                            list.add(entry.getValue());

                            final HashMap<String, ModelNode> properties = cfConfigPropertiesOperations.get(entry.getKey());
                            if (properties != null) {
                                for (Map.Entry<String, ModelNode> configEntry : properties.entrySet()) {
                                    final ModelNode configEnv = env.clone();
                                    configEnv.add(CONFIG_PROPERTIES.getName(), configEntry.getKey());
                                    configEnv.protect();

                                    configEntry.getValue().get(OP_ADDR).set(configEnv);
                                    list.add(configEntry.getValue());
                                }
                            }
                        }

                        for (Map.Entry<String, ModelNode> entry : adminObjectsOperations.entrySet()) {
                            final ModelNode env = raAddress.clone();
                            env.add(ADMIN_OBJECTS_NAME, entry.getKey());
                            env.protect();

                            entry.getValue().get(OP_ADDR).set(env);
                            list.add(entry.getValue());

                            final HashMap<String, ModelNode> aoProperties = aoConfigPropertiesOperations.get(entry.getKey());
                            if (aoProperties != null) {
                                for (Map.Entry<String, ModelNode> configEntry : aoProperties.entrySet()) {
                                    final ModelNode configEnv = env.clone();
                                    configEnv.add(CONFIG_PROPERTIES.getName(), configEntry.getKey());
                                    configEnv.protect();

                                    configEntry.getValue().get(OP_ADDR).set(configEnv);
                                    list.add(configEntry.getValue());
                                }
                            }
                        }


                        return;

                    } else {
                        if (AS7ResourceAdapterTags.forName(reader.getLocalName()) == AS7ResourceAdapterTags.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (AS7ResourceAdapterTags.forName(reader.getLocalName())) {
                        case ADMIN_OBJECTS:
                        case CONNECTION_DEFINITIONS:
                        case BEAN_VALIDATION_GROUPS: {
                            //ignore it,we will parse bean-validation-group,admin_object and connection_definition directly
                            break;
                        }
                        case ADMIN_OBJECT: {
                            parseAdminObjects(reader, adminObjectsOperations, aoConfigPropertiesOperations);
                            break;
                        }

                        case CONNECTION_DEFINITION: {
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                case RESOURCEADAPTERS_1_0:
                                case RESOURCEADAPTERS_1_1:
                                case RESOURCEADAPTERS_2_0:
                                    parseConnectionDefinitions_1_0(reader, connectionDefinitionsOperations, cfConfigPropertiesOperations, isXa);
                                    break;
                                case RESOURCEADAPTERS_3_0:
                                    parseConnectionDefinitions_3_0(reader, connectionDefinitionsOperations, cfConfigPropertiesOperations, isXa);
                                    break;
                                case RESOURCEADAPTERS_4_0:
                                    parseConnectionDefinitions_4_0(reader, connectionDefinitionsOperations, cfConfigPropertiesOperations, isXa);
                                    break;
                                default:
                                    parseConnectionDefinitions_5_0(reader, connectionDefinitionsOperations, cfConfigPropertiesOperations, isXa);
                                    break;
                            }
                            break;
                        }
                        case BEAN_VALIDATION_GROUP: {
                            String value = rawElementText(reader);
                            operation.get(BEANVALIDATION_GROUPS.getName()).add(parse(BEANVALIDATIONGROUP, value, reader));
                            break;
                        }
                        case BOOTSTRAP_CONTEXT: {
                            String value = rawElementText(reader);
                            BOOTSTRAP_CONTEXT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case CONFIG_PROPERTY: {
                            parseConfigProperties(reader, configPropertiesOperations);
                            break;

                        }
                        case TRANSACTION_SUPPORT: {
                            if (txSupportMatched) {
                                throw new ParserException(bundle.unexpectedElement(TRANSACTION_SUPPORT.getXmlName()));
                            }
                            String value = rawElementText(reader);
                            TRANSACTION_SUPPORT.parseAndSetParameter(value, operation, reader);
                            ModelNode transactionSupport = parse(TRANSACTION_SUPPORT, value, reader);
                            // so we need to know the transaction support level to give a chance to throw XMLStreamException for
                            // unexpectedElement when parsing connection-definitions in CommonIronJacamarParser ?
                            String transactionSupportResolved = transactionSupport.resolve().asString();
                            isXa = value != null && TransactionSupportEnum.valueOf(transactionSupportResolved) == TransactionSupportEnum.XATransaction;
                            txSupportMatched = true;
                            break;
                        }
                        case WORKMANAGER: {
                            parseWorkManager(operation, reader);
                            break;
                        }
                        case ARCHIVE: {
                            if (archiveOrModuleMatched) {
                                throw new ParserException(bundle.unexpectedElement(ARCHIVE.getXmlName()));
                            }
                            archiveOrModuleName = rawElementText(reader);
                            ARCHIVE.parseAndSetParameter(archiveOrModuleName, operation, reader);
                            archiveOrModuleMatched = true;
                            break;
                        }
                        case MODULE: {
                            if (archiveOrModuleMatched) {
                                throw new ParserException(bundle.unexpectedElement(MODULE.getXmlName()));
                            }

                            String moduleId = rawAttributeText(reader, "id");
                            String moduleSlot = rawAttributeText(reader, "slot", "main");
                            archiveOrModuleName = moduleId + ":" + moduleSlot;
                            MODULE.parseAndSetParameter(archiveOrModuleName, operation, reader);

                            archiveOrModuleMatched = true;
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }

        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    protected WorkManager parseWorkManager(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException, ParserException {
        WorkManagerSecurity security = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.WORKMANAGER) {
                        return new WorkManagerImpl(security);
                    } else {
                        if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (WorkManager.Tag.forName(reader.getLocalName())) {
                        case SECURITY: {
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                case RESOURCEADAPTERS_1_0:
                                case RESOURCEADAPTERS_1_1:
                                case RESOURCEADAPTERS_2_0:
                                case RESOURCEADAPTERS_3_0:
                                case RESOURCEADAPTERS_4_0:
                                    security = parseWorkManagerSecurity(operation, reader);
                                    break;
                                case RESOURCEADAPTERS_5_0:
                                case RESOURCEADAPTERS_6_0:
                                case RESOURCEADAPTERS_6_1:
                                    security = parseWorkManagerSecurity_5_0(operation, reader);
                                    break;
                                default: // If the switch statement contains a default it does not need to be revisited each time a new schema is added unless it actually affects it.
                                    security = parseWorkManagerSecurity_7_0(operation, reader);
                            }
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    protected WorkManagerSecurity parseWorkManagerSecurity(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException,
            ParserException {
        boolean mappingRequired = false;
        String domain = null;
        String defaultPrincipal = null;
        List<String> defaultGroups = null;
        Map<String, String> userMappings = null;
        Map<String, String> groupMappings = null;

        boolean userMappingEnabled = false;

        WM_SECURITY.parseAndSetParameter("true", operation, reader);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (WorkManager.Tag.forName(reader.getLocalName()) == WorkManager.Tag.SECURITY) {
                        return new WorkManagerSecurityImpl(mappingRequired, domain,false, defaultPrincipal, defaultGroups,
                                userMappings, groupMappings);
                    } else {
                        if (WorkManagerSecurity.Tag.forName(reader.getLocalName()) == WorkManagerSecurity.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (WorkManagerSecurity.Tag.forName(reader.getLocalName())) {
                        case DEFAULT_GROUPS:
                        case MAPPINGS: {
                            // Skip
                            break;
                        }
                        case MAPPING_REQUIRED: {
                            String value = rawElementText(reader);
                            WM_SECURITY_MAPPING_REQUIRED.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DOMAIN: {
                            String value = domain = rawElementText(reader);
                            WM_SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DEFAULT_PRINCIPAL: {
                            String value = rawElementText(reader);
                            WM_SECURITY_DEFAULT_PRINCIPAL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case GROUP: {
                            String value = rawElementText(reader);
                            operation.get(WM_SECURITY_DEFAULT_GROUPS.getName()).add(parse(WM_SECURITY_DEFAULT_GROUP, value, reader));
                            break;
                        }
                        case USERS: {
                            userMappingEnabled = true;
                            break;
                        }
                        case GROUPS: {
                            userMappingEnabled = false;
                            break;
                        }
                        case MAP: {
                            if (userMappingEnabled) {
                                String from = rawAttributeText(reader, WorkManagerSecurity.Attribute.FROM.getLocalName());
                                if (from == null || from.trim().equals(""))
                                    throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.FROM.getLocalName(), reader.getLocalName()));
                                String to = rawAttributeText(reader, WorkManagerSecurity.Attribute.TO.getLocalName());
                                if (to == null || to.trim().equals(""))
                                    throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.TO.getLocalName(), reader.getLocalName()));
                                ModelNode object = new ModelNode();
                                WM_SECURITY_MAPPING_FROM.parseAndSetParameter(from, object, reader);
                                WM_SECURITY_MAPPING_TO.parseAndSetParameter(to, object, reader);
                                operation.get(WM_SECURITY_MAPPING_USERS.getName()).add(object);
                            } else {
                                String from = rawAttributeText(reader, WorkManagerSecurity.Attribute.FROM.getLocalName());
                                if (from == null || from.trim().equals(""))
                                    throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.FROM.getLocalName(), reader.getLocalName()));
                                String to = rawAttributeText(reader, WorkManagerSecurity.Attribute.TO.getLocalName());
                                if (to == null || to.trim().equals(""))
                                    throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.TO.getLocalName(), reader.getLocalName()));
                                ModelNode object = new ModelNode();
                                WM_SECURITY_MAPPING_FROM.parseAndSetParameter(from, object, reader);
                                WM_SECURITY_MAPPING_TO.parseAndSetParameter(to, object, reader);
                                operation.get(WM_SECURITY_MAPPING_GROUPS.getName()).add(object);
                            }
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    protected WorkManagerSecurity parseWorkManagerSecurity_5_0(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException,
            ParserException {
        WM_SECURITY.parseAndSetParameter("true", operation, reader);
        return parseWorkManagerSecurityElements(operation, reader);
    }

    protected WorkManagerSecurity parseWorkManagerSecurity_7_0(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException,
            ParserException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    String value = reader.getAttributeValue(i);
                    WM_SECURITY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    throw new ParserException(bundle.unexpectedAttribute(reader.getAttributeLocalName(i), reader.getLocalName()));
            }
        }
        return parseWorkManagerSecurityElements(operation, reader);
    }

    /**
     * Parses the common elements of Work Manager Security of the Resource Adapter subsystem version 5.0, 6.0, 6.1 and 7.0
     */
    private WorkManagerSecurity parseWorkManagerSecurityElements(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException,
            ParserException {
        boolean mappingRequired = false;
        String domain = null;
        boolean elytronEnabled = false;
        boolean userMappingEnabled = false;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (WorkManager.Tag.forName(reader.getLocalName()) == WorkManager.Tag.SECURITY) {
                        return new WorkManagerSecurityImpl(mappingRequired, domain, elytronEnabled, null,
                                null, null, null);
                    } else {
                        if (WorkManagerSecurity.Tag.forName(reader.getLocalName()) == WorkManagerSecurity.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (WorkManagerSecurity.Tag.forName(reader.getLocalName())) {
                        case DEFAULT_GROUPS:
                        case MAPPINGS: {
                            // Skip
                            break;
                        }
                        case MAPPING_REQUIRED: {
                            String value = rawElementText(reader);
                            WM_SECURITY_MAPPING_REQUIRED.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DOMAIN: {
                            String value = domain = rawElementText(reader);
                            WM_SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case ELYTRON_SECURITY_DOMAIN: {
                            elytronEnabled = true;
                            String value = domain = rawElementText(reader);
                            WM_ELYTRON_SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DEFAULT_PRINCIPAL: {
                            String value = rawElementText(reader);
                            WM_SECURITY_DEFAULT_PRINCIPAL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case GROUP: {
                            String value = rawElementText(reader);
                            operation.get(WM_SECURITY_DEFAULT_GROUPS.getName()).add(parse(WM_SECURITY_DEFAULT_GROUP, value, reader));
                            break;
                        }
                        case USERS: {
                            userMappingEnabled = true;
                            break;
                        }
                        case GROUPS: {
                            userMappingEnabled = false;
                            break;
                        }
                        case MAP: {
                            String from = rawAttributeText(reader, WorkManagerSecurity.Attribute.FROM.getLocalName());
                            if (from == null || from.trim().equals(""))
                                throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.FROM.getLocalName(), reader.getLocalName()));
                            String to = rawAttributeText(reader, WorkManagerSecurity.Attribute.TO.getLocalName());
                            if (to == null || to.trim().equals(""))
                                throw new ParserException(bundle.requiredAttributeMissing(WorkManagerSecurity.Attribute.TO.getLocalName(), reader.getLocalName()));
                            ModelNode object = new ModelNode();
                            WM_SECURITY_MAPPING_FROM.parseAndSetParameter(from, object, reader);
                            WM_SECURITY_MAPPING_TO.parseAndSetParameter(to, object, reader);
                            if (userMappingEnabled) {
                                operation.get(WM_SECURITY_MAPPING_USERS.getName()).add(object);
                            } else {
                                operation.get(WM_SECURITY_MAPPING_GROUPS.getName()).add(object);
                            }
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    protected void parseReportDirectory(final XMLStreamReader reader, final ModelNode subsystemAddOperation) throws XMLStreamException,
            ParserException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            switch (attribute) {
                case PATH:
                    REPORT_DIRECTORY.parseAndSetParameter(value, subsystemAddOperation, reader);
                    break;
                default:
                    throw new ParserException(bundle.unexpectedAttribute(attribute.getLocalName(), reader.getLocalName()));
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    if (Tag.forName(reader.getLocalName()) == Tag.REPORT_DIRECTORY) {
                        return;
                    } else {
                        throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                    }
                default:
                    throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
            }
        }
    }

    private static ModelNode parse(AttributeDefinition ad, String value, XMLStreamReader reader) throws XMLStreamException {
        return ad.getParser().parse(ad, value, reader);
    }

    /**
     * A Tag.
     *
     * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
     */
    public enum Tag {
        /**
         * always first
         */
        UNKNOWN(null),

        /**
         * jboss-ra tag name
         */
        RESOURCE_ADAPTERS("resource-adapters"),

        REPORT_DIRECTORY("report-directory");

        private final String name;

        /**
         * Create a new Tag.
         *
         * @param name a name
         */
        Tag(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Tag> MAP;

        static {
            final Map<String, Tag> map = new HashMap<>();
            for (Tag element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }

        /**
         * Static method to get enum instance given localName string
         *
         * @param localName a string used as localname (typically tag name as defined in xsd)
         * @return the enum instance
         */
        public static Tag forName(String localName) {
            final Tag element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    /**
     * An Attribute.
     *
     * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
     */
    public enum Attribute {

        /**
         * always first
         */
        UNKNOWN(null),
        /**
         * id attribute
         */
        ID("id"),
        /**
         * statistics-enabled attribute
         */
        STATISTICS_ENABLED("statistics-enabled"),
        /**
         * path
         */
        PATH("path"),
        /**
         * enabled attribute
         */
        ENABLED("enabled");

        private String name;

        /**
         * Create a new Tag.
         *
         * @param name a name
         */
        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return name;
        }

        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<>();
            for (Attribute element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }

        /**
         * Set the value
         *
         * @param v The name
         * @return The value
         */
        Attribute value(String v) {
            name = v;
            return this;
        }

        /**
         * Static method to get enum instance given localName XsdString
         *
         * @param localName a XsdString used as localname (typically tag name as defined in xsd)
         * @return the enum instance
         */
        public static Attribute forName(String localName) {
            final Attribute element = MAP.get(localName);
            return element == null ? UNKNOWN.value(localName) : element;
        }

    }

}
