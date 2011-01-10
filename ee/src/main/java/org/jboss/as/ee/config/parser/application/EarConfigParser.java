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

package org.jboss.as.ee.config.parser.application;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.metadata.ear.spec.Ear13DTDMetaData;
import org.jboss.metadata.ear.spec.Ear14MetaData;
import org.jboss.metadata.ear.spec.Ear50MetaData;
import org.jboss.metadata.ear.spec.Ear5xMetaData;
import org.jboss.metadata.ear.spec.Ear60MetaData;
import org.jboss.metadata.ear.spec.Ear6xMetaData;
import org.jboss.metadata.ear.spec.EarEnvironmentRefsGroupMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.metadata.parser.ee.EnvironmentRefsGroupMetaDataParser;
import org.jboss.metadata.parser.ee.SecurityRoleMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;

/**
 * @author John Bailey
 */
public class EarConfigParser extends MetaDataElementParser {

    public static EarMetaData parse(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(START_DOCUMENT, null, null);

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
                if (reader.getAttributeNamespace(i) != null) {
                    continue;
                }
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                if (attribute == Attribute.VERSION) {
                    versionString = reader.getAttributeValue(i);
                }
            }
            if ("1.4".equals(versionString)) {
                version = Version.APP_1_4;
            } else if ("5".equals(versionString)) {
                version = Version.APP_5_0;
            } else if ("6".equals(versionString)) {
                version = Version.APP_6_0;
            }
        }

        if (version == null || Version.UNKNOWN.equals(version)) {
            version = Version.APP_6_0;
        }

        EarMetaData earMetaData = null;
        switch (version) {
            case APP_1_3: {
                earMetaData = new Ear13DTDMetaData();
                break;
            }
            case APP_1_4: {
                earMetaData = new Ear14MetaData();
                break;
            }
            case APP_5_0: {
                earMetaData = new Ear50MetaData();
                break;
            }
            case APP_6_0: {
                earMetaData = new Ear60MetaData();
                break;
            }
        }


        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    earMetaData.setId(value);
                    break;
                }
                case VERSION: {
                    earMetaData.setVersion(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        // Handler Attributes

        final DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        final SecurityRolesMetaData securityRolesMetaData = new SecurityRolesMetaData();
        final EarEnvironmentRefsGroupMetaData environmentRefsGroupMetaData = new EarEnvironmentRefsGroupMetaData();
        final ModulesMetaData modulesMetaData = new ModulesMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                continue;
            }
            if (EnvironmentRefsGroupMetaDataParser.parse(reader, environmentRefsGroupMetaData)) {
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case APPLICATION_NAME: {
                    if (earMetaData instanceof Ear6xMetaData) {
                        Ear6xMetaData.class.cast(earMetaData).setApplicationName(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                }
                case INITIALIZATION_IN_ORDER: {
                    if (earMetaData instanceof Ear6xMetaData) {
                        Ear6xMetaData.class.cast(earMetaData).setInitializeInOrder(Boolean.parseBoolean(reader.getElementText()));
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                }
                case LIBRARY_DIRECTORY: {
                    if (earMetaData instanceof Ear5xMetaData) {
                        Ear5xMetaData.class.cast(earMetaData).setLibraryDirectory(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                }
                case MODULE: {
                    modulesMetaData.add(EarModuleConfigParser.parse(reader));
                    break;
                }
                case SECURITY_ROLE: {
                    securityRolesMetaData.add(SecurityRoleMetaDataParser.parse(reader));
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        earMetaData.setDescriptionGroup(descriptionGroup);
        earMetaData.setModules(modulesMetaData);
        earMetaData.setSecurityRoles(securityRolesMetaData);
        if(earMetaData instanceof Ear6xMetaData) {
            Ear6xMetaData.class.cast(earMetaData).setEarEnvironmentRefsGroup(environmentRefsGroupMetaData);
        }
        return earMetaData;
    }
}
