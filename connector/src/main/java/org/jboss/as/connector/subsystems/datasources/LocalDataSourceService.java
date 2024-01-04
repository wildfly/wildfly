/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * Local data-source service implementation.
 * @author John Bailey
 * @author Stefano Maestri
 */
public class LocalDataSourceService extends AbstractDataSourceService {

    private final InjectedValue<ModifiableDataSource> dataSourceConfig = new InjectedValue<ModifiableDataSource>();

    public LocalDataSourceService(final String dsName, final ContextNames.BindInfo jndiName, final ClassLoader classLoader) {
        super(dsName, jndiName, classLoader);
    }

    public LocalDataSourceService(final String dsName, final ContextNames.BindInfo jndiName) {
        super(dsName, jndiName, null);
    }

    @Override
    public AS7DataSourceDeployer getDeployer() throws ValidateException {
        return new AS7DataSourceDeployer(dataSourceConfig.getValue().getUnModifiableInstance());
    }

    public Injector<ModifiableDataSource> getDataSourceConfigInjector() {
        return dataSourceConfig;
    }
}
