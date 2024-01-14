/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Global session cookie config
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class PersistentSessionsDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.PERSISTENT_SESSIONS);

    protected static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(PATH, RELATIVE_TO);

    PersistentSessionsDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKeyValuePair()))
                .setAddHandler(new PersistentSessionsAdd())
                .setRemoveHandler(new PersistentSessionsRemove())
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    public static boolean isEnabled(final ModelNode model) throws OperationFailedException {
        return model.isDefined();
    }


    private static class PersistentSessionsAdd extends RestartParentResourceAddHandler {
        protected PersistentSessionsAdd() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            if (requiresRuntime(context)) {
                context.addStep(new OperationStepHandler() {
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        performRuntime(context, operation, operation);


                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                rollbackRuntime(context, operation, operation);
                            }
                        });
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }

        private void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            if (isEnabled(model)) {
                final ModelNode pathValue = PATH.resolveModelAttribute(context, model);
                final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addService(AbstractPersistentSessionManager.SERVICE_NAME);
                final Consumer<SessionPersistenceManager> sConsumer = sb.provides(AbstractPersistentSessionManager.SERVICE_NAME);
                final Supplier<ModuleLoader> mlSupplier = sb.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
                if (pathValue.isDefined()) {
                    final String path = pathValue.asString();
                    final ModelNode relativeToValue = RELATIVE_TO.resolveModelAttribute(context, model);
                    final String relativeTo = relativeToValue.isDefined() ? relativeToValue.asString() : null;
                    final Supplier<PathManager> pmSupplier = sb.requires(PathManager.SERVICE_DESCRIPTOR);
                    sb.setInstance(new DiskBasedModularPersistentSessionManager(sConsumer, mlSupplier, pmSupplier, path, relativeTo));
                } else {
                    sb.setInstance(new InMemoryModularPersistentSessionManager(sConsumer, mlSupplier));
                }
                sb.install();
            }
        }

        private void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }

    private static class PersistentSessionsRemove extends RestartParentResourceRemoveHandler {

        protected PersistentSessionsRemove() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        @Override
        protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
            super.removeServices(context, parentService, parentModel);
            context.removeService(AbstractPersistentSessionManager.SERVICE_NAME);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }
}
