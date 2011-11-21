/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public class DataSourceAdd extends AbstractDataSourceAdd {
    static final DataSourceAdd INSTANCE = new DataSourceAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        populateAddModel(operation, model, CONNECTION_PROPERTIES.getName(), DATASOURCE_ATTRIBUTE);
    }

    protected AbstractDataSourceService createDataSourceService(final String jndiName) throws OperationFailedException {

        LocalDataSourceService service = new LocalDataSourceService(jndiName);
        return service;
    }

    @Override
    protected void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
            AbstractDataSourceService dataSourceService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation, final ServiceVerificationHandler handler)
            throws OperationFailedException {

        final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(jndiName);

        dataSourceServiceBuilder.addDependency(dataSourceCongServiceName, ModifiableDataSource.class,
                ((LocalDataSourceService) dataSourceService).getDataSourceConfigInjector());

    }
}
