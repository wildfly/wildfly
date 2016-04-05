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

package org.jboss.as.ejb3.clustering;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses the urn:clustering namespace elements for clustering related metadata on EJBs.
 *
 * @author Jaikiran Pai
 * @author Flavia Rainone
 */
public class EJBBoundClusteringMetaDataParser extends AbstractEJBBoundMetaDataParser<EJBBoundClusteringMetaData> {

    private final ClusteringSchema schema;

    public EJBBoundClusteringMetaDataParser(ClusteringSchema schema) {
        this.schema = schema;
    }

    @Override
    public EJBBoundClusteringMetaData parse(final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        // we only parse <clustering> (root) element
        if (!reader.getLocalName().equals("clustering")) {
            throw unexpectedElement(reader);
        }
        if (this.schema != ClusteringSchema.CURRENT) {
            EjbLogger.ROOT_LOGGER.deprecatedNamespace(reader.getNamespaceURI(), reader.getLocalName());
        }
        EJBBoundClusteringMetaData metaData = new EJBBoundClusteringMetaData();
        this.processElements(metaData, reader, propertyReplacer);
        return metaData;
    }

    @Override
    protected void processElement(final EJBBoundClusteringMetaData metaData, final XMLStreamReader reader, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        if (this.schema.getNamespaceUri().equals(reader.getNamespaceURI())) {
            switch (reader.getLocalName()) {
                case "clustered": {
                    if (this.schema.since(ClusteringSchema.VERSION_1_1)) {
                        throw unexpectedElement(reader);
                    }
                    // Swallow ignored content
                    reader.getElementText();
                    break;
                }
                case "clustered-singleton":
                    if (this.schema.since(ClusteringSchema.VERSION_1_1)) {
                        requireNoAttributes(reader);
                        final String text = getElementText(reader, propertyReplacer);
                        if (text != null) {
                            final boolean isClusteredSingleton = Boolean.parseBoolean(text.trim());
                            metaData.setClusteredSingleton(isClusteredSingleton);
                        }
                        break;
                    }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        } else {
            super.processElement(metaData, reader, propertyReplacer);
        }
    }
}
