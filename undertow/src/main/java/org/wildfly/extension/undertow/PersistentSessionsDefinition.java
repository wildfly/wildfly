/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Global session cookie config
 *
 * @author Stuart Douglas
 */
class PersistentSessionsDefinition extends PersistentResourceDefinition {

    static final PersistentSessionsDefinition INSTANCE = new PersistentSessionsDefinition();

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

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = {
            PATH,
            RELATIVE_TO
    };
    static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }
    }


    private PersistentSessionsDefinition() {
        super(UndertowExtension.PATH_PERSISTENT_SESSIONS,
                UndertowExtension.getResolver(UndertowExtension.PATH_PERSISTENT_SESSIONS.getKeyValuePair()),
                new PersistentSessionsAdd(),
                new PersistentSessionsRemove());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    public static boolean isEnabled(final OperationContext context, final ModelNode model) throws OperationFailedException {
        return model.isDefined();
    }


    private static class PersistentSessionsAdd extends RestartParentResourceAddHandler {
        protected PersistentSessionsAdd() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey());
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
            if (isEnabled(context, model)) {
                ModelNode pathValue = PATH.resolveModelAttribute(context, model);
                ServiceBuilder<SessionPersistenceManager> builder;
                if (pathValue.isDefined()) {
                    String path = pathValue.asString();
                    ModelNode relativeToValue = RELATIVE_TO.resolveModelAttribute(context, model);
                    String relativeTo = relativeToValue.isDefined() ? relativeToValue.asString() : null;
                    final DiskBasedModularPersistentSessionManager service = new DiskBasedModularPersistentSessionManager(path, relativeTo);
                    builder = context.getServiceTarget().addService(AbstractPersistentSessionManager.SERVICE_NAME, service)
                            .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, service.getModuleLoaderInjectedValue())
                            .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManager());

                } else {
                    final InMemoryModularPersistentSessionManager service = new InMemoryModularPersistentSessionManager();
                    builder = context.getServiceTarget().addService(AbstractPersistentSessionManager.SERVICE_NAME, service)
                            .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, service.getModuleLoaderInjectedValue());
                }
                builder.install();
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
            ServletContainerAdd.INSTANCE.installRuntimeServices(context, parentModel, parentAddress.getLastElement().getValue());
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return UndertowService.SERVLET_CONTAINER.append(parentAddress.getLastElement().getValue());
        }
    }

    private static class PersistentSessionsRemove extends RestartParentResourceRemoveHandler {

        protected PersistentSessionsRemove() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey());
        }

        @Override
        protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
            super.removeServices(context, parentService, parentModel);
            context.removeService(AbstractPersistentSessionManager.SERVICE_NAME);
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.INSTANCE.installRuntimeServices(context, parentModel, parentAddress.getLastElement().getValue());
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return UndertowService.SERVLET_CONTAINER.append(parentAddress.getLastElement().getValue());
        }


    }
}
