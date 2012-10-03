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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.AttachmentKey;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.RollbackHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.LogContextConfiguration;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class LoggingOperations {

    /**
     * Get the address name from the operation.
     *
     * @param operation the operation to extract the address name from
     *
     * @return the name
     */
    public static String getAddressName(final ModelNode operation) {
        return getAddress(operation).getLastElement().getValue();
    }

    /**
     * Get the address from the operation.
     *
     * @param operation the operation to extract the address from
     *
     * @return the address
     */
    public static PathAddress getAddress(final ModelNode operation) {
        return PathAddress.pathAddress(operation.require(OP_ADDR));
    }

    /**
     * The base logging OSH.
     */
    private abstract static class LoggingOperationStepHandler implements OperationStepHandler {
        private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

        @Override
        public final void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // Get the address and the name of the logger or handler
            final String name = getAddressName(operation);
            final ConfigurationPersistence configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence();
            final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();

            execute(context, operation, name, logContextConfiguration);
            if (context.getProcessType().isServer()) {
                // Add a new OSH for writing the configuration
                context.addStep(new OperationStepHandler() {
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        context.attachIfAbsent(ATTACHMENT_KEY, Boolean.TRUE);
                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                                final Boolean addCommit = context.getAttachment(ATTACHMENT_KEY);
                                logContextConfiguration.commit();
                                if (addCommit != null && addCommit) {
                                    final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();
                                    logContextConfiguration.commit();
                                    configurationPersistence.writeConfiguration(context);
                                    context.detach(ATTACHMENT_KEY);

                                    context.completeStep(new RollbackHandler() {
                                        @Override
                                        public void handleRollback(OperationContext context, ModelNode operation) {
                                            // The real rollbacks should happen in the subclasses
                                            logContextConfiguration.forget();
                                            try {
                                                configurationPersistence.writeConfiguration(context);
                                            } catch (OperationFailedException e) {
                                                throw LoggingMessages.MESSAGES.rollbackFailure(e);
                                            }
                                        }
                                    });
                                } else {
                                    context.completeStep(RollbackHandler.NOOP_ROLLBACK_HANDLER);
                                }
                            }
                        }, Stage.RUNTIME);
                        context.completeStep(RollbackHandler.NOOP_ROLLBACK_HANDLER);
                    }
                }, Stage.RUNTIME);
            }
            context.stepCompleted();
        }

        public abstract void execute(OperationContext context, ModelNode operation, String name, LogContextConfiguration logContextConfiguration) throws OperationFailedException;
    }


    /**
     * A base step handler for logging operations.
     */
    abstract static class LoggingAddOperationStepHandler extends LoggingOperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation, final String name, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            updateModel(operation, model);
            if (context.getProcessType().isServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, logContextConfiguration, name, model);
                        context.completeStep(new RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                logContextConfiguration.forget();
                                try {
                                    performRollback(context, operation, logContextConfiguration, name);
                                    logContextConfiguration.commit();
                                } catch (OperationFailedException e) {
                                    throw LoggingMessages.MESSAGES.rollbackFailure(e);
                                }
                            }
                        });
                    }
                }, Stage.RUNTIME);
            }
        }

        /**
         * Updates the model based on the operation.
         *
         * @param operation the operation being executed
         * @param model     the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException;

        /**
         * Executes additional processing for this step.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param model                   the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void performRuntime(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name, ModelNode model) throws OperationFailedException;

        /**
         * Perform any rollback operations to rollback the changes. Any changes in the {@link LogContextConfiguration
         * logContextConfiguration} will first be {@link LogContextConfiguration#forget() forgotten} then {@link
         * LogContextConfiguration#commit() committed} before and after this method is invoked.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         *
         * @throws OperationFailedException if the rollback fails
         */
        public abstract void performRollback(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name) throws OperationFailedException;

    }


    /**
     * A base update step handler for logging operations.
     */
    abstract static class LoggingUpdateOperationStepHandler extends LoggingOperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation, final String name, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            final ModelNode originalModel = model.clone();
            updateModel(operation, model);
            if (context.getProcessType().isServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, logContextConfiguration, name, model);
                        context.completeStep(new RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                // Forget the original changes
                                logContextConfiguration.forget();
                                try {
                                    performRollback(context, operation, model, logContextConfiguration, name, originalModel);
                                    // Commit any new changes
                                    logContextConfiguration.commit();
                                } catch (OperationFailedException e) {
                                    throw LoggingMessages.MESSAGES.rollbackFailure(e);
                                }
                            }
                        });
                    }
                }, Stage.RUNTIME);
            }
        }

        /**
         * Updates the model based on the operation.
         *
         * @param operation the operation being executed
         * @param model     the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException;

        /**
         * Executes additional processing for this step.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param model                   the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void performRuntime(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name, ModelNode model) throws OperationFailedException;

        /**
         * Perform any rollback operations to rollback the changes. Any changes in the {@link LogContextConfiguration
         * logContextConfiguration} will first be {@link LogContextConfiguration#forget() forgotten} then {@link
         * LogContextConfiguration#commit() committed} before and after this method is invoked.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param model                   the model current model
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param originalModel           the original model
         *
         * @throws OperationFailedException if the rollback fails
         */
        public abstract void performRollback(OperationContext context, ModelNode operation, final ModelNode model, LogContextConfiguration logContextConfiguration, String name, ModelNode originalModel) throws OperationFailedException;

    }


    /**
     * A base remove step handler for logging operations.
     */
    abstract static class LoggingRemoveOperationStepHandler extends LoggingOperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation, final String name, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {

            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
            final ModelNode originalModel = model.clone();

            performRemove(context, operation, logContextConfiguration, name, model);
            if (context.getProcessType().isServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, logContextConfiguration, name, model);
                        context.completeStep(new RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                logContextConfiguration.forget();
                                try {
                                    performRollback(context, operation, logContextConfiguration, name, originalModel);
                                    logContextConfiguration.commit();
                                } catch (OperationFailedException e) {
                                    throw LoggingMessages.MESSAGES.rollbackFailure(e);
                                }
                            }
                        });
                    }
                }, Stage.RUNTIME);
            }
        }

        /**
         * Performs the actual remove from the configuration.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param model                   the model to update
         *
         * @throws OperationFailedException if the remove fails
         */
        protected abstract void performRemove(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name, ModelNode model) throws OperationFailedException;

        /**
         * Executes additional processing for this step.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param model                   the model to update
         *
         * @throws OperationFailedException if a processing error occurs
         */
        public abstract void performRuntime(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name, ModelNode model) throws OperationFailedException;

        /**
         * Perform any rollback operations to rollback the changes. Any changes in the {@link LogContextConfiguration
         * logContextConfiguration} will first be {@link LogContextConfiguration#forget() forgotten} then {@link
         * LogContextConfiguration#commit() committed} before and after this method is invoked.
         *
         * @param context                 the operation context
         * @param operation               the operation being executed
         * @param logContextConfiguration the logging context configuration
         * @param name                    the name of the logger
         * @param originalModel           the original model
         *
         * @throws OperationFailedException if the rollback fails
         */
        protected abstract void performRollback(OperationContext context, ModelNode operation, LogContextConfiguration logContextConfiguration, String name, ModelNode originalModel) throws OperationFailedException;

    }


    /**
     * A default log handler write attribute step handler.
     */
    abstract static class LoggingWriteAttributeHandler extends AbstractWriteAttributeHandler<ConfigurationPersistence> {
        private final AttributeDefinition[] attributes;

        protected LoggingWriteAttributeHandler(final AttributeDefinition[] attributes) {
            super(attributes);
            this.attributes = attributes;
        }

        @Override
        protected final boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<ConfigurationPersistence> handbackHolder) throws OperationFailedException {
            final String name = getAddressName(operation);
            final ConfigurationPersistence configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence();
            final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();
            handbackHolder.setHandback(configurationPersistence);
            final boolean restartRequired = applyUpdate(context, attributeName, name, resolvedValue, logContextConfiguration);
            logContextConfiguration.commit();
            // Write the configuration
            configurationPersistence.writeConfiguration(context);
            return restartRequired;
        }

        /**
         * Applies the update to the runtime.
         *
         * @param context                 the operation context
         * @param attributeName           the name of the attribute being written
         * @param addressName             the name of the handler or logger
         * @param value                   the value to set the attribute to
         * @param logContextConfiguration the log context configuration
         *
         * @return {@code true} if a restart is required, otherwise {@code false}
         *
         * @throws OperationFailedException if an error occurs
         */
        protected abstract boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException;

        @Override
        protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final ConfigurationPersistence configurationPersistence) throws OperationFailedException {
            final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();
            // First forget the configuration
            logContextConfiguration.forget();
            final String name = getAddressName(operation);
            applyUpdate(context, attributeName, name, valueToRestore, logContextConfiguration);
            // Write the configuration
            configurationPersistence.writeConfiguration(context);
        }

        /**
         * Returns a collection of attributes used for the write attribute.
         *
         * @return a collection of attributes
         */
        public final AttributeDefinition[] getAttributes() {
            return attributes;
        }
    }
}
