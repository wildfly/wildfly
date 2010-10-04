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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class UnboundedQueueThreadPoolExecutor extends AbstractExecutorElement<UnboundedQueueThreadPoolExecutor> {

    private static final long serialVersionUID = 8095990490745474752L;

    public UnboundedQueueThreadPoolExecutor(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: break;
                default: throw unexpectedAttribute(reader, i);
            }
        }
        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    switch (Element.forName(reader.getLocalName())) {
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

    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        TimeSpec keepAlive = getKeepaliveTime();
        if(keepAlive == null)
            keepAlive = TimeSpec.DEFAULT_KEEPALIVE;
        final UnboundedQueueThreadPoolService service = new UnboundedQueueThreadPoolService(getMaxThreads().getScaledCount(), keepAlive);
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
    }

    @Override
    public long elementHash() {
        return 0;
    }

    @Override
    protected Class<UnboundedQueueThreadPoolExecutor> getElementClass() {
        return UnboundedQueueThreadPoolExecutor.class;
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", getName());
        final ScaledCount maxThreads = getMaxThreads();
        if (maxThreads != null) writeScaledCountElement(streamWriter, maxThreads, "max-threads");
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
        streamWriter.writeEndElement();
    }

    @Override
    protected Element getStandardElement() {
        return Element.UNBOUNDED_QUEUE_THREAD_POOL_EXECUTOR;
    }
}
