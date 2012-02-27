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

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Timer;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.as.ejb3.deployment.processors.AroundTimeoutAnnotationParsingProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceDeploymentProcessor;
import org.jboss.as.ejb3.deployment.processors.annotation.TimerServiceAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.TimerMethodMergingProcessor;
import org.jboss.as.ejb3.timerservice.persistence.filestore.FileTimerPersistence;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
/**
 * Adds the timer service
 *
 * @author Stuart Douglas
 */
public class TimerServiceAdd extends AbstractBoottimeAddStepHandler {


    public static final TimerServiceAdd INSTANCE = new TimerServiceAdd();


    /**
     * Populate the <code>timerService</code> from the <code>operation</code>
     *
     * @param operation         the operation
     * @param timerServiceModel strict-max-pool ModelNode
     * @throws org.jboss.as.controller.OperationFailedException
     *
     */

    protected void populateModel(ModelNode operation, ModelNode timerServiceModel) throws OperationFailedException {

        for (AttributeDefinition attr : TimerServiceResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, timerServiceModel);
        }
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ModelNode pathNode = TimerServiceResourceDefinition.PATH.resolveModelAttribute(context, model);
        final String path = pathNode.isDefined() ? pathNode.asString() : null;
        final ModelNode relativeToNode = TimerServiceResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

        final String threadPoolName = TimerServiceResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final ServiceName threadPoolServiceName = EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Configuring timers");

                //install the ejb timer service data store path service
                if (path != null) {
                    if (relativeTo != null) {
                        RelativePathService.addService(TimerServiceDeploymentProcessor.PATH_SERVICE_NAME, path, false, relativeTo,
                                context.getServiceTarget(), newControllers, verificationHandler);
                    } else {
                        AbsolutePathService.addService(TimerServiceDeploymentProcessor.PATH_SERVICE_NAME, path,
                                context.getServiceTarget(), newControllers, verificationHandler);
                    }

                    //we only add the timer service DUP's when the timer service in enabled in XML
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TIMEOUT_ANNOTATION, new TimerServiceAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDTIMEOUT_ANNOTATION, new AroundTimeoutAnnotationParsingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TIMER_METADATA_MERGE, new TimerMethodMergingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TIMER_SERVICE, new TimerServiceDeploymentProcessor(threadPoolServiceName));
                }
            }
        }, OperationContext.Stage.RUNTIME);

        newControllers.add(context.getServiceTarget().addService(TimerServiceDeploymentProcessor.TIMER_SERVICE_NAME, new TimerValueService())
                .install());
        final FileTimerPersistence fileTimerPersistence = new FileTimerPersistence(true);
        newControllers.add(context.getServiceTarget().addService(FileTimerPersistence.SERVICE_NAME, fileTimerPersistence)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, fileTimerPersistence.getModuleLoader())
                .addDependency(TimerServiceDeploymentProcessor.PATH_SERVICE_NAME, String.class, fileTimerPersistence.getBaseDir())
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, fileTimerPersistence.getTransactionManager())
                .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, fileTimerPersistence.getTransactionSynchronizationRegistry())
                .install());

    }

    private static final class TimerValueService implements Service<Timer> {

        private Timer timer;

        @Override
        public synchronized void start(final StartContext context) throws StartException {
            timer = new Timer();
        }

        @Override
        public synchronized void stop(final StopContext context) {
            timer.cancel();
            timer = null;
        }

        @Override
        public synchronized Timer getValue() throws IllegalStateException, IllegalArgumentException {
            return timer;
        }
    }
}
