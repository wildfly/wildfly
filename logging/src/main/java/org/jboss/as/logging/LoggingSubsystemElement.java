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

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * The logging subsystem root element implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoggingSubsystemElement extends AbstractSubsystemElement<LoggingSubsystemElement> {

    private static final long serialVersionUID = -615878954033668252L;

    public static final ServiceName JBOSS_LOGGING = ServiceName.JBOSS.append("logging");

    private final NavigableMap<String, AbstractLoggerElement<?>> loggers = new TreeMap<String, AbstractLoggerElement<?>>();
    private final NavigableMap<String, AbstractHandlerElement<?>> handlers = new TreeMap<String, AbstractHandlerElement<?>>();

    public LoggingSubsystemElement() {
        super(Namespace.LOGGING_1_0.getUriString());
    }

    boolean addLogger(String name, AbstractLoggerElement<?> logger) {
        if (loggers.containsKey(name)) {
            return false;
        }
        loggers.put(name, logger);
        return true;
    }

    AbstractLoggerElement<?> getLogger(final String name) {
        return loggers.get(name);
    }

    AbstractLoggerElement<?> removeLogger(final String name) {
        return loggers.remove(name);
    }

    boolean addHandler(String name, AbstractHandlerElement<?> handler) {
        if (handlers.containsKey(name)) {
            return false;
        }
        handlers.put(name, handler);
        return true;
    }

    AbstractHandlerElement<?> getHandler(final String name) {
        return this.handlers.get(name);
    }

    AbstractHandlerElement<?> removeHandler(final String name) {
        return this.handlers.remove(name);
    }

    protected Class<LoggingSubsystemElement> getElementClass() {
        return LoggingSubsystemElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        for (AbstractHandlerElement<?> element : handlers.values()) {
            final QName elementName = element.getElementName();
            streamWriter.writeStartElement(elementName.getNamespaceURI(), elementName.getLocalPart());
            element.writeContent(streamWriter);
        }
        for (AbstractLoggerElement<?> element : loggers.values()) {
            final QName elementName = element.getElementName();
            streamWriter.writeStartElement(elementName.getNamespaceURI(), elementName.getLocalPart());
            element.writeContent(streamWriter);
        }
    }

    /** {@inheritDoc} */
    protected void getClearingUpdates(List<? super AbstractSubsystemUpdate<LoggingSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }
}
