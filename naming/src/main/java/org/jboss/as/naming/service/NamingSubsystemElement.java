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

package org.jboss.as.naming.service;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.naming.InitialContextFactoryBuilder;
import org.jboss.as.naming.context.NamespaceObjectFactory;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.value.Values;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
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
    private static final String PACKAGE_PREFIXES = "org.jboss.as.naming.interfaces";

    private static final Logger log = Logger.getLogger("org.jboss.as.naming");

    private boolean supportEvents = true;
    private boolean bindAppContext;
    private boolean bindModuleContext;
    private boolean bindCompContext;

    /**
     * Create a new instance without a stream reader.
     *
     * @param location The source location
     * @param supportEvents Should the naming system support events
     */
    public NamingSubsystemElement(final Location location, final boolean supportEvents) {
        super(location, new QName("urn:jboss:domain:naming:1.0", "subsystem"));
        this.supportEvents = supportEvents;
    }

    /**
     * Create a new instance from a stream reader.
     *
     * @param reader The stream reader
     * @throws XMLStreamException
     */
    public NamingSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SUPPORT_EVENTS: {
                    supportEvents = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                case BIND_APP_CONTEXT: {
                    bindAppContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                case BIND_MODULE_CONTEXT: {
                    bindModuleContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }case BIND_COMP_CONTEXT: {
                    bindCompContext = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }
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
        streamWriter.writeAttribute(Attribute.SUPPORT_EVENTS.getLocalName(), Boolean.toString(isSupportEvents()));
        streamWriter.writeAttribute(Attribute.BIND_APP_CONTEXT.getLocalName(), Boolean.toString(isBindAppContext()));
        streamWriter.writeAttribute(Attribute.BIND_MODULE_CONTEXT.getLocalName(), Boolean.toString(isBindModuleContext()));
        streamWriter.writeAttribute(Attribute.BIND_COMP_CONTEXT.getLocalName(), Boolean.toString(isBindCompContext()));
    }

    /**
     * Activate the naming subsystem.  Add a service for the naming server as well as a placeholder service for the java: namespace.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Naming Subsystem");

        // Setup naming environment
        System.setProperty(Context.URL_PKG_PREFIXES, PACKAGE_PREFIXES);
        try {
            NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder());
        } catch (NamingException e) {
            log.warn("Failed to set InitialContextFactoryBuilder", e);
        }

        // Create the Naming Service
        final BatchBuilder builder = context.getBatchBuilder();
        builder.addService(NamingService.SERVICE_NAME, new NamingService(isSupportEvents()));

        // Create java: context service
        final JavaContextService contextService = new JavaContextService();
        builder.addService(JavaContextService.SERVICE_NAME, contextService)
            .addDependency(NamingService.SERVICE_NAME);

        if(isBindAppContext()) {
            addContextFactory(builder, "app");
        }
        if(isBindModuleContext()) {
            addContextFactory(builder, "module");
        }
        if(isBindCompContext()) {
            addContextFactory(builder, "comp");
        }
    }

    private void addContextFactory(final BatchBuilder builder, final String contextName) {
        final Reference appReference = NamespaceObjectFactory.createReference(contextName);
        final BinderService<Reference> binderService = new BinderService<Reference>(contextName, Values.immediateValue(appReference));
        builder.addService(JavaContextService.SERVICE_NAME.append(contextName), binderService)
            .addDependency(JavaContextService.SERVICE_NAME, Context.class, binderService.getContextInjector());
    }

    /** {@inheritDoc} */
    public Collection<String> getReferencedSocketBindings() {
        return null;
    }

    public boolean isSupportEvents() {
        return supportEvents;
    }

    public boolean isBindAppContext() {
        return bindAppContext;
    }

    public boolean isBindModuleContext() {
        return bindModuleContext;
    }

    public boolean isBindCompContext() {
        return bindCompContext;
    }
}