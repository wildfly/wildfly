/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.delivery.parser;

import org.jboss.as.ejb3.delivery.metadata.EJBBoundMdbDeliveryMetaData;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for EJBBoundMdbDeliveryMetaData components, namespace delivery:3.0
 */
public class EJBBoundMdbDeliveryMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundMdbDeliveryMetaData> {

    private static final String ROOT_ELEMENT_DELIVERY = "delivery";
    private static final String ACTIVE = "active";
    private static final String GROUP = "group";

    private final EjbBoundMdbDeliveryMetaDataSchema schema;

    public EJBBoundMdbDeliveryMetaDataParser(EjbBoundMdbDeliveryMetaDataSchema schema) {
        this.schema = schema;
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
        if (schema.getNamespace().getUri().equals(reader.getNamespaceURI())) {
            final String localName = reader.getLocalName();
            switch (localName) {
                case ACTIVE:
                    final String val = getElementText(reader, propertyReplacer);
                    metaData.setDeliveryActive(Boolean.parseBoolean(val.trim()));
                    break;
                case GROUP:
                    if (schema.since(EjbBoundMdbDeliveryMetaDataSchema.VERSION_1_2)) {
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
                    } else if (schema.since(EjbBoundMdbDeliveryMetaDataSchema.VERSION_1_1)) {
                        metaData.setDeliveryGroups(getElementText(reader, propertyReplacer).trim());
                        break;
                    }
                default:
                    throw unexpectedElement(reader);
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }

}
