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

import java.util.List;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.naming.context.NamespaceObjectFactory;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.value.Values;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.naming.Context;
import javax.naming.Reference;

import javax.xml.stream.XMLStreamException;

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
     * Create a new instance.
     */
    public NamingSubsystemElement() {
        super(NamingExtension.NAMESPACE);
    }

    /**
     * Create a new instance without a stream reader.
     *
     * @param supportEvents Should the naming system support events
     */
    public NamingSubsystemElement(final boolean supportEvents) {
        super(NamingExtension.NAMESPACE);
        this.supportEvents = supportEvents;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<NamingSubsystemElement> getElementClass() {
        return NamingSubsystemElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.SUPPORT_EVENTS.getLocalName(), Boolean.toString(isSupportEvents()));
        streamWriter.writeAttribute(Attribute.BIND_APP_CONTEXT.getLocalName(), Boolean.toString(isBindAppContext()));
        streamWriter.writeAttribute(Attribute.BIND_MODULE_CONTEXT.getLocalName(), Boolean.toString(isBindModuleContext()));
        streamWriter.writeAttribute(Attribute.BIND_COMP_CONTEXT.getLocalName(), Boolean.toString(isBindCompContext()));
        streamWriter.writeEndElement();
    }

    private void addContextFactory(final BatchBuilder builder, final String contextName) {
        final Reference appReference = NamespaceObjectFactory.createReference(contextName);
        final BinderService<Reference> binderService = new BinderService<Reference>(contextName, Values.immediateValue(appReference));
        builder.addService(JavaContextService.SERVICE_NAME.append(contextName), binderService)
            .addDependency(JavaContextService.SERVICE_NAME, Context.class, binderService.getContextInjector());
    }

    public boolean isSupportEvents() {
        return supportEvents;
    }

    void setSupportEvents(boolean supportEvents) {
        this.supportEvents = supportEvents;
    }

    public boolean isBindAppContext() {
        return bindAppContext;
    }

    void setBindAppContext(boolean bindAppContext) {
        this.bindAppContext = bindAppContext;
    }

    public boolean isBindModuleContext() {
        return bindModuleContext;
    }

    void setBindModuleContext(boolean bindModuleContext) {
        this.bindModuleContext = bindModuleContext;
    }

    public boolean isBindCompContext() {
        return bindCompContext;
    }

    void setBindCompContext(boolean bindCompContext) {
        this.bindCompContext = bindCompContext;
    }

    /** {@inheritDoc} */
    protected void getClearingUpdates(List<? super AbstractSubsystemUpdate<NamingSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }
}
