/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.datasources;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.util.Injection;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.core.naming.ExplicitJndiStrategy;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.deployers.common.AbstractDsDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.logging.Logger;

/**
 * A ConnectorConfigService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
final class DataSourcesService implements Service<DataSources> {
    private static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    private final DataSources value;

    private static final String DEPLOYMENT_NAME = "DataSourcesDeployment";

    private final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txm = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    private final InjectedValue<String> jdbcLocal = new InjectedValue<String>();

    private final InjectedValue<String> jdbcXA = new InjectedValue<String>();

    /** create an instance **/
    public DataSourcesService(DataSources value) {
        super();
        this.value = value;

    }

    @Override
    public DataSources getValue() throws IllegalStateException {
        return DataSourcesServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {

        if (value != null) {
            try {

                String deploymentName = DEPLOYMENT_NAME;

                ClassLoader cl = getContextClassLoader();

                // Set<String> raDeployments =
                // mdr.getValue().getResourceAdapters();

                AS7Deployer deployer = new AS7Deployer(log);

                // TODO: when value will be really injected
                String uniqueJdbcLocalId = null;
                String uniqueJdbcXAId = null;
                // deployer.setJDBCLocal(jdbcLocal.getValue());
                // deployer.setJDBCXA(jdbcXA.getValue());

                deployer.setTransactionManager(getTransactionManager());

                deployer.setMetadataRepository(mdr.getValue());

                deployer.doDeploy(new URL("file://DataSourceDeployment"), deploymentName, uniqueJdbcLocalId, uniqueJdbcXAId,
                        value, cl);

            } catch (Throwable t) {
                throw new StartException(t);
            }
        }
    }

    @Override
    public void stop(StopContext context) {

    }

    protected TransactionManager getTransactionManager() {
        AccessController.doPrivileged(new SetContextLoaderAction(com.arjuna.ats.jbossatx.jta.TransactionManagerService.class
                .getClassLoader()));
        try {
            return getTxm().getValue().getTransactionManager();
        } finally {
            AccessController.doPrivileged(CLEAR_ACTION);
        }
    }

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static class AS7Deployer extends AbstractDsDeployer {

        public AS7Deployer(Logger log) {
            super(log);
        }

        public CommonDeployment doDeploy(URL url, String deploymentName, String uniqueJdbcLocalId, String uniqueJdbcXaId,
                DataSources dataSources, ClassLoader parentClassLoader) throws DeployException {

            return createObjectsAndInjectValue(url, deploymentName, uniqueJdbcLocalId, uniqueJdbcXaId, dataSources,
                    parentClassLoader);
        }

        @Override
        protected ClassLoader getDeploymentClassLoader(String uniqueId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String[] bindConnectionFactory(String deployment, String jndi, Object cf) throws Throwable {
            JndiStrategy js = new ExplicitJndiStrategy();

            return js.bindConnectionFactories(deployment, new Object[] { cf }, new String[] { jndi });
        }

        @Override
        protected Object initAndInject(String className, List<? extends ConfigProperty> configs, ClassLoader cl)
                throws DeployException {
            try {
                Class clz = Class.forName(className, true, cl);
                Object o = clz.newInstance();

                if (configs != null) {
                    Injection injector = new Injection();
                    for (ConfigProperty cpmd : configs) {
                        if (cpmd.isValueSet()) {
                            boolean setValue = true;

                            if (cpmd instanceof org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16) {
                                org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16 cpmd16 = (org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16) cpmd;

                                if (cpmd16.getConfigPropertyIgnore() != null && cpmd16.getConfigPropertyIgnore().booleanValue())
                                    setValue = false;
                            }

                            if (setValue)
                                injector.inject(cpmd.getConfigPropertyType().getValue(), cpmd.getConfigPropertyName()
                                        .getValue(), cpmd.getConfigPropertyValue().getValue(), o);
                        }
                    }
                }

                return o;
            } catch (Throwable t) {
                throw new DeployException("Deployment " + className + " failed", t);
            }

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

    public Value<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxm() {
        return txm;
    }

    public Injector<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxmInjector() {
        return txm;
    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<String> getJdbcLocalInjector() {
        return jdbcLocal;
    }

    public Injector<String> getJdbcXAInjector() {
        return jdbcXA;
    }

}
