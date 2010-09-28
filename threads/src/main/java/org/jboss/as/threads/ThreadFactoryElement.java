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

import java.util.HashMap;
import java.util.Map;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadFactoryElement extends AbstractModelElement<ThreadFactoryElement> {

    private static final long serialVersionUID = 8873007541743218790L;

    private final String name;
    private String groupName;
    private String threadNamePattern;
    private Integer priority;
    private Map<String, String> properties;

    public ThreadFactoryElement(final String name) {
        this.name = name;
    }

    protected Class<ThreadFactoryElement> getElementClass() {
        return ThreadFactoryElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", name);
        if (groupName != null) streamWriter.writeAttribute("thread-group", groupName);
        if (threadNamePattern != null) streamWriter.writeAttribute("thread-name-pattern", threadNamePattern);
        if (priority != null) streamWriter.writeAttribute("priority", priority.toString());
        if (! properties.isEmpty()) {
            streamWriter.writeStartElement(Element.PROPERTIES.getLocalName());
            for (String key : properties.keySet()) {
                streamWriter.writeEmptyElement(Element.PROPERTY.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), key);
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), properties.get(key));
            }
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    public String getName() {
        return name;
    }

    void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    void setThreadNamePattern(final String threadNamePattern) {
        this.threadNamePattern = threadNamePattern;
    }

    void setPriority(final Integer priority) {
        this.priority = priority;
    }

    void setProperties(final Map<String, String> properties) {
        this.properties = new HashMap<String, String>(properties);
    }

    Integer getPriority() {
        return priority;
    }

    String getGroupName() {
        return groupName;
    }

    String getThreadNamePattern() {
        return threadNamePattern;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
