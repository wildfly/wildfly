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
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.deployment.processors.merging.AsynchronousMergingProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * A {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler} to handle the add operation for the EJB
 * remote service, in the EJB subsystem
 * <p/>
 *
 * @author Stuart Douglas
 */
public class EJB3AsyncServiceAdd extends AbstractBoottimeAddStepHandler {
    static final EJB3AsyncServiceAdd INSTANCE = new EJB3AsyncServiceAdd();


    private EJB3AsyncServiceAdd() {
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final String threadPoolName = EJB3AsyncResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final ServiceName threadPoolServiceName = EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName);
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Adding EJB @Asynchronous support");
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_ASYNCHRONOUS_MERGE, new AsynchronousMergingProcessor(threadPoolServiceName));
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        EJB3AsyncResourceDefinition.THREAD_POOL_NAME.validateAndSet(operation, model);
    }
}
