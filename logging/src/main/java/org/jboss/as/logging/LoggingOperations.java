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
import org.jboss.as.controller.OperationContext.ResultHandler;
import org.jboss.as.controller.OperationContext.RollbackHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
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
     * Adds a {@link Stage#RUNTIME runtime} step to the context that will commit or rollback any logging changes. Also
     * if not a logging profile writes the {@code logging.properties} file.
     *
     * @param context                  the context to add the step to
     * @param configurationPersistence the configuration to commit
     */
    static void addCommitStep(final OperationContext context, final ConfigurationPersistence configurationPersistence) {
        context.addStep(new CommitOperationStepHandler(configurationPersistence), Stage.RUNTIME);
    }

    private static final class CommitOperationStepHandler implements OperationStepHandler {
        private static AttachmentKey<Boolean> WRITTEN_KEY = AttachmentKey.create(Boolean.class);
        private final ConfigurationPersistence configurationPersistence;

        CommitOperationStepHandler(final ConfigurationPersistence configurationPersistence) {
            this.configurationPersistence = configurationPersistence;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            configurationPersistence.prepare();
            context.completeStep(new ResultHandler() {
                @Override
                public void handleResult(final ResultAction resultAction, final OperationContext context, final ModelNode operation) {
                    if (resultAction == ResultAction.KEEP) {
                        configurationPersistence.commit();
                        if (!LoggingProfileOperations.isLoggingProfileAddress(getAddress(operation))) {
                            // Write once
                            if (context.getAttachment(WRITTEN_KEY) == null) {
                                context.attachIfAbsent(WRITTEN_KEY, Boolean.TRUE);
                                configurationPersistence.writeConfiguration(context);
                            }
                        }
                    } else if (resultAction == ResultAction.ROLLBACK) {
                        configurationPersistence.rollback();
                    }
                }
            });
        }
    }

    public static class ReadFilterOperationStepHandler implements OperationStepHandler {

        public static final ReadFilterOperationStepHandler INSTANCE = new ReadFilterOperationStepHandler();

        private ReadFilterOperationStepHandler() {

        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            final ModelNode filter = CommonAttributes.FILTER_SPEC.resolveModelAttribute(context, model);
            if (filter.isDefined()) {
                context.getResult().set(Filters.filterSpecToFilter(filter.asString()));
            }
            context.stepCompleted();
        }
    }

    /**
     * The base logging OSH.
     */
    private abstract static class LoggingOperationStepHandler implements OperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // Get the address and the name of the logger or handler
            final PathAddress address = getAddress(operation);
            final String name = getAddressName(operation);
            final ConfigurationPersistence configurationPersistence;
            final boolean isLoggingProfile = LoggingProfileOperations.isLoggingProfileAddress(address);
            if (isLoggingProfile) {
                final LogContext logContext = LoggingProfileContextSelector.getInstance().getOrCreate(LoggingProfileOperations.getLoggingProfileName(address));
                configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence(logContext);
            } else {
                configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence();
            }
            final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();

            execute(context, operation, name, logContextConfiguration);
            if (context.getProcessType().isServer()) {
                addCommitStep(context, configurationPersistence);
                // Add rollback handler in case rollback is invoked before a commit step is invoked
                context.completeStep(new RollbackHandler() {
                    @Override
                    public void handleRollback(final OperationContext context, final ModelNode operation) {
                        configurationPersistence.rollback();
                    }
                });
            } else {
                context.stepCompleted();
            }
        }

        public abstract void execute(OperationContext context, ModelNode operation, String name, LogContextConfiguration logContextConfiguration) throws OperationFailedException;

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
                        context.stepCompleted();
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

    }


    /**
     * A base update step handler for logging operations.
     */
    abstract static class LoggingUpdateOperationStepHandler extends LoggingOperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation, final String name, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            updateModel(operation, model);
            if (context.getProcessType().isServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, logContextConfiguration, name, model);
                        context.stepCompleted();
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

    }


    /**
     * A base remove step handler for logging operations.
     */
    abstract static class LoggingRemoveOperationStepHandler extends LoggingOperationStepHandler {

        @Override
        public final void execute(final OperationContext context, final ModelNode operation, final String name, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {

            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

            performRemove(context, operation, logContextConfiguration, name, model);
            if (context.getProcessType().isServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, logContextConfiguration, name, model);
                        context.stepCompleted();
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
            final PathAddress address = getAddress(operation);
            final ConfigurationPersistence configurationPersistence;
            final boolean isLoggingProfile = LoggingProfileOperations.isLoggingProfileAddress(address);
            if (isLoggingProfile) {
                final LogContext logContext = LoggingProfileContextSelector.getInstance().getOrCreate(LoggingProfileOperations.getLoggingProfileName(address));
                configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence(logContext);
            } else {
                configurationPersistence = ConfigurationPersistence.getOrCreateConfigurationPersistence();
            }
            final LogContextConfiguration logContextConfiguration = configurationPersistence.getLogContextConfiguration();
            handbackHolder.setHandback(configurationPersistence);
            final boolean restartRequired = applyUpdate(context, attributeName, name, resolvedValue, logContextConfiguration);
            addCommitStep(context, configurationPersistence);
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
        }

        @Override
        protected void validateUpdatedModel(final OperationContext context, final Resource model) throws OperationFailedException {
            super.validateUpdatedModel(context, model);
            final ModelNode submodel = model.getModel();
            if (submodel.hasDefined(CommonAttributes.FILTER.getName())) {
                final String filterSpec = Filters.filterToFilterSpec(CommonAttributes.FILTER.resolveModelAttribute(context, submodel));
                submodel.remove(CommonAttributes.FILTER.getName());
                submodel.get(CommonAttributes.FILTER_SPEC.getName()).set(filterSpec);
            }
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
