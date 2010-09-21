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

package org.jboss.as.remoting;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.OptionMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import static org.jboss.as.threads.AbstractExecutorElement.JBOSS_THREAD_SCHEDULED_EXECUTOR;

/**
 * A Remoting subsystem definition.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingSubsystemElement extends AbstractSubsystemElement<RemotingSubsystemElement> {

    private static final long serialVersionUID = 8225457441023207312L;

    private static final Logger log = Logger.getLogger("org.jboss.as.remoting");

    /**
     * The service name "jboss.remoting".
     */
    public static final ServiceName JBOSS_REMOTING = ServiceName.JBOSS.append("remoting");

    /**
     * The service name of the Remoting endpoint, "jboss.remoting.endpoint".
     */
    public static final ServiceName JBOSS_REMOTING_ENDPOINT = JBOSS_REMOTING.append("endpoint");

    private final SortedMap<String, ConnectorElement> connectors = new TreeMap<String, ConnectorElement>();

    private String threadPoolName;

    public static final String NAMESPACE_1_0 = "urn:jboss:domain:remoting:1.0";

    public static final String NAMESPACE = NAMESPACE_1_0;

    public static final Set<String> NAMESPACES = Collections.singleton(NAMESPACE);

    /**
     * Construct a new instance.
     *
     * @param threadPoolName the name of the thread pool for the remoting subsystem
     * @param elementName the name of the subsystem element
     */
    public RemotingSubsystemElement(final String threadPoolName, final QName elementName) {
        super(elementName);
        if (threadPoolName == null) {
            throw new IllegalArgumentException("threadPoolName is null");
        }
        this.threadPoolName = threadPoolName;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which the subsystem element should be read
     */
    public RemotingSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String threadPoolName = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case THREAD_POOL: {
                        threadPoolName = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (threadPoolName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.THREAD_POOL));
        }
        this.threadPoolName = threadPoolName;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTOR: {
                            final ConnectorElement connector = new ConnectorElement(reader);
                            connectors.put(connector.getName(), connector);
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

    /** {@inheritDoc} */
    public long elementHash() {
        return calculateElementHashOf(connectors.values(), 0L);
    }

    /** {@inheritDoc} */
    protected Class<RemotingSubsystemElement> getElementClass() {
        return RemotingSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        for (ConnectorElement element : connectors.values()) {
            streamWriter.writeStartElement("connector");
            element.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Remoting Subsystem");
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        final EndpointService endpointService = new EndpointService();
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        final BatchServiceBuilder<Endpoint> serviceBuilder = batchBuilder.addService(JBOSS_REMOTING_ENDPOINT, endpointService);
        serviceBuilder.addDependency(JBOSS_THREAD_SCHEDULED_EXECUTOR.append(threadPoolName), Executor.class, executorInjector);
        serviceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        // todo configure option map
        endpointService.setOptionMap(OptionMap.EMPTY);
    }

    /**
     * Get the name of the thread pool configured for this subsystem.
     *
     * @return the thread pool name
     */
    public String getThreadPoolName() {
        return threadPoolName;
    }

    String addConnector(final ConnectorElement element) {
        final String name = element.getName();
        if (connectors.containsKey(name)) {
            throw new IllegalArgumentException("A connector with this name already exists");
        }
        connectors.put(name, element);
        return name;
    }

    ConnectorElement removeConnector(final String name) {
        final ConnectorElement element = connectors.remove(name);
        if (element == null) {
            throw new IllegalArgumentException("No such connector exists");
        }
        return element;
    }
}
