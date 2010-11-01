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

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.connector.deployers.RaDeploymentChainSelector;
import org.jboss.as.connector.deployers.processors.DsDependencyProcessor;
import org.jboss.as.connector.deployers.processors.DsDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.IronJacamarDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RaDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.processors.RaXmlDeploymentProcessor;
import org.jboss.as.connector.deployers.processors.RarConfigProcessor;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.module.NestedJarInlineProcessor;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;

import static org.jboss.as.connector.deployers.RaDeploymentActivator.RAR_DEPLOYMENT_CHAIN_SERVICE_NAME;

/**
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DataSourcesAdd extends AbstractSubsystemAdd<DataSourcesSubsystemElement> {

    private static final long serialVersionUID = -874698675049495644L;

    private DataSources datasources;

    public DataSources getDatasources() {
        return datasources;
    }

    public void setDatasources(DataSources datasources) {
        this.datasources = datasources;
    }

    protected DataSourcesAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();

        final DataSourcesService dsService = new DataSourcesService(datasources);
        BatchServiceBuilder<?> serviceBuilder = builder.addService(ConnectorServices.DATASOURCES_SERVICE,
                dsService);
        serviceBuilder.setInitialMode(Mode.ACTIVE);

        if (datasources == null)
            return;

        if (datasources.getDataSource().size() > 0 || datasources.getXaDataSource().size() > 0) {
            serviceBuilder = builder.addServiceValueIfNotExist(JDBCRARDeployService.NAME, new JDBCRARDeployService())
                .addDependency(ConnectorServices.RESOURCEADAPTERS_SERVICE)
                .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR)

                 // Even uglier hack
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(NestedJarInlineProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ManifestAttachmentProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(AnnotationIndexProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(RarConfigProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ModuleDependencyProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ModuleConfigProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(DeploymentModuleLoaderProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ModuleDeploymentProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ModuleContextProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(RaDeploymentParsingProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(IronJacamarDeploymentParsingProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(ParsedRaDeploymentProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(RaXmlDeploymentProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(DsDependencyProcessor.class.getName()))
                .addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(DsDeploymentProcessor.class.getName()))

                .setInitialMode(Mode.ACTIVE);
        }
    }

    protected DataSourcesSubsystemElement createSubsystemElement() {
        DataSourcesSubsystemElement element = new DataSourcesSubsystemElement();
        element.setDatasources(datasources);
        return element;
    }

}
