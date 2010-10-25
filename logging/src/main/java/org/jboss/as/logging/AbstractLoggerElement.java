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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractLoggerElement<E extends AbstractLoggerElement<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = -4071924125278221764L;

    private List<String> handlers = new ArrayList<String>();

    private String level;

    AbstractLoggerElement() {
    }

    void setLevel(final String level) {
        this.level = level;
    }

    public List<String> getHandlers() {
        return handlers;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (level != null) {
            streamWriter.writeEmptyElement(Element.LEVEL.getLocalName());
            streamWriter.writeAttribute(Attribute.NAME.getLocalName(), level);
        }

        if (handlers != null && handlers.size() > 0) {
            streamWriter.writeStartElement(Element.HANDLERS.getLocalName());
            for (String handler : handlers) {
                streamWriter.writeEmptyElement(Element.HANDLER.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), handler);
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }

    public String getLevel() {
        return level;
    }

    protected abstract void writeAttributes(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException;
}
