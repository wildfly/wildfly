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

package org.jboss.as.resourceadapters;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A DataSourcesSubsystemElement.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
final class ResourceAdaptersSubsystemElement extends AbstractSubsystemElement<ResourceAdaptersSubsystemElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 6451041006443208660L;

    private ResourceAdapters resourceAdapters;

    public ResourceAdaptersSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected Class<ResourceAdaptersSubsystemElement> getElementClass() {
        return ResourceAdaptersSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // TODO
        streamWriter.writeEndElement();
    }

    @Override
    protected void getUpdates(final List<? super AbstractSubsystemUpdate<ResourceAdaptersSubsystemElement, ?>> objects) {
        // empty
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected ResourceAdaptersAdd getAdd() {
        final ResourceAdaptersAdd add = new ResourceAdaptersAdd();
        add.setResourceAdapters(resourceAdapters);
        return add;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        // requires restart
    }
}
