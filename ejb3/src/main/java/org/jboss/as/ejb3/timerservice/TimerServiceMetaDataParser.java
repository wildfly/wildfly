/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for timer service EJB meta data.
 * @author Stuart Douglas
 * @author Paul Ferraro
 */
public class TimerServiceMetaDataParser extends AbstractEJBBoundMetaDataParser<TimerServiceMetaData> {

    private final TimerServiceMetaDataSchema schema;

    public TimerServiceMetaDataParser(TimerServiceMetaDataSchema schema) {
        this.schema = schema;
    }

    @Override
    public TimerServiceMetaData parse(XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (this.schema != TimerServiceMetaDataSchema.CURRENT) {
            EjbLogger.ROOT_LOGGER.deprecatedNamespace(reader.getNamespaceURI(), reader.getLocalName());
        }
        TimerServiceMetaData metaData = new TimerServiceMetaData();
        processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(TimerServiceMetaData metaData, XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (this.schema.getNamespace().getUri().equals(reader.getNamespaceURI())) {
            switch (reader.getLocalName()) {
                case "persistence-store-name":
                    metaData.setDataStoreName(getElementText(reader, propertyReplacer));
                    break;
                case "persistent-timer-management":
                    if (this.schema.since(TimerServiceMetaDataSchema.VERSION_2_0)) {
                        metaData.setPersistentTimerManagementProvider(getElementText(reader, propertyReplacer));
                        break;
                    }
                case "transient-timer-management":
                    if (this.schema.since(TimerServiceMetaDataSchema.VERSION_2_0)) {
                        metaData.setTransientTimerManagementProvider(getElementText(reader, propertyReplacer));
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
