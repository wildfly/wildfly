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

import org.jboss.as.connector.util.ParserException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.v11.WorkManager;
import org.jboss.jca.common.api.metadata.common.v11.WorkManagerSecurity;
import org.jboss.jca.common.api.metadata.ironjacamar.v11.IronJacamar;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.v11.WorkManagerImpl;
import org.jboss.jca.common.metadata.common.v11.WorkManagerSecurityImpl;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
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

/**
 * A ResourceAdapterParserr.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ResourceAdapterParser extends CommonIronJacamarParser {
    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    public void parse(final XMLExtendedStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws Exception {

        ResourceAdapters adapters = null;


        //iterate over tags
        int iterate;
        try {
            iterate = reader.nextTag();
        } catch (XMLStreamException e) {
            //founding a non tag..go on. Normally non-tag found at beginning are comments or DTD declaration
            iterate = reader.nextTag();
        }
        switch (iterate) {
            case END_ELEMENT: {
                // should mean we're done, so ignore it.
                break;
            }
            case START_ELEMENT: {

                switch (Tag.forName(reader.getLocalName())) {
                    case RESOURCE_ADAPTERS: {
                        parseResourceAdapters(reader, list, parentAddress);
                        break;
                    }
                    default:
                        throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                }

                break;
            }
            default:
                throw new IllegalStateException();
        }

        return;

    }

    private void parseResourceAdapters(final XMLExtendedStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Tag.forName(reader.getLocalName()) == Tag.RESOURCE_ADAPTERS) {
                        return;
                    } else {
                        if (ResourceAdapters.Tag.forName(reader.getLocalName()) == ResourceAdapters.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (ResourceAdapters.Tag.forName(reader.getLocalName())) {
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
        HashMap<String, ModelNode> configPropertiesOperations = new HashMap<String, ModelNode>();
        HashMap<String, ModelNode> connectionDefinitionsOperations = new HashMap<String, ModelNode>();
        HashMap<String, HashMap<String, ModelNode>> cfConfigPropertiesOperations = new HashMap<String, HashMap<String, ModelNode>>();

        HashMap<String, ModelNode> adminObjectsOperations = new HashMap<String, ModelNode>();
        HashMap<String, HashMap<String, ModelNode>> aoConfigPropertiesOperations = new HashMap<String, HashMap<String, ModelNode>>();


        boolean archiveOrModuleMatched = false;
        boolean txSupportMatched = false;
        boolean isXa = false;
        boolean isModule = false;
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
                        default:
                            break;
                    }
                }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ResourceAdapters.Tag.forName(reader.getLocalName()) == ResourceAdapters.Tag.RESOURCE_ADAPTER) {
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


                        if (isModule) {
                            final ModelNode activateOp = new ModelNode();
                            activateOp.get(OP).set(Constants.ACTIVATE);
                            activateOp.get(OP_ADDR).set(raAddress);
                            list.add(activateOp);
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
                            parseConnectionDefinitions(reader, connectionDefinitionsOperations, cfConfigPropertiesOperations, isXa);
                            break;
                        }
                        case BEAN_VALIDATION_GROUP: {
                            String value = rawElementText(reader);
                            operation.get(BEANVALIDATION_GROUPS.getName()).add(BEANVALIDATIONGROUP.parse(value, reader));
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
                            isXa = value != null && TransactionSupportEnum.valueOf(value) == TransactionSupportEnum.XATransaction;
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
                            isModule = true;

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

    protected WorkManager parseWorkManager(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException, ParserException,
            ValidateException {
        WorkManagerSecurity security = null;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (IronJacamar.Tag.forName(reader.getLocalName()) == IronJacamar.Tag.WORKMANAGER) {
                        return new WorkManagerImpl(security);
                    } else {
                        if (IronJacamar.Tag.forName(reader.getLocalName()) == IronJacamar.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (WorkManager.Tag.forName(reader.getLocalName())) {
                        case SECURITY: {
                            WM_SECURITY.parseAndSetParameter("true", operation, reader);
                            security = parseWorkManagerSecurity(operation, reader);
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

    protected WorkManagerSecurity parseWorkManagerSecurity(final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException,
            ParserException, ValidateException {
        boolean mappingRequired = false;
        String domain = null;
        String defaultPrincipal = null;
        List<String> defaultGroups = null;
        Map<String, String> userMappings = null;
        Map<String, String> groupMappings = null;

        boolean userMappingEnabled = false;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (WorkManager.Tag.forName(reader.getLocalName()) == WorkManager.Tag.SECURITY) {
                        return new WorkManagerSecurityImpl(mappingRequired, domain,
                                defaultPrincipal, defaultGroups,
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
                            String value = rawElementText(reader);
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
                            operation.get(WM_SECURITY_DEFAULT_GROUPS.getName()).add(WM_SECURITY_DEFAULT_GROUP.parse(value, reader));
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
        RESOURCE_ADAPTERS("resource-adapters");

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
            final Map<String, Tag> map = new HashMap<String, Tag>();
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
        ID("id"),;

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
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
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
