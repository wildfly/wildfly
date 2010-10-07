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

package org.jboss.as.logging;

import java.util.Arrays;
import java.util.List;
import org.jboss.as.model.AbstractModelRootElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractHandlerElement<E extends AbstractHandlerElement<E>> extends AbstractModelRootElement<E> {

    private static final long serialVersionUID = -8326499785021909600L;

    private static final String[] NONE = new String[0];

    private final String name;
    private String[] subhandlers = NONE;
    private String encoding;
    private Boolean autoflush;
    private FilterElement filter;
    private Level level;
    private AbstractFormatterElement<?> formatter;
    private PropertiesElement properties;

    protected AbstractHandlerElement(final String name, final QName elementName) {
        super(elementName);
        this.name = name;
    }

    public List<String> getSubhandlers() {
        return Arrays.asList(subhandlers.clone());
    }

    public String getName() {
        return name;
    }

    public String getEncoding() {
        return encoding;
    }

    public Boolean getAutoflush() {
        return autoflush;
    }

    public FilterElement getFilter() {
        return filter;
    }

    public Level getLevel() {
        return level;
    }

    public AbstractFormatterElement<?> getFormatter() {
        return formatter;
    }

    public PropertiesElement getProperties() {
        return properties;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // Attributes
        if (name != null) {
            streamWriter.writeAttribute("name", name);
        }
        if (autoflush != null) {
            streamWriter.writeAttribute("autoflush", autoflush.toString());
        }
        if (encoding != null) {
            streamWriter.writeAttribute("encoding", encoding.toString());
        }
        // Elements
        if (level != null) {
            streamWriter.writeEmptyElement("level");
            streamWriter.writeAttribute("name", level.getName());
        }
        if (filter != null) {
            streamWriter.writeStartElement("filter");
            filter.writeContent(streamWriter);
        }
        if (formatter != null) {
            streamWriter.writeStartElement("formatter");
            final QName elementName = formatter.getElementName();
            streamWriter.writeStartElement(elementName.getNamespaceURI(), elementName.getLocalPart());
            formatter.writeContent(streamWriter);
            streamWriter.writeEndElement();
        }
        if (subhandlers != null && subhandlers.length > 0) {
            streamWriter.writeStartElement("subhandlers");
            for (String name : subhandlers) {
                streamWriter.writeEmptyElement("handler");
                streamWriter.writeAttribute("name", name);
            }
            streamWriter.writeEndElement();
        }
        if (properties != null) {
            streamWriter.writeStartElement("properties");
            properties.writeContent(streamWriter);
        }
    }

    void setLevel(final Level level) {
        this.level = level;
    }

    void setFilter(final FilterElement filter) {
        this.filter = filter;
    }

    void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    void setAutoflush(final Boolean autoflush) {
        this.autoflush = autoflush;
    }

    void setFormatter(final AbstractFormatterElement<?> formatter) {
        this.formatter = formatter;
    }

    void setProperties(final PropertiesElement properties) {
        this.properties = properties;
    }

    void setSubhandlers(final String[] subhandlers) {
        this.subhandlers = subhandlers.clone();
    }

    AbstractHandlerAdd getAdd() {
        final AbstractHandlerAdd add = createAdd(name);
        add.setLevel(level);
        add.setAutoflush(autoflush);
        add.setEncoding(encoding);
        add.setFormatter(formatter.getSpecification());
        add.setSubhandlers(subhandlers.clone());
        return add;
    }

    abstract AbstractHandlerAdd createAdd(String name);
}
