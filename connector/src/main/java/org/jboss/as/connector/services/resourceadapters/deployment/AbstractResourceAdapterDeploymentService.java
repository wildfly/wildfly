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

package org.jboss.as.connector.services.resourceadapters.deployment;

import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;
import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;

import javax.naming.Reference;
import javax.resource.spi.ResourceAdapter;
import javax.security.auth.Subject;
import javax.transaction.TransactionManager;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.resourceadapter.WorkManagerSecurity;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.security.CallbackImpl;
import org.jboss.as.connector.security.ElytronSubjectFactory;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.AdminObjectReferenceFactoryService;
import org.jboss.as.connector.services.resourceadapters.AdminObjectService;
import org.jboss.as.connector.services.resourceadapters.ConnectionFactoryReferenceFactoryService;
import org.jboss.as.connector.services.resourceadapters.ConnectionFactoryService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.services.workmanager.NamedWorkManager;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.connector.util.JCAValidatorFactory;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.jca.common.api.metadata.common.SecurityMetadata;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.spec.ConfigProperty;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.api.metadata.spec.XsdString;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.security.picketbox.PicketBoxSubjectFactory;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecovery;
import org.jboss.jca.core.spi.transaction.recovery.XAResourceRecoveryRegistry;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.BeanValidation;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.BasicLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.ClearContextClassLoaderAction;
import org.wildfly.security.manager.action.SetContextClassLoaderFromClassAction;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public abstract class AbstractResourceAdapterDeploymentService {

    // Must be set by the start method
    protected ResourceAdapterDeployment value;

    protected final InjectedValue<AS7MetadataRepository> mdr = new InjectedValue<AS7MetadataRepository>();

    protected final InjectedValue<ResourceAdapterRepository> raRepository = new InjectedValue<ResourceAdapterRepository>();

    protected final InjectedValue<ResourceAdapterDeploymentRegistry> registry = new InjectedValue<ResourceAdapterDeploymentRegistry>();

    protected final InjectedValue<ManagementRepository> managementRepository = new InjectedValue<ManagementRepository>();

    protected final InjectedValue<JcaSubsystemConfiguration> config = new InjectedValue<JcaSubsystemConfiguration>();
    protected final InjectedValue<TransactionIntegration> txInt = new InjectedValue<TransactionIntegration>();
    protected final InjectedValue<SubjectFactory> subjectFactory = new InjectedValue<SubjectFactory>();
    protected final InjectedValue<CachedConnectionManager> ccmValue = new InjectedValue<CachedConnectionManager>();
    protected final InjectedValue<ExecutorService> executorServiceInjector = new InjectedValue<ExecutorService>();
    private final InjectedValue<ServerSecurityManager> secManager = new InjectedValue<ServerSecurityManager>();

    protected String raRepositoryRegistrationId;
    protected String connectorServicesRegistrationName;
    protected String mdrRegistrationName;

    public ResourceAdapterDeployment getValue() {
        return ConnectorServices.notNull(value);
    }


    public void unregisterAll(String deploymentName) {
        if (value != null) {
            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Unregistering: %s", deploymentName);

            if (registry != null && registry.getValue() != null) {
                registry.getValue().unregisterResourceAdapterDeployment(value);
            }

            if (managementRepository != null && managementRepository.getValue() != null &&
                value.getDeployment() != null && value.getDeployment().getConnector() != null) {
                managementRepository.getValue().getConnectors().remove(value.getDeployment().getConnector());
            }

            if (mdr != null && mdr.getValue() != null && value.getDeployment() != null
                    && value.getDeployment().getCfs() != null && value.getDeployment().getCfJndiNames() != null) {
                for (int i = 0; i < value.getDeployment().getCfs().length; i++) {
                    try {
                        String cf = value.getDeployment().getCfs()[i].getClass().getName();
                        String jndi = value.getDeployment().getCfJndiNames()[i];

                        mdr.getValue().unregisterJndiMapping(value.getDeployment().getURL().toExternalForm(), cf, jndi);

                    } catch (Throwable nfe) {
                        DEPLOYMENT_CONNECTOR_LOGGER.debug("Exception during JNDI unbinding", nfe);
                    }
                }
            }

            if (mdr != null && mdr.getValue() != null && value.getDeployment().getAos() != null
                    && value.getDeployment().getAoJndiNames() != null) {
                for (int i = 0; i < value.getDeployment().getAos().length; i++) {
                    try {
                        String ao = value.getDeployment().getAos()[i].getClass().getName();
                        String jndi = value.getDeployment().getAoJndiNames()[i];

                        mdr.getValue().unregisterJndiMapping(value.getDeployment().getURL().toExternalForm(), ao, jndi);
                    } catch (Throwable nfe) {
                        DEPLOYMENT_CONNECTOR_LOGGER.debug("Exception during JNDI unbinding", nfe);
                    }
                }
            }

            if (value!= null && value.getDeployment() != null && value.getDeployment().getRecovery() != null && txInt !=null && txInt.getValue() != null) {
                XAResourceRecoveryRegistry rr = txInt.getValue().getRecoveryRegistry();

                if (rr != null) {
                    for (XAResourceRecovery recovery : value.getDeployment().getRecovery()) {
                        if (recovery!= null) {
                            try {
                                recovery.shutdown();
                            } catch (Exception e) {
                                DEPLOYMENT_CONNECTOR_LOGGER.errorDuringRecoveryShutdown(e);
                            } finally {
                                rr.removeXAResourceRecovery(recovery);
                            }
                        }
                    }
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getConnectionManagers() != null) {
                for (ConnectionManager cm : value.getDeployment().getConnectionManagers()) {
                    cm.shutdown();
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getResourceAdapter() != null) {
                ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(value.getDeployment().getResourceAdapter().getClass().getClassLoader());
                    value.getDeployment().getResourceAdapter().stop();
                } catch (Throwable nfe) {
                    DEPLOYMENT_CONNECTOR_LOGGER.errorStoppingRA(nfe);
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                }
            }

            if (value.getDeployment() != null && value.getDeployment().getBootstrapContextIdentifier() != null) {
                BootstrapContextCoordinator.getInstance().removeBootstrapContext(value.getDeployment().getBootstrapContextIdentifier());
            }
        }
        if (raRepositoryRegistrationId != null  && raRepository != null && raRepository.getValue() != null) {
            try {
                raRepository.getValue().unregisterResourceAdapter(raRepositoryRegistrationId);
            } catch (Throwable e) {
                DEPLOYMENT_CONNECTOR_LOGGER.debug("Failed to unregister RA from RA Repository", e);
            }
        }
        if (connectorServicesRegistrationName != null) {
            try {
                ConnectorServices.unregisterResourceAdapterIdentifier(connectorServicesRegistrationName);
            } catch (Throwable e) {
                DEPLOYMENT_CONNECTOR_LOGGER.debug("Failed to unregister RA from ConnectorServices", e);
            }
        }
        if (mdrRegistrationName != null && mdr != null && mdr.getValue() != null) {
            try {
                mdr.getValue().unregisterResourceAdapter(mdrRegistrationName);
            } catch (Throwable e) {
                DEPLOYMENT_CONNECTOR_LOGGER.debug("Failed to unregister RA from MDR", e);
            }
        }
    }

    public Injector<AS7MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<ResourceAdapterRepository> getRaRepositoryInjector() {
        return raRepository;
    }

    public Injector<ManagementRepository> getManagementRepositoryInjector() {
        return managementRepository;
    }

    public Injector<ResourceAdapterDeploymentRegistry> getRegistryInjector() {
        return registry;
    }

    public InjectedValue<JcaSubsystemConfiguration> getConfig() {
        return config;
    }

    public InjectedValue<TransactionIntegration> getTxIntegration() {
        return txInt;
    }

    public Injector<TransactionIntegration> getTxIntegrationInjector() {
        return txInt;
    }

    public Injector<JcaSubsystemConfiguration> getConfigInjector() {
        return config;
    }

    public Injector<SubjectFactory> getSubjectFactoryInjector() {
        return subjectFactory;
    }

    public Injector<ServerSecurityManager> getServerSecurityManager() {
        return secManager;
    }

    public Injector<CachedConnectionManager> getCcmInjector() {
        return ccmValue;
    }

    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executorServiceInjector;
    }

    protected final ExecutorService getLifecycleExecutorService() {
        ExecutorService result = executorServiceInjector.getOptionalValue();
        if (result == null) {
            // We added injection of the server executor late in WF 8, so in case some
            // add-on projects don't know to inject it....
            final ThreadGroup threadGroup = new ThreadGroup("ResourceAdapterDeploymentService ThreadGroup");
            final String namePattern = "ResourceAdapterDeploymentService Thread Pool -- %t";
            final ThreadFactory threadFactory = doPrivileged(new PrivilegedAction<JBossThreadFactory>() {
                public JBossThreadFactory run() {
                    return new JBossThreadFactory(threadGroup, Boolean.FALSE, null, namePattern, null, null);
                }
            });
            result = Executors.newSingleThreadExecutor(threadFactory);
        }
        return result;
    }

    public ContextNames.BindInfo getBindInfo(String jndi) {
        return ContextNames.bindInfoFor(jndi);
    }

    /**
     * @return {@code true} if the binder service must be created to bind the connection factory
     */
    public boolean isCreateBinderService() {
        return true;
    }

    protected final void cleanupStartAsync(final StartContext context, final String deploymentName,
                final ServiceName serviceName, final Throwable cause) {
        ExecutorService executorService = getLifecycleExecutorService();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO -- one of the 3 previous synchronous calls to this method don't had the TCCL set,
                    // but the other two don't. I (BES 2013/10/21) intepret from that that setting the TCCL
                    // was not necessary and in caller that had it set it was an artifact of
                    WritableServiceBasedNamingStore.pushOwner(serviceName);
                    unregisterAll(deploymentName);
                } finally {
                    WritableServiceBasedNamingStore.popOwner();
                    context.failed(ConnectorLogger.ROOT_LOGGER.failedToStartRaDeployment(cause, deploymentName));
                }
            }
        };
        try {
            executorService.execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        } finally {
            context.asynchronous();
        }
    }

    protected void stopAsync(final StopContext context, final String deploymentName, final ServiceName serviceName) {
        ExecutorService executorService = getLifecycleExecutorService();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    DEPLOYMENT_CONNECTOR_LOGGER.debugf("Stopping service %s", serviceName);
                    WritableServiceBasedNamingStore.pushOwner(serviceName);
                    unregisterAll(deploymentName);
                } finally {
                    WritableServiceBasedNamingStore.popOwner();
                    context.complete();
                }
            }
        };
        try {
            executorService.execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        } finally {
            context.asynchronous();
        }
    }

    protected abstract class AbstractWildFlyRaDeployer extends AbstractResourceAdapterDeployer {

        protected final ServiceTarget serviceTarget;
        protected final URL url;
        protected final String deploymentName;
        protected final File root;
        protected final ClassLoader cl;
        protected final Connector cmd;
        protected final ServiceName deploymentServiceName;

        protected AbstractWildFlyRaDeployer(ServiceTarget serviceTarget, URL url, String deploymentName, File root, ClassLoader cl,
                                            Connector cmd, final ServiceName deploymentServiceName) {
            super(true);
            this.serviceTarget = serviceTarget;
            this.url = url;
            this.deploymentName = deploymentName;
            this.root = root;
            this.cl = cl;
            this.cmd = cmd;
            this.deploymentServiceName = deploymentServiceName;
        }

        public abstract CommonDeployment doDeploy() throws Throwable;

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf) throws Throwable {
            throw ConnectorLogger.ROOT_LOGGER.jndiBindingsNotSupported();
        }

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf, final String jndi) throws Throwable {

            mdr.getValue().registerJndiMapping(url.toExternalForm(), cf.getClass().getName(), jndi);

            DEPLOYMENT_CONNECTOR_LOGGER.registeredConnectionFactory(jndi);

            final ConnectionFactoryService connectionFactoryService = new ConnectionFactoryService(cf);

            final ServiceName connectionFactoryServiceName = ConnectionFactoryService.SERVICE_NAME_BASE.append(jndi);

            ServiceBuilder connectionFactoryBuilder = serviceTarget.addService(connectionFactoryServiceName, connectionFactoryService);
            if (deploymentServiceName != null)
                connectionFactoryBuilder.addDependency(deploymentServiceName);

            connectionFactoryBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

            // use a BindInfo to build the referenceFactoryService's service name.
            // if the CF is in a java:app/ or java:module/ namespace, we need the whole bindInfo's binder service name
            // to distinguish CFs with same name in different application (or module).
            final ContextNames.BindInfo bindInfo = getBindInfo(jndi);

            final ConnectionFactoryReferenceFactoryService referenceFactoryService = new ConnectionFactoryReferenceFactoryService();
            final ServiceName referenceFactoryServiceName = ConnectionFactoryReferenceFactoryService.SERVICE_NAME_BASE
                    .append(bindInfo.getBinderServiceName());
            serviceTarget.addService(referenceFactoryServiceName, referenceFactoryService)
                    .addDependency(connectionFactoryServiceName, Object.class, referenceFactoryService.getConnectionFactoryInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            if (isCreateBinderService()) {
                final BinderService binderService = new BinderService(bindInfo.getBindName());
                serviceTarget
                        .addService(bindInfo.getBinderServiceName(), binderService)
                        .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class,
                                binderService.getManagedObjectInjector())
                        .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class,
                                binderService.getNamingStoreInjector())
                        .addListener(new LifecycleListener() {
                            public void handleEvent(final ServiceController<? extends Object> controller, final LifecycleEvent event) {
                                switch (event) {
                                    case UP: {
                                        DEPLOYMENT_CONNECTOR_LOGGER.boundJca("ConnectionFactory", jndi);
                                        break;
                                    }
                                    case DOWN: {
                                        DEPLOYMENT_CONNECTOR_LOGGER.unboundJca("ConnectionFactory", jndi);
                                        break;
                                    }
                                    case REMOVED: {
                                        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Removed JCA ConnectionFactory [%s]", jndi);
                                    }
                                }
                            }
                        })
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
            }

            // AS7-2222: Just hack it
            if (cf instanceof javax.resource.Referenceable) {
                ((javax.resource.Referenceable)cf).setReference(new Reference(jndi));
            }

            return new String[] { jndi };
        }

        @Override
        public String[] bindAdminObject(URL url, String deployment, Object ao) throws Throwable {
            throw ConnectorLogger.ROOT_LOGGER.jndiBindingsNotSupported();
        }

        @Override
        public String[] bindAdminObject(URL url, String deployment, Object ao, final String jndi) throws Throwable {

            mdr.getValue().registerJndiMapping(url.toExternalForm(), ao.getClass().getName(), jndi);

            DEPLOYMENT_CONNECTOR_LOGGER.registeredAdminObject(jndi);

            final AdminObjectService adminObjectService = new AdminObjectService(ao);

            final ServiceName adminObjectServiceName = AdminObjectService.SERVICE_NAME_BASE.append(jndi);
            serviceTarget.addService(adminObjectServiceName, adminObjectService).setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            // use a BindInfo to build the referenceFactoryService's service name.
            // if the CF is in a java:app/ or java:module/ namespace, we need the whole bindInfo's binder service name
            // to distinguish CFs with same name in different application (or module).
            final ContextNames.BindInfo bindInfo = getBindInfo(jndi);

            final AdminObjectReferenceFactoryService referenceFactoryService = new AdminObjectReferenceFactoryService();
            final ServiceName referenceFactoryServiceName = AdminObjectReferenceFactoryService.SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName());
            serviceTarget.addService(referenceFactoryServiceName, referenceFactoryService)
                    .addDependency(adminObjectServiceName, Object.class, referenceFactoryService.getAdminObjectInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            if (isCreateBinderService()) {
                final BinderService binderService = new BinderService(bindInfo.getBindName());
                final ServiceName binderServiceName = bindInfo.getBinderServiceName();

                serviceTarget
                        .addService(binderServiceName, binderService)
                        .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class,
                                binderService.getManagedObjectInjector())
                        .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class,
                                binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {

                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                DEPLOYMENT_CONNECTOR_LOGGER.boundJca("AdminObject", jndi);
                                break;
                            }
                            case DOWN: {
                                DEPLOYMENT_CONNECTOR_LOGGER.unboundJca("AdminObject", jndi);
                                break;
                            }
                            case REMOVED: {
                                DEPLOYMENT_CONNECTOR_LOGGER.debugf("Removed JCA AdminObject [%s]", jndi);
                            }
                        }
                    }
                }).setInitialMode(ServiceController.Mode.ACTIVE).install();
            }
            // AS7-2222: Just hack it
            if (ao instanceof javax.resource.Referenceable) {
                ((javax.resource.Referenceable)ao).setReference(new Reference(jndi));
            }

            return new String[] { jndi };
        }

        @Override
        protected abstract boolean checkActivation(Connector cmd, Activation activation);

        @Override
        protected boolean checkConfigurationIsValid() {
            return this.getConfiguration() != null;
        }

        @Override
        protected PrintWriter getLogPrintWriter() {
            return new JBossLogPrintWriter(deploymentName, (BasicLogger)this.log);
        }

        @Override
        protected File getReportDirectory() {
            // TODO: evaluate if provide something in config about that. atm
            // returning null and so skip its use
            return null;
        }

        @Override
        protected TransactionManager getTransactionManager() {
            if (! WildFlySecurityManager.isChecking()) {
                currentThread().setContextClassLoader(TransactionIntegration.class.getClassLoader());
            } else {
                doPrivileged(new SetContextClassLoaderFromClassAction(TransactionIntegration.class));
            }
            try {
                return getTxIntegration().getValue().getTransactionManager();
            } finally {
                if (! WildFlySecurityManager.isChecking()) {
                    currentThread().setContextClassLoader(null);
                } else {
                    doPrivileged(ClearContextClassLoaderAction.getInstance());
                }
            }
        }

        @Override
        public Object initAndInject(String className, List<? extends ConfigProperty> configs, ClassLoader cl)
                throws DeployException {
            try {
                Class clz = Class.forName(className, true, cl);
                Object o = clz.newInstance();

                if (configs != null) {
                    Injection injector = new Injection();
                    for (ConfigProperty cpmd : configs) {
                        if (cpmd.isValueSet()) {

                            if (XsdString.isNull(cpmd.getConfigPropertyType())) {
                                injector.inject(o,
                                        cpmd.getConfigPropertyName().getValue(),
                                        cpmd.getConfigPropertyValue().getValue());
                            } else {
                                injector.inject(o,
                                        cpmd.getConfigPropertyName().getValue(),
                                        cpmd.getConfigPropertyValue().getValue(),
                                        cpmd.getConfigPropertyType().getValue());
                            }


                        }
                    }
                }

                return o;
            } catch (Throwable t) {
                throw ConnectorLogger.ROOT_LOGGER.deploymentFailed(t, className);
            }
        }

        @Override
        protected void registerResourceAdapterToMDR(URL url, File file, Connector connector, Activation ij)
                throws AlreadyExistsException {
            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Registering ResourceAdapter %s", deploymentName);
            mdr.getValue().registerResourceAdapter(deploymentName, file, connector, ij);
            mdrRegistrationName = deploymentName;
        }

        @Override
        protected String registerResourceAdapterToResourceAdapterRepository(ResourceAdapter instance) {
            raRepositoryRegistrationId = raRepository.getValue().registerResourceAdapter(instance);
            // make a note of this identifier for future use
            if (connectorServicesRegistrationName != null) {
                ConnectorServices.registerResourceAdapterIdentifier(connectorServicesRegistrationName, raRepositoryRegistrationId);
            }
            return raRepositoryRegistrationId;

        }

        @Override
        protected org.jboss.jca.core.spi.security.SubjectFactory getSubjectFactory(
                SecurityMetadata securityMetadata, final String jndiName) throws DeployException {
            if (securityMetadata == null)
                return null;
            final String securityDomain = securityMetadata.resolveSecurityDomain();
            if (securityMetadata instanceof org.jboss.as.connector.metadata.api.common.SecurityMetadata &&
                    ((org.jboss.as.connector.metadata.api.common.SecurityMetadata)securityMetadata).isElytronEnabled()) {
                try {
                    return new ElytronSubjectFactory(null, new URI(jndiName));
                } catch (URISyntaxException e) {
                    throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
                }
            } else if (securityDomain == null || securityDomain.trim().equals("")) {
                return null;
            } else {
                return new PicketBoxSubjectFactory(subjectFactory.getValue()){

                    @Override
                    public Subject createSubject(final String sd) {
                        ServerSecurityManager sm = secManager.getOptionalValue();
                        if (sm != null) {
                            sm.push(sd);
                        }
                        try {
                            return super.createSubject(sd);
                        } finally {
                            if (sm != null) {
                                sm.pop();
                            }
                        }
                    }
                };
            }
        }

        @Override
        protected Callback createCallback(org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity workManagerSecurity) {
            if (workManagerSecurity != null) {
                if (workManagerSecurity instanceof WorkManagerSecurity){
                    WorkManagerSecurity wms = (WorkManagerSecurity) workManagerSecurity;
                    String[] defaultGroups = wms.getDefaultGroups() != null ?
                            wms.getDefaultGroups().toArray(new String[workManagerSecurity.getDefaultGroups().size()]) : null;

                    return new CallbackImpl(wms.isMappingRequired(), wms.getDomain(), wms.isElytronEnabled(),
                            wms.getDefaultPrincipal(), defaultGroups, wms.getUserMappings(), wms.getGroupMappings());
                } else {
                    return super.createCallback(workManagerSecurity);

                }
            }
            return null;
        }

        @Override
        protected void setCallbackSecurity(org.jboss.jca.core.api.workmanager.WorkManager workManager, Callback cb) {
            if (cb instanceof  CallbackImpl) {
                if (((CallbackImpl) cb).isElytronEnabled() != ((NamedWorkManager) workManager).isElytronEnabled())
                    throw ConnectorLogger.ROOT_LOGGER.invalidElytronWorkManagerSetting();
                workManager.setCallbackSecurity(cb);

            } else {
                super.setCallbackSecurity(workManager, cb);
            }
        }

        @Override
        protected TransactionIntegration getTransactionIntegration() {
            return getTxIntegration().getValue();
        }

        @Override
        protected CachedConnectionManager getCachedConnectionManager() {
            return ccmValue.getValue();
        }

        // Override this method to change how jndiName is build in AS7
        @Override
        protected String buildJndiName(String rawJndiName, Boolean javaContext) {
            final String jndiName;
            if (!rawJndiName.startsWith("java:")) {
                if (rawJndiName.startsWith("jboss/")) {
                    // Bind to java:jboss/ namespace
                    jndiName = "java:" + rawJndiName;
                } else {
                    // Bind to java:/ namespace
                    jndiName= "java:/" + rawJndiName;
                }
            } else {
                jndiName = rawJndiName;
            }
            return jndiName;
        }

        @Override
        protected BeanValidation getBeanValidation() {
            return new BeanValidation(new JCAValidatorFactory(cl));
        }
    }
}
