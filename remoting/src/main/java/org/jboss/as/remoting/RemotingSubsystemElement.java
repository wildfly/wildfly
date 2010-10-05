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


import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Remoting subsystem definition.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingSubsystemElement extends AbstractSubsystemElement<RemotingSubsystemElement> {

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

    public static final String NAMESPACE_1_0 = "urn:jboss:domain:remoting:1.0";

    public static final String NAMESPACE = NAMESPACE_1_0;

    public static final Set<String> NAMESPACES = Collections.singleton(NAMESPACE);

    /**
     * Construct a new instance.
     */
    public RemotingSubsystemElement() {
        super(Namespace.REMOTING_1_0.getUriString());
    }

    /** {@inheritDoc} */
    @Override
    protected Class<RemotingSubsystemElement> getElementClass() {
        return RemotingSubsystemElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.THREAD_POOL.getLocalName(), threadPoolName);
        for (ConnectorElement element : connectors.values()) {
            streamWriter.writeStartElement("connector");
            element.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /**
     * Get the name of the thread pool configured for this subsystem.
     *
     * @return the thread pool name
     */
    public String getThreadPoolName() {
        return threadPoolName;
    }

    void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    ConnectorElement getConnector(String name) {
        return connectors.get(name);
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

    /** {@inheritDoc} */
    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<RemotingSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    protected RemotingSubsystemAdd getAdd() {
        return new RemotingSubsystemAdd(threadPoolName);
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        final ServiceContainer container = updateContext.getServiceContainer();
        final ServiceController<?> controller = container.getService(RemotingServices.ENDPOINT);
        if (controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    ConnectorElement addConnector(String name, String socketBinding) {
        if (connectors.containsKey(name)) {
            return null;
        }
        final ConnectorElement connector = new ConnectorElement(name, socketBinding);
        connectors.put(name, connector);
        return connector;
    }
}
