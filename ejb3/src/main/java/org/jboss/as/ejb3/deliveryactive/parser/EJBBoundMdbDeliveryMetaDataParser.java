/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deliveryactive.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.deliveryactive.metadata.EJBBoundMdbDeliveryMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundMdbDeliveryMetaData components.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Flavia Rainone
 */
public class EJBBoundMdbDeliveryMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundMdbDeliveryMetaData> {

    public static final String NAMESPACE_URI_1_0 = "urn:delivery-active:1.0";

    private static final String ROOT_ELEMENT_DELIVERY = "delivery";
    private static final String ACTIVE = "active";

    public static final EJBBoundMdbDeliveryMetaDataParser INSTANCE = new EJBBoundMdbDeliveryMetaDataParser();

    private EJBBoundMdbDeliveryMetaDataParser() {

    }

    @Override
    public EJBBoundMdbDeliveryMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        // we only parse <delivery> (root) element
        if (!ROOT_ELEMENT_DELIVERY.equals(reader.getLocalName())) {
            throw unexpectedElement(reader);
        }
        EJBBoundMdbDeliveryMetaData metaData = new EJBBoundMdbDeliveryMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(EJBBoundMdbDeliveryMetaData metaData, XMLStreamReader reader,  final PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String namespaceURI = reader.getNamespaceURI();
        final String localName = reader.getLocalName();
        if (NAMESPACE_URI_1_0.equals(namespaceURI)) {
            if (ACTIVE.equals(localName)) {
                final String val = getElementText(reader, propertyReplacer);
                metaData.setDeliveryActive(Boolean.parseBoolean(val.trim()));
            } else {
                throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}
