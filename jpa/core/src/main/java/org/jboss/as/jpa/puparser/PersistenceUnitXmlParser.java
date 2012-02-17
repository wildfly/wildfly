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

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataImpl;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.metadata.parser.util.MetaDataElementParser;

/**
 * Parse a persistence.xml into a list of persistence unit definitions.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitXmlParser extends MetaDataElementParser {
    // cache the trace enabled flag
    private static final boolean traceEnabled = JPA_LOGGER.isTraceEnabled();

    public static PersistenceUnitMetadataHolder parse(final XMLStreamReader reader) throws XMLStreamException {

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
            } else if ("2".equals(versionString)) {
                version = Version.JPA_2_0;
            } else {
                version = Version.JPA_2_0;
            }
        }

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
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
                    PersistenceUnitMetadata pu = parsePU(reader, version);
                    PUs.add(pu);
                    JPA_LOGGER.readingPersistenceXml(pu.getPersistenceUnitName());
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
        PersistenceUnitMetadataHolder result = new PersistenceUnitMetadataHolder().setPersistenceUnits(PUs);
        if (JPA_LOGGER.isTraceEnabled())
            JPA_LOGGER.trace(result.toString());

        return result;
    }

    /**
     * Parse the persistence unit definitions based on persistence_2_0.xsd.
     *
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private static PersistenceUnitMetadata parsePU(XMLStreamReader reader, Version version) throws XMLStreamException {
        PersistenceUnitMetadata pu = new PersistenceUnitMetadataImpl();
        List<String> classes = new ArrayList<String>(1);
        List<String> jarFiles = new ArrayList<String>(1);
        List<String> mappingFiles = new ArrayList<String>(1);
        Properties properties = new Properties();

        // set defaults
        pu.setTransactionType(PersistenceUnitTransactionType.JTA);
        pu.setValidationMode(ValidationMode.AUTO);
        pu.setSharedCacheMode(SharedCacheMode.UNSPECIFIED);
        pu.setPersistenceProviderClassName("org.hibernate.ejb.HibernatePersistence");  // TODO: move to domain.xml?
        if (version.equals(Version.JPA_1_0)) {
            pu.setPersistenceXMLSchemaVersion("1.0");
        } else {
            pu.setPersistenceXMLSchemaVersion("2.0");
        }

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (traceEnabled) {
                JPA_LOGGER.tracef("parse persistence.xml: attribute value(%d) = %s", i, value);
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
                JPA_LOGGER.tracef("parse persistence.xml: element=%s", element.getLocalName());
            }
            switch (element) {
                case CLASS:
                    classes.add(reader.getElementText());
                    break;

                case DESCRIPTION:
                    final String description = reader.getElementText();
                    break;

                case EXCLUDEUNLISTEDCLASSES:
                    String text = reader.getElementText();
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
                    String file = reader.getElementText();
                    jarFiles.add(file);
                    break;

                case JTADATASOURCE:
                    pu.setJtaDataSourceName(reader.getElementText());
                    break;

                case NONJTADATASOURCE:
                    pu.setNonJtaDataSourceName(reader.getElementText());
                    break;

                case MAPPINGFILE:
                    mappingFiles.add(reader.getElementText());
                    break;

                case PROPERTIES:
                    parseProperties(reader, properties);
                    break;

                case PROVIDER:
                    pu.setPersistenceProviderClassName(reader.getElementText());
                    break;

                case SHAREDCACHEMODE:
                    String cm = reader.getElementText();
                    pu.setSharedCacheMode(SharedCacheMode.valueOf(cm));
                    break;

                case VALIDATIONMODE:
                    String validationMode = reader.getElementText();
                    pu.setValidationMode(ValidationMode.valueOf(validationMode));
                    break;

                default:
                    throw unexpectedElement(reader);
            }
        }
        if (traceEnabled) {
            JPA_LOGGER.trace("parse persistence.xml: reached ending persistence-unit tag");
        }
        pu.setManagedClassNames(classes);
        pu.setJarFiles(jarFiles);
        pu.setMappingFiles(mappingFiles);
        pu.setProperties(properties);
        return pu;
    }

    private static void parseProperties(XMLStreamReader reader, Properties properties) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY:
                    final int count = reader.getAttributeCount();
                    String name = null, value = null;
                    for (int i = 0; i < count; i++) {
                        final String attributeValue = reader.getAttributeValue(i);
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

    /**
     * Simple test driver for parsing the specified persistence.xml file
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            String filename;
            if (args.length < 1) {
                filename = "persistence.xml";
            } else
                filename = args[0];
            System.out.println("will parse " + filename);
            XMLInputFactory xmlif = XMLInputFactory.newInstance();

            XMLStreamReader reader =
                xmlif.createXMLStreamReader(filename, new
                    FileInputStream(filename));

            PersistenceUnitMetadataHolder h = parse(reader);
            System.out.println("result = " + h.getPersistenceUnits());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
