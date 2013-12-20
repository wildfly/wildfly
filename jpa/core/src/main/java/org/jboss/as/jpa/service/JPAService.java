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

package org.jboss.as.jpa.service;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.HashSet;
import java.util.Set;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.management.DynamicManagementStatisticsResource;
import org.jboss.as.jpa.management.EntityManagerFactoryLookup;
import org.jboss.as.jpa.management.ManagementResourceDefinition;
import org.jboss.as.jpa.processor.CacheDeploymentHelper;
import org.jboss.as.jpa.subsystem.JPAExtension;
import org.jboss.as.jpa.transaction.TransactionUtil;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jipijapa.management.spi.Statistics;
import org.jipijapa.plugin.spi.ManagementAdaptor;

/**
 * represents the global JPA Service
 *
 * @author Scott Marlow
 */
public class JPAService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = JPAServiceNames.getJPAServiceName();

    private static volatile String defaultDataSourceName = null;
    private static volatile ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance = null;
    private static final Set<String> existingResourceDescriptionResolver = new HashSet<>();
    private final CacheDeploymentHelper cacheDeploymentHelper = new CacheDeploymentHelper();

    public static String getDefaultDataSourceName() {
        ROOT_LOGGER.tracef("JPAService.getDefaultDataSourceName() == %s", JPAService.defaultDataSourceName);
        return defaultDataSourceName;
    }

    public static void setDefaultDataSourceName(String dataSourceName) {
        ROOT_LOGGER.tracef("JPAService.setDefaultDataSourceName(%s), previous value = %s", dataSourceName, JPAService.defaultDataSourceName);
        defaultDataSourceName = dataSourceName;
    }

    public static ExtendedPersistenceInheritance getDefaultExtendedPersistenceInheritance() {
        ROOT_LOGGER.tracef("JPAService.getDefaultExtendedPersistenceInheritance() == %s", defaultExtendedPersistenceInheritance.toString());
        return defaultExtendedPersistenceInheritance;
    }

    public static void setDefaultExtendedPersistenceInheritance(ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance) {
        ROOT_LOGGER.tracef("JPAService.setDefaultExtendedPersistenceInheritance(%s)", defaultExtendedPersistenceInheritance.toString());
        JPAService.defaultExtendedPersistenceInheritance = defaultExtendedPersistenceInheritance;
    }

    public static ServiceController<?> addService(
            final ServiceTarget target,
            final String defaultDataSourceName,
            final ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance,
            final ServiceListener<Object>... listeners) {
        JPAService jpaService = new JPAService();
        setDefaultDataSourceName(defaultDataSourceName);
        setDefaultExtendedPersistenceInheritance(defaultExtendedPersistenceInheritance);

        // set the transaction manager to be accessible via TransactionUtil
        final Injector<TransactionManager> transactionManagerInjector =
            new Injector<TransactionManager>() {
                public void inject(final TransactionManager value) throws InjectionException {
                    TransactionUtil.setTransactionManager(value);
                }

                public void uninject() {
                    // injector.uninject();
                }
            };
        // set the transaction service registry to be accessible via TransactionUtil (after service is installed below)
        final Injector<TransactionSynchronizationRegistry> transactionRegistryInjector =
            new Injector<TransactionSynchronizationRegistry>() {
                public void inject(final TransactionSynchronizationRegistry value) throws
                    InjectionException {
                    TransactionUtil.setTransactionSynchronizationRegistry(value);
                }

                public void uninject() {
                    // injector.uninject();
                }
            };

        return target.addService(SERVICE_NAME, jpaService)
            .addListener(listeners)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependency(TransactionManagerService.SERVICE_NAME, new CastingInjector<TransactionManager>(transactionManagerInjector, TransactionManager.class))
            .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, new CastingInjector<TransactionSynchronizationRegistry>(transactionRegistryInjector, TransactionSynchronizationRegistry.class))
            .addDependency(JPAUserTransactionListenerService.SERVICE_NAME)
            .install();
    }

    /**
     * Create single instance of management statistics resource per managementAdaptor version.
     *
     * ManagementAccess
     *
     * The persistence provider and jipijapa adapters will be in the same classloader,
     * either a static module or included directly in the application.  Those are the two supported use
     * cases for management of deployment persistence units also.
     *
     * From a management point of view, the requirements are:
     *   1.  show management statistics for static persistence provider modules and applications that have
     *       their own persistence provider module.
     *
     *   2.  persistence provider adapters will provide a unique key that identifies the management version of supported
     *       management statistics/operations.  For example, Hibernate 3.x might be 1.0, Hibernate 4.1/4.2 might
     *       be version 2.0 and Hibernate 4.3 could be 2.0 also as long as its compatible (same stats) with 4.1/4.2.
     *       Eventually, a Hibernate (later version) change in statistics is likely to happen, the management version
     *       will be incremented.
     *
     *
     * @param managementAdaptor the management adaptor that will provide statistics
     * @param scopedPersistenceUnitName name of the persistence unit
     * @param deploymentUnit deployment unit for the deployment requesting a resource
     * @return the management resource
     */
    public static Resource createManagementStatisticsResource(
            final ManagementAdaptor managementAdaptor,
            final String scopedPersistenceUnitName,
            final DeploymentUnit deploymentUnit) {

        synchronized (existingResourceDescriptionResolver) {
            final EntityManagerFactoryLookup entityManagerFactoryLookup = new EntityManagerFactoryLookup();
            final Statistics statistics = managementAdaptor.getStatistics();


            if (false == existingResourceDescriptionResolver.contains(managementAdaptor.getVersion())) {

                // setup statistics (this used to be part of JPA subsystem startup)
                ResourceDescriptionResolver resourceDescriptionResolver = new StandardResourceDescriptionResolver(
                        statistics.getResourceBundleKeyPrefix(), statistics.getResourceBundleName(), statistics.getClass().getClassLoader());

                PathElement subsystemPE = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME);
                ManagementResourceRegistration deploymentResourceRegistration = deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
                ManagementResourceRegistration deploymentSubsystemRegistration =
                        deploymentResourceRegistration.getSubModel(PathAddress.pathAddress(subsystemPE));
                ManagementResourceRegistration subdeploymentSubsystemRegistration =
                        deploymentResourceRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT), subsystemPE));

                deploymentSubsystemRegistration.registerSubModel(
                        new ManagementResourceDefinition(PathElement.pathElement(managementAdaptor.getIdentificationLabel()), resourceDescriptionResolver, statistics, entityManagerFactoryLookup));

                subdeploymentSubsystemRegistration.registerSubModel(
                        new ManagementResourceDefinition(PathElement.pathElement(managementAdaptor.getIdentificationLabel()), resourceDescriptionResolver, statistics, entityManagerFactoryLookup));

                existingResourceDescriptionResolver.add(managementAdaptor.getVersion());
            }
            // create (per deployment) dynamic Resource implementation that can reflect the deployment specific names (e.g. jpa entity classname/Hibernate region name)
            return new DynamicManagementStatisticsResource(statistics, scopedPersistenceUnitName, managementAdaptor.getIdentificationLabel(), entityManagerFactoryLookup);
        }
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        cacheDeploymentHelper.register();
    }

    @Override
    public void stop(StopContext stopContext) {
        cacheDeploymentHelper.unregister();
        synchronized (existingResourceDescriptionResolver) {
            existingResourceDescriptionResolver.clear();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

}
