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
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jpa.beanvalidation.ValidatorFactoryProvider;
import org.jboss.as.jpa.classloader.TempClassLoader;
import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.jpa.transaction.TransactionLocal;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.txn.TransactionManagerService;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Handle the installation of the Persistence Unit service
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.jpa");

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
            log.trace("install persistence unit definitions for war " + deploymentRoot.getRootName());
            addPuService(phaseContext, deploymentRoot, puList);
        }
    }

    private void handleEarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isEarDeployment(deploymentUnit) && JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            // handle META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            PersistenceUnitMetadataHolder holder;
            ArrayList<PersistenceUnitMetadataHolder> puList = new ArrayList<PersistenceUnitMetadataHolder>(1);

            if (deploymentRoot != null &&
                (holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS)) != null &&
                holder.getPersistenceUnits().size() > 0) {
                // assemble and install the PU service
                puList.add(holder);
            }
            log.trace("install persistence unit definitions for ear " + deploymentRoot.getRootName());
            addPuService(phaseContext, deploymentRoot, puList);
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
            final HashSet<ServiceName> serviceDependencies = new HashSet<ServiceName>();

            if (module == null)
                throw new DeploymentUnitProcessingException("Failed to get module attachment for " + phaseContext.getDeploymentUnit());

            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            final ModuleClassLoader classLoader = module.getClassLoader();

            for (PersistenceUnitMetadataHolder holder : puList) {
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                    pu.setClassLoader(classLoader);
                    pu.setTempClassloader(new TempClassLoader(classLoader));
                    if (pu.getJtaDataSourceName() != null) {
                        serviceDependencies.add(AbstractDataSourceService.SERVICE_NAME_BASE.append(pu.getJtaDataSourceName()));
                    }
                    if (pu.getNonJtaDataSourceName() != null) {
                        serviceDependencies.add(AbstractDataSourceService.SERVICE_NAME_BASE.append(pu.getNonJtaDataSourceName()));
                    }
                    try {
                        PersistenceUnitService service = new PersistenceUnitService(pu, resourceRoot);
                        // TODO:  move this to a standalone service
                        final Injector<TransactionManager> injector =
                            new Injector<TransactionManager>() {
                                public void inject(final TransactionManager value) throws InjectionException {
                                    TransactionLocal.setTransactionManager(value);
                                }

                                public void uninject() {
                                    // injector.uninject();
                                }
                            };

                        final HashMap properties = new HashMap();
                        properties.put("javax.persistence.validation.factory", ValidatorFactoryProvider.getInstance().getValidatorFactory());
                        addHibernateProps(properties);
                        final ServiceName serviceName = PersistenceUnitService.getPUServiceName(pu);
                        serviceTarget.addService(serviceName, service)
                            .addDependencies(serviceDependencies)
                            .addDependency(TransactionManagerService.SERVICE_NAME, new CastingInjector<TransactionManager>(injector, TransactionManager.class))
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .addInjection(service.getPropertiesInjector(), properties)
                            .install();

                        log.trace("added PersistenceUnitService for '" + serviceName + "'.  PU is ready for injector action. ");

                    } catch (ServiceRegistryException e) {
                        throw new DeploymentUnitProcessingException("Failed to add persistence unit service for " + pu.getPersistenceUnitName(), e);
                    }
                }
            }
        }
    }

    // TODO:  move these to a per provider settings file.
    private void addHibernateProps(Map properties) {
        properties.put("hibernate.transaction.manager_lookup_class", "org.jboss.as.jpa.hibernate.TransactionManagerLookup");
        properties.put("hibernate.jndi.java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        properties.put("hibernate.jndi.java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        properties.put("hibernate.id.new_generator_mappings", "true");
        // properties.put("hibernate.cache.provider_class","org.hibernate.cache.HashtableCacheProvider");

    }


    static boolean isEarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.EAR, context));
    }

    static boolean isWarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.WAR, context));
    }

}
