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

import org.jboss.as.model.PropertiesElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BoundedQueueThreadPoolExecutorElement extends AbstractExecutorElement<BoundedQueueThreadPoolExecutorElement> {

    private static final long serialVersionUID = -6314205265652284301L;

    private String handoffExecutor;
    private boolean blocking;
    private boolean allowCoreTimeout;
    private ScaledCount queueLength;
    private ScaledCount coreThreads;

    public BoundedQueueThreadPoolExecutorElement(final String name) {
        super(name);
    }

    public BoundedQueueThreadPoolExecutorElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ALLOW_CORE_TIMEOUT: {
                    allowCoreTimeout = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                case BLOCKING: {
                    blocking = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                case NAME: break;
                default: throw unexpectedAttribute(reader, i);
            }
        }
        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    switch (Element.forName(reader.getLocalName())) {
                        case CORE_THREADS: {
                            coreThreads = readScaledCountElement(reader);
                            break;
                        }
                        case MAX_THREADS: {
                            setMaxThreads(readScaledCountElement(reader));
                            break;
                        }
                        case KEEPALIVE_TIME: {
                            setKeepaliveTime(readTimeSpecElement(reader));
                            break;
                        }
                        case THREAD_FACTORY: {
                            setThreadFactory(readStringAttributeElement(reader, "name"));
                            break;
                        }
                        case PROPERTIES: {
                            setProperties(new PropertiesElement(reader));
                            break;
                        }
                        case QUEUE_LENGTH: {
                            queueLength = readScaledCountElement(reader);
                            break;
                        }
                        case HANDOFF_EXECUTOR: {
                            handoffExecutor = readStringAttributeElement(reader, "handoff-executor");
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    public void activate(final ServiceActivatorContext context) {final BatchBuilder batchBuilder = context.getBatchBuilder();
        final ScaledCount maxThreads = getMaxThreads();
        int maxThreadsValue = maxThreads.getScaledCount();
        final ScaledCount queueLength = this.queueLength;
        int queueLengthValue = queueLength.getScaledCount();

        final ScaledCount coreThreads = this.coreThreads;
        int coreThreadsValue = coreThreads != null ? coreThreads.getScaledCount() : maxThreadsValue;

        TimeSpec keepAlive = getKeepaliveTime();
        if(keepAlive == null)
            keepAlive = TimeSpec.DEFAULT_KEEPALIVE;
        final BoundedQueueThreadPoolService service = new BoundedQueueThreadPoolService(coreThreadsValue, maxThreadsValue, queueLengthValue, blocking, keepAlive, allowCoreTimeout);
        final ServiceName serviceName = JBOSS_THREAD_EXECUTOR.append(getName());
        final BatchServiceBuilder<ExecutorService> serviceBuilder = batchBuilder.addService(serviceName, service);
        final String threadFactory = getThreadFactory();
        final ServiceName threadFactoryName;
        if (threadFactory == null) {
            threadFactoryName = serviceName.append("thread-factory");
            batchBuilder.addService(threadFactoryName, new ThreadFactoryService());
        } else {
            threadFactoryName = JBOSS_THREAD_FACTORY.append(threadFactory);
        }
        serviceBuilder.addDependency(threadFactoryName, ThreadFactory.class, service.getThreadFactoryInjector());
        final String handoffExecutor = this.handoffExecutor;
        if (handoffExecutor != null) {
            final ServiceName handoffExecutorName = JBOSS_THREAD_EXECUTOR.append(handoffExecutor);
            serviceBuilder.addDependency(handoffExecutorName, Executor.class, service.getHandoffExecutorInjector());
        }
    }

    @Override
    public long elementHash() {
        long hash = super.elementHash();
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(blocking).hashCode() & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(allowCoreTimeout).hashCode() & 0xffffffffL;
        if (handoffExecutor != null) hash = Long.rotateLeft(hash, 1) ^ handoffExecutor.hashCode() & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ queueLength.elementHash();
        if (coreThreads != null) hash = Long.rotateLeft(hash, 1) ^ coreThreads.elementHash();
        return hash;
    }

    @Override
    protected Class<BoundedQueueThreadPoolExecutorElement> getElementClass() {
        return BoundedQueueThreadPoolExecutorElement.class;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", getName());
        if (blocking) { streamWriter.writeAttribute("blocking", "true"); }
        if (allowCoreTimeout) { streamWriter.writeAttribute("allow-core-timeout", "true"); }
        if (coreThreads != null) {
            writeScaledCountElement(streamWriter, coreThreads, "core-threads");
        }
        writeScaledCountElement(streamWriter, queueLength, "queue-length");
        writeScaledCountElement(streamWriter, getMaxThreads(), "max-threads");
        final TimeSpec keepaliveTime = getKeepaliveTime();
        if (keepaliveTime != null) writeTimeSpecElement(streamWriter, keepaliveTime, "keepalive-time");
        final String threadFactory = getThreadFactory();
        if (threadFactory != null) {
            streamWriter.writeEmptyElement("thread-factory");
            streamWriter.writeAttribute("name", threadFactory);
        }
        final PropertiesElement properties = getProperties();
        if (properties != null) {
            streamWriter.writeStartElement("properties");
            properties.writeContent(streamWriter);
        }
        if (handoffExecutor != null) {
            streamWriter.writeEmptyElement("handoff-executor");
            streamWriter.writeAttribute("name", handoffExecutor);
        }
        streamWriter.writeEndElement();
    }

    @Override
    protected Element getStandardElement() {
        return Element.BOUNDED_QUEUE_THREAD_POOL_EXECUTOR;
    }
}
