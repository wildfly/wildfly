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

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadsSubsystemElement extends AbstractSubsystemElement<ThreadsSubsystemElement> {

    private static final long serialVersionUID = -8577568464935736902L;
    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    private final NavigableMap<String, ThreadFactoryElement> threadFactories = new TreeMap<String, ThreadFactoryElement>();
    private final NavigableMap<String, ScheduledThreadPoolExecutorElement> scheduledExecutors = new TreeMap<String, ScheduledThreadPoolExecutorElement>();
    private final NavigableMap<String, AbstractExecutorElement<?>> executors = new TreeMap<String, AbstractExecutorElement<?>>();

    protected ThreadsSubsystemElement(final Location location, final QName elementName) {
        super(location, elementName);
    }

    protected ThreadsSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case THREAD_FACTORY: {
                            final ThreadFactoryElement threadFactoryElement = new ThreadFactoryElement(reader);
                            final String name = threadFactoryElement.getName();
                            if (threadFactories.containsKey(name)) {
                                throw duplicateNamedElement(reader, name);
                            }
                            threadFactories.put(name, threadFactoryElement);
                            break;
                        }
                        case SCHEDULED_THREAD_POOL_EXECUTOR: {
                            final ScheduledThreadPoolExecutorElement executorElement = new ScheduledThreadPoolExecutorElement(reader);
                            final String name = executorElement.getName();
                            if (scheduledExecutors.containsKey(name)) {
                                throw duplicateNamedElement(reader, name);
                            }
                            scheduledExecutors.put(name, executorElement);
                            break;
                        }
                        case BOUNDED_QUEUE_THREAD_POOL_EXECUTOR:
                        case QUEUELESS_THREAD_POOL_EXECUTOR:
                        case THREAD_FACTORY_EXECUTOR:
                        case UNBOUNDED_QUEUE_THREAD_POOL_EXECUTOR: {
                            final AbstractExecutorElement<?> executorElement;
                            switch (element) {
                                case BOUNDED_QUEUE_THREAD_POOL_EXECUTOR: {
                                    executorElement = new BoundedQueueThreadPoolExecutorElement(reader);
                                    break;
                                }
                                case QUEUELESS_THREAD_POOL_EXECUTOR: {
                                    executorElement = new QueuelessThreadPoolExecutorElement(reader);
                                    break;
                                }
                                case THREAD_FACTORY_EXECUTOR: {
                                    executorElement = new ThreadFactoryExecutorElement(reader);
                                    break;
                                }
                                case UNBOUNDED_QUEUE_THREAD_POOL_EXECUTOR: {
                                    executorElement = new UnboundedQueueThreadPoolExecutor(reader);
                                    break;
                                }
                                default: {
                                    throw new IllegalStateException();
                                }
                            }
                            final String name = executorElement.getName();
                            if (executors.containsKey(name)) {
                                throw duplicateNamedElement(reader, name);
                            }
                            executors.put(name, executorElement);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Threading Subsystem");
        for (ThreadFactoryElement element : threadFactories.values()) {
            element.activate(context);
        }
    }

    public Collection<String> getReferencedSocketBindings() {
        return null;
    }

    public long elementHash() {
        long hash = 0L;
        hash = calculateElementHashOf(threadFactories.values(), hash);
        return hash;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<ThreadsSubsystemElement>> target, final ThreadsSubsystemElement other) {
    }

    protected Class<ThreadsSubsystemElement> getElementClass() {
        return null;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
    }
}
