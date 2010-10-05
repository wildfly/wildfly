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

package org.jboss.as.service;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


/**
 * SAR deployment subsystem element.
 *
 * @author John Bailey
 */
final class SarSubsystemElement extends AbstractSubsystemElement<SarSubsystemElement> {

    private static final long serialVersionUID = -5742210831771948955L;

    SarSubsystemElement(){
        super(SarExtension.NAMESPACE);
    }

    /** {@inheritDoc} */
    protected Class<SarSubsystemElement> getElementClass() {
        return SarSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<SarSubsystemElement, ?>> list) {
        // nothing to do
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        return true;
    }

    protected AbstractSubsystemAdd<SarSubsystemElement> getAdd() {
        return null;
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }
}
