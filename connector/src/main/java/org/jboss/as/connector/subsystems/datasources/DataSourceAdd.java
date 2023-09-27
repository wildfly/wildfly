/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
