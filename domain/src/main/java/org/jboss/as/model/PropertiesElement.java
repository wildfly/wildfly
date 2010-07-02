/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * An element representing a list of properties (name/value pairs).
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertiesElement extends AbstractModelElement<PropertiesElement> {

    private static final long serialVersionUID = 1614693052895734582L;

    private transient final SortedMap<String, PropertyElement> properties = new TreeMap<String, PropertyElement>();

    /** {@inheritDoc} */
    public long elementHash() {
        long total = 0;
        for (PropertyElement element : properties.values()) {
            total = Long.rotateLeft(total, 5) ^ element.elementHash();
        }
        return total;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<PropertiesElement>> target, final PropertiesElement other) {
        calculateDifference(target, properties, other.properties, new DifferenceHandler<String, PropertyElement, PropertiesElement>() {
            public void handleAdd(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final PropertyElement newElement) {
                target.add(new PropertiesUpdate(PropertiesUpdate.Kind.ADD, name, new PropertyUpdate(newElement.getValue())));
            }

            public void handleRemove(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final PropertyElement oldElement) {
                target.add(new PropertiesUpdate(PropertiesUpdate.Kind.REMOVE, name, null));
            }

            public void handleChange(final Collection<AbstractModelUpdate<PropertiesElement>> target, final String name, final PropertyElement oldElement, final PropertyElement newElement) {
                target.add(new PropertiesUpdate(PropertiesUpdate.Kind.CHANGE, name, new PropertyUpdate(newElement.getValue())));
            }
        });
    }

    /** {@inheritDoc} */
    protected Class<PropertiesElement> getElementClass() {
        return PropertiesElement.class;
    }

    /** {@inheritDoc} */
    public boolean isSameElement(final PropertiesElement other) {
        // properties elements don't have identity within their containers; there is only one.
        return true;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        for (Map.Entry<String, PropertyElement> entry : properties.entrySet()) {
            streamWriter.writeEmptyElement("property");
            entry.getValue().writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    void addProperty(final String name, final String value) {
        properties.put(name, new PropertyElement(name, value));
    }

    void removeProperty(final String name) {
        properties.remove(name);
    }

    void changeProperty(final String name, final String value) {
        properties.get(name).setValue(value);
    }

    /**
     * Get the value of a property defined in this element.
     *
     * @param name the property name
     * @return the value, or {@code null} if the property does not exist
     */
    public String getProperty(final String name) {
        final PropertyElement element = properties.get(name);
        return element == null ? null : element.getValue();
    }
}
