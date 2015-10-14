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

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_PROPERTIES_ATTRIBUTES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a DataSource.
 *
 * @author Stefano Maestrioperation2.get(OP).set("write-attribute");
 */
public class DataSourceAdd extends AbstractDataSourceAdd {
    static final DataSourceAdd INSTANCE = new DataSourceAdd();

    private DataSourceAdd() {
        super(join(DATASOURCE_ATTRIBUTE, DATASOURCE_PROPERTIES_ATTRIBUTES));
    }

    protected AbstractDataSourceService createDataSourceService(final String dsName,final String jndiName) throws OperationFailedException {
        return new LocalDataSourceService(dsName, ContextNames.bindInfoFor(jndiName));
    }

    @Override
    protected boolean isXa() {
        return false;
    }

    @Override
    protected void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
            AbstractDataSourceService dataSourceService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation)
            throws OperationFailedException {

        final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(jndiName);

        dataSourceServiceBuilder.addDependency(dataSourceCongServiceName, ModifiableDataSource.class,
                ((LocalDataSourceService) dataSourceService).getDataSourceConfigInjector());

    }


}
