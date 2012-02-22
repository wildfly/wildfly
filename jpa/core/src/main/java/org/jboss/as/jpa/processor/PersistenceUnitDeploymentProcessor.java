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

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;

import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderLoader;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jboss.as.jpa.validator.SerializableValidatorFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

/**
 * Handle the installation of the Persistence Unit service
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDeploymentProcessor implements DeploymentUnitProcessor {

    public static final String JNDI_PROPERTY = "jboss.entity.manager.factory.jndi.name";

    private static final AttachmentKey<Map<String,PersistenceProviderAdaptor>> providerAdaptorMapKey = AttachmentKey.create(Map.class);

    private final PersistenceUnitRegistryImpl persistenceUnitRegistry;

    public PersistenceUnitDeploymentProcessor(final PersistenceUnitRegistryImpl persistenceUnitRegistry) {
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        handleWarDeployment(phaseContext);
        handleEarDeployment(phaseContext);
        handleJarDeployment(phaseContext);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        List<PersistenceAdaptorRemoval> removals = context.getAttachmentList(REMOVAL_KEY);
        if (removals != null) {
            for (PersistenceAdaptorRemoval removal : removals) {
                removal.cleanup();
            }
        }
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
                JPA_LOGGER.tracef("install persistence unit definition for jar %s", deploymentRoot.getRootName());
                // assemble and install the PU service
                addPuService(phaseContext, puList);
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
                if (resourceRoot.getRoot().getName().toLowerCase().endsWith(".jar")) {
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

            JPA_LOGGER.tracef("install persistence unit definitions for war %s", deploymentRoot.getRootName());
            addPuService(phaseContext, puList);
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

                    JPA_LOGGER.tracef("install persistence unit definitions for ear %s", root.getRootName());
                    addPuService(phaseContext, puList);
                }
            }
        }
    }

    /**
     * Add one PU service per top level deployment that represents
     *
     * @param phaseContext
     * @param puList
     * @throws DeploymentUnitProcessingException
     *
     */
    private void addPuService(DeploymentPhaseContext phaseContext, ArrayList<PersistenceUnitMetadataHolder> puList
    )
        throws DeploymentUnitProcessingException {

        if (puList.size() > 0) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            final Collection<ComponentDescription> components = eeModuleDescription.getComponentDescriptions();
            if (module == null)
                throw MESSAGES.failedToGetModuleAttachment(phaseContext.getDeploymentUnit());

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            final ModuleClassLoader classLoader = module.getClassLoader();
            PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder = deploymentUnit.getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
            if (persistenceProviderDeploymentHolder == null && deploymentUnit.getParent() != null) {
                persistenceProviderDeploymentHolder = deploymentUnit.getParent().getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
            }



            for (PersistenceUnitMetadataHolder holder : puList) {
                setAnnotationIndexes(holder, deploymentUnit);
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {

                    pu.setClassLoader(classLoader);
                    try {
                        final HashMap properties = new HashMap();
                        if (!ValidationMode.NONE.equals(pu.getValidationMode())) {
                            ValidatorFactory validatorFactory = SerializableValidatorFactory.validatorFactory();
                            properties.put("javax.persistence.validation.factory", validatorFactory);
                        }
                        final PersistenceProviderAdaptor adaptor = getPersistenceProviderAdaptor(pu, persistenceProviderDeploymentHolder, deploymentUnit);


                        PersistenceProvider provider = null;
                        if (persistenceProviderDeploymentHolder != null &&
                            persistenceProviderDeploymentHolder.getProvider() != null) {

                            List<PersistenceProvider> providerList = persistenceProviderDeploymentHolder.getProvider();
                            for (PersistenceProvider persistenceProvider : providerList) {
                                if (persistenceProvider.getClass().getName().equals(pu.getPersistenceProviderClassName())) {
                                    provider = persistenceProvider;
                                    break;
                                }
                            }
                        }
                        //  look provider up if we didn't use the providers packaged with the application
                        if (provider == null) {
                            provider = lookupProvider(pu);
                        }

                        final PersistenceUnitServiceImpl service = new PersistenceUnitServiceImpl(classLoader, pu, adaptor, provider);

                        phaseContext.getDeploymentUnit().addToAttachmentList(REMOVAL_KEY, new PersistenceAdaptorRemoval(pu, adaptor));

                        // add persistence provider specific properties
                        adaptor.addProviderProperties(properties, pu);


                        final ServiceName puServiceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                        // add the PU service as a dependency to all EE components in this scope
                        this.addPUServiceDependencyToComponents(components, puServiceName);

                        deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, puServiceName);

                        ServiceBuilder<PersistenceUnitServiceImpl> builder = serviceTarget.addService(puServiceName, service);
                        boolean useDefaultDataSource = true;
                        final String jtaDataSource = adjustJndi(pu.getJtaDataSourceName());
                        final String nonJtaDataSource = adjustJndi(pu.getNonJtaDataSourceName());

                        if (jtaDataSource != null && jtaDataSource.length() > 0) {
                            if (jtaDataSource.startsWith("java:")) {
                                builder.addDependency(ContextNames.bindInfoForEnvEntry(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), false, jtaDataSource).getBinderServiceName(), ManagedReferenceFactory.class, new ManagedReferenceFactoryInjector(service.getJtaDataSourceInjector()));
                                useDefaultDataSource = false;
                            } else {
                                builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(jtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                                useDefaultDataSource = false;
                            }
                        }
                        if (nonJtaDataSource != null && nonJtaDataSource.length() > 0) {
                            if (nonJtaDataSource.startsWith("java:")) {
                                builder.addDependency(ContextNames.bindInfoForEnvEntry(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), false, nonJtaDataSource).getBinderServiceName(), ManagedReferenceFactory.class, new ManagedReferenceFactoryInjector(service.getNonJtaDataSourceInjector()));
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
                                JPA_LOGGER.tracef("%s is using the default data source '%s'", puServiceName, defaultJtaDataSource);
                            }
                        }

                        adaptor.addProviderDependencies(phaseContext.getServiceRegistry(), serviceTarget, builder, pu);

                        if (pu.getProperties().containsKey(JNDI_PROPERTY)) {
                            String jndiName = pu.getProperties().get(JNDI_PROPERTY).toString();
                            final ContextNames.BindInfo bindingInfo;
                            if (jndiName.startsWith("java:")) {
                                bindingInfo =  ContextNames.bindInfoForEnvEntry(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), false, jndiName);
                            }
                            else {
                                bindingInfo = ContextNames.bindInfoFor(jndiName);
                            }
                            JPA_LOGGER.tracef("binding the entity manager factory to jndi name '%s'", bindingInfo.getAbsoluteJndiName());
                            final BinderService binderService = new BinderService(bindingInfo.getBindName());
                            serviceTarget.addService(bindingInfo.getBinderServiceName(), binderService)
                                .addDependency(bindingInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                                .addDependency(puServiceName, PersistenceUnitServiceImpl.class, new Injector<PersistenceUnitServiceImpl>() {
                                    @Override
                                    public void inject(final PersistenceUnitServiceImpl value) throws
                                        InjectionException {
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
                            .addInjection(persistenceUnitRegistry.getInjector())
                            .install();

                        JPA_LOGGER.tracef("added PersistenceUnitService for '%s'.  PU is ready for injector action.", puServiceName);
                        addManagementConsole(deploymentUnit, pu, service, adaptor);

                    } catch (ServiceRegistryException e) {
                        throw MESSAGES.failedToAddPersistenceUnit(e, pu.getPersistenceUnitName());
                    }

                }
            }
        }
    }

    /**
     * Setup the annotation index map
     *
     * @param puHolder
     * @param deploymentUnit
     */
    private void setAnnotationIndexes(
            final PersistenceUnitMetadataHolder puHolder,
            DeploymentUnit deploymentUnit ) {

        final Map<URL, Index> annotationIndexes = new HashMap<URL, Index>();

        do {
            for (ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {
                final Index index = root.getAttachment(Attachments.ANNOTATION_INDEX);
                if (index != null) {
                    try {
                        JPA_LOGGER.tracef("adding '%s' to annotation index map", root.getRoot().toURL());
                        annotationIndexes.put(root.getRoot().toURL(), index);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            deploymentUnit = deploymentUnit.getParent(); // get annotation indexes for top level also
        }
        while (deploymentUnit != null);

        for (PersistenceUnitMetadata pu : puHolder.getPersistenceUnits()) {
            pu.setAnnotationIndex(annotationIndexes);   // hold onto the annotation index for Persistence Provider use during deployment
        }
    }

    private String adjustJndi(String dataSourceName) {
        if (dataSourceName != null && dataSourceName.length() > 0 && !dataSourceName.startsWith("java:")) {
            if (dataSourceName.startsWith("jboss/")) {
                return "java:" + dataSourceName;
            }
            return "java:/" + dataSourceName;
        }

        return dataSourceName;
    }

    /**
     * Get the persistence provider adaptor.  Will load the adapter module if needed.
     *
     * @param pu
     * @param persistenceProviderDeploymentHolder
     *
     * @return
     * @throws DeploymentUnitProcessingException
     *
     */
    private PersistenceProviderAdaptor getPersistenceProviderAdaptor(
        final PersistenceUnitMetadata pu,
        final PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder,
        final DeploymentUnit deploymentUnit
    ) throws
        DeploymentUnitProcessingException {
        String adaptorModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
        PersistenceProviderAdaptor adaptor = null;
        if (persistenceProviderDeploymentHolder != null) {
            adaptor = persistenceProviderDeploymentHolder.getAdapter();
        }
        if (adaptor == null) {

            if (adaptorModule == null &&
                (pu.getPersistenceProviderClassName() == null ||
                    pu.getPersistenceProviderClassName().equals(Configuration.PROVIDER_CLASS_DEFAULT))) {
                // if using default provider, load default adapter module
                adaptorModule = Configuration.ADAPTER_MODULE_DEFAULT;
            }

            adaptor = getPerDeploymentSharedPersistenceProviderAdaptor(deploymentUnit, adaptorModule);
            if (adaptor == null) {
                try {
                    // will load the persistence provider adaptor (integration classes).  if adaptorModule is null
                    // the noop adaptor is returned (can be used against any provider but the integration classes
                    // are handled externally via properties or code in the persistence provider).
                    adaptor = PersistenceProviderAdaptorLoader.loadPersistenceAdapterModule(adaptorModule);
                } catch (ModuleLoadException e) {
                    throw new DeploymentUnitProcessingException("persistence provider adapter module load error "
                        + adaptorModule, e);
                }
                adaptor = savePerDeploymentSharedPersistenceProviderAdaptor(deploymentUnit, adaptorModule, adaptor);
            }

        }
        if (adaptor == null) {
            throw MESSAGES.failedToGetAdapter(pu.getPersistenceProviderClassName());
        }
        return adaptor;
    }

    /**
     * Will save the PersistenceProviderAdaptor at the top level application deployment unit level for sharing with other persistence units
     * @param deploymentUnit
     * @param adaptorModule
     * @param adaptor
     * @return the application level shared PersistenceProviderAdaptor (which may of been set by a different thread)
     */
    private PersistenceProviderAdaptor savePerDeploymentSharedPersistenceProviderAdaptor(DeploymentUnit deploymentUnit, String adaptorModule, PersistenceProviderAdaptor adaptor) {
        if (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        synchronized (deploymentUnit) {
            Map<String,PersistenceProviderAdaptor> map = deploymentUnit.getAttachment(providerAdaptorMapKey);
            PersistenceProviderAdaptor current = map.get(adaptorModule);
            // saved if not already set by another thread
            if (current == null) {
                map.put(adaptorModule, adaptor);
                current = adaptor;
            }
            return current;
        }
    }

    private PersistenceProviderAdaptor getPerDeploymentSharedPersistenceProviderAdaptor(DeploymentUnit deploymentUnit, String adaptorModule) {
        if (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        synchronized (deploymentUnit) {
            Map<String,PersistenceProviderAdaptor> map = deploymentUnit.getAttachment(providerAdaptorMapKey);
            if( map == null) {
                map = new HashMap<String,PersistenceProviderAdaptor>();
                deploymentUnit.putAttachment(providerAdaptorMapKey, map);
            }
            return map.get(adaptorModule);
        }
    }

    /**
     * Look up the persistence provider
     *
     * @param pu
     * @return
     */
    private PersistenceProvider lookupProvider(PersistenceUnitMetadata pu) throws DeploymentUnitProcessingException {

        // ensure that the persistence provider module is loaded
        String persistenceProviderModule = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
        String persistenceProviderClassName = pu.getPersistenceProviderClassName();

        if (persistenceProviderClassName == null) {
            persistenceProviderClassName = Configuration.PROVIDER_CLASS_DEFAULT;
        }

        // try to determine the provider module name (ignore if we can't, it might already be loaded)
        if (persistenceProviderModule == null) {
            persistenceProviderModule = Configuration.getProviderModuleNameFromProviderClassName(persistenceProviderClassName);
        }

        PersistenceProvider provider = getProviderByName(pu, persistenceProviderModule);

        // if we haven't loaded the provider yet, load it
        if (provider == null) {
            if (persistenceProviderModule != null) {
                try {
                    PersistenceProviderLoader.loadProviderModuleByName(persistenceProviderModule);
                    provider = getProviderByName(pu, persistenceProviderModule);
                } catch (ModuleLoadException e) {
                    throw MESSAGES.cannotLoadPersistenceProviderModule(e, persistenceProviderModule, persistenceProviderClassName);
                }
            }
        }

        if (provider == null)
            throw MESSAGES.persistenceProviderNotFound(persistenceProviderClassName);
        return provider;
    }

    private PersistenceProvider getProviderByName(PersistenceUnitMetadata pu, String persistenceProviderModule) {
        String providerName = pu.getPersistenceProviderClassName();
        List<PersistenceProvider> providers =
            PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders();
        for (PersistenceProvider provider : providers) {
            if (provider.getClass().getName().equals(providerName)) {
                if (providerName.equals(Configuration.PROVIDER_CLASS_DEFAULT)) {
                    // could be Hibernate 3 or Hibernate 4 (OGM will not match PROVIDER_CLASS_DEFAULT)
                    if (persistenceProviderModule.equals(Configuration.PROVIDER_MODULE_HIBERNATE3)) {
                        if (isHibernate3(provider)) {
                            return provider;            // return Hibernate3 provider
                        }
                    } else if (!isHibernate3(provider)) { // looking for Hibernate4
                        return provider;                // return Hibernate 4 provider
                    }
                } else {
                    return provider;                    // return the provider that matched classname
                }
            }
        }
        return null;
    }


    private boolean isHibernate3(PersistenceProvider provider) {
        boolean result = false;
        // invoke org.hibernate.Version.getVersionString()
        try {
            Class<?> targetCls = provider.getClass().getClassLoader().loadClass("org.hibernate.Version");
            Method m = targetCls.getMethod("getVersionString");
            Object version = m.invoke(null);
            JPA_LOGGER.tracef("lookup provider checking provider version (%s)", version);
            if (version instanceof String &&
                ((String) version).startsWith("3.")) {
                result = true;
            }
        } catch (ClassNotFoundException ignore) {
        } catch (NoSuchMethodException ignore) {
        } catch (InvocationTargetException ignore) {
        } catch (IllegalAccessException ignore) {
        }

        return result;
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
            dataSourceInjector.uninject();
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
            JPA_LOGGER.debugf("Adding dependency on PU service %s for component %s", puServiceName, component.getComponentClassName());
            component.addDependency(puServiceName, ServiceBuilder.DependencyType.REQUIRED);
        }
    }

    /**
     * add to management console (if ManagementAdapter is supported for provider).
     * <p/>
     * full path to management data will be:
     * <p/>
     * /deployment=Deployment/subsystem=jpa/hibernate-persistence-unit=FullyAppQualifiedPath#PersistenceUnitName/cache=EntityClassName
     * <p/>
     * example of full path:
     * <p/>
     * /deployment=jpa_SecondLevelCacheTestCase.jar/subsystem=jpa/hibernate-persistence-unit=jpa_SecondLevelCacheTestCase.jar#mypc/
     * cache=org.jboss.as.test.integration.jpa.hibernate.Employee
     *
     * @param deploymentUnit
     * @param pu
     * @param persistenceUnitService
     * @param adaptor
     */
    private void addManagementConsole(final DeploymentUnit deploymentUnit, final PersistenceUnitMetadata pu,
                                      final PersistenceUnitService persistenceUnitService, final PersistenceProviderAdaptor adaptor) {
        ManagementAdaptor managementAdaptor = adaptor.getManagementAdaptor();
        if (managementAdaptor != null) {
            final String providerLabel = managementAdaptor.getIdentificationLabel();
            final String scopedPersistenceUnitName = pu.getScopedPersistenceUnitName();

            Resource providerResource = managementAdaptor.createPersistenceUnitResource(scopedPersistenceUnitName, providerLabel);
            ModelNode perPuNode = providerResource.getModel();
            perPuNode.get("scoped-unit-name").set(pu.getScopedPersistenceUnitName());
            // TODO this is a temporary hack into internals until DeploymentUnit exposes a proper Resource-based API
            final Resource deploymentResource = deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
            Resource subsystemResource;
            synchronized (deploymentResource) {
                subsystemResource = getOrCreateResource(deploymentResource, PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "jpa"));
            }
            synchronized (subsystemResource) {
                subsystemResource.registerChild(PathElement.pathElement(providerLabel, scopedPersistenceUnitName), providerResource);
            }
        }
    }

    /**
     * TODO this is a temporary hack into internals until DeploymentUnit exposes a proper Resource-based API
     */
    private static Resource getOrCreateResource(final Resource parent, final PathElement element) {
        synchronized (parent) {
            if (parent.hasChild(element)) {
                return parent.requireChild(element);
            } else {
                final Resource resource = Resource.Factory.create();
                parent.registerChild(element, resource);
                return resource;
            }
        }
    }

    private static class PersistenceAdaptorRemoval {
        final PersistenceUnitMetadata pu;
        final PersistenceProviderAdaptor adaptor;

        public PersistenceAdaptorRemoval(PersistenceUnitMetadata pu, PersistenceProviderAdaptor adaptor) {
            this.pu = pu;
            this.adaptor = adaptor;
        }

        private void cleanup() {
            adaptor.cleanup(pu);
        }
    }

    private static AttachmentKey<AttachmentList<PersistenceAdaptorRemoval>> REMOVAL_KEY = AttachmentKey.createList(PersistenceAdaptorRemoval.class);
}
