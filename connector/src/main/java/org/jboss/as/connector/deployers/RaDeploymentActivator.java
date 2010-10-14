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

package org.jboss.as.connector.deployers;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.mdr.MdrServices;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProviderInjector;
import org.jboss.as.deployment.chain.DeploymentChainProviderService;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.txn.TxnServices;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * Service activator which installs the various service required for rar
 * deployments.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class RaDeploymentActivator implements ServiceActivator {

    public static final long RAR_DEPLOYMENT_CHAIN_PRIORITY = 1000000L;
    public static final ServiceName RAR_DEPLOYMENT_CHAIN_SERVICE_NAME = DeploymentChain.SERVICE_NAME.append("rar");
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.service");

    /**
     * Activate the services required for service deployments.
     * @param context The service activator context
     */
    @Override
    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        batchBuilder.addServiceValueIfNotExist(DeploymentChainProviderService.SERVICE_NAME,
                new DeploymentChainProviderService());

        final Value<DeploymentChain> deploymentChainValue = Values.immediateValue((DeploymentChain) new DeploymentChainImpl(
                RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.toString()));
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChainValue);
        batchBuilder.addService(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME, deploymentChainService).addDependency(
                DeploymentChainProviderService.SERVICE_NAME,
                DeploymentChainProvider.class,
                new DeploymentChainProviderInjector<DeploymentChain>(deploymentChainValue, new RaDeploymentChainSelector(),
                        RAR_DEPLOYMENT_CHAIN_PRIORITY));

        addDeploymentProcessor(batchBuilder, new ManifestAttachmentProcessor(), ManifestAttachmentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new AnnotationIndexProcessor(), AnnotationIndexProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleConfigProcessor(), ModuleConfigProcessor.PRIORITY);
        final InjectedValue<MetadataRepository> mdrInjector = new InjectedValue<MetadataRepository>();
        addDeploymentProcessor(batchBuilder, new RaDeploymentParsingProcessor(mdrInjector),
                RaDeploymentParsingProcessor.PRIORITY).addDependency(MdrServices.IRONJACAMAR_MDR, MetadataRepository.class,
                mdrInjector);
        addDeploymentProcessor(batchBuilder, new IronJacamarDeploymentParsingProcessor(mdrInjector),
                IronJacamarDeploymentParsingProcessor.PRIORITY).addDependency(MdrServices.IRONJACAMAR_MDR,
                MetadataRepository.class, mdrInjector);
        final InjectedValue<TransactionManager> txmInjector = new InjectedValue<TransactionManager>();
        final InjectedValue<ConnectorSubsystemConfiguration> configInjector = new InjectedValue<ConnectorSubsystemConfiguration>();

        addDeploymentProcessor(batchBuilder, new ParsedRaDeploymentProcessor(mdrInjector, txmInjector, configInjector),
                ParsedRaDeploymentProcessor.PRIORITY)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, txmInjector)
                .addDependency(MdrServices.IRONJACAMAR_MDR, MetadataRepository.class, mdrInjector)
                .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, ConnectorSubsystemConfiguration.class,
                        configInjector);
    }

    private <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder,
            final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(
                deploymentUnitProcessor);
        return batchBuilder.addService(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()),
                deploymentUnitProcessorService).addDependency(RAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class,
                new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }
}
