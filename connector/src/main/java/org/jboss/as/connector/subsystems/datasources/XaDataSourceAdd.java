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

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_PROPERTIES_ATTRIBUTES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a XA data-source.
 *
 * @author John Bailey
 * @author Stefano Maestri
 */
public class XaDataSourceAdd extends AbstractDataSourceAdd {
    static final XaDataSourceAdd INSTANCE = new XaDataSourceAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populateAddModel(operation, model, XADATASOURCE_PROPERTIES.getName(), XA_DATASOURCE_ATTRIBUTE, XA_DATASOURCE_PROPERTIES_ATTRIBUTES);
    }

    protected AbstractDataSourceService createDataSourceService(final String dsName, final String jndiName) throws OperationFailedException {
        return new XaDataSourceService(dsName, jndiName);
    }

    @Override
    protected void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder, AbstractDataSourceService dataSourceService,
                                               String jndiName, ServiceTarget serviceTarget, final ModelNode operation) throws OperationFailedException {

        final ServiceName dataSourceCongServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(jndiName);

        dataSourceServiceBuilder.addDependency(dataSourceCongServiceName, ModifiableXaDataSource.class, ((XaDataSourceService) dataSourceService).getDataSourceConfigInjector());

    }
}
