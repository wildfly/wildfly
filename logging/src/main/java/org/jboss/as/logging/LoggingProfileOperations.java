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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.ResultHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.config.LogContextConfiguration;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingProfileOperations {


    static OperationStepHandler ADD_PROFILE = new AbstractAddStepHandler() {
        @Override
        protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            model.setEmptyObject();
        }
    };

    static OperationStepHandler REMOVE_PROFILE = new AbstractRemoveStepHandler() {

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Get the address and the name of the logger or handler
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            // Get the logging profile
            final String loggingProfile = getLoggingProfileName(address);
            final LoggingProfileContextSelector contextSelector = LoggingProfileContextSelector.getInstance();
            final LogContext logContext = contextSelector.get(loggingProfile);
            if (logContext != null) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        final ConfigurationPersistence configuration = ConfigurationPersistence.getConfigurationPersistence(logContext);
                        if (configuration != null) {
                            final LogContextConfiguration logContextConfiguration = configuration.getLogContextConfiguration();
                            // Remove all loggers
                            for (String loggerName : logContextConfiguration.getLoggerNames()) {
                                logContextConfiguration.removeLoggerConfiguration(loggerName);
                            }
                            // Remove all the handlers
                            for (String handlerName : logContextConfiguration.getHandlerNames()) {
                                logContextConfiguration.removeHandlerConfiguration(handlerName);
                            }
                            // Remove all the filters
                            for (String filterName : logContextConfiguration.getFilterNames()) {
                                logContextConfiguration.removeFilterConfiguration(filterName);
                            }
                            // Remove all the formatters
                            for (String formatterName : logContextConfiguration.getFormatterNames()) {
                                logContextConfiguration.removeFormatterConfiguration(formatterName);
                            }
                            // Remove all the error managers
                            for (String errorManager : logContextConfiguration.getErrorManagerNames()) {
                                logContextConfiguration.removeErrorManagerConfiguration(errorManager);
                            }
                            // Add a commit step
                            LoggingOperations.addCommitStep(context, configuration);
                            context.reloadRequired();
                        }
                        context.completeStep(new ResultHandler() {
                            @Override
                            public void handleResult(final ResultAction resultAction, final OperationContext context, final ModelNode operation) {
                                if (resultAction == ResultAction.KEEP) {
                                    contextSelector.remove(loggingProfile);
                                }
                            }
                        });
                    }
                }, Stage.RUNTIME);
            }
        }

        @Override
        protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        }
    };

    /**
     * Checks if the address is a logging profile address.
     *
     * @param address the address to check for the logging profile
     *
     * @return {@code true} if the address is a logging profile address, otherwise {@code false}
     */
    static boolean isLoggingProfileAddress(final PathAddress address) {
        return getLoggingProfileName(address) != null;
    }

    /**
     * Gets the logging profile name. If the address is not in a logging profile path, {@code null} is returned.
     *
     * @param address the address to check for the logging profile name
     *
     * @return the logging profile name or {@code null}
     */
    static String getLoggingProfileName(final PathAddress address) {
        for (PathElement pathElement : address) {
            if (CommonAttributes.LOGGING_PROFILE.equals(pathElement.getKey())) {
                return pathElement.getValue();
            }
        }
        return null;
    }
}
