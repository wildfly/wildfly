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

import java.util.Locale;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public final class AsyncHandlerElement extends AbstractHandlerElement<AsyncHandlerElement> {

    private static final long serialVersionUID = 6954036272784574253L;

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.ASYNC_HANDLER.getLocalName());

    private int queueLength = 512;

    private OverflowAction overflowAction = OverflowAction.BLOCK;

    protected AsyncHandlerElement(final String name) {
        super(name, ELEMENT_NAME);
    }

    protected Class<AsyncHandlerElement> getElementClass() {
        return AsyncHandlerElement.class;
    }

    void setQueueLength(final int queueLength) {
        this.queueLength = queueLength;
    }

    void setOverflowAction(final OverflowAction overflowAction) {
        this.overflowAction = overflowAction;
    }

    protected void writeElements(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement(Element.QUEUE_LENGTH.getLocalName());
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), Integer.toString(queueLength));
        streamWriter.writeEmptyElement(Element.OVERFLOW_ACTION.getLocalName());
        streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), overflowAction.name().toLowerCase(Locale.US));
        super.writeElements(streamWriter);
    }

    AbstractHandlerAdd createAdd(final String name) {
        final AsyncHandlerAdd add = new AsyncHandlerAdd(name);
        add.setOverflowAction(overflowAction);
        add.setQueueLength(queueLength);
        return add;
    }
}
