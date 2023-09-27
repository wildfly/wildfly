/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deliveryactive.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.deliveryactive.metadata.EJBBoundMdbDeliveryMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for EJBBoundMdbDeliveryMetaData components, namespace delivery-active:1.1
 *
 * @author Flavia Rainone
 */
public class EJBBoundMdbDeliveryMetaDataParser11 extends AbstractEJBBoundMetaDataParser<EJBBoundMdbDeliveryMetaData> {

    public static final String NAMESPACE_URI_1_1 = "urn:delivery-active:1.1";

    private static final String ROOT_ELEMENT_DELIVERY = "delivery";
    private static final String ACTIVE = "active";
    private static final String GROUP = "group";

    public static final EJBBoundMdbDeliveryMetaDataParser11 INSTANCE = new EJBBoundMdbDeliveryMetaDataParser11();

    private EJBBoundMdbDeliveryMetaDataParser11() {}

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
        if (NAMESPACE_URI_1_1.equals(namespaceURI)) {
            switch (localName) {
                case ACTIVE:
                    final String val = getElementText(reader, propertyReplacer);
                    metaData.setDeliveryActive(Boolean.parseBoolean(val.trim()));
                    break;
                case GROUP:
                    metaData.setDeliveryGroups(getElementText(reader, propertyReplacer).trim());
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}
