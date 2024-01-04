/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.clustering;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaDataParser;
import org.jboss.metadata.property.PropertyReplacer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses the urn:clustering namespace elements for clustering related metadata on Jakarta Enterprise Beans.
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
        if (this.schema.getNamespace().getUri().equals(reader.getNamespaceURI())) {
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
