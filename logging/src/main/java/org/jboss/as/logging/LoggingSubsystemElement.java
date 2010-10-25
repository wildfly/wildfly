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
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
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

    private RootLoggerElement rootLoggerElement;

    private final NavigableMap<String, LoggerElement> loggers = new TreeMap<String, LoggerElement>();

    private final NavigableMap<String, AbstractHandlerElement<?>> handlers = new TreeMap<String, AbstractHandlerElement<?>>();

    public LoggingSubsystemElement() {
        super(Namespace.LOGGING_1_0.getUriString());
    }

    boolean addLogger(LoggerElement logger) {
        final String name = logger.getName();
        if (loggers.containsKey(name)) {
            return false;
        }
        loggers.put(name, logger);
        return true;
    }

    RootLoggerElement getRootLogger() {
        return rootLoggerElement;
    }

    boolean setRootLogger(RootLoggerElement rootLoggerElement) {
        if (this.rootLoggerElement != null) {
            return false;
        }
        this.rootLoggerElement = rootLoggerElement;
        return true;
    }

    RootLoggerElement clearRootLogger() {
        try {
            return rootLoggerElement;
        } finally {
            rootLoggerElement = null;
        }
    }

    LoggerElement getLogger(final String name) {
        return loggers.get(name);
    }

    LoggerElement removeLogger(final String name) {
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
        return handlers.get(name);
    }

    AbstractHandlerElement<?> removeHandler(final String name) {
        return handlers.remove(name);
    }

    @Override
    protected Class<LoggingSubsystemElement> getElementClass() {
        return LoggingSubsystemElement.class;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        for (AbstractHandlerElement<?> element : handlers.values()) {
            final QName elementName = element.getElementName();
            streamWriter.writeStartElement(elementName.getNamespaceURI(), elementName.getLocalPart());
            element.writeContent(streamWriter);
        }
        for (LoggerElement element : loggers.descendingMap().values()) {
            streamWriter.writeStartElement(Element.LOGGER.getLocalName());
            element.writeContent(streamWriter);
        }
        final RootLoggerElement rootLoggerElement = this.rootLoggerElement;
        if (rootLoggerElement != null) {
            streamWriter.writeStartElement(Element.ROOT_LOGGER.getLocalName());
            rootLoggerElement.writeContent(streamWriter);
        }

        streamWriter.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<LoggingSubsystemElement, ?>> list) {
        if (rootLoggerElement != null) {
            final RootLoggerAdd rootLoggerAdd = new RootLoggerAdd();
            rootLoggerAdd.setLevelName(rootLoggerElement.getLevel());
            list.add(rootLoggerAdd);
            for (String handlerName : rootLoggerElement.getHandlers()) {
                list.add(new LoggerHandlerAdd("", handlerName));
            }
        }
        for (LoggerElement element : loggers.values()) {
            final String name = element.getName();
            final LoggerAdd add = new LoggerAdd(name);
            add.setLevelName(element.getLevel());
            add.setUseParentHandlers(element.isUseParentHandlers());
            list.add(add);
            for (String handlerName : element.getHandlers()) {
                list.add(new LoggerHandlerAdd(name, handlerName));
            }
        }
        for (AbstractHandlerElement<?> handlerElement : handlers.values()) {
            list.add(handlerElement.getAdd());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEmpty() {
        return rootLoggerElement == null && loggers.isEmpty() && handlers.isEmpty();
    }

    @Override
    protected LoggingSubsystemAdd getAdd() {
        return new LoggingSubsystemAdd();
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }
}
