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

package org.jboss.as.jpa.processor;

import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jpa.classloader.TempClassLoader;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.jpa.validator.SerializableValidatorFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Handle the installation of the Persistence Unit service
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.jpa");

    public static final String JNDI_PROPERTY = "jboss.entity.manager.factory.jndi.name";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        handleWarDeployment(phaseContext);
        handleEarDeployment(phaseContext);
        handleJarDeployment(phaseContext);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void handleJarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isEarDeployment(deploymentUnit) && !isWarDeployment(deploymentUnit) && JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            PersistenceUnitMetadataHolder holder;
            if (deploymentRoot != null &&
                    (holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null &&
                    holder.getPersistenceUnits().size() > 0) {
                ArrayList<PersistenceUnitMetadataHolder> puList = new ArrayList<PersistenceUnitMetadataHolder>(1);
                puList.add(holder);
                log.trace("install persistence unit definition for jar " + deploymentRoot.getRootName());
                // assemble and install the PU service
                addPuService(phaseContext, deploymentRoot, puList);
            }
        }
    }

    private void handleWarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWarDeployment(deploymentUnit) && JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            PersistenceUnitMetadataHolder holder;
            ArrayList<PersistenceUnitMetadataHolder> puList = new ArrayList<PersistenceUnitMetadataHolder>(1);

            // handle persistence.xml definition in the root of the war
            if (deploymentRoot != null &&
                    (holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null &&
                    holder.getPersistenceUnits().size() > 0) {
                // assemble and install the PU service
                puList.add(holder);
            }

            // look for persistence.xml in war files in the META-INF/persistence.xml directory
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    if ((holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null
                            && holder.getPersistenceUnits().size() > 0) {

                        // assemble and install the PU service
                        puList.add(holder);
                    }
                }
            }

            // Add EM valve
            final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            if (warMetaData != null && warMetaData.getMergedJBossWebMetaData() != null) {
                List<ValveMetaData> valves = warMetaData.getMergedJBossWebMetaData().getValves();
                if (valves == null) {
                    valves = new ArrayList<ValveMetaData>();
                    warMetaData.getMergedJBossWebMetaData().setValves(valves);
                }
                ValveMetaData valve = new ValveMetaData();
                valve.setModule("org.jboss.as.jpa");
                valve.setValveClass("org.jboss.as.jpa.interceptor.WebNonTxEmCloserValve");
                valves.add(valve);
            }

            log.trace("install persistence unit definitions for war " + deploymentRoot.getRootName());
            addPuService(phaseContext, deploymentRoot, puList);
        }
    }

    private void handleEarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isEarDeployment(deploymentUnit) && JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            // handle META-INF/persistence.xml
            final List<ResourceRoot> deploymentRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
            for (final ResourceRoot root : deploymentRoots) {
                if (!SubDeploymentMarker.isSubDeployment(root)) {
                    PersistenceUnitMetadataHolder holder;
                    ArrayList<PersistenceUnitMetadataHolder> puList = new ArrayList<PersistenceUnitMetadataHolder>(1);

                    if (root != null &&
                            (holder = root.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null &&
                            holder.getPersistenceUnits().size() > 0) {
                        // assemble and install the PU service
                        puList.add(holder);
                    }

                    log.trace("install persistence unit definitions for ear " + root.getRootName());
                    addPuService(phaseContext, root, puList);
                }
            }
        }
    }

    /**
     * Add one PU service per top level deployment that represents
     *
     * @param phaseContext
     * @param resourceRoot
     * @param puList
     * @throws DeploymentUnitProcessingException
     *
     */
    private void addPuService(DeploymentPhaseContext phaseContext, ResourceRoot resourceRoot,
                              ArrayList<PersistenceUnitMetadataHolder> puList
    )
            throws DeploymentUnitProcessingException {

        if (puList.size() > 0) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            final Collection<ComponentDescription> components = eeModuleDescription.getComponentDescriptions();
            if (module == null)
                throw new DeploymentUnitProcessingException("Failed to get module attachment for " + phaseContext.getDeploymentUnit());

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            final ModuleClassLoader classLoader = module.getClassLoader();
            final PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder =
                top(deploymentUnit).getAttachment(PersistenceProviderDeploymentHolder.DEPLOYED_PERSISTENCE_PROVIDER);

            for (PersistenceUnitMetadataHolder holder : puList) {
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                    pu.setClassLoader(classLoader);
                    pu.setTempClassloader(new TempClassLoader(classLoader));
                    try {
                        final PersistenceUnitService service = new PersistenceUnitService(pu, persistenceProviderDeploymentHolder);
                        final HashMap properties = new HashMap();
                        if (!ValidationMode.NONE.equals(pu.getValidationMode())) {
                            ValidatorFactory validatorFactory = SerializableValidatorFactory.getINSTANCE();
                            properties.put("javax.persistence.validation.factory", validatorFactory);
                        }

                        addProviderProperties(pu, properties, persistenceProviderDeploymentHolder);

                        loadPersistenceProviderModule(pu, persistenceProviderDeploymentHolder);

                        final ServiceName puServiceName = PersistenceUnitService.getPUServiceName(pu);
                        // add the PU service as a dependency to all EE components in this scope
                        this.addPUServiceDependencyToComponents(components, puServiceName);

                        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, puServiceName);

                        ServiceBuilder builder = serviceTarget.addService(puServiceName, service);
                        boolean useDefaultDataSource = true;
                        final String jtaDataSource = adjustJndi(pu.getJtaDataSourceName());
                        final String nonJtaDataSource = adjustJndi(pu.getNonJtaDataSourceName());

                        if (jtaDataSource != null) {
                            if (jtaDataSource.startsWith("java:")) {
                                builder.addDependency(ContextNames.serviceNameOfContext(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), jtaDataSource), ManagedReferenceFactory.class, new ManagedReferenceFactoryInjector(service.getJtaDataSourceInjector()));
                                useDefaultDataSource = false;
                            } else {
                                builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(jtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                                useDefaultDataSource = false;
                            }
                        }
                        if (nonJtaDataSource != null) {
                            if (nonJtaDataSource.startsWith("java:")) {
                                builder.addDependency(ContextNames.serviceNameOfContext(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), nonJtaDataSource), ManagedReferenceFactory.class, new ManagedReferenceFactoryInjector(service.getNonJtaDataSourceInjector()));
                                useDefaultDataSource = false;
                            } else {
                                builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(nonJtaDataSource), new CastingInjector<DataSource>(service.getNonJtaDataSourceInjector(), DataSource.class));
                                useDefaultDataSource = false;
                            }
                        }
                        // JPA 2.0 8.2.1.5, container provides default JTA datasource
                        if (useDefaultDataSource) {
                            final String defaultJtaDataSource = adjustJndi(JPAService.getDefaultDataSourceName());
                            if (defaultJtaDataSource != null &&
                                    defaultJtaDataSource.length() > 0) {
                                builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(defaultJtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                                log.trace(puServiceName + " is using the default data source '" + defaultJtaDataSource + "'");
                            }
                        }

                        Iterable<ServiceName> providerDependencies = getProviderDependencies(pu, persistenceProviderDeploymentHolder);
                        if (providerDependencies != null) {
                            builder.addDependencies(providerDependencies);
                        }

                        if (pu.getProperties().containsKey(JNDI_PROPERTY)) {
                            String jndiName = pu.getProperties().get(JNDI_PROPERTY).toString();
                            final ServiceName bindingServiceName = ContextNames.serviceNameOfContext(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), jndiName);
                            final BinderService binderService = new BinderService(jndiName);
                            serviceTarget.addService(bindingServiceName, binderService)
                                    .addDependency(ContextNames.serviceNameOfNamingStore(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), jndiName), NamingStore.class, binderService.getNamingStoreInjector())
                                    .addDependency(puServiceName, PersistenceUnitService.class, new Injector<PersistenceUnitService>() {
                                        @Override
                                        public void inject(final PersistenceUnitService value) throws InjectionException {
                                            binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value.getEntityManagerFactory())));
                                        }

                                        @Override
                                        public void uninject() {
                                            binderService.getNamingStoreInjector().uninject();
                                        }
                                    }).install();
                        }

                        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                            .addInjection(service.getPropertiesInjector(), properties)
                            .install();

                        log.trace("added PersistenceUnitService for '" + puServiceName + "'.  PU is ready for injector action. ");

                    } catch (ServiceRegistryException e) {
                        throw new DeploymentUnitProcessingException("Failed to add persistence unit service for " + pu.getPersistenceUnitName(), e);
                    }
                }
            }
        }
    }

    private DeploymentUnit top(DeploymentUnit deploymentUnit) {
        while (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        return deploymentUnit;
    }

    private String adjustJndi(String dataSourceName) {
        if (dataSourceName != null && !dataSourceName.startsWith("java:")) {
            if (dataSourceName.startsWith("jboss/")) {
                return "java:" + dataSourceName;
            }
            return "java:/" + dataSourceName;
        }

        return dataSourceName;
    }

    private void addProviderProperties(PersistenceUnitMetadata pu, Map properties, PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder) throws
        DeploymentUnitProcessingException {
        String adaptorModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
        PersistenceProviderAdaptor adaptor=null;
        if (persistenceProviderDeploymentHolder != null) {
            adaptor = persistenceProviderDeploymentHolder.getAdapter();
        }
        if (adaptor == null) {
            if (adaptorModule != null) {
                adaptor = PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(pu.getPersistenceProviderClassName(), adaptorModule);
            }
            else {
                adaptor = PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(pu.getPersistenceProviderClassName());
            }
        }
        if (adaptor == null) {
            adaptor = loadPersistenceAdapterModule(pu.getPersistenceProviderClassName(), adaptorModule);
        }

        if (adaptor == null) {
            throw new DeploymentUnitProcessingException("Failed to get adaptor for persistence provider '" + pu.getPersistenceProviderClassName() +"'");
        }
        adaptor.addProviderProperties(properties, pu);
    }

    /**
     * Loads the persistence provider adapter into the PersistenceProviderAdapterRegistry for later access.
     * TODO:  test the case where app contains the persistence provider adapter but also uses a system provided adapter.
     *
     * @param persistenceProviderClass may specify the persistence provider class name (can be null for default Configuration.PROVIDER_CLASS_DEFAULT).
     * @param adapterModule may specify the adapter module name (can be null to use default Configuration.ADAPTER_MODULE_DEFAULT)
     * @return the persistence provider adaptor for the provider class
     * @throws DeploymentUnitProcessingException
     */
    private PersistenceProviderAdaptor loadPersistenceAdapterModule(String persistenceProviderClass, String adapterModule) throws
        DeploymentUnitProcessingException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        PersistenceProviderAdaptor persistenceProviderAdaptor = null;

        if (adapterModule == null) {
            adapterModule = Configuration.ADAPTER_MODULE_DEFAULT;
        }

        if (persistenceProviderClass == null) {
            persistenceProviderClass = Configuration.PROVIDER_CLASS_DEFAULT;
        }

        try {

            Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(adapterModule));
            final ServiceLoader<PersistenceProviderAdaptor> serviceLoader =
                module.loadService(PersistenceProviderAdaptor.class);
            if (serviceLoader != null) {
                for(PersistenceProviderAdaptor adaptor: serviceLoader) {
                    if (persistenceProviderAdaptor != null) {
                        throw new DeploymentUnitProcessingException(
                            "persistence provider adapter module has more than one adapters "
                            + adapterModule);
                    }
                    persistenceProviderAdaptor = adaptor;
                    log.debugf("loaded persistence provider adapter %s", adapterModule);
                }
                if (persistenceProviderAdaptor != null) {
                    persistenceProviderAdaptor.setJtaManager(JtaManagerImpl.getInstance());
                    PersistenceProviderAdapterRegistry.putPersistenceProviderAdaptor(persistenceProviderClass, persistenceProviderAdaptor);
                    PersistenceProviderAdapterRegistry.putPersistenceProviderAdaptor(persistenceProviderClass, adapterModule, persistenceProviderAdaptor);
                }
            }
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException("persistence provider adapter module load error"
                + adapterModule +"(class "+persistenceProviderClass+")",e);
        }
        return persistenceProviderAdaptor;
    }

    /**
     * Handles loading the persistence provider module into PersistenceProviderResolverImpl for later access
     * @param pu is the persistence unit
     * @param persistenceProviderDeploymentHolder holds the persistence provider if one is packaged with the app deployment
     * @throws DeploymentUnitProcessingException
     */
    private void loadPersistenceProviderModule(PersistenceUnitMetadata pu, PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder) throws
        DeploymentUnitProcessingException {

        // only load the persistence provider module if we don't have a persistence provider packaged with the app deployment
        if (persistenceProviderDeploymentHolder == null || persistenceProviderDeploymentHolder.getProvider() == null) {
            String persistenceProviderModule = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
            String persistenceProviderClassName = pu.getPersistenceProviderClassName();

            if (persistenceProviderClassName == null) {
                persistenceProviderClassName = Configuration.PROVIDER_CLASS_DEFAULT;
            }

            // try to determine the provider module name (ignore if we can't, it might already be loaded)
            if (persistenceProviderModule == null) {
                if (persistenceProviderClassName.equals(Configuration.PROVIDER_CLASS_DEFAULT)) {
                    persistenceProviderModule = Configuration.PROVIDER_MODULE_DEFAULT;
                }
            }

            // if we haven't loaded the provider yet, load it
            if (!PersistenceProviderResolverImpl.getInstance().getPersistenceProviders().
                contains(persistenceProviderClassName)) {
                if (persistenceProviderModule != null) {
                    final ModuleLoader moduleLoader = Module.getBootModuleLoader();
                    Module module = null;
                    try {
                        module = moduleLoader.loadModule(ModuleIdentifier.fromString(persistenceProviderModule));
                    } catch (ModuleLoadException e) {
                        throw new DeploymentUnitProcessingException("persistence provider module load error"
                            + persistenceProviderModule + "(class " + persistenceProviderClassName + ")", e);
                    }
                    final ServiceLoader<PersistenceProvider> serviceLoader =
                        module.loadService(PersistenceProvider.class);
                    if (serviceLoader != null) {
                        PersistenceProvider persistenceProvider = null;
                        for (PersistenceProvider provider : serviceLoader) {
                            if (persistenceProvider != null) {
                                throw new DeploymentUnitProcessingException(
                                    "persistence provider module has more than one provider"
                                        + persistenceProviderModule + "(class " + persistenceProviderClassName + ")");
                            }
                            persistenceProvider = provider;
                        }

                        PersistenceProviderResolverImpl.getInstance().addPersistenceProvider(persistenceProvider);
                    }
                }
            }
        }
    }


    private Iterable<ServiceName> getProviderDependencies(PersistenceUnitMetadata pu, PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder) throws
        DeploymentUnitProcessingException {
        String adaptorModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
        PersistenceProviderAdaptor adaptor = null;

        if (persistenceProviderDeploymentHolder != null) {
            adaptor = persistenceProviderDeploymentHolder.getAdapter();
        }
        if (adaptor == null) {
            if (adaptorModule != null) {
                adaptor = PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(pu.getPersistenceProviderClassName(), adaptorModule);
            } else {
                adaptor = PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(pu.getPersistenceProviderClassName());
            }
        }
        if (adaptor == null) {
            throw new DeploymentUnitProcessingException("Failed to get adaptor for persistence provider '" + pu.getPersistenceProviderClassName() + "'");
        }

        Iterable<ServiceName> providerDependencies = adaptor.getProviderDependencies(pu);
        return providerDependencies;
    }

    static boolean isEarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.EAR, context));
    }

    static boolean isWarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.WAR, context));
    }

    private static class ManagedReferenceFactoryInjector implements Injector<ManagedReferenceFactory> {
        private volatile ManagedReference reference;
        private final Injector<DataSource> dataSourceInjector;

        public ManagedReferenceFactoryInjector(Injector<DataSource> dataSourceInjector) {
            this.dataSourceInjector = dataSourceInjector;
        }

        @Override
        public void inject(final ManagedReferenceFactory value) throws InjectionException {
            this.reference = value.getReference();
            dataSourceInjector.inject((DataSource) reference.getInstance());
        }

        @Override
        public void uninject() {
            reference.release();
            reference = null;
        }
    }

    /**
     * Add the <code>puServiceName</code> as a dependency on each of the passed <code>components</code>
     *
     * @param components    The components to which the PU service is added as a dependency
     * @param puServiceName The persistence unit service name
     */
    private void addPUServiceDependencyToComponents(final Collection<ComponentDescription> components, final ServiceName puServiceName) {
        if (components == null || components.isEmpty()) {
            return;
        }
        for (final ComponentDescription component : components) {
            log.debug("Adding dependency on PU service " + puServiceName + " for component " + component.getComponentClassName());
            component.addDependency(puServiceName, ServiceBuilder.DependencyType.REQUIRED);
        }
    }
}
