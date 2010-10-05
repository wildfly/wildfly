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

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ChildElement;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadsSubsystemElement extends AbstractSubsystemElement<ThreadsSubsystemElement> {

    private static final long serialVersionUID = -8577568464935736902L;
    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    private final NavigableMap<String, ThreadFactoryElement> threadFactories = new TreeMap<String, ThreadFactoryElement>();
    private final NavigableMap<String, ChildElement<? extends AbstractExecutorElement<?>>> executors = new TreeMap<String, ChildElement<? extends AbstractExecutorElement<?>>>();

    protected ThreadsSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected Class<ThreadsSubsystemElement> getElementClass() {
        return null;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        for (ThreadFactoryElement tfe : threadFactories.values()) {
            streamWriter.writeStartElement(Element.THREAD_FACTORY.getLocalName());
            tfe.writeContent(streamWriter);
        }
        for (ChildElement<? extends AbstractExecutorElement<?>> childElement : executors.values()) {
            streamWriter.writeStartElement(childElement.getLocalName());
            childElement.getElement().writeContent(streamWriter);
        }

        streamWriter.writeEndElement();
    }

    protected void getUpdates(final List<? super AbstractSubsystemUpdate<ThreadsSubsystemElement, ?>> objects) {
        for (String name : threadFactories.keySet()) {
            objects.add(new ThreadFactoryAdd(name));
        }
        for (String name : executors.keySet()) {
            objects.add(executors.get(name).getElement().getAdd());
        }
    }

    protected boolean isEmpty() {
        return threadFactories.isEmpty() && executors.isEmpty();
    }

    protected ThreadsSubsystemAdd getAdd() {
        return new ThreadsSubsystemAdd();
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        // no operation
    }

    ThreadFactoryElement getThreadFactory(final String name) {
        return threadFactories.get(name);
    }

    void removeThreadFactory(final String name) {
        threadFactories.remove(name);
    }

    ThreadFactoryElement addThreadFactory(final String name) {
        if (threadFactories.containsKey(name)) {
            return null;
        }
        final ThreadFactoryElement element = new ThreadFactoryElement(name);
        threadFactories.put(name, element);
        return element;
    }

    AbstractExecutorElement<?> getExecutor(final String name) {
        final ChildElement<? extends AbstractExecutorElement<?>> childElement = executors.get(name);
        return childElement == null ? null : childElement.getElement();
    }

    void removeExecutor(final String name) {
        executors.remove(name);
    }

    void addExecutor(final String name, final ChildElement<? extends AbstractExecutorElement<?>> element) {
        executors.put(name, element);
    }
}
