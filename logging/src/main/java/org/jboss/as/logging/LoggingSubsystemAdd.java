/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LoggingSubsystemAdd extends AbstractAddStepHandler {

    static final LoggingSubsystemAdd ADD_INSTANCE = new LoggingSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(CommonAttributes.LOGGER).setEmptyObject();
        model.get(CommonAttributes.ASYNC_HANDLER).setEmptyObject();
        model.get(CommonAttributes.CONSOLE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.CUSTOM_HANDLER).setEmptyObject();
        model.get(CommonAttributes.FILE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.SIZE_ROTATING_FILE_HANDLER).setEmptyObject();
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(LoggingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_LOGGING_CONFIG, LoggingConfigurationProcessor.INSTANCE);
            }
        }, Stage.RUNTIME);

        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        final LogContextConfiguration logContextConfiguration = ConfigurationPersistence.getOrCreateConfigurationPersistence().getLogContextConfiguration();
        try {
            // root logger
            if (!resource.hasChild(LoggingExtension.rootLoggerPath)) {
                LoggingLogger.ROOT_LOGGER.tracef("Removing the root logger configuration.");
                logContextConfiguration.removeLoggerConfiguration(CommonAttributes.ROOT_LOGGER_NAME);
            }

            // remove all configured loggers which aren't in the model
            if (resource.hasChild(PathElement.pathElement(CommonAttributes.LOGGER))) {
                final Set<String> loggerNames = resource.getChildrenNames(CommonAttributes.LOGGER);
                final List<String> configuredLoggerNames = logContextConfiguration.getLoggerNames();
                // Always remove the root
                configuredLoggerNames.remove(CommonAttributes.ROOT_LOGGER_NAME);
                configuredLoggerNames.removeAll(loggerNames);
                for (String name : configuredLoggerNames) {
                    LoggingLogger.ROOT_LOGGER.tracef("Removing logger configuration for '%s'", name);
                    logContextConfiguration.removeLoggerConfiguration(name);
                }
            }
            // handlers
            final List<String> configuredHandlerNames = logContextConfiguration.getHandlerNames();
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.ASYNC_HANDLER));
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.CONSOLE_HANDLER));
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.CUSTOM_HANDLER));
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.FILE_HANDLER));
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER));
            configuredHandlerNames.removeAll(resource.getChildrenNames(CommonAttributes.SIZE_ROTATING_FILE_HANDLER));
            for (String name : configuredHandlerNames) {
                LoggingLogger.ROOT_LOGGER.tracef("Removing handler configuration for '%s'", name);
                logContextConfiguration.removeHandlerConfiguration(name);
            }
            logContextConfiguration.commit();
            LoggingLogger.ROOT_LOGGER.trace("Logging subsystem has been added.");
        } finally {
            logContextConfiguration.forget();
        }
    }

    static ModelNode createOperation(ModelNode address) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        return subsystem;
    }
}
