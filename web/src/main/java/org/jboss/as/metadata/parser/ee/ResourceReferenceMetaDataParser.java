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

package org.jboss.as.metadata.parser.ee;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.EJBReferenceMetaData;
import org.jboss.metadata.javaee.spec.EJBReferenceType;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;
import org.jboss.metadata.javaee.spec.ResourceAuthorityType;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceSharingScopeType;

/**
 * @author Remy Maucherat
 */
public class ResourceReferenceMetaDataParser extends MetaDataElementParser {

    public static ResourceReferenceMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        ResourceReferenceMetaData resourceReference = new ResourceReferenceMetaData();

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
                    resourceReference.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionsImpl descriptions = new DescriptionsImpl();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionsMetaDataParser.parse(reader, descriptions)) {
                if (resourceReference.getDescriptions() == null) {
                    resourceReference.setDescriptions(descriptions);
                }
                continue;
            }
            if (ResourceInjectionMetaDataParser.parse(reader, resourceReference)) {
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case RES_REF_NAME:
                    resourceReference.setResourceRefName(reader.getElementText());
                    break;
                case RES_TYPE:
                    resourceReference.setType(reader.getElementText());
                    break;
                case RES_AUTH:
                    resourceReference.setResAuth(ResourceAuthorityType.valueOf(reader.getElementText()));
                    break;
                case RES_SHARING_SCOPE:
                    resourceReference.setResSharingScope(ResourceSharingScopeType.valueOf(reader.getElementText()));
                    break;
                case RESOURCE_NAME:
                    resourceReference.setResourceName(reader.getElementText());
                    break;
                case RES_URL:
                    resourceReference.setResUrl(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return resourceReference;
    }

}
