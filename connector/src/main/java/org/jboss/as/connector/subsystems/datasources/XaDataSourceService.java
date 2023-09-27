/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecoveryRegistry;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;

/**
 * XA data-source service implementation.
 * @author John Bailey
 * @author Stefano Maestri
 */
public class XaDataSourceService extends AbstractDataSourceService {

    private final InjectedValue<ModifiableXaDataSource> dataSourceConfig = new InjectedValue<ModifiableXaDataSource>();

    public XaDataSourceService(final String dsName, final ContextNames.BindInfo jndiName, final ClassLoader classLoader) {
        super(dsName, jndiName, classLoader);
    }

    public XaDataSourceService(final String dsName, final ContextNames.BindInfo jndiName) {
        this(dsName, jndiName, null);
    }

    @Override
    protected synchronized void stopService() {
        if (deploymentMD != null &&
                deploymentMD.getRecovery() != null &&
                transactionIntegrationValue.getValue() != null &&
                transactionIntegrationValue.getValue().getRecoveryRegistry() != null) {

            XAResourceRecoveryRegistry rr = transactionIntegrationValue.getValue().getRecoveryRegistry();

            for (XAResourceRecovery recovery : deploymentMD.getRecovery()) {
                if (recovery != null) {
                    try {
                        recovery.shutdown();
                    } catch (Exception e) {
                        ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.errorDuringRecoveryShutdown(e);
                    } finally {
                        rr.removeXAResourceRecovery(recovery);
                    }
                }
            }
        }

        super.stopService();
    }

    @Override
    public AS7DataSourceDeployer getDeployer() throws ValidateException {
        return new AS7DataSourceDeployer(dataSourceConfig.getValue().getUnModifiableInstance());
    }

    public Injector<ModifiableXaDataSource> getDataSourceConfigInjector() {
        return dataSourceConfig;
    }
}
