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

package org.jboss.as.datasources;

import java.util.List;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * A DataSourcesSubsystemElement.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
final class DataSourcesSubsystemElement extends AbstractSubsystemElement<DataSourcesSubsystemElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 6451041006443208660L;

    private DataSources datasources;

    public DataSourcesSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected Class<DataSourcesSubsystemElement> getElementClass() {
        return DataSourcesSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // TODO
    }

    protected void getUpdates(final List<? super AbstractSubsystemUpdate<DataSourcesSubsystemElement, ?>> objects) {
        // empty
    }

    protected boolean isEmpty() {
        return true;
    }

    protected DataSourcesAdd getAdd() {
        final DataSourcesAdd add = new DataSourcesAdd();
        add.setDatasources(datasources);
        return add;
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        // requires restart
    }
}
