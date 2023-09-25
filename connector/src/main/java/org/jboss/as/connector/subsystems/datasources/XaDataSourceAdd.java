/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

    package org.jboss.as.connector.subsystems.datasources;

    import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_PROPERTIES_ATTRIBUTES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a XA data-source.
 *
 * @author Stefano Maestri
 */
public class XaDataSourceAdd extends AbstractDataSourceAdd {
    static final XaDataSourceAdd INSTANCE = new XaDataSourceAdd();

    private XaDataSourceAdd() {
        super(join(XA_DATASOURCE_ATTRIBUTE, XA_DATASOURCE_PROPERTIES_ATTRIBUTES));
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);
    }

    protected AbstractDataSourceService createDataSourceService(final String dsName, final String jndiName) throws OperationFailedException {
        return new XaDataSourceService(dsName, ContextNames.bindInfoFor(jndiName));
    }

    @Override
    protected boolean isXa() {
        return true;
    }

    @Override
    protected void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder, AbstractDataSourceService dataSourceService,
                                               String jndiName, ServiceTarget serviceTarget, final ModelNode operation) throws OperationFailedException {

        final ServiceName dataSourceCongServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(jndiName);

        dataSourceServiceBuilder.addDependency(dataSourceCongServiceName, ModifiableXaDataSource.class, ((XaDataSourceService) dataSourceService).getDataSourceConfigInjector());

    }
}
