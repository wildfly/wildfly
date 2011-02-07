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

package org.jboss.as.webservices;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The jboss webservices subsystem configuration.
 *
 * @author alessio.soldano@jboss.com
 * @since 08-Nov-2010
 */
public final class WSSubsystemElement extends AbstractSubsystemElement<WSSubsystemElement> {

    private static final long serialVersionUID = -1034512960159372919L;

    private WSConfigurationElement configuration;

    public WSSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<WSSubsystemElement, ?>> list) {
        // TODO: updates not supported for now
    }

    @Override
    protected boolean isEmpty() {
        return true; // TODO: deal with incremental addition/removal of the subsystem's elements
    }

    @Override
    protected AbstractSubsystemAdd<WSSubsystemElement> getAdd() {
        final WSSubsystemAdd add = new WSSubsystemAdd();
        add.setConfiguration(configuration);
        return add;
    }

    @Override
    protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = updateContext.getServiceRegistry().getService(WSServices.CONFIG_SERVICE);
        if (service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
        // add removal of other part of subsystem here?
    }

    @Override
    protected Class<WSSubsystemElement> getElementClass() {
        return WSSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (configuration != null) {
            streamWriter.writeStartElement(Element.CONFIGURATION.getLocalName());
            configuration.writeContent(streamWriter);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    public WSConfigurationElement getConfiguration() {
        return configuration;
    }

    public void setConfiguration(WSConfigurationElement configuration) {
        this.configuration = configuration;
    }

}
