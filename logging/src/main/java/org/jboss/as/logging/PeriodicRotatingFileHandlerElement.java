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

import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PeriodicRotatingFileHandlerElement extends AbstractFileHandlerElement<PeriodicRotatingFileHandlerElement> {

    private static final long serialVersionUID = -9165961552395250206L;

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());

    private String suffix;

    protected PeriodicRotatingFileHandlerElement(final String name) {
        super(name, ELEMENT_NAME);
    }

    protected Class<PeriodicRotatingFileHandlerElement> getElementClass() {
        return PeriodicRotatingFileHandlerElement.class;
    }

    void setSuffix(final String suffix) {
        this.suffix = suffix;
    }

    protected void writeElements(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement(Element.FILE.getLocalName());
        final String relativeTo = getRelativeTo();
        if (relativeTo != null) streamWriter.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), relativeTo);
        streamWriter.writeAttribute(Attribute.PATH.getLocalName(), getPath());
        streamWriter.writeEmptyElement(Element.SUFFIX.getLocalName());
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), suffix);
        super.writeElements(streamWriter);
    }

    AbstractHandlerAdd createAdd(final String name) {
        final PeriodicRotatingFileHandlerAdd add = new PeriodicRotatingFileHandlerAdd(name);
        add.setAppend(isAppend());
        add.setRelativeTo(getRelativeTo());
        add.setPath(getPath());
        add.setSuffix(suffix);
        return add;
    }
}
