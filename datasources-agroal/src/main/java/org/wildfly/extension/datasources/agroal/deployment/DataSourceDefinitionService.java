/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal.deployment;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.narayana.NarayanaTransactionIntegration;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;
import org.wildfly.extension.datasources.agroal.logging.LoggingDataSourceListener;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.sql.SQLException;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceDefinitionService implements Service<ManagedReferenceFactory>, ContextListAndJndiViewManagedReferenceFactory {

    private final String dataSourceName;
    private final String jndiBinding;
    private final boolean transactional;
    private final AgroalDataSourceConfigurationSupplier dataSourceConfiguration;
    private AgroalDataSource agroalDataSource;

    public DataSourceDefinitionService(ContextNames.BindInfo bindInfo, boolean transactional, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        this.dataSourceName = bindInfo.getParentContextServiceName().getSimpleName() + "." + bindInfo.getBindName().replace('/', '.');
        this.jndiBinding = bindInfo.getBindName();
        this.transactional = transactional;
        this.dataSourceConfiguration = dataSourceConfiguration;
    }

    @Override
    public String getInstanceClassName() {
        return agroalDataSource == null ? DEFAULT_INSTANCE_CLASS_NAME : agroalDataSource.getClass().getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        return agroalDataSource == null ? DEFAULT_JNDI_VIEW_INSTANCE_VALUE : agroalDataSource.toString();
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (transactional) {
            TransactionManager transactionManager = ContextTransactionManager.getInstance();
            TransactionSynchronizationRegistry transactionSynchronizationRegistry = ContextTransactionSynchronizationRegistry.getInstance();

            if (transactionManager == null || transactionSynchronizationRegistry == null) {
                throw AgroalLogger.SERVICE_LOGGER.missingTransactionManager();
            }

            NarayanaTransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager, transactionSynchronizationRegistry, jndiBinding, false);
            dataSourceConfiguration.connectionPoolConfiguration().transactionIntegration(txIntegration);
        }

        try {
            agroalDataSource = AgroalDataSource.from(dataSourceConfiguration, new LoggingDataSourceListener(dataSourceName));
            AgroalLogger.SERVICE_LOGGER.startedDataSource(dataSourceName, jndiBinding);
        } catch (SQLException e) {
            agroalDataSource = null;
            throw AgroalLogger.SERVICE_LOGGER.datasourceStartException(e, dataSourceName);
        }
    }

    @Override
    public void stop(StopContext context) {
        agroalDataSource.close();
        AgroalLogger.SERVICE_LOGGER.stoppedDataSource(dataSourceName);
    }

    @Override
    public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public ManagedReference getReference() {
        return new ImmediateManagedReference(agroalDataSource);
    }

}
