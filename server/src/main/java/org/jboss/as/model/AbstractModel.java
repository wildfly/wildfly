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

package org.jboss.as.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A controlled object model which is related to an XML representation.  Such an object model can be serialized to
 * XML or to binary.
 *
 * @param <M> the concrete model type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModel<M extends AbstractModel<M>> extends AbstractModelRootElement<M> {

    private static final long serialVersionUID = 66064050420378211L;

    private final String namespaceUri;
    private final Map<String, NamespacePrefix> prefixes = new LinkedHashMap<String, NamespacePrefix>();
    private final Map<String, SchemaLocation> schemaLocations = new LinkedHashMap<String, SchemaLocation>();
    private String noNamespaceSchemaLocation;

    /**
     * Construct a new instance.
     *
     * @param elementName the root element name
     */
    protected AbstractModel(final QName elementName) {
        super(elementName);
        final String namespaceURI = elementName.getNamespaceURI();
        namespaceUri = namespaceURI == null ? XMLConstants.NULL_NS_URI : namespaceURI;
    }

    /**
     * Construct a new instance.
     *
     * @deprecated going away...
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    @Deprecated
    protected AbstractModel(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        final String namespaceURI = reader.getNamespaceURI();
        namespaceUri = namespaceURI == null ? XMLConstants.NULL_NS_URI : namespaceURI;
    }

    /**
     * Apply an update to this model.
     *
     * @param update the update to apply
     * @param <R> the update's result type
     * @throws UpdateFailedException if an error occurs
     */
    public <R> void update(AbstractModelUpdate<M, R> update) throws UpdateFailedException {
        update.applyUpdate(cast());
    }

    /**
     * Replace the set of namespace prefixes.
     *
     * @param prefixes the new set of prefixes
     */
    protected void setPrefixes(final Iterable<NamespacePrefix> prefixes) {
        final Map<String, NamespacePrefix> prefixMap = this.prefixes;
        prefixMap.clear();
        for (NamespacePrefix prefix : prefixes) {
            if (prefix != null) {
                final String key = prefix.getPrefix();
                if (key != null) {
                    prefixMap.put(key, prefix);
                }
            }
        }
    }

    /**
     * Get the default namespace URI of this model.
     *
     * @return the default namespace URI
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * Get the namespace URI for a prefix declared on this model.
     *
     * @param prefix the prefix to look up
     * @return the URI, or {@code null} if the prefix is unknown
     */
    public String getNamespaceUri(String prefix) {
        final NamespacePrefix namespace = prefixes.get(prefix);
        return namespace == null ? null : namespace.getNamespaceURI();
    }

    /**
     * Get the list of prefix names in declaration order.
     *
     * @return the prefix name list
     */
    public List<String> getPrefixNames() {
        return new ArrayList<String>(prefixes.keySet());
    }

    /**
     * Get the list of prefixes in declaration order.
     *
     * @return the prefix list
     */
    public List<NamespacePrefix> getPrefixes() {
        return new ArrayList<NamespacePrefix>(prefixes.values());
    }

    /**
     * Get the location URI of namespace-less schema items.
     *
     * @return the URL
     */
    public String getNoNamespaceSchemaLocation() {
        return noNamespaceSchemaLocation;
    }

    /**
     * Set the no-namespace schema location.
     *
     * @param noNamespaceSchemaLocation the location
     */
    void setNoNamespaceSchemaLocation(final String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
    }

    /**
     * Replace the set of schema locations.
     *
     * @param locations the new set of locations
     */
    void setSchemaLocations(final Iterable<SchemaLocation> locations) {
        final Map<String, SchemaLocation> locationMap = schemaLocations;
        locationMap.clear();
        for (SchemaLocation location : locations) {
            if (location != null) {
                final String locationUri = location.getLocationUri();
                final String namespaceUri = location.getNamespaceUri();
                if (locationUri != null && namespaceUri != null) {
                    locationMap.put(namespaceUri, location);
                }
            }
        }
    }

    /**
     * Get the list of schema locations in declaration order.
     *
     * @return the schema location list
     */
    public List<SchemaLocation> getSchemaLocations() {
        return new ArrayList<SchemaLocation>(schemaLocations.values());
    }

    /**
     * Write the configured namespaces and XSI attributes for this model.
     *
     * @param writer the writer
     * @throws XMLStreamException if an error occurs
     */
    protected void writeNamespaces(XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeDefaultNamespace(namespaceUri);
        for (NamespacePrefix namespace : prefixes.values()) {
            writer.writeNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
        }
        final Iterator<SchemaLocation> it = schemaLocations.values().iterator();
        if (it.hasNext()) {
            final StringBuilder str = new StringBuilder();
            do {
                final SchemaLocation location = it.next();
                str.append(location.getNamespaceUri()).append(' ').append(location.getLocationUri());
                if (it.hasNext()) {
                    str.append(' ');
                }
            } while (it.hasNext());
            writer.writeAttribute(Namespace.XML_SCHEMA_INSTANCE.getUriString(), Attribute.SCHEMA_LOCATION.getLocalName(), str.toString());
        }
        if (noNamespaceSchemaLocation != null) {
            writer.writeAttribute(Namespace.XML_SCHEMA_INSTANCE.getUriString(), Attribute.NO_NAMESPACE_SCHEMA_LOCATION.getLocalName(), noNamespaceSchemaLocation);
        }
    }
}
