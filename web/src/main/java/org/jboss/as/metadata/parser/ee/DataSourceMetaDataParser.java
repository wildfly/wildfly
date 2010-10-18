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
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.IsolationLevelType;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;

/**
 * @author Remy Maucherat
 */
public class DataSourceMetaDataParser extends MetaDataElementParser {

    public static DataSourceMetaData parse(XMLStreamReader reader) throws XMLStreamException {
        DataSourceMetaData dataSource = new DataSourceMetaData();

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
                    dataSource.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionsImpl descriptions = new DescriptionsImpl();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionsMetaDataParser.parse(reader, descriptions)) {
                if (dataSource.getDescriptions() == null) {
                    dataSource.setDescriptions(descriptions);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    dataSource.setName(reader.getElementText());
                    break;
                case CLASS_NAME:
                    dataSource.setClassName(reader.getElementText());
                    break;
                case SERVER_NAME:
                    dataSource.setServerName(reader.getElementText());
                    break;
                case PORT_NUMBER:
                    dataSource.setPortNumber(Integer.valueOf(reader.getElementText()));
                    break;
                case DATABASE_NAME:
                    dataSource.setDatabaseName(reader.getElementText());
                    break;
                case URL:
                    dataSource.setUrl(reader.getElementText());
                    break;
                case USER:
                    dataSource.setUser(reader.getElementText());
                    break;
                case PASSWORD:
                    dataSource.setPassword(reader.getElementText());
                    break;
                case PROPERTY:
                    PropertiesMetaData properties = dataSource.getProperties();
                    if (properties == null) {
                        properties = new PropertiesMetaData();
                        dataSource.setProperties(properties);
                    }
                    properties.add(PropertyMetaDataParser.parse(reader));
                    break;
                case LOGIN_TIMEOUT:
                    dataSource.setLoginTimeout(Integer.valueOf(reader.getElementText()));
                    break;
                case TRANSACTIONAL:
                    dataSource.setTransactional(Boolean.valueOf(reader.getElementText()));
                    break;
                case ISOLATION_LEVEL:
                    dataSource.setIsolationLevel(IsolationLevelType.valueOf(reader.getElementText()));
                    break;
                case INITAL_POOL_SIZE:
                    dataSource.setInitialPoolSize(Integer.valueOf(reader.getElementText()));
                    break;
                case MAX_POOL_SIZE:
                    dataSource.setMaxPoolSize(Integer.valueOf(reader.getElementText()));
                    break;
                case MIN_POOL_SIZE:
                    dataSource.setMinPoolSize(Integer.valueOf(reader.getElementText()));
                    break;
                case MAX_IDLE_TIME:
                    dataSource.setMaxIdleTime(Integer.valueOf(reader.getElementText()));
                    break;
                case MAX_STATEMENTS:
                    dataSource.setMaxStatements(Integer.valueOf(reader.getElementText()));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return dataSource;
    }

}
