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

package org.jboss.as.threads;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.PropertiesElement;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryElement extends AbstractModelElement<ThreadFactoryElement> implements ServiceActivator {

    private static final long serialVersionUID = 8873007541743218790L;

    private final String name;
    private String groupName;
    private String threadNamePattern;
    private Integer priority;
    private PropertiesElement propertiesElement;

    public ThreadFactoryElement(final Location location, final String name) {
        super(location);
        this.name = name;
    }

    public ThreadFactoryElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        String name = null;
        final String myNamespace = reader.getNamespaceURI();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) throw unexpectedAttribute(reader, i);
            switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                case GROUP_NAME: {
                    groupName = reader.getAttributeValue(i);
                    break;
                }
                case THREAD_NAME_PATTERN: {
                    threadNamePattern = reader.getAttributeValue(i);
                    break;
                }
                case PRIORITY: {
                    final int priorityValue = reader.getIntAttributeValue(i);
                    if (priorityValue < 1 || priorityValue > 10) {
                        throw new XMLStreamException("Out-of-range value for priority attribute");
                    }
                    priority = Integer.valueOf(priorityValue);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        // elements
        boolean gotProperties = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (! myNamespace.equals(reader.getNamespaceURI()) || Element.forName(reader.getLocalName()) != Element.PROPERTIES || gotProperties) {
                throw unexpectedElement(reader);
            }
            gotProperties = true;
            propertiesElement = new PropertiesElement(reader);
        }
    }

    public long elementHash() {
        long hash = name.hashCode() & 0xFFFFFFFFL;
        if (groupName != null) hash = Long.rotateLeft(hash, 1) ^ groupName.hashCode() & 0xFFFFFFFFL;
        if (threadNamePattern != null) hash = Long.rotateLeft(hash, 1) ^ threadNamePattern.hashCode() & 0xFFFFFFFFL;
        if (priority != null) hash = Long.rotateLeft(hash, 1) ^ priority.longValue();
        if (propertiesElement != null) hash = Long.rotateLeft(hash, 1) ^ propertiesElement.elementHash();
        return hash;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<ThreadFactoryElement>> target, final ThreadFactoryElement other) {
    }

    protected Class<ThreadFactoryElement> getElementClass() {
        return ThreadFactoryElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", name);
        if (groupName != null) streamWriter.writeAttribute("thread-group", groupName);
        if (threadNamePattern != null) streamWriter.writeAttribute("thread-name-pattern", threadNamePattern);
        if (priority != null) streamWriter.writeAttribute("priority", priority.toString());
        if (propertiesElement != null) {
            streamWriter.writeStartElement("properties");
            propertiesElement.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    public void activate(final ServiceActivatorContext context) {
        
    }

    public String getName() {
        return name;
    }
}
