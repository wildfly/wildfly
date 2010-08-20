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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * An element representing a list of properties (name/value pairs).
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertiesElement extends AbstractModelElement<PropertiesElement> {

    private static final long serialVersionUID = 1614693052895734582L;

    private final NavigableMap<String, String> properties = new TreeMap<String, String>(); 
    private final Element propertyType;
    private final boolean allowNullValue;
    
    /**
     * Construct a new instance.
     *
     * @param location the location at which this element was declared
     */
    public PropertiesElement(final Location location, final Element propertyType, final boolean allowNullValue) {
        super(location);
        this.propertyType = propertyType;
        this.allowNullValue = allowNullValue;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to construct this element.
     */
    public PropertiesElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        this(reader, Element.PROPERTY, true);
    }
    
    public PropertiesElement(final XMLExtendedStreamReader reader, final Element propertyType, boolean allowNullValue) throws XMLStreamException {
        super(reader);
        this.propertyType = propertyType;
        this.allowNullValue = allowNullValue;
        // Handle attributes
        requireNoAttributes(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());                    
                    if (element == propertyType) {
                        // Handle attributes
                        String name = null;
                        String value = null;
                        int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            final String attrValue = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw unexpectedAttribute(reader, i);
                            } 
                            else {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME: {
                                        name = attrValue;
                                        if (properties.containsKey(name)) {
                                            throw new XMLStreamException("Property " + name + " already exists", reader.getLocation());
                                        }
                                        break;
                                    }
                                    case VALUE: {
                                        value = attrValue;
                                        break;
                                    }
                                    default: throw unexpectedAttribute(reader, i);
                                }
                            }
                        }
                        if (name == null) {
                            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }
                        if (value == null && !allowNullValue) {
                            throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());
                        }
                        properties.put(name, value);
                        // Handle elements
                        requireNoContent(reader);
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (properties.size() == 0) {
            throw missingRequiredElement(reader, Collections.singleton(propertyType));
        }
    }
    
    public PropertiesElement(final Element propertyType, boolean allowNullValue, PropertiesElement ... toCombine) {
        // FIXME -- hack Location
        super(new Location("N/A", 0, 0, null));
        this.allowNullValue = allowNullValue;
        this.propertyType = propertyType;
        if (toCombine != null) {
            for (PropertiesElement pe : toCombine) {
                if (pe == null)
                    continue;
                for (String name : pe.getPropertyNames()) {
                    String val = pe.getProperty(name);
                    if (!allowNullValue && val == null) {
                        throw new IllegalStateException("Property " + name + " has a null value");
                    }
                    else {
                        properties.put(name, val);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long total = 0;
        synchronized (properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String val = entry.getValue();
                int valHash = val == null ? 0 : val.hashCode();
                total = Long.rotateLeft(total, 1) ^ ((long)entry.getKey().hashCode() << 32L | valHash & 0xffffffffL);
            }
        }
        return total;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<PropertiesElement>> target, final PropertiesElement other) {
        
        calculateDifference(target, safeCopyMap(properties), safeCopyMap(other.properties), new DifferenceHandler<String, String, PropertiesElement>() {
            public void handleAdd(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final String newElement) {
                target.add(new PropertyAdd(name, newElement));
            }

            public void handleRemove(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final String oldElement) {
                target.add(new PropertyRemove(name));
            }

            public void handleChange(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final String oldElement, final String newElement) {
                target.add(new PropertyRemove(name));
                target.add(new PropertyAdd(name, newElement));
            }
        });
    }

    /** {@inheritDoc} */
    protected Class<PropertiesElement> getElementClass() {
        return PropertiesElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        synchronized (properties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                streamWriter.writeEmptyElement(propertyType.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), entry.getKey());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue());
            }
        }
        streamWriter.writeEndElement();
    }

    void addProperty(final String name, final String value) {
        synchronized (properties) {
            if (properties.containsKey(name)) {
                throw new IllegalArgumentException("Property " + name + " already exists");
            }
            if (value == null && !allowNullValue) {
                throw new IllegalArgumentException("Value for property " + name + " is null");
            }
            properties.put(name, value);
        }
    }

    String removeProperty(final String name) {
        synchronized (properties) {
            final String old = properties.remove(name);
            if (old == null) {
                throw new IllegalArgumentException("Property " + name + " does not exist");
            }
            return old;
        }
    }

    public int size() {
        synchronized (properties) {
            return properties.size();
        }
    }

    /**
     * Get the value of a property defined in this element.
     *
     * @param name the property name
     * @return the value, or {@code null} if the property does not exist
     */
    public String getProperty(final String name) {
        synchronized (properties) {
            return properties.get(name);
        }
    }
    
    /**
     * Gets the names of the properties.
     * 
     * @return the names. Will not return <code>null</code>
     */
    public Set<String> getPropertyNames() {
        synchronized (properties) {
            return new HashSet<String>(properties.keySet());
        }
    }
    
    public Map<String, String> getProperties() {
        synchronized (properties) {
            return new HashMap<String, String>(properties);
        }
    }
}
