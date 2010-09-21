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

package org.jboss.as.service;

import javax.xml.stream.XMLStreamException;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProviderInjector;
import org.jboss.as.deployment.chain.DeploymentChainProviderService;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorDependencyProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


/**
 * SAR deployment subystem element.
 *
 * @author John Bailey
 */
final class SarSubsystemElement extends AbstractSubsystemElement<SarSubsystemElement> {
    private final Logger log = Logger.getLogger("org.jboss.as.deployment.managedbean");

    public static final long SAR_DEPLOYMENT_CHAIN_PRIORITY = JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_PRIORITY - 1000000L;
    public static final ServiceName SAR_DEPLOYMENT_CHAIN_SERVICE_NAME = DeploymentChain.SERVICE_NAME.append("sar");

    SarSubsystemElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        requireNoContent(reader);
    }

    /** @inheritDoc} */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating SAR Subsystem");

        final BatchBuilder batchBuilder = context.getBatchBuilder();
        batchBuilder.addServiceValueIfNotExist(DeploymentChainProviderService.SERVICE_NAME, new DeploymentChainProviderService());

        final Value<DeploymentChain> deploymentChainValue = Values.immediateValue((DeploymentChain)new DeploymentChainImpl(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME.toString()))   ;
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChainValue);
        batchBuilder.addService(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME, deploymentChainService)
            .addDependency(DeploymentChainProviderService.SERVICE_NAME, DeploymentChainProvider.class, new DeploymentChainProviderInjector<DeploymentChain>(deploymentChainValue, new SarDeploymentChainSelector(), SAR_DEPLOYMENT_CHAIN_PRIORITY));

        addDeploymentProcessor(batchBuilder, new ServiceActivatorDependencyProcessor(), ServiceActivatorDependencyProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleConfigProcessor(), ModuleConfigProcessor.PRIORITY);
        final InjectedValue<DeploymentModuleLoader> deploymentModuleLoaderValue = new InjectedValue<DeploymentModuleLoader>();
        addDeploymentProcessor(batchBuilder, new DeploymentModuleLoaderProcessor(deploymentModuleLoaderValue), DeploymentModuleLoaderProcessor.PRIORITY)
            .addDependency(DeploymentModuleLoaderService.SERVICE_NAME, DeploymentModuleLoader.class, deploymentModuleLoaderValue);
        addDeploymentProcessor(batchBuilder, new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ServiceDeploymentParsingProcessor(), ServiceDeploymentParsingProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ModuleContextProcessor(), ModuleContextProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ServiceActivatorProcessor(), ServiceActivatorProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ParsedServiceDeploymentProcessor(), ParsedServiceDeploymentProcessor.PRIORITY);
    }

    private <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

    /** @inheritDoc} */
    public long elementHash() {
        return 42;
    }

    /** @inheritDoc} */
    protected Class<SarSubsystemElement> getElementClass() {
        return SarSubsystemElement.class;
    }

    /** @inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}
