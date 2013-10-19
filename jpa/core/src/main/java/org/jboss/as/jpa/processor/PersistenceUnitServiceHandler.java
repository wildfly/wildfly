/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.SynchronizationType;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;

import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.jpa.beanmanager.ProxyBeanManager;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.interceptor.WebNonTxEmCloserAction;
import org.jboss.as.jpa.messages.JpaMessages;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderLoader;
import org.jboss.as.jpa.processor.secondLevelCache.CacheDeploymentListener;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.service.PhaseOnePersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.subsystem.PersistenceUnitRegistryImpl;
import org.jboss.as.jpa.util.JPAServiceNames;
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
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Index;
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
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;


import static org.jboss.as.jpa.messages.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;
import static org.jboss.as.server.Services.addServerExecutorDependency;

/**
 * Handle the installation of the Persistence Unit service
 *
 * @author Scott Marlow
 */
public class PersistenceUnitServiceHandler {

    private static final String ENTITYMANAGERFACTORY_JNDI_PROPERTY = "jboss.entity.manager.factory.jndi.name";
    private static final String ENTITYMANAGER_JNDI_PROPERTY = "jboss.entity.manager.jndi.name";
    public static final ServiceName BEANMANAGER_NAME = ServiceName.of("beanmanager");

    private static final AttachmentKey<Map<String,PersistenceProviderAdaptor>> providerAdaptorMapKey = AttachmentKey.create(Map.class);
    private static final String SCOPED_UNIT_NAME = "scoped-unit-name";
    private static final String FIRST_PHASE = "__FIRST_PHASE__";

    public static void deploy(DeploymentPhaseContext phaseContext, boolean startEarly, Platform platform) throws DeploymentUnitProcessingException {
        handleWarDeployment(phaseContext, startEarly, platform);
        handleEarDeployment(phaseContext, startEarly, platform);
        handleJarDeployment(phaseContext, startEarly, platform);
    }

    public static void undeploy(DeploymentUnit context) {
        List<PersistenceAdaptorRemoval> removals = context.getAttachmentList(REMOVAL_KEY);
        if (removals != null) {
            for (PersistenceAdaptorRemoval removal : removals) {
                removal.cleanup();
            }
            context.removeAttachment(REMOVAL_KEY);
        }
    }

