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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertyElement extends AbstractModelElement<PropertyElement> {

    private static final long serialVersionUID = -7906009273074776240L;

    private final String name;

    private String value;

    protected PropertyElement(final String name, final String value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.name = name;
        this.value = value;
    }

    public long elementHash() {
        return (long)name.hashCode() << 32L | value.hashCode() & 0xffffffffL;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<PropertyElement>> target, final PropertyElement other) {
        assert isSameElement(other);
        if (! value.equals(other.value)) {
            target.add(new PropertyUpdate(value));
        }
    }

    protected Class<PropertyElement> getElementClass() {
        return PropertyElement.class;
    }

    public boolean isSameElement(final PropertyElement other) {
        return name.equals(other.name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    protected void setValue(String value) {
        this.value = value;
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("name", name);
        streamWriter.writeAttribute("value", value);
    }
}
