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
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.EJBLocalReferenceMetaData;
import org.jboss.metadata.javaee.spec.EJBReferenceType;

/**
 * @author Remy Maucherat
 */
public class EJBLocalReferenceMetaDataParser extends MetaDataElementParser {

    public static EJBLocalReferenceMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        EJBLocalReferenceMetaData ejbReference = new EJBLocalReferenceMetaData();

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
                    ejbReference.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionsImpl descriptions = new DescriptionsImpl();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionsMetaDataParser.parse(reader, descriptions)) {
                if (ejbReference.getDescriptions() == null) {
                    ejbReference.setDescriptions(descriptions);
                }
                continue;
            }
            if (ResourceInjectionMetaDataParser.parse(reader, ejbReference)) {
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case EJB_REF_NAME:
                    ejbReference.setEjbRefName(reader.getElementText());
                    break;
                case EJB_REF_TYPE:
                    ejbReference.setEjbRefType(EJBReferenceType.valueOf(reader.getElementText()));
                    break;
                case LOCAL_HOME:
                    ejbReference.setLocalHome(reader.getElementText());
                    break;
                case LOCAL:
                    ejbReference.setLocal(reader.getElementText());
                    break;
                case LOCAL_JNDI_NAME:
                    ejbReference.setLocalJndiName(reader.getElementText());
                    break;
                case EJB_LINK:
                    ejbReference.setLink(reader.getElementText());
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return ejbReference;
    }

}
