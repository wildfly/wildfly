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

package org.jboss.as.naming;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;

/**
 * Subsystem element representing the naming subsystem.
 * 
 * @author John E. Bailey
 */
final class NamingSubsystemElement extends AbstractSubsystemElement<NamingSubsystemElement> {
    private static final long serialVersionUID = -5701304143558865658L;

    private static final Logger log = Logger.getLogger("org.jboss.as.naming");

    /**
     * Create a new instance without a stream reader.
     *
     * @param location The source location
     */
    public NamingSubsystemElement(final Location location) {
        super(location, new QName("urn:jboss:domain:naming:1.0", "subsystem"));
    }

    /**
     * Create a new instance from a stream reader.
     *
     * @param reader The stream reader
     * @throws XMLStreamException
     */
    public NamingSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        requireNoContent(reader);
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return 42;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<NamingSubsystemElement>> target, final NamingSubsystemElement other) {
    }

    /** {@inheritDoc} */
    protected Class<NamingSubsystemElement> getElementClass() {
        return NamingSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
    }

    /**
     * Activate the naming subsystem.  Add a service for the naming server as well as a placeholder service for the java: namespace.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Naming Subsystem");

        // Create the Naming Service
        final BatchBuilder builder = context.getBatchBuilder();
        builder.addService(NamingService.SERVICE_NAME, new NamingService());

        // Create java: context service
        final JavaContextService contextService = new JavaContextService();
        builder.addService(JavaContextService.SERVICE_NAME, contextService)
            .addDependency(NamingService.SERVICE_NAME);
    }

    /** {@inheritDoc} */
    public Collection<String> getReferencedSocketBindings() {
        return null;
    }
}