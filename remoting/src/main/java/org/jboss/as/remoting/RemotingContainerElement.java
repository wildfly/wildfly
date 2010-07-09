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

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import org.jboss.as.model.AbstractContainerElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.services.ThreadPoolExecutorService;
import org.jboss.remoting3.Endpoint;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.OptionMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A Remoting container definition.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingContainerElement extends AbstractContainerElement<RemotingContainerElement> {

    private static final long serialVersionUID = 8225457441023207312L;

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
    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param threadPoolName
     */
    public RemotingContainerElement(final Location location, final String threadPoolName) {
        super(location);
        if (threadPoolName == null) {
            throw new IllegalArgumentException("threadPoolName is null");
        }
        this.threadPoolName = threadPoolName;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return calculateElementHashOf(connectors.values(), 0L);
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<RemotingContainerElement>> target, final RemotingContainerElement other) {
        calculateDifference(target, connectors, other.connectors, new DifferenceHandler<String, ConnectorElement, RemotingContainerElement>() {
            public void handleAdd(final Collection<AbstractModelUpdate<RemotingContainerElement>> target, final String name, final ConnectorElement newElement) {
                target.add(new AddConnectorUpdate(newElement.clone()));
            }

            public void handleRemove(final Collection<AbstractModelUpdate<RemotingContainerElement>> target, final String name, final ConnectorElement oldElement) {
                target.add(new RemoveConnectorUpdate(name));
            }

            public void handleChange(final Collection<AbstractModelUpdate<RemotingContainerElement>> target, final String name, final ConnectorElement oldElement, final ConnectorElement newElement) {
                target.add(new RemoveConnectorUpdate(name));
                target.add(new AddConnectorUpdate(newElement.clone()));
            }
        });
    }

    /** {@inheritDoc} */
    protected Class<RemotingContainerElement> getElementClass() {
        return RemotingContainerElement.class;
    }

    private static final QName ELEMENT_NAME = new QName("urn:jboss:as:remoting:1.0", "remoting-container");

    /** {@inheritDoc} */
    protected QName getElementName() {
        return ELEMENT_NAME;
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
    public Collection<String> getReferencedSocketBindings() {
        final HashSet<String> set = new HashSet<String>();
        for (ConnectorElement element : connectors.values()) {
            set.add(element.getSocketBinding());
        }
        return set;
    }

    /** {@inheritDoc} */
    public void activate(final ServiceContainer container, final BatchBuilder batchBuilder) {
        final EndpointService endpointService = new EndpointService();
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        final BatchServiceBuilder<Endpoint> serviceBuilder = batchBuilder.addService(JBOSS_REMOTING_ENDPOINT, endpointService);
        serviceBuilder.addDependency(ServiceName.of(ThreadPoolExecutorService.JBOSS_THREADS_EXECUTOR.append(threadPoolName))).toInjector(executorInjector);
        serviceBuilder.setLocation(getLocation());
        // todo configure option map
        endpointService.setOptionMap(OptionMap.EMPTY);
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
