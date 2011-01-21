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

package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.NewExtensionContextImpl;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewOperationContextImpl;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.AddNamespaceHandler;
import org.jboss.as.controller.operations.common.AddSchemaLocationHandler;
import org.jboss.as.controller.operations.common.RemoveNamespaceHandler;
import org.jboss.as.controller.operations.common.RemoveSchemaLocationHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.operations.AddExtensionHandler;
import org.jboss.as.server.operations.RemoveExtensionHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewServerControllerImpl extends BasicModelController implements NewServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ServiceContainer container;
    private final ServiceRegistry serviceRegistry;
    private final ServerEnvironment serverEnvironment;
    private volatile State state;

    NewServerControllerImpl(final ServiceContainer container, final ServerEnvironment serverEnvironment, final NewConfigurationPersister configurationPersister) {
        super(configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
        this.container = container;
        this.serverEnvironment = serverEnvironment;
        serviceRegistry = new DelegatingServiceRegistry(container);
    }

    void init() {
        state = State.STARTING;

        createCoreModel();
        // Build up the core model registry
        ModelNodeRegistration root = getRegistry();
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1));
        root.registerOperationHandler(AddNamespaceHandler.OPERATION_NAME, AddNamespaceHandler.INSTANCE, AddNamespaceHandler.INSTANCE, false);
        root.registerOperationHandler(RemoveNamespaceHandler.OPERATION_NAME, RemoveNamespaceHandler.INSTANCE, RemoveNamespaceHandler.INSTANCE, false);
        root.registerOperationHandler(AddSchemaLocationHandler.OPERATION_NAME, AddSchemaLocationHandler.INSTANCE, AddSchemaLocationHandler.INSTANCE, false);
        root.registerOperationHandler(RemoveSchemaLocationHandler.OPERATION_NAME, RemoveSchemaLocationHandler.INSTANCE, RemoveSchemaLocationHandler.INSTANCE, false);

        ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);

        ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        NewExtensionContext extensionContext = new NewExtensionContextImpl(getRegistry(), deployments);
        AddExtensionHandler addExtensionHandler = new AddExtensionHandler(extensionContext);
        extensions.registerOperationHandler(AddExtensionHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(RemoveExtensionHandler.OPERATION_NAME, RemoveExtensionHandler.INSTANCE, RemoveExtensionHandler.INSTANCE, false);

    }

    private void createCoreModel() {
        ModelNode root = getModel();
        root.get(NAMESPACE).setEmptyList();
        root.get(SCHEMA_LOCATION).setEmptyList();
        root.get(NAME);
        root.get(MANAGEMENT);
        root.get(PROFILE_NAME);
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SUBSYSTEM);
        root.get(INTERFACE);
        root.get(SOCKET_BINDING_GROUP);
        root.get(SYSTEM_PROPERTY);
        root.get(DEPLOYMENT);
    }

    void finishBoot() {
        state = State.RUNNING;
    }

    /** {@inheritDoc} */
    @Override
    public ServerEnvironment getServerEnvironment() {
        return serverEnvironment;
    }

    /**
     * Get this server's service container registry.
     *
     * @return the container registry
     */
    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Get the server controller state.
     *
     * @return the state
     */
    @Override
    public State getState() {
        return state;
    }

    /** {@inheritDoc} */
    @Override
    protected NewOperationContext getOperationContext(final ModelNode subModel, final ModelNode operation, final OperationHandler operationHandler) {
        if (operationHandler instanceof BootOperationHandler) {
            if (state == State.STARTING) {
                return new BootContextImpl(subModel, getRegistry());
            } else {
                state = State.RESTART_REQUIRED;
                return super.getOperationContext(subModel, operation, operationHandler);
            }
        } else if (operationHandler instanceof RuntimeOperationHandler && ! (state == State.RESTART_REQUIRED && operationHandler instanceof ModelUpdateOperationHandler)) {
            return new RuntimeContextImpl(subModel, getRegistry());
        } else {
            return super.getOperationContext(subModel, operation, operationHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Cancellable doExecute(final NewOperationContext context, final ModelNode operation, final OperationHandler operationHandler, final ResultHandler resultHandler) {
        if (context instanceof NewBootOperationContext) {
            return ((BootOperationHandler)operationHandler).execute(context, operation, resultHandler);
        } else if (context instanceof NewRuntimeOperationContext) {
            return ((RuntimeOperationHandler)operationHandler).execute(context, operation, resultHandler);
        } else {
            return super.doExecute(context, operation, operationHandler, resultHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void persistConfiguration(final ModelNode model) {
        // do not persist during startup
        if (state != State.STARTING) {
            super.persistConfiguration(model);
        }
    }

    private class BootContextImpl extends NewOperationContextImpl implements NewBootOperationContext {

        private BootContextImpl(final ModelNode subModel, final ModelNodeRegistration registry) {
            super(NewServerControllerImpl.this, registry, subModel);
        }

        @Override
        public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
        }

        @Override
        public NewServerController getController() {
            return (NewServerController) super.getController();
        }

        @Override
        public ServiceTarget getServiceTarget() {
            // TODO: A tracking service listener which will somehow call complete when the operation is done
            return container;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
        }
    }

    private class RuntimeContextImpl extends NewOperationContextImpl implements NewRuntimeOperationContext {

        private RuntimeContextImpl(final ModelNode subModel, final ModelNodeRegistration registry) {
            super(NewServerControllerImpl.this, registry, subModel);
        }

        @Override
        public NewServerController getController() {
            return (NewServerController) super.getController();
        }

        @Override
        public ServiceTarget getServiceTarget() {
            // TODO: A tracking service listener which will somehow call complete when the operation is done
            return container;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
        }
    }
}
