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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.deployments.LoggingConfigDeploymentProcessor;
import org.jboss.as.logging.deployments.LoggingDependencyDeploymentProcessor;
import org.jboss.as.logging.deployments.LoggingProfileDeploymentProcessor;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LoggingSubsystemAdd extends AbstractAddStepHandler {

    static final LoggingSubsystemAdd INSTANCE = new LoggingSubsystemAdd();

    private LoggingSubsystemAdd() {

    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
        for (SimpleAttributeDefinition attribute : LoggingRootResource.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final boolean addDependencies = LoggingRootResource.ADD_LOGGING_API_DEPENDENCIES.resolveModelAttribute(context, model).asBoolean();
        final boolean useLoggingConfig = LoggingRootResource.USE_DEPLOYMENT_LOGGING_CONFIG.resolveModelAttribute(context, model).asBoolean();
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                if (addDependencies) {
                    processorTarget.addDeploymentProcessor(LoggingExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_LOGGING, new LoggingDependencyDeploymentProcessor());
                }
                processorTarget.addDeploymentProcessor(LoggingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_LOGGING_CONFIG,
                        new LoggingConfigDeploymentProcessor(LoggingExtension.CONTEXT_SELECTOR, LoggingRootResource.USE_DEPLOYMENT_LOGGING_CONFIG.getName(), useLoggingConfig));
                processorTarget.addDeploymentProcessor(LoggingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_LOGGING_PROFILE, new LoggingProfileDeploymentProcessor(LoggingExtension.CONTEXT_SELECTOR));
            }
        }, Stage.RUNTIME);

        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        final ConfigurationPersistence configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence();
        final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();
        // root logger
        if (!resource.hasChild(RootLoggerResourceDefinition.ROOT_LOGGER_PATH)) {
            LoggingLogger.ROOT_LOGGER.tracef("Removing the root logger configuration.");
            logContextConfiguration.removeLoggerConfiguration(CommonAttributes.ROOT_LOGGER_NAME);
        }

        // remove all configured loggers which aren't in the model
        if (resource.hasChild(PathElement.pathElement(LoggerResourceDefinition.LOGGER))) {
            final Set<String> loggerNames = resource.getChildrenNames(LoggerResourceDefinition.LOGGER);
            final List<String> configuredLoggerNames = logContextConfiguration.getLoggerNames();
            // Always remove the root
            configuredLoggerNames.remove(CommonAttributes.ROOT_LOGGER_NAME);
            configuredLoggerNames.removeAll(loggerNames);
            for (String name : configuredLoggerNames) {
                LoggingLogger.ROOT_LOGGER.tracef("Removing logger configuration for '%s'", name);
                logContextConfiguration.removeLoggerConfiguration(name);
            }
        }

        // Create a collection of all subsystem handlers
        final Collection<String> subsystemHandlers = new ArrayList<String>();
        subsystemHandlers.addAll(resource.getChildrenNames(AsyncHandlerResourceDefinition.ASYNC_HANDLER));
        subsystemHandlers.addAll(resource.getChildrenNames(ConsoleHandlerResourceDefinition.CONSOLE_HANDLER));
        subsystemHandlers.addAll(resource.getChildrenNames(CustomHandlerResourceDefinition.CUSTOM_HANDLER));
        subsystemHandlers.addAll(resource.getChildrenNames(FileHandlerResourceDefinition.FILE_HANDLER));
        subsystemHandlers.addAll(resource.getChildrenNames(PeriodicHandlerResourceDefinition.PERIODIC_ROTATING_FILE_HANDLER));
        subsystemHandlers.addAll(resource.getChildrenNames(SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_FILE_HANDLER));

        // handlers
        final List<String> configuredHandlerNames = logContextConfiguration.getHandlerNames();
        configuredHandlerNames.removeAll(subsystemHandlers);
        for (String name : configuredHandlerNames) {
            LoggingLogger.ROOT_LOGGER.tracef("Removing handler configuration for '%s'", name);
            // Clean up any possible POJO references
            logContextConfiguration.removePojoConfiguration(name);
            // Remove the handler configuration
            logContextConfiguration.removeHandlerConfiguration(name);
        }

        // Remove formatters
        final List<String> configuredFormatters = logContextConfiguration.getFormatterNames();
        configuredFormatters.removeAll(resource.getChildrenNames(PatternFormatterResourceDefinition.PATTERN_FORMATTER.getName()));
        configuredFormatters.removeAll(resource.getChildrenNames(CustomFormatterResourceDefinition.CUSTOM_FORMATTER.getName()));
        // Formatter names could also be the name of a handler if the formatter attribute is used rather than a named-formatter
        configuredFormatters.removeAll(subsystemHandlers);

        for (String name : configuredFormatters) {
            LoggingLogger.ROOT_LOGGER.tracef("Removing formatter configuration for '%s'", name);
            // Remove the formatter configuration
            logContextConfiguration.removeFormatterConfiguration(name);
        }

        LoggingOperations.addCommitStep(context, configurationPersistence);
        LoggingLogger.ROOT_LOGGER.trace("Logging subsystem has been added.");
    }
}
