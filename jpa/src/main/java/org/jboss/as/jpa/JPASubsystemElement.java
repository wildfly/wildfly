/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * @author Scott Marlow
 */
public class JPASubsystemElement extends AbstractSubsystemElement<JPASubsystemElement> {
    JPASubsystemElement() {
        super(JPASubsystemParser.NAMESPACE);
    }

    @Override
    protected void getUpdates(List<? super AbstractSubsystemUpdate<JPASubsystemElement, ?>> objects) {
        // TODO:
    }

    @Override
    protected boolean isEmpty() {
        // TODO:
        return false;
    }

    @Override
    protected AbstractSubsystemAdd<JPASubsystemElement> getAdd() {
        return new JPASubsystemAdd();
    }

    @Override
    protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO:
    }

    @Override
    protected Class<JPASubsystemElement> getElementClass() {
        return JPASubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // TODO: really write out some stuff
        streamWriter.writeEndElement();
    }
}
