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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.ParserException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

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
                            Integer identifier = ConnectorServices.getResourceIdentifier(archiveOrModuleName);
                            if (identifier != null && identifier != 0) {
                                archiveOrModuleName = archiveOrModuleName + ConnectorServices.RA_SERVICE_NAME_SEPARATOR + identifier;
                            }

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
     * A Attribute.
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
