/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;

import static org.jboss.as.deployment.chain.JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_SERVICE_NAME;

/**
 * Microcontainer substem add.
 * Define processors for MC config handling.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class McSubsystemAdd extends AbstractSubsystemAdd<McSubsystemElement> {

    private static final long serialVersionUID = 1L;

    McSubsystemAdd() {
        super(MicrocontainerExtension.NAMESPACE);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        addDeploymentProcessor(batchBuilder, new KernelDeploymentParsingProcessor(), KernelDeploymentParsingProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ParsedKernelDeploymentProcessor(), ParsedKernelDeploymentProcessor.PRIORITY);
    }

    @Override
    protected McSubsystemElement createSubsystemElement() {
        return new McSubsystemElement();
    }

    private static <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(JAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(JAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }
}