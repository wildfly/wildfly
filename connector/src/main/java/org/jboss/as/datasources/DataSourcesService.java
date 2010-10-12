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

import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ConnectorConfigService.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
final class DataSourcesService implements Service<DataSources> {

    private final DataSources value;

    /** create an instance **/
    public DataSourcesService(DataSources value) {
        this.value = value;
    }

    @Override
    public DataSources getValue() throws IllegalStateException {
        return DataSourcesServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        // TODO invoke dsDeployer, merging and registering to mdr/jndi
    }

    @Override
    public void stop(StopContext context) {

    }

}
