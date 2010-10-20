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

package org.jboss.as.osgi.parser;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.osgi.parser.OSGiSubsystemState.Activation;
import org.jboss.as.osgi.parser.OSGiSubsystemState.OSGiModule;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.spi.NotImplementedException;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Subsystem element representing the OSGi subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public final class OSGiSubsystemElement extends AbstractSubsystemElement<OSGiSubsystemElement> {

    private static final long serialVersionUID = 1543336372548202423L;

    private OSGiSubsystemState subsystemState = new OSGiSubsystemState();

    OSGiSubsystemElement() {
        super(OSGiExtension.NAMESPACE);
    }

    OSGiSubsystemState getSubsystemState() {
        return subsystemState;
    }

    void setSubsystemState(OSGiSubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected Class<OSGiSubsystemElement> getElementClass() {
        return OSGiSubsystemElement.class;
    }

    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<OSGiSubsystemElement, ?>> list) {
        list.add(new OSGiSubsystemUpdate(subsystemState));
    }

    @Override
    protected boolean isEmpty() {
        return subsystemState.isEmpty();
    }

    @Override
    protected AbstractSubsystemAdd<OSGiSubsystemElement> getAdd() {
        return new OSGiSubsystemAdd();
    }

    @Override
    protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        throw new NotImplementedException();
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        synchronized (this) {
            Activation policy = subsystemState.getActivationPolicy();
            streamWriter.writeStartElement(Element.ACTIVATION.getLocalName());
            streamWriter.writeAttribute(Attribute.POLICY.getLocalName(), policy.name().toLowerCase());
            streamWriter.writeEndElement();

            Map<String, Object> properties = subsystemState.getProperties();
            if (properties.isEmpty() == false) {
                streamWriter.writeStartElement(Element.PROPERTIES.getLocalName());
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    streamWriter.writeStartElement(Element.PROPERTY.getLocalName());
                    streamWriter.writeAttribute(Attribute.NAME.getLocalName(), entry.getKey());
                    streamWriter.writeCharacters((String) entry.getValue());
                    streamWriter.writeEndElement();
                }
                streamWriter.writeEndElement();
            }

            List<OSGiModule> modules = subsystemState.getModules();
            if (modules.isEmpty() == false) {
                streamWriter.writeStartElement(Element.MODULES.getLocalName());
                for (OSGiModule module : modules) {
                    ModuleIdentifier identifier = module.getIdentifier();
                    streamWriter.writeStartElement(Element.MODULE.getLocalName());
                    String canonicalName = identifier.getName() + ":" + identifier.getSlot();
                    streamWriter.writeAttribute(Attribute.IDENTIFIER.getLocalName(), canonicalName);
                    if (module.isStart())
                        streamWriter.writeAttribute(Attribute.START.getLocalName(), "true");
                    streamWriter.writeEndElement();
                }
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();
        }
    }
}
