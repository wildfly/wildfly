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
