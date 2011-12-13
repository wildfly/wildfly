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

package org.jboss.as.cmp.processors;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.TransactionEntityMap;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.component.CmpEntityBeanComponentCreateService;
import org.jboss.as.cmp.component.CmpEntityBeanComponentDescription;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JdbcStoreManagerForeignKeysService;
import org.jboss.as.cmp.jdbc.JdbcStoreManagerInitService;
import org.jboss.as.cmp.jdbc.JdbcStoreManagerRelationshipsService;
import org.jboss.as.cmp.jdbc.JdbcStoreManagerStartService;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactoryRegistry;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author John Bailey
 */
public class CmpStoreManagerProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        final Catalog catalog = new Catalog();  // One per deployment

        final TransactionEntityMap entityMap = new TransactionEntityMap();
        final ServiceName entityMapServiceName = deploymentUnit.getServiceName().append("cmp", "entity-map");
        serviceTarget.addService(entityMapServiceName, entityMap)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, entityMap.getTransactionManagerInjector())
                .install();


        //  Hate to use barriers across the board, but since the queries can add dependencies that are difficult to detect
        //  we do all entity stores in phase chunks
        final ServiceName relationsBarrierName = deploymentUnit.getServiceName().append("jdbc", "store-manager", "relations-barrier");
        final ServiceBuilder<?> relationsBarrierBuilder = serviceTarget.addService(relationsBarrierName, Service.NULL);

        final ServiceName startBarrierName = deploymentUnit.getServiceName().append("jdbc", "store-manager", "start-barrier");
        final ServiceBuilder<?> startBarrierBuilder = serviceTarget.addService(startBarrierName, Service.NULL);

        final ServiceName fkBarrierName = deploymentUnit.getServiceName().append("jdbc", "store-manager", "fk-barrier");
        final ServiceBuilder<?> fkBarrierBuilder = serviceTarget.addService(fkBarrierName, Service.NULL);

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component instanceof CmpEntityBeanComponentDescription) {
                final ServiceName serviceNameBase = component.getServiceName().append("jdbc", "store-manager");
                final ServiceName initName = serviceNameBase.append("INIT");
                final ServiceName relationsName = serviceNameBase.append("RELATIONS");
                final ServiceName startName = serviceNameBase.append("START");

                // Add deps to barriers
                relationsBarrierBuilder.addDependency(initName);
                startBarrierBuilder.addDependency(relationsName);
                fkBarrierBuilder.addDependency(startName);

                component.getConfigurators().add(new ComponentConfigurator() {
                    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                        final CmpEntityBeanComponentDescription componentDescription = (CmpEntityBeanComponentDescription) description;

                        final JDBCEntityMetaData entityMetaData = componentDescription.getEntityMetaData();

                        final JDBCStoreManager storeManager = new JDBCStoreManager(context.getDeploymentUnit(), entityMetaData, new CmpConfig(), catalog);

                        // Phase 1: Init the store
                        final JdbcStoreManagerInitService initService = new JdbcStoreManagerInitService(storeManager);
                        final ServiceBuilder<?> initBuilder = context.getServiceTarget().addService(initName, initService);
                        initBuilder.addDependency(KeyGeneratorFactoryRegistry.SERVICE_NAME, KeyGeneratorFactoryRegistry.class, storeManager.getKeyGeneratorFactoryInjector());
                        addDataSourceDependency(initBuilder, storeManager, entityMetaData.getDataSourceName());
                        for (JDBCRelationshipRoleMetaData roleMetaData : entityMetaData.getRelationshipRoles()) {
                            final String dsName = roleMetaData.getRelationMetaData().getDataSourceName();
                            if (dsName != null) {
                                addDataSourceDependency(initBuilder, storeManager, dsName);
                            }
                        }
                        initBuilder.addDependency(description.getCreateServiceName(), CmpEntityBeanComponent.class, storeManager.getComponentInjector());
                        initBuilder.install();

                        // Phase 2: Resolve relationships
                        final JdbcStoreManagerRelationshipsService resolveRelationshipsService = new JdbcStoreManagerRelationshipsService(storeManager);
                        context.getServiceTarget().addService(relationsName, resolveRelationshipsService)
                                .addDependency(relationsBarrierName)
                                .install();

                        // Phase 3: Start store
                        final JdbcStoreManagerStartService startService = new JdbcStoreManagerStartService(storeManager);
                        context.getServiceTarget().addService(startName, startService)
                                .addDependency(startBarrierName)
                                .install();

                        // Phase 3: Add foreign keys
                        final JdbcStoreManagerForeignKeysService fkService = new JdbcStoreManagerForeignKeysService(storeManager);
                        final ServiceName fkName = serviceNameBase.append("FOREIGN_KEYS");
                        context.getServiceTarget().addService(fkName, fkService)
                                .addDependency(fkBarrierName)
                                .install();

                        final InjectedValue<JDBCEntityPersistenceStore> persistenceStoreInjector = new InjectedValue<JDBCEntityPersistenceStore>();
                        configuration.getCreateDependencies().add(new DependencyConfigurator<Service<Component>>() {
                            public void configureDependency(final ServiceBuilder<?> serviceBuilder, final Service<Component> service) throws DeploymentUnitProcessingException {
                                final CmpEntityBeanComponentCreateService createService = (CmpEntityBeanComponentCreateService) service;
                                createService.setStoreManagerValue(persistenceStoreInjector);
                                serviceBuilder.addDependency(entityMapServiceName, TransactionEntityMap.class, createService.getTransactionEntityMapInjector());
                            }
                        });
                        configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                            public void configureDependency(ServiceBuilder<?> serviceBuilder, ComponentStartService service) throws DeploymentUnitProcessingException {
                                serviceBuilder.addDependency(initName, JDBCEntityPersistenceStore.class, persistenceStoreInjector);
                                serviceBuilder.addDependency(fkName);
                            }
                        });
                    }
                });

            }
        }
        relationsBarrierBuilder.install();
        startBarrierBuilder.install();
        fkBarrierBuilder.install();
    }

    private void addDataSourceDependency(final ServiceBuilder<?> builder, final JDBCStoreManager storeManager, final String dataSourceName) {
        builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(dataSourceName), DataSource.class, storeManager.getDataSourceInjector(dataSourceName));
    }

    public void undeploy(DeploymentUnit context) {
    }
}
