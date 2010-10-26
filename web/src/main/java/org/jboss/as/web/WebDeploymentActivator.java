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

package org.jboss.as.web;

import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProviderInjector;
import org.jboss.as.deployment.chain.DeploymentChainProviderService;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.web.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationIndexProcessor;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.as.web.deployment.WarModuleConfigProcessor;
import org.jboss.as.web.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.web.deployment.WebClassloadingDependencyProcessor;
import org.jboss.as.web.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WebParsingDeploymentProcessor;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.vfs.VirtualFile;

/**
 * @author Emanuel Muckenhuber
 */
class WebDeploymentActivator {

    public static final long WAR_DEPLOYMENT_CHAIN_PRIORITY = 100000L;
    public static final ServiceName WAR_DEPLOYMENT_CHAIN_SERVICE_NAME = DeploymentChain.SERVICE_NAME.append("war");

    public static class WarDeploymentChainSelector implements DeploymentChainProvider.Selector {
        static final String WAR_EXTENSION = ".war";

        /** {@inheritDoc} */
        public boolean supports(DeploymentUnitContext deploymentUnitContext) {
            VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(deploymentUnitContext);
            return virtualFile.getName().toLowerCase().endsWith(WAR_EXTENSION);
        }
    }

    static void activate(final String defaultHost, final SharedWebMetaDataBuilder sharedWebBuilder, final SharedTldsMetaDataBuilder sharedTldsBuilder, final BatchBuilder batchBuilder) {
        batchBuilder.addServiceValueIfNotExist(DeploymentChainProviderService.SERVICE_NAME, new DeploymentChainProviderService());

        final Value<DeploymentChain> deploymentChainValue = Values.immediateValue((DeploymentChain) new DeploymentChainImpl(WAR_DEPLOYMENT_CHAIN_SERVICE_NAME.toString()));
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChainValue);
        batchBuilder.addService(WAR_DEPLOYMENT_CHAIN_SERVICE_NAME, deploymentChainService).addDependency(DeploymentChainProviderService.SERVICE_NAME, DeploymentChainProvider.class,
                new DeploymentChainProviderInjector<DeploymentChain>(deploymentChainValue, new WarDeploymentChainSelector(), WAR_DEPLOYMENT_CHAIN_PRIORITY));

        addDeploymentProcessor(batchBuilder, new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        final InjectedValue<DeploymentModuleLoader> moduleLoaderInjector = new InjectedValue<DeploymentModuleLoader>();
        addDeploymentProcessor(batchBuilder, new DeploymentModuleLoaderProcessor(moduleLoaderInjector), DeploymentModuleLoaderProcessor.PRIORITY)
            .addDependency(DeploymentModuleLoaderService.SERVICE_NAME, DeploymentModuleLoader.class, moduleLoaderInjector);
        addDeploymentProcessor(batchBuilder, new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleContextProcessor(), ModuleContextProcessor.PRIORITY);

        // Web specific deployment processors ....
        addDeploymentProcessor(batchBuilder, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder.create()), WarStructureDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WarAnnotationIndexProcessor(), WarAnnotationIndexProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WarModuleConfigProcessor(), WarModuleConfigProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WebParsingDeploymentProcessor(), WebParsingDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WebFragmentParsingDeploymentProcessor(), WebFragmentParsingDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new JBossWebParsingDeploymentProcessor(), JBossWebParsingDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new TldParsingDeploymentProcessor(), TldParsingDeploymentProcessor.PRIORITY);
        // FIXME: SCIs
        addDeploymentProcessor(batchBuilder, new WebClassloadingDependencyProcessor(), WebClassloadingDependencyProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WarAnnotationDeploymentProcessor(), WarAnnotationDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WarMetaDataProcessor(), WarMetaDataProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new WarDeploymentProcessor(defaultHost), WarDeploymentProcessor.PRIORITY);

    }

    static <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(WAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService).addDependency(WAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class,
                new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

}
