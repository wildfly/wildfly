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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Driver;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.arjuna.ats.jbossatx.jta.TransactionManagerService;

/**
 * Base service for managing a data-source.
 * @author John Bailey
 */
public abstract class AbstractDataSourceService implements Service<DataSource> {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source");
    private final InjectedValue<TransactionIntegration> transactionIntegrationValue = new InjectedValue<TransactionIntegration>();
    private final InjectedValue<Driver> driverValue = new InjectedValue<Driver>();
    private final InjectedValue<ManagementRepository> managementRepository = new InjectedValue<ManagementRepository>();

    private final String jndiName;

    private javax.sql.DataSource sqlDataSource;

    public AbstractDataSourceService(final String jndiName) {
        this.jndiName = jndiName;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        try {
            final ManagedConnectionFactory mcf = createManagedConnectionFactory(jndiName, driverValue.getValue());
            final PoolConfiguration pc = createPoolConfig();
            final Pool pool = createPool(jndiName, mcf, pc);
            final ConnectionManager cm = createConnectionManager(jndiName, pool);
            sqlDataSource = (javax.sql.DataSource) mcf.createConnectionFactory(cm);

            org.jboss.jca.core.api.management.DataSource mgtDs = new org.jboss.jca.core.api.management.DataSource(false);// Register
                                                                                                                         // data
                                                                                                                         // sources
            mgtDs.setJndiName(jndiName);
            mgtDs.setPoolConfiguration(pc);
            mgtDs.setPool(pool);

            log.debugf("Adding management datasource: %s", mgtDs);
            managementRepository.getValue().getDataSources().add(mgtDs);
        } catch (Throwable t) {
            throw new StartException("Error during the deployment of " + jndiName, t);
        }
    }

    public synchronized void stop(StopContext stopContext) {

        sqlDataSource = null;
    }

    public synchronized DataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return sqlDataSource;
    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return transactionIntegrationValue;
    }

    public Injector<Driver> getDriverInjector() {
        return driverValue;
    }

    public Injector<ManagementRepository> getmanagementRepositoryInjector() {
        return managementRepository;
    }

    protected abstract BaseWrapperManagedConnectionFactory createManagedConnectionFactory(final String jndiName,
            final Driver driver) throws ResourceException, StartException;

    protected abstract Pool createPool(final String jndiName, final ManagedConnectionFactory mcf, final PoolConfiguration pc);

    protected abstract ConnectionManager createConnectionManager(final String jndiName, final Pool pool);

    protected abstract PoolConfiguration createPoolConfig();

    /**
     * Create an instance of the pool configuration based on the input
     * @param pp The pool parameters
     * @param tp The timeout parameters
     * @param vp The validation parameters
     * @return The configuration
     */
    protected PoolConfiguration createPoolConfiguration(final CommonPool pp, final CommonTimeOut tp, final CommonValidation vp) {
        final PoolConfiguration pc = new PoolConfiguration();

        if (pp != null) {
            if (pp.getMinPoolSize() != null)
                pc.setMinSize(pp.getMinPoolSize().intValue());

            if (pp.getMaxPoolSize() != null)
                pc.setMaxSize(pp.getMaxPoolSize().intValue());

            if (pp.isPrefill() != null)
                pc.setPrefill(pp.isPrefill());

            if (pp.isUseStrictMin() != null)
                pc.setStrictMin(pp.isUseStrictMin());
        }

        if (tp != null) {
            if (tp.getBlockingTimeoutMillis() != null)
                pc.setBlockingTimeout(tp.getBlockingTimeoutMillis().longValue());

            if (tp.getIdleTimeoutMinutes() != null)
                pc.setIdleTimeout(tp.getIdleTimeoutMinutes().longValue());
        }

        if (vp != null) {
            if (vp.isBackgroundValidation() != null)
                pc.setBackgroundValidation(vp.isBackgroundValidation().booleanValue());

            if (vp.getBackgroundValidationMinutes() != null)
                pc.setBackgroundValidationMinutes(vp.getBackgroundValidationMinutes().intValue());

            if (vp.isUseFastFail() != null)
                pc.setUseFastFail(vp.isUseFastFail());
        }

        return pc;
    }

    protected String buildConfigPropsString(Map<String, String> configProps) {
        final StringBuffer valueBuf = new StringBuffer();
        for (Map.Entry<String, String> connProperty : configProps.entrySet()) {
            valueBuf.append(connProperty.getKey());
            valueBuf.append("=");
            valueBuf.append(connProperty.getValue());
            valueBuf.append(";");
        }
        return valueBuf.toString();
    }

    protected TransactionIntegration getTransactionIntegration() {
        AccessController.doPrivileged(new SetContextLoaderAction(TransactionIntegration.class.getClassLoader()));
        try {
            return transactionIntegrationValue.getValue();
        } finally {
            AccessController.doPrivileged(CLEAR_ACTION);
        }
    }

    private static final SetContextLoaderAction CLEAR_ACTION = new SetContextLoaderAction(null);

    private static class SetContextLoaderAction implements PrivilegedAction<Void> {

        private final ClassLoader classLoader;

        public SetContextLoaderAction(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public Void run() {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
        }
    }
}