    private static void handleJarDeployment(DeploymentPhaseContext phaseContext, boolean startEarly, Platform platform) throws DeploymentUnitProcessingException {
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
                addPuService(phaseContext, puList, startEarly, platform);
            }
        }
    }

    private static void handleWarDeployment(DeploymentPhaseContext phaseContext, boolean startEarly, Platform platform) throws DeploymentUnitProcessingException {
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
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                    if ((holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null
                        && holder.getPersistenceUnits().size() > 0) {

                        // assemble and install the PU service
                        puList.add(holder);
                    }
                }
            }

            if (startEarly) { // only add the WebNonTxEmCloserAction valve on the earlier invocation (AS7-6690).
                deploymentUnit.addToAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS, new WebNonTxEmCloserAction());
            }

            JPA_LOGGER.tracef("install persistence unit definitions for war %s", deploymentRoot.getRootName());
            addPuService(phaseContext, puList, startEarly, platform);
        }
    }

    private static void handleEarDeployment(DeploymentPhaseContext phaseContext, boolean startEarly, Platform platform) throws DeploymentUnitProcessingException {
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
                    addPuService(phaseContext, puList, startEarly, platform);
                }
            }
        }
    }

    /**
     * Add one PU service per top level deployment that represents
     *
     *
     * @param phaseContext
     * @param puList
     * @param startEarly
     * @param platform
     * @throws DeploymentUnitProcessingException
     *
     */
    private static void addPuService(final DeploymentPhaseContext phaseContext, final ArrayList<PersistenceUnitMetadataHolder> puList,
                                     final boolean startEarly, final Platform platform)
        throws DeploymentUnitProcessingException {

        if (puList.size() > 0) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            final Collection<ComponentDescription> components = eeModuleDescription.getComponentDescriptions();
            if (module == null) {
                // Unresolved OSGi bundles would not have a module attached
                ROOT_LOGGER.failedToGetModuleAttachment(deploymentUnit);
                return;
            }

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            final ModuleClassLoader classLoader = module.getClassLoader();
            PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder = getPersistenceProviderDeploymentHolder(deploymentUnit);

            for (PersistenceUnitMetadataHolder holder : puList) {
                setAnnotationIndexes(holder, deploymentUnit);
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {

                    // only start the persistence unit if JPA_CONTAINER_MANAGED is true
                    String jpaContainerManaged = pu.getProperties().getProperty(Configuration.JPA_CONTAINER_MANAGED);
                    boolean deployPU = (jpaContainerManaged == null? true : Boolean.parseBoolean(jpaContainerManaged));

                    if (deployPU) {
                        final PersistenceProvider provider = lookupProvider(pu, persistenceProviderDeploymentHolder, deploymentUnit);
                        final PersistenceProviderAdaptor adaptor = getPersistenceProviderAdaptor(pu, persistenceProviderDeploymentHolder, deploymentUnit, provider, platform);
                        final boolean twoPhaseBootStrapCapable = (adaptor instanceof TwoPhaseBootstrapCapable) && Configuration.allowTwoPhaseBootstrap(pu);

                        if (startEarly) {
                            if (twoPhaseBootStrapCapable) {
                                deployPersistenceUnitPhaseOne(phaseContext, deploymentUnit, eeModuleDescription, components, serviceTarget, classLoader, pu, adaptor);
                            }
                            else if (false == Configuration.needClassFileTransformer(pu)) {
                                // will start later when startEarly == false
                                JPA_LOGGER.tracef("persistence unit %s in deployment %s is configured to not need class transformer to be set, no class rewriting will be allowed",
                                    pu.getPersistenceUnitName(), deploymentUnit.getName());
                            }
                            else {
                                // we need class file transformer to work, don't allow cdi bean manager to be access since that
                                // could cause application classes to be loaded (workaround by setting jboss.as.jpa.classtransformer to false).  WFLY-1463
                                final boolean allowCdiBeanManagerAccess = false;
                                deployPersistenceUnit(phaseContext, deploymentUnit, eeModuleDescription, components, serviceTarget, classLoader, pu, startEarly, provider, adaptor, allowCdiBeanManagerAccess);
                            }
                        }
                        else { // !startEarly
                            if (twoPhaseBootStrapCapable) {
                                deployPersistenceUnitPhaseTwo(phaseContext, deploymentUnit, eeModuleDescription, components, serviceTarget, classLoader, pu, provider, adaptor);
                            } else if (false == Configuration.needClassFileTransformer(pu)) {
                                final boolean allowCdiBeanManagerAccess = true;
                                // PUs that have Configuration.JPA_CONTAINER_CLASS_TRANSFORMER = false will start during INSTALL phase
                                deployPersistenceUnit(phaseContext, deploymentUnit, eeModuleDescription, components, serviceTarget, classLoader, pu, startEarly, provider, adaptor, allowCdiBeanManagerAccess);
                            }
                        }

                    }
                    else {
                        JPA_LOGGER.tracef("persistence unit %s in deployment %s is not container managed (%s is set to false)",
                                pu.getPersistenceUnitName(), deploymentUnit.getName(), Configuration.JPA_CONTAINER_MANAGED);
                    }
                }
            }
        }
    }

    /**
     * start the persistence unit in one phase
     *
     * @param phaseContext
     * @param deploymentUnit
     * @param eeModuleDescription
     * @param components
     * @param serviceTarget
     * @param classLoader
     * @param pu
     * @param startEarly
     * @param provider
     * @param adaptor
     * @param allowCdiBeanManagerAccess
     * @throws DeploymentUnitProcessingException
     */
    private static void deployPersistenceUnit(
            final DeploymentPhaseContext phaseContext,
            final DeploymentUnit deploymentUnit,
            final EEModuleDescription eeModuleDescription,
            final Collection<ComponentDescription> components,
            final ServiceTarget serviceTarget,
            final ModuleClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final boolean startEarly,
            final PersistenceProvider provider,
            final PersistenceProviderAdaptor adaptor,
            final boolean allowCdiBeanManagerAccess) throws DeploymentUnitProcessingException {
        pu.setClassLoader(classLoader);
        try {
            ValidatorFactory validatorFactory = null;
            final HashMap<String, ValidatorFactory> properties = new HashMap();
            if (!ValidationMode.NONE.equals(pu.getValidationMode())) {
                // Get the CDI-enabled ValidatorFactory
                validatorFactory = deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
            }

            final PersistenceUnitServiceImpl service = new PersistenceUnitServiceImpl(classLoader, pu, adaptor, provider, PersistenceUnitRegistryImpl.INSTANCE, deploymentUnit.getServiceName(), validatorFactory);

            deploymentUnit.addToAttachmentList(REMOVAL_KEY, new PersistenceAdaptorRemoval(pu, adaptor));

            // add persistence provider specific properties
            adaptor.addProviderProperties(properties, pu);

            final ServiceName puServiceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
            deploymentUnit.putAttachment(JpaAttachments.PERSISTENCE_UNIT_SERVICE_KEY, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, puServiceName);

            ServiceBuilder<PersistenceUnitService> builder = serviceTarget.addService(puServiceName, service);
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
                // try the one defined in the jpa subsystem
                String defaultJtaDataSource = adjustJndi(JPAService.getDefaultDataSourceName());
                if ((defaultJtaDataSource == null ||
                        defaultJtaDataSource.isEmpty()) &&
                        eeModuleDescription != null) {
                    // try the one defined in the ee subsystem
                    defaultJtaDataSource = eeModuleDescription.getDefaultResourceJndiNames().getDataSource();
                }
                if (defaultJtaDataSource != null &&
                    defaultJtaDataSource.length() > 0) {
                    builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(defaultJtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                    JPA_LOGGER.tracef("%s is using the default data source '%s'", puServiceName, defaultJtaDataSource);
                }
            }

            // JPA 2.1 sections 3.5.1 + 9.1 require the CDI bean manager to be passed to the peristence provider
            // if the persistence unit is contained in a deployment that is a CDI bean archive (has beans.xml).
            if (allowCdiBeanManagerAccess && WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                builder.addDependency(beanManagerServiceName(deploymentUnit),  new CastingInjector<BeanManager>(service.getBeanManagerInjector(), BeanManager.class));
            }

            try {
                // save a thread local reference to the builder for setting up the second level cache dependencies
                CacheDeploymentListener.setInternalDeploymentServiceBuilder(builder);
                adaptor.addProviderDependencies(pu);
            }
            finally {
                CacheDeploymentListener.clearInternalDeploymentServiceBuilder();
            }

            /**
             * handle extension that binds a transaction scoped entity manager to specified JNDI location
             */
            entityManagerBind(eeModuleDescription, serviceTarget, pu, puServiceName);

            /**
             * handle extension that binds an entity manager factory to specified JNDI location
             */
            entityManagerFactoryBind(eeModuleDescription, serviceTarget, pu, puServiceName);

            if (startEarly) {   // require that the pu service start before the next deployment phase starts
                phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, puServiceName);
            }

            builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addInjection(service.getPropertiesInjector(), properties);

            // get async executor from Services.addServerExecutorDependency
            addServerExecutorDependency(builder, service.getExecutorInjector(), false);

            builder.install();

            JPA_LOGGER.tracef("added PersistenceUnitService for '%s'.  PU is ready for injector action.", puServiceName);
            addManagementConsole(deploymentUnit, pu, adaptor);

        } catch (ServiceRegistryException e) {
            throw JpaMessages.MESSAGES.failedToAddPersistenceUnit(e, pu.getPersistenceUnitName());
        }
    }

    /**
     * first phase of starting the persistence unit
     *
     * @param phaseContext
     * @param deploymentUnit
     * @param eeModuleDescription
     * @param components
     * @param serviceTarget
     * @param classLoader
     * @param pu
     * @param adaptor
     * @throws DeploymentUnitProcessingException
     */
    private static void deployPersistenceUnitPhaseOne(
            final DeploymentPhaseContext phaseContext,
            final DeploymentUnit deploymentUnit,
            final EEModuleDescription eeModuleDescription,
            final Collection<ComponentDescription> components,
            final ServiceTarget serviceTarget,
            final ModuleClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final PersistenceProviderAdaptor adaptor) throws DeploymentUnitProcessingException {
        pu.setClassLoader(classLoader);
        try {
            ValidatorFactory validatorFactory = null;
            final HashMap<String, ValidatorFactory> properties = new HashMap();

            ProxyBeanManager proxyBeanManager = null;
            // JPA 2.1 sections 3.5.1 + 9.1 require the CDI bean manager to be passed to the peristence provider
            // if the persistence unit is contained in a deployment that is a CDI bean archive (has beans.xml).
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                proxyBeanManager = new ProxyBeanManager();
            }

            final PhaseOnePersistenceUnitServiceImpl service = new PhaseOnePersistenceUnitServiceImpl(classLoader, pu, adaptor, deploymentUnit.getServiceName(), proxyBeanManager);

            deploymentUnit.addToAttachmentList(REMOVAL_KEY, new PersistenceAdaptorRemoval(pu, adaptor));

            // add persistence provider specific properties
            adaptor.addProviderProperties(properties, pu);

            final ServiceName puServiceName = PersistenceUnitServiceImpl.getPUServiceName(pu).append(FIRST_PHASE);

            deploymentUnit.putAttachment(JpaAttachments.PERSISTENCE_UNIT_SERVICE_KEY, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, puServiceName);

            ServiceBuilder<PhaseOnePersistenceUnitServiceImpl> builder = serviceTarget.addService(puServiceName, service);
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
                // try the one defined in the jpa subsystem
                String defaultJtaDataSource = adjustJndi(JPAService.getDefaultDataSourceName());
                if ((defaultJtaDataSource == null ||
                        defaultJtaDataSource.isEmpty()) &&
                        eeModuleDescription != null) {
                    // try the one defined in the ee subsystem
                    defaultJtaDataSource = eeModuleDescription.getDefaultResourceJndiNames().getDataSource();
                }
                if (defaultJtaDataSource != null &&
                    defaultJtaDataSource.length() > 0) {
                    builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(defaultJtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                    JPA_LOGGER.tracef("%s is using the default data source '%s'", puServiceName, defaultJtaDataSource);
                }
            }

            try {
                // save a thread local reference to the builder for setting up the second level cache dependencies
                CacheDeploymentListener.setInternalDeploymentServiceBuilder(builder);
                adaptor.addProviderDependencies(pu);
            }
            finally {
                CacheDeploymentListener.clearInternalDeploymentServiceBuilder();
            }

            phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, puServiceName);

            builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addInjection(service.getPropertiesInjector(), properties);

            // get async executor from Services.addServerExecutorDependency
            addServerExecutorDependency(builder, service.getExecutorInjector(), false);

            builder.install();

            JPA_LOGGER.tracef("added PersistenceUnitService (phase 1 of 2) for '%s'.  PU is ready for injector action.", puServiceName);
        } catch (ServiceRegistryException e) {
            throw JpaMessages.MESSAGES.failedToAddPersistenceUnit(e, pu.getPersistenceUnitName());
        }
    }

    /**
     * Second phase of starting the persistence unit
     *
     * @param phaseContext
     * @param deploymentUnit
     * @param eeModuleDescription
     * @param components
     * @param serviceTarget
     * @param classLoader
     * @param pu
     * @param provider
     * @param adaptor
     * @throws DeploymentUnitProcessingException
     */
    private static void deployPersistenceUnitPhaseTwo(
            final DeploymentPhaseContext phaseContext,
            final DeploymentUnit deploymentUnit,
            final EEModuleDescription eeModuleDescription,
            final Collection<ComponentDescription> components,
            final ServiceTarget serviceTarget,
            final ModuleClassLoader classLoader,
            final PersistenceUnitMetadata pu,
            final PersistenceProvider provider,
            final PersistenceProviderAdaptor adaptor) throws DeploymentUnitProcessingException {
        pu.setClassLoader(classLoader);
        try {
            ValidatorFactory validatorFactory = null;
            final HashMap<String, ValidatorFactory> properties = new HashMap();
            if (!ValidationMode.NONE.equals(pu.getValidationMode())) {
                // Get the CDI-enabled ValidatorFactory
                validatorFactory = deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
            }

            final PersistenceUnitServiceImpl service = new PersistenceUnitServiceImpl(classLoader, pu, adaptor, provider, PersistenceUnitRegistryImpl.INSTANCE, deploymentUnit.getServiceName(), validatorFactory);

            deploymentUnit.addToAttachmentList(REMOVAL_KEY, new PersistenceAdaptorRemoval(pu, adaptor));

            // add persistence provider specific properties
            adaptor.addProviderProperties(properties, pu);

            final ServiceName puServiceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
            deploymentUnit.putAttachment(JpaAttachments.PERSISTENCE_UNIT_SERVICE_KEY, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, puServiceName);

            deploymentUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, puServiceName);

            ServiceBuilder<PersistenceUnitService> builder = serviceTarget.addService(puServiceName, service);
            // the PU service has to depend on the JPAService which is responsible for setting up the necessary JPA infrastructure (like registering the cache EventListener(s))
            // @see https://issues.jboss.org/browse/WFLY-1531 for details
            builder.addDependency(JPAServiceNames.getJPAServiceName());

            // add dependency on first phase
            builder.addDependency(puServiceName.append(FIRST_PHASE), new CastingInjector<>(service.getPhaseOnePersistenceUnitServiceImplInjector(), PhaseOnePersistenceUnitServiceImpl.class));

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
                // try the one defined in the jpa subsystem
                String defaultJtaDataSource = adjustJndi(JPAService.getDefaultDataSourceName());
                if ((defaultJtaDataSource == null ||
                        defaultJtaDataSource.isEmpty()) &&
                        eeModuleDescription != null) {
                    // try the one defined in the ee subsystem
                    defaultJtaDataSource = eeModuleDescription.getDefaultResourceJndiNames().getDataSource();
                }
                if (defaultJtaDataSource != null &&
                    defaultJtaDataSource.length() > 0) {
                    builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(defaultJtaDataSource), new CastingInjector<DataSource>(service.getJtaDataSourceInjector(), DataSource.class));
                    JPA_LOGGER.tracef("%s is using the default data source '%s'", puServiceName, defaultJtaDataSource);
                }
            }

            // JPA 2.1 sections 3.5.1 + 9.1 require the CDI bean manager to be passed to the peristence provider
            // if the persistence unit is contained in a deployment that is a CDI bean archive (has beans.xml).
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                builder.addDependency(beanManagerServiceName(deploymentUnit),  new CastingInjector<BeanManager>(service.getBeanManagerInjector(), BeanManager.class));
            }

            try {
                // save a thread local reference to the builder for setting up the second level cache dependencies
                CacheDeploymentListener.setInternalDeploymentServiceBuilder(builder);
                adaptor.addProviderDependencies(pu);
            }
            finally {
                CacheDeploymentListener.clearInternalDeploymentServiceBuilder();
            }


            /**
             * handle extension that binds a transaction scoped entity manager to specified JNDI location
             */
            entityManagerBind(eeModuleDescription, serviceTarget, pu, puServiceName);

            /**
             * handle extension that binds an entity manager factory to specified JNDI location
             */
            entityManagerFactoryBind(eeModuleDescription, serviceTarget, pu, puServiceName);

            builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addInjection(service.getPropertiesInjector(), properties);

            // get async executor from Services.addServerExecutorDependency
            addServerExecutorDependency(builder, service.getExecutorInjector(), false);

            builder.install();

            JPA_LOGGER.tracef("added PersistenceUnitService (phase 2 of 2) for '%s'.  PU is ready for injector action.", puServiceName);
            addManagementConsole(deploymentUnit, pu, adaptor);

        } catch (ServiceRegistryException e) {
            throw JpaMessages.MESSAGES.failedToAddPersistenceUnit(e, pu.getPersistenceUnitName());
        }
    }

    private static void entityManagerBind(EEModuleDescription eeModuleDescription, ServiceTarget serviceTarget, final PersistenceUnitMetadata pu, ServiceName puServiceName) {
        if (pu.getProperties().containsKey(ENTITYMANAGER_JNDI_PROPERTY)) {
            String jndiName = pu.getProperties().get(ENTITYMANAGER_JNDI_PROPERTY).toString();
            final ContextNames.BindInfo bindingInfo;
            if (jndiName.startsWith("java:")) {
                bindingInfo =  ContextNames.bindInfoForEnvEntry(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getModuleName(), false, jndiName);
            }
            else {
                bindingInfo = ContextNames.bindInfoFor(jndiName);
            }
            JPA_LOGGER.tracef("binding the transaction scoped entity manager to jndi name '%s'", bindingInfo.getAbsoluteJndiName());
            final BinderService binderService = new BinderService(bindingInfo.getBindName());
            serviceTarget.addService(bindingInfo.getBinderServiceName(), binderService)
                .addDependency(bindingInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(puServiceName, PersistenceUnitServiceImpl.class, new Injector<PersistenceUnitServiceImpl>() {
                    @Override
                    public void inject(final PersistenceUnitServiceImpl value) throws
                            InjectionException {
                        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(
                                new ImmediateValue<Object>(
                                        new TransactionScopedEntityManager(
                                                pu.getScopedPersistenceUnitName(),
                                                Collections.emptyMap(),
                                                value.getEntityManagerFactory(),
                                                SynchronizationType.SYNCHRONIZED))));
                    }

                    @Override
                    public void uninject() {
                        binderService.getNamingStoreInjector().uninject();
                    }
                }).install();
        }
    }

    private static void entityManagerFactoryBind(EEModuleDescription eeModuleDescription, ServiceTarget serviceTarget, PersistenceUnitMetadata pu, ServiceName puServiceName) {
        if (pu.getProperties().containsKey(ENTITYMANAGERFACTORY_JNDI_PROPERTY)) {
            String jndiName = pu.getProperties().get(ENTITYMANAGERFACTORY_JNDI_PROPERTY).toString();
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
    }

    private static ServiceName beanManagerServiceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(BEANMANAGER_NAME);
    }

    /**
     * Setup the annotation index map
     *
     * @param puHolder
     * @param deploymentUnit
     */
    private static void setAnnotationIndexes(
            final PersistenceUnitMetadataHolder puHolder,
            DeploymentUnit deploymentUnit ) {

        final Map<URL, Index> annotationIndexes = new HashMap();

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

    private static String adjustJndi(String dataSourceName) {
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
     *
     * @param pu
     * @param persistenceProviderDeploymentHolder
     *
     * @param provider
     * @param platform
     * @return
     * @throws DeploymentUnitProcessingException
     *
     */
    private static PersistenceProviderAdaptor getPersistenceProviderAdaptor(
            final PersistenceUnitMetadata pu,
            final PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder,
            final DeploymentUnit deploymentUnit,
            final PersistenceProvider provider,
            final Platform platform) throws
        DeploymentUnitProcessingException {
        String adaptorModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
        PersistenceProviderAdaptor adaptor = null;
        if (persistenceProviderDeploymentHolder != null) {
            adaptor = persistenceProviderDeploymentHolder.getAdapter();
        }
        if (adaptor == null) {
            adaptor = getPerDeploymentSharedPersistenceProviderAdaptor(deploymentUnit, adaptorModule, provider);
            if (adaptor == null) {
                try {
                    // will load the persistence provider adaptor (integration classes).  if adaptorModule is null
                    // the noop adaptor is returned (can be used against any provider but the integration classes
                    // are handled externally via properties or code in the persistence provider).
                    if (adaptorModule != null) { // legacy way of loading adapter module
                        adaptor = PersistenceProviderAdaptorLoader.loadPersistenceAdapterModule(adaptorModule, platform);
                    }
                    else {
                        adaptor = PersistenceProviderAdaptorLoader.loadPersistenceAdapter(provider, platform);
                    }
                } catch (ModuleLoadException e) {
                    throw new DeploymentUnitProcessingException("persistence provider adapter module load error "
                        + adaptorModule, e);
                }
                adaptor = savePerDeploymentSharedPersistenceProviderAdaptor(deploymentUnit, adaptorModule, adaptor, provider);
            }

        }
        if (adaptor == null) {
            throw JpaMessages.MESSAGES.failedToGetAdapter(pu.getPersistenceProviderClassName());
        }
        return adaptor;
    }

    /**
     * Will save the PersistenceProviderAdaptor at the top level application deployment unit level for sharing with other persistence units
     *
     * @param deploymentUnit
     * @param adaptorModule
     * @param adaptor
     * @param provider
     * @return the application level shared PersistenceProviderAdaptor (which may of been set by a different thread)
     */
    private static PersistenceProviderAdaptor savePerDeploymentSharedPersistenceProviderAdaptor(DeploymentUnit deploymentUnit, String adaptorModule, PersistenceProviderAdaptor adaptor, PersistenceProvider provider) {
        if (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        synchronized (deploymentUnit) {
            Map<String,PersistenceProviderAdaptor> map = deploymentUnit.getAttachment(providerAdaptorMapKey);
            String key;

            if (adaptorModule != null) {
                key = adaptorModule;  // handle legacy adapter module
            }
            else {
                key = provider.getClass().getName();
            }
            PersistenceProviderAdaptor current = map.get(key);

            // saved if not already set by another thread
            if (current == null) {
                map.put(key, adaptor);
                current = adaptor;
            }
            return current;
        }
    }

    private static PersistenceProviderAdaptor getPerDeploymentSharedPersistenceProviderAdaptor(DeploymentUnit deploymentUnit, String adaptorModule, PersistenceProvider provider) {
        if (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        synchronized (deploymentUnit) {
            Map<String,PersistenceProviderAdaptor> map = deploymentUnit.getAttachment(providerAdaptorMapKey);
            if( map == null) {
                map = new HashMap();
                deploymentUnit.putAttachment(providerAdaptorMapKey, map);
            }
            String key;

            if (adaptorModule != null) {
                key = adaptorModule;  // handle legacy adapter module
            }
            else {
                key = provider.getClass().getName();
            }
            return map.get(key);
        }
    }

    /**
     * Look up the persistence provider
     *
     *
     * @param pu
     * @param deploymentUnit
     * @return
     */
    private static PersistenceProvider lookupProvider(
            PersistenceUnitMetadata pu,
            PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder,
            DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {

        PersistenceProvider provider = null;
        if (persistenceProviderDeploymentHolder != null &&
            persistenceProviderDeploymentHolder.getProvider() != null) {

            List<PersistenceProvider> providerList = persistenceProviderDeploymentHolder.getProvider();
            for (PersistenceProvider persistenceProvider : providerList) {
                if (persistenceProvider.getClass().getName().equals(pu.getPersistenceProviderClassName())) {
                    provider = persistenceProvider;
                    JPA_LOGGER.tracef("deployment %s is using its own copy of %s", deploymentUnit.getName(), pu.getPersistenceProviderClassName());
                    break;
                }
            }
        }

        if (provider == null) {
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

            provider = getProviderByName(pu, persistenceProviderModule);

            // if we haven't loaded the provider yet, load it
            if (provider == null) {
                if (persistenceProviderModule != null) {
                    try {
                        PersistenceProviderLoader.loadProviderModuleByName(persistenceProviderModule);
                        provider = getProviderByName(pu, persistenceProviderModule);
                    } catch (ModuleLoadException e) {
                        throw JpaMessages.MESSAGES.cannotLoadPersistenceProviderModule(e, persistenceProviderModule, persistenceProviderClassName);
                    }
                }
            }

            if (provider == null)
                throw JpaMessages.MESSAGES.persistenceProviderNotFound(persistenceProviderClassName);
        }

        return provider;
    }

    private static PersistenceProvider getProviderByName(PersistenceUnitMetadata pu, String persistenceProviderModule) {
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


    private static boolean isHibernate3(PersistenceProvider provider) {
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
     * @param adaptor
     */
    private static void addManagementConsole(final DeploymentUnit deploymentUnit, final PersistenceUnitMetadata pu,
                                      final PersistenceProviderAdaptor adaptor) {
        ManagementAdaptor managementAdaptor = adaptor.getManagementAdaptor();
        // workaround for AS7-4441, if a custom hibernate.cache.region_prefix is specified, don't show the persistence
        // unit in management console.
        if (managementAdaptor != null &&
                adaptor.doesScopedPersistenceUnitNameIdentifyCacheRegionName(pu)) {
            final String providerLabel = managementAdaptor.getIdentificationLabel();
            final String scopedPersistenceUnitName = pu.getScopedPersistenceUnitName();
            Resource providerResource = JPAService.createManagementStatisticsResource(managementAdaptor, scopedPersistenceUnitName, deploymentUnit);

            // Resource providerResource = managementAdaptor.createPersistenceUnitResource(scopedPersistenceUnitName, providerLabel);
            ModelNode perPuNode = providerResource.getModel();
            perPuNode.get(SCOPED_UNIT_NAME).set(pu.getScopedPersistenceUnitName());
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

    private static PersistenceProviderDeploymentHolder getPersistenceProviderDeploymentHolder(DeploymentUnit deploymentUnit) {
        deploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        return deploymentUnit.getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
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
