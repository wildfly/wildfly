/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.diagnostics.extension.deployment.processors;

import static org.wildfly.clustering.diagnostics.extension.ClusteringDiagnosticsSubsystemLogger.ROOT_LOGGER;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.distributable.DistributableCache;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.SessionID;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredDeploymentRepository;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredEjbDeploymentInformation;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredModuleDeployment;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredWarDeploymentInformation;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.infinispan.InfinispanBeanManager;

/**
 * A deployment unit processor which extracts information concerning clustered applications
 * and stores that information in the ClusteredDeploymentRepository.
 * <p/>
 * In order to obtain deployment information from deployment unit services which have not started yet,
 * this deployment processor installs services which PASSIVEly depend on those deployment unit services,
 * and populate the ClusteredDeploymentRepository once they have started.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteredDeploymentRepositoryProcessor implements DeploymentUnitProcessor {

    private volatile ClusteredDeploymentRepository repository = null;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return;
        }

        // if we can't access the repository, nothing to do
        this.repository = phaseContext.getAttachment(ClusteredDeploymentRepositoryDependenciesProcessor.CLUSTERED_DEPLOYMENT_REPOSITORY_SERVICE_KEY);
        if (repository == null) {
            ROOT_LOGGER.clusteredDeploymentRepositoryNotAvailable();
            return;
        }

        // the deployment name can be used to identify the type of deployment:
        //   if war, then we just need to confirm that it is distributable and create a ClusteredWarDeploymentInformation instance
        //   if jar, then we cycle through all components and identify the @Clustered SFSBs

        // process wars
        if (isWarModule(deploymentUnit)) {
            processWarDeployment(phaseContext, deploymentUnit);
        }

        // process ejb jars
        if (isJarModule(deploymentUnit)) {
            processEjbJarDeployment(phaseContext, deploymentUnit);
        }

        // ear deployments
        if (isEarModule(deploymentUnit)) {
            // do nothing - the war and jar sub-deployments are processed above
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

        if (repository == null) {
            ROOT_LOGGER.clusteredDeploymentRepositoryNotAvailable();
            return;
        }
        String deploymentName = context.getName();
        repository.remove(deploymentName);
        ROOT_LOGGER.removeDeploymentFromClusteredDeploymentRepository(deploymentName);
    }

    /*
     * Create a deployment module identifier for this module.
     */
    private DeploymentModuleIdentifier getDeploymentModuleIdentifier(DeploymentUnit deploymentUnit) {
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        String applicationName = eeModuleDescription.getEarApplicationName();
        applicationName = applicationName == null ? "" : applicationName;
        return new DeploymentModuleIdentifier(applicationName, eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());
    }

    /*
     * Process a war deployment.
     */
    private void processWarDeployment(DeploymentPhaseContext phaseContext, DeploymentUnit deploymentUnit) {

        final String deploymentName = deploymentUnit.getName();
        final DeploymentModuleIdentifier identifier = getDeploymentModuleIdentifier(deploymentUnit);

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData != null) {
            JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
            if (metaData != null) {
                if (metaData.getDistributable() != null) {
                    // this is a distributable war deployment
                    ModelNode undertow = deploymentUnit.getDeploymentSubsystemModel("undertow");
                    String virtualHost = undertow.get("virtual-host").asString();
                    String contextRoot = undertow.get("context-root").asString();

                    // find out what the web session cache container/cache names are
                    DeploymentCacheInfo cacheInfo = getWebDeploymentCache(metaData, virtualHost, contextRoot);

                    ClusteredWarDeploymentInformation info = new ClusteredWarDeploymentInformation(identifier, cacheInfo.getContainerName(), cacheInfo.getCacheName());
                    ClusteredModuleDeployment deployment = new ClusteredModuleDeployment(identifier, info, null);
                    repository.add(deploymentName, deployment);
                    ROOT_LOGGER.addDeploymentToClusteredDeploymentRepository("war", deploymentName);
                }
            }
        }
    }

    /*
     * Process an EJB jar deployment.
     */
    private void processEjbJarDeployment(DeploymentPhaseContext phaseContext, DeploymentUnit deploymentUnit) {
        final String deploymentName = deploymentUnit.getName();
        final DeploymentModuleIdentifier identifier = getDeploymentModuleIdentifier(deploymentUnit);

        final EEModuleConfiguration moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        for (final ComponentConfiguration configuration : moduleDescription.getComponentConfigurations()) {
            final ComponentDescription componentDescription = configuration.getComponentDescription();
            // EJB components
            if (componentDescription instanceof EJBComponentDescription) {
                if (componentDescription instanceof StatefulComponentDescription) {
                    StatefulComponentDescription statefulSessionBean = (StatefulComponentDescription) componentDescription;
                    String beanName = statefulSessionBean.getComponentName();

                    // process a potentially @Clustered SFSB:
                    // we need to get the bean session cache container and cache, but the service is not up yet, and will not be up
                    // until deployment processing is complete. Therefore, we install a service which will complete the registration
                    // process once the service is up

                    // install service to register the component once the component's CREATE service has started
                    CacheInfoPostProcessor postProcessor = new CacheInfoPostProcessor(repository, deploymentName, identifier, beanName);
                    ServiceName postProcessorServiceName = postProcessor.getServiceName(deploymentName, beanName);

                    // service name of the dependency we need to be up
                    ServiceName ejbComponentCreateServiceName = getJarDeploymentUnitSTARTServiceName(deploymentUnit, beanName);
                    ServiceTarget target = phaseContext.getServiceTarget();

                    ServiceController<?> postProcessorService = target.addService(postProcessorServiceName, postProcessor)
                            .addDependency(ejbComponentCreateServiceName, StatefulSessionComponent.class, postProcessor.getStatefulSessionComponentInjector())
                            .setInitialMode(ServiceController.Mode.PASSIVE)
                            .install();
                }
            }
        }
    }

    /*
     * The service names for deployment units differ whether we are in a war/jar or an ear:
     * jboss.deployment.unit.<moduleName>
     *     vs
     * jboss.deployment.subunit.<application name>.<module name>
     *
     * We also need to re-instate the suffixes of the deployment names as they are removed in EE processing.
     */
    private ServiceName getJarDeploymentUnitSTARTServiceName(DeploymentUnit deploymentUnit, String beanName) {
        ServiceName deploymentUnitServiceName = null;

        DeploymentModuleIdentifier identifier = getDeploymentModuleIdentifier(deploymentUnit);
        if (identifier.getApplicationName() != null && !identifier.getApplicationName().isEmpty()) {
            // we are in a sub-deployment
            deploymentUnitServiceName = Services.deploymentUnitName(identifier.getApplicationName() + ".ear", identifier.getModuleName() + ".jar");
        } else {
            // we are not in a sub-deployment
            deploymentUnitServiceName = Services.deploymentUnitName(identifier.getModuleName() + ".jar");
        }
        ServiceName ejbComponentCreateServiceName = BasicComponent.serviceNameOf(deploymentUnitServiceName, beanName).append("START");
        return ejbComponentCreateServiceName;
    }

    /*
     * This method gets the full model of the deployment subsystem, where some otherwise
     * very difficult to obtain deployment information is held.
     */
    private ModelNode getDeploymentSubsystemModel(DeploymentUnit deploymentUnit) {
        final Resource root = deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        if (root == null)
            return null;
        return Resource.Tools.readModel(root);
    }

    private boolean isWarModule(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getName().endsWith(".war");
    }

    private boolean isJarModule(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getName().endsWith(".jar");
    }

    private boolean isEarModule(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getName().endsWith(".ear");
    }

    /*
     * What follows is some code for obtaining cache container and cache information for
     * distributable web apps and @Clustered SFSB apps.
     *
     * Getting access to this information at runtime is tricky - in some cases, I have had
     * to mimic computations of cache names in order to get the information. Until a better way
     * is found ...stay tuned.
     */

    /*
     * Get the cache container and cache associated with this distributable web app.
     */
    private DeploymentCacheInfo getWebDeploymentCache(JBossWebMetaData metaData, String virtualHost, String contextRoot) {

        ServiceName templateCacheServiceName = getCacheServiceName(metaData.getReplicationConfig());
        String templateCacheName = templateCacheServiceName.getSimpleName();
        ServiceName containerServiceName = templateCacheServiceName.getParent();
        String containerName = containerServiceName.getSimpleName();
        StringBuilder cacheNameBuilder = new StringBuilder(virtualHost).append(contextRoot);
        if (contextRoot.isEmpty() || contextRoot.equals("/")) {
            cacheNameBuilder.append("ROOT");
        }
        String cacheName = cacheNameBuilder.toString();
        return new DeploymentCacheInfo(containerName, cacheName);
    }

    /*
     * EVIL: This has been copied from InfinispanSessionManagerFactoryBuilder
     */
    private static final String DEFAULT_CACHE_CONTAINER = "web";

    private static ServiceName getCacheServiceName(ReplicationConfig config) {
        ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
        String cacheName = (config != null) ? config.getCacheName() : null;
        ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        return (serviceName.length() < 4) ? serviceName.append(CacheContainer.DEFAULT_CACHE_ALIAS) : serviceName;
    }

    private class DeploymentCacheInfo {
        String containerName;
        String cacheName;

        private DeploymentCacheInfo(String containerName, String cacheName) {
            this.containerName = containerName;
            this.cacheName = cacheName;
        }

        public String getContainerName() {
            return containerName;
        }

        public String getCacheName() {
            return cacheName;
        }
    }

    /*
     * Service which, when started, looks up cache deployment information for an EJB SFSB and
     * stores it in the ClusteredDeploymentRepository if the bean is clustered.
     */
    private class CacheInfoPostProcessor implements Service<CacheInfoPostProcessor> {
        private final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("clustering").append("diagnostics").append("post-processor");

        public ServiceName getServiceName(String deploymentName, String componentName) {
            return SERVICE_NAME.append(deploymentName).append(componentName);
        }

        private final ClusteredDeploymentRepository repository;
        private final String deploymentName;
        private final DeploymentModuleIdentifier identifier;
        private final String beanName;
        private volatile InjectedValue<StatefulSessionComponent> componentService = new InjectedValue<StatefulSessionComponent>();


        public CacheInfoPostProcessor(ClusteredDeploymentRepository repository, String deploymentName, DeploymentModuleIdentifier identifier, String beanName) {
            this.repository = repository;
            this.deploymentName = deploymentName;
            this.identifier = identifier;
            this.beanName = beanName;
        }

        @Override
        public void start(StartContext context) {
            StatefulSessionComponent component = componentService.getValue();
            Cache<SessionID, StatefulSessionComponentInstance> cache = component.getCache();
            boolean clustered = false;

            // follow the chain back to the session cache for this component to check if it is clustered
            if (cache instanceof DistributableCache) {
                BeanManager beanManager = getBeanManager((DistributableCache) cache);
                if (beanManager instanceof InfinispanBeanManager) {
                    org.infinispan.Cache groupCache = getGroupCache((InfinispanBeanManager) beanManager);

                    // check if the cache is clustered
                    clustered = groupCache.getCacheManager().getCacheManagerConfiguration().isClustered();
                    if (clustered) {
                        // we need to add the cache info for the bean to the repository
                        String cacheContainer = groupCache.getCacheManager().getClusterName();
                        String beanCache = groupCache.getName();

                        Map<String, ClusteredModuleDeployment> modules = repository.getModules();
                        Map<String, ClusteredEjbDeploymentInformation> ejbs = null;
                        ClusteredModuleDeployment deployment = null;

                        // check if we need to initialise the repository entry for this deployment
                        if (!modules.keySet().contains(deploymentName)) {
                            addRepositoryEntryForDeployment();
                        }

                        // get the clustered EJB map
                        // need to call get modules again as the unmodifiable map has now changed!
                        modules = repository.getModules();
                        deployment = modules.get(deploymentName);
                        ejbs = deployment.getEjbs();

                        // now add the entry for the clustered bean
                        ClusteredEjbDeploymentInformation info = new ClusteredEjbDeploymentInformation(identifier, beanName);
                        info.setSessionCacheContainer(cacheContainer);
                        info.setSessionCache(beanCache);
                        ejbs.put(beanName, info);
                    }
                }
            }
        }

        @Override
        public void stop(StopContext context) {

            Map<String, ClusteredModuleDeployment> modules = null;
            ClusteredModuleDeployment deployment = null;
            Map<String, ClusteredEjbDeploymentInformation> ejbs = null;

            // remove the bean info, if present, from the clustered EJB map
            modules = repository.getModules();
            if (modules != null) {
                deployment = modules.get(deploymentName);
                if (deployment != null) {
                    ejbs = deployment.getEjbs();
                    if (ejbs != null && ejbs.containsKey(beanName)) {
                        ejbs.remove(beanName);
                    }
                }
            }
        }

        @Override
        public CacheInfoPostProcessor getValue() {
            return this;
        }

        public Injector<StatefulSessionComponent> getStatefulSessionComponentInjector() {
            return componentService;
        }

        /*
         * Add an entry for this deployment to the ClusteredDeploymentRepository
         */
        private void addRepositoryEntryForDeployment() {
            Map<String, ClusteredEjbDeploymentInformation> ejbs = new HashMap<String, ClusteredEjbDeploymentInformation>();
            ClusteredModuleDeployment deployment = new ClusteredModuleDeployment(identifier, null, ejbs);
            repository.add(deploymentName, deployment);
            ROOT_LOGGER.addDeploymentToClusteredDeploymentRepository("ejb", deploymentName);
        }

    }

    /*
     * Given a DistributableCache, use reflection to get its BeanManager member object.
     */
    private BeanManager getBeanManager(DistributableCache distributableCache) {
        // use reflection to get this private field
        BeanManager beanManager = null;
        beanManager = getMemberObjectField(distributableCache, "manager");
        return beanManager;
    }

    /*
     * Given a GroupAwareBackingCache, use reflection to get its SerializationGroupMemberContainer.
     */
    private org.infinispan.Cache getGroupCache(InfinispanBeanManager infinispanBeanManager) {
        // use reflection to get this private field
        org.infinispan.Cache groupCache = null;
        groupCache = getMemberObjectField(infinispanBeanManager, "groupCache");
        return groupCache;
    }

    /**
     * This method returns a member object field of a class instance, via reflection.
     *
     * @param i         instance of object holding the member field
     * @param fieldName name of the member field
     * @param <I>       type of the object holding the member field
     * @param <O>       type of the member field
     * @return the member field
     */
    static <I, O> O getMemberObjectField(I i, String fieldName) {
        O o = null;
        try {
            Field f = i.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            o = (O) f.get(i);

        } catch (NoSuchFieldException nsfe) {
            ROOT_LOGGER.noSuchFieldExceptionHandled(fieldName, i.getClass().getSimpleName());
        } catch (IllegalAccessException iae) {
            ROOT_LOGGER.illegalAccessExceptionHandled(fieldName, i.getClass().getSimpleName());
        }
        return o;
    }
}
