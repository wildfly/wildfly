/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.clustering.controller.Schema;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * Parser for timer service EJB meta data.
 * @author Stuart Douglas
 * @author Paul Ferraro
 */
public class TimerServiceMetaDataParser extends AbstractEJBBoundMetaDataParser<TimerServiceMetaData> {

    private final Schema<TimerServiceMetaDataSchema> schema;

    public TimerServiceMetaDataParser(Schema<TimerServiceMetaDataSchema> schema) {
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
        if (this.schema.getNamespaceUri().equals(reader.getNamespaceURI())) {
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
