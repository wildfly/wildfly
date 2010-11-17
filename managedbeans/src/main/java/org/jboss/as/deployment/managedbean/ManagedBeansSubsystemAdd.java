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

package org.jboss.as.deployment.managedbean;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanDependencyProcessor;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;

/**
 * The managed subsystem add update.
 *
 * @author Emanuel Muckenhuber
 */
public class ManagedBeansSubsystemAdd extends AbstractSubsystemAdd<ManagedBeansSubsystemElement> {

    private static final long serialVersionUID = 8639964348855747105L;

    protected ManagedBeansSubsystemAdd() {
        super(ManagedBeansExtension.NAMESPACE);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        addDeploymentProcessor(batchBuilder, new ManagedBeanDependencyProcessor(), ManagedBeanDependencyProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ManagedBeanAnnotationProcessor(), ManagedBeanAnnotationProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new ManagedBeanDeploymentProcessor(), ManagedBeanDeploymentProcessor.PRIORITY);
    }

    /** {@inheritDoc} */
    protected ManagedBeansSubsystemElement createSubsystemElement() {
        return new ManagedBeansSubsystemElement();
    }

    static <T extends DeploymentUnitProcessor> void addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        batchBuilder.addService(DeploymentUnitProcessor.SERVICE_NAME_BASE.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(DeploymentChain.SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

}
