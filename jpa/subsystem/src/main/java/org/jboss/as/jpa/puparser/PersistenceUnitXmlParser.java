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

package org.jboss.as.jpa.puparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataImpl;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.property.PropertyReplacer;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

/**
 * Parse a persistence.xml into a list of persistence unit definitions.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitXmlParser extends MetaDataElementParser {
    // cache the trace enabled flag
    private static final boolean traceEnabled = ROOT_LOGGER.isTraceEnabled();

    public static PersistenceUnitMetadataHolder parse(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {

        reader.require(START_DOCUMENT, null, null);  // check for a bogus document and throw error

        // Read until the first start element
        Version version = null;
        while (reader.hasNext() && reader.next() != START_ELEMENT) {
            if (reader.getEventType() == DTD) {
                final String dtdLocation = readDTDLocation(reader);
                if (dtdLocation != null) {
                    version = Version.forLocation(dtdLocation);
                }
            }
        }
        final String schemaLocation = readSchemaLocation(reader);
        if (schemaLocation != null) {
            version = Version.forLocation(schemaLocation);
        }
        if (version == null || Version.UNKNOWN.equals(version)) {
            // Look at the version attribute
            String versionString = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String attributeNamespace = reader.getAttributeNamespace(i);
                if (attributeNamespace != null && !attributeNamespace.isEmpty()) {
                    continue;
                }
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                if (attribute == Attribute.VERSION) {
                    versionString = reader.getAttributeValue(i);
                }
            }
            if ("1.0".equals(versionString)) {
                version = Version.JPA_1_0;
            } else if ("1".equals(versionString)) {
                version = Version.JPA_1_0;
            } else if ("2.0".equals(versionString)) {
                version = Version.JPA_2_0;
            } else if ("2.1".equals(versionString)) {
                version = Version.JPA_2_1;
            } else if ("2.2".equals(versionString)) {
                version = Version.JPA_2_2;
            } else if ("2".equals(versionString)) {
                version = Version.JPA_2_0;
            } else {
                version = Version.JPA_2_1;
            }
        }

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attributeNamespace = reader.getAttributeNamespace(i);
            if (attributeNamespace != null && !attributeNamespace.isEmpty()) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VERSION:
                    // log.info("version = " + value);
                    // TODO:  handle version
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        final List<PersistenceUnitMetadata> PUs = new ArrayList<PersistenceUnitMetadata>();
        // until the ending PERSISTENCE tag
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PERSISTENCEUNIT:
                    PersistenceUnitMetadata pu = parsePU(reader, version, propertyReplacer);
                    PUs.add(pu);
                    ROOT_LOGGER.readingPersistenceXml(pu.getPersistenceUnitName());
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
        PersistenceUnitMetadataHolder result = new PersistenceUnitMetadataHolder(PUs);
        if (ROOT_LOGGER.isTraceEnabled())
            ROOT_LOGGER.trace(result.toString());

        return result;
    }

    /**
     * Parse the persistence unit definitions based on persistence_2_0.xsd.
     *
     *
     * @param reader
     * @param propertyReplacer
     * @return
     * @throws XMLStreamException
     */
    private static PersistenceUnitMetadata parsePU(XMLStreamReader reader, Version version, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        PersistenceUnitMetadata pu = new PersistenceUnitMetadataImpl();
        List<String> classes = new ArrayList<String>(1);
        List<String> jarFiles = new ArrayList<String>(1);
        List<String> mappingFiles = new ArrayList<String>(1);
        Properties properties = new Properties();

        // set defaults
        pu.setTransactionType(PersistenceUnitTransactionType.JTA);
        pu.setValidationMode(ValidationMode.AUTO);
        pu.setSharedCacheMode(SharedCacheMode.UNSPECIFIED);
        pu.setPersistenceProviderClassName(Configuration.PROVIDER_CLASS_DEFAULT);
        if (version.equals(Version.JPA_1_0)) {
            pu.setPersistenceXMLSchemaVersion("1.0");
        } else if (version.equals(Version.JPA_2_0)) {
            pu.setPersistenceXMLSchemaVersion("2.0");
        } else if (version.equals(Version.JPA_2_1)) {
            pu.setPersistenceXMLSchemaVersion("2.1");
        } else {
            pu.setPersistenceXMLSchemaVersion("2.2");
        }

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (traceEnabled) {
                ROOT_LOGGER.tracef("parse persistence.xml: attribute value(%d) = %s", i, value);
            }
            final String attributeNamespace = reader.getAttributeNamespace(i);
            if (attributeNamespace != null && !attributeNamespace.isEmpty()) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    pu.setPersistenceUnitName(value);
                    break;
                case TRANSACTIONTYPE:
                    if (value.equalsIgnoreCase("RESOURCE_LOCAL"))
                        pu.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        // until the ending PERSISTENCEUNIT tag
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (traceEnabled) {
                ROOT_LOGGER.tracef("parse persistence.xml: element=%s", element.getLocalName());
            }
            switch (element) {
                case CLASS:
                    classes.add(getElement(reader, propertyReplacer));
                    break;

                case DESCRIPTION:
                    final String description = getElement(reader, propertyReplacer);
                    break;

                case EXCLUDEUNLISTEDCLASSES:
                    String text = getElement(reader, propertyReplacer);
                    if (text == null || text.isEmpty()) {
                        //the spec has examples where an empty
                        //exclude-unlisted-classes element has the same
                        //effect as setting it to true
                        pu.setExcludeUnlistedClasses(true);
                    } else {
                        pu.setExcludeUnlistedClasses(Boolean.valueOf(text));
                    }
                    break;

                case JARFILE:
                    String file = getElement(reader, propertyReplacer);
                    jarFiles.add(file);
                    break;

                case JTADATASOURCE:
                    pu.setJtaDataSourceName(getElement(reader, propertyReplacer));
                    break;

                case NONJTADATASOURCE:
                    pu.setNonJtaDataSourceName(getElement(reader, propertyReplacer));
                    break;

                case MAPPINGFILE:
                    mappingFiles.add(getElement(reader, propertyReplacer));
                    break;

                case PROPERTIES:
                    parseProperties(reader, properties, propertyReplacer);
                    break;

                case PROVIDER:
                    pu.setPersistenceProviderClassName(getElement(reader, propertyReplacer));
                    break;

                case SHAREDCACHEMODE:
                    String cm = getElement(reader, propertyReplacer);
                    pu.setSharedCacheMode(SharedCacheMode.valueOf(cm));
                    break;

                case VALIDATIONMODE:
                    String validationMode = getElement(reader, propertyReplacer);
                    pu.setValidationMode(ValidationMode.valueOf(validationMode));
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
        if (traceEnabled) {
            ROOT_LOGGER.trace("parse persistence.xml: reached ending persistence-unit tag");
        }
        pu.setManagedClassNames(classes);
        pu.setJarFiles(jarFiles);
        pu.setMappingFiles(mappingFiles);
        pu.setProperties(properties);
        return pu;
    }

    private static void parseProperties(XMLStreamReader reader, Properties properties, final PropertyReplacer propertyReplacer) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final int count = reader.getAttributeCount();
                    String name = null, value = null;
                    for (int i = 0; i < count; i++) {
                        final String attributeValue = getAttribute(reader, i, propertyReplacer);
                        final String attributeNamespace = reader.getAttributeNamespace(i);
                        if (attributeNamespace != null && !attributeNamespace.isEmpty()) {
                            continue;
                        }
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME:
                                name = attributeValue;
                                break;
                            case VALUE:
                                value = attributeValue;
                                if (name != null && value != null) {
                                    properties.put(name, value);
                                }
                                name = value = null;
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (reader.hasNext() && (reader.nextTag() != END_ELEMENT))
                        throw unexpectedElement(reader);

                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private static String getElement(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        return propertyReplacer.replaceProperties(reader.getElementText());
    }

    private static String getAttribute(final XMLStreamReader reader, int i, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        return propertyReplacer.replaceProperties(reader.getAttributeValue(i));
    }
}
