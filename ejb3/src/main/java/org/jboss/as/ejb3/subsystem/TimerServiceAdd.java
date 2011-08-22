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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.deployment.processors.AroundTimeoutAnnotationParsingProcessor;
import org.jboss.as.ejb3.deployment.processors.annotation.TimerServiceAnnotationProcessor;
import org.jboss.as.ejb3.deployment.processors.TimerServiceDeploymentProcessor;
import org.jboss.as.ejb3.deployment.processors.dd.TimeoutMethodDeploymentDescriptorProcessor;
import org.jboss.as.ejb3.timerservice.TimerServiceFactoryService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.Locale;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CORE_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;

/**
 * Adds the timer service
 * <p/>
 *
 * @author Stuart Douglas
 */
public class TimerServiceAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    private static final Logger logger = Logger.getLogger(TimerServiceAdd.class);

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

        // max-pool-size
        if (operation.hasDefined(EJB3SubsystemModel.MAX_THREADS)) {
            int maxThreads = operation.get(EJB3SubsystemModel.MAX_THREADS).asInt();
            if (maxThreads <= 0) {
                throw new IllegalArgumentException("Invalid value: " + maxThreads + " for " + EJB3SubsystemModel.MAX_THREADS);
            }
            timerServiceModel.get(MAX_THREADS).set(maxThreads);
        }
        if (operation.hasDefined(EJB3SubsystemModel.CORE_THREADS)) {
            int coreThreads = operation.get(EJB3SubsystemModel.CORE_THREADS).asInt();
            if (coreThreads <= 0) {
                throw new IllegalArgumentException("Invalid value: " + coreThreads + " for " + EJB3SubsystemModel.CORE_THREADS);
            }
            timerServiceModel.get(CORE_THREADS).set(coreThreads);
        }

        if (operation.hasDefined(EJB3SubsystemModel.RELATIVE_TO)) {
            String relativeTo = operation.get(EJB3SubsystemModel.RELATIVE_TO).asString();
            timerServiceModel.get(RELATIVE_TO).set(relativeTo);
        }

        if (operation.hasDefined(EJB3SubsystemModel.PATH)) {
            String dataDir = operation.get(EJB3SubsystemModel.PATH).asString();
            timerServiceModel.get(PATH).set(dataDir);
        }
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        // Only take runtime action if EJB3 Lite isn't configured
        final ModelNode rootResource = context.getRootResource().getChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME)).getModel();
        final boolean lite = rootResource.hasDefined(EJB3SubsystemModel.LITE) && rootResource.get(EJB3SubsystemModel.LITE).asBoolean();
        if (!lite) {
            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    logger.debug("Configuring timers");

                    ModelNode timerServiceModel = model;

                    final ModelNode pathNode = timerServiceModel.get(PATH);
                    final String path = pathNode.isDefined() ? pathNode.asString() : null;
                    final ModelNode relativeToNode = timerServiceModel.get(RELATIVE_TO);
                    final String relativeTo = relativeToNode.isDefined() ? relativeToNode.asString() : null;

                    //install the ejb timer service data store path service
                    if (path != null) {
                        if (relativeTo != null) {
                            RelativePathService.addService(TimerServiceFactoryService.PATH_SERVICE_NAME, path, relativeTo, context.getServiceTarget());
                        } else {
                            AbsolutePathService.addService(TimerServiceFactoryService.PATH_SERVICE_NAME, path, context.getServiceTarget());
                        }
                    }

                    int coreThreadCount = timerServiceModel.get(CORE_THREADS).asInt(0);
                    int maxThreadCount = timerServiceModel.get(MAX_THREADS).asInt(Runtime.getRuntime().availableProcessors());

                    //we only add the timer service DUP's when the timer service in enabled in XML
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TIMEOUT_ANNOTATION, new TimerServiceAnnotationProcessor());
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDTIMEOUT_ANNOTATION, new AroundTimeoutAnnotationParsingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TIMER_METADATA_MERGE, new TimeoutMethodDeploymentDescriptorProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EJB_TIMER_SERVICE, new TimerServiceDeploymentProcessor(coreThreadCount, maxThreadCount, true));
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return EJB3SubsystemDescriptions.getTimerServiceAddDescription(locale);
    }
}
