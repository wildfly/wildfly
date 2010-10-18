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

package org.jboss.as.metadata.parser.servlet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;

/**
 * @author Remy Maucherat
 */
public class SecurityConstraintMetaDataParser extends MetaDataElementParser {

    public static SecurityConstraintMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        SecurityConstraintMetaData securityConstraint = new SecurityConstraintMetaData();

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    securityConstraint.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DISPLAY_NAME:
                    securityConstraint.setDisplayName(reader.getElementText());
                    break;
                case WEB_RESOURCE_COLLECTION:
                    WebResourceCollectionsMetaData resourceCollections = securityConstraint.getResourceCollections();
                    if (resourceCollections == null) {
                        resourceCollections = new WebResourceCollectionsMetaData();
                        securityConstraint.setResourceCollections(resourceCollections);
                    }
                    resourceCollections.add(WebResourceCollectionMetaDataParser.parse(reader));
                    break;
                case AUTH_CONSTRAINT:
                    securityConstraint.setAuthConstraint(AuthConstraintMetaDataParser.parse(reader));
                    break;
                case USER_DATA_CONSTRAINT:
                    securityConstraint.setUserDataConstraint(UserDataConstraintMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return securityConstraint;
    }

}
