/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.deliveryactive.metadata.EJBBoundMdbDeliveryMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundMdbDeliveryMetaData components, namespace delivery-active:1.2
 *
 * @author Flavia Rainone
 */
public class EJBBoundMdbDeliveryMetaDataParser20 extends AbstractEJBBoundMetaDataParser<EJBBoundMdbDeliveryMetaData> {

    public static final String NAMESPACE_URI_2_0 = "urn:delivery-active:2.0";

    private static final String ROOT_ELEMENT_DELIVERY = "delivery";
    private static final String ACTIVE = "active";
    private static final String GROUP = "group";

    public static final EJBBoundMdbDeliveryMetaDataParser20 INSTANCE = new EJBBoundMdbDeliveryMetaDataParser20();

    private EJBBoundMdbDeliveryMetaDataParser20() {}

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
        if (NAMESPACE_URI_2_0.equals(namespaceURI)) {
            switch (localName) {
                case ACTIVE:
                    final String val = getElementText(reader, propertyReplacer);
                    metaData.setDeliveryActive(Boolean.parseBoolean(val.trim()));
                    break;
                case GROUP:
                    final String deliveryGroup = getElementText(reader, propertyReplacer).trim();
                    if (metaData.getDeliveryGroups() == null) {
                        metaData.setDeliveryGroups(deliveryGroup);
                    } else {
                        final List<String> deliveryGroups = new ArrayList<String>(metaData.getDeliveryGroups().length + 1);
                        Collections.addAll(deliveryGroups, metaData.getDeliveryGroups());
                        deliveryGroups.add(deliveryGroup);
                        metaData.setDeliveryGroups(deliveryGroups.toArray(new String[deliveryGroups.size()]));
                    }
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}
