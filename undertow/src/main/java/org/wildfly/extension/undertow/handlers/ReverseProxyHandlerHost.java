/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class ReverseProxyHandlerHost extends PersistentResourceDefinition {

    public static final ReverseProxyHandlerHost INSTANCE = new ReverseProxyHandlerHost();

    public static final ServiceName SERVICE_NAME = UndertowService.HANDLER.append("reverse-proxy", "host");


    public static final AttributeDefinition INSTANCE_ID = new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();


    private ReverseProxyHandlerHost() {
        super(PathElement.pathElement(Constants.HOST), UndertowExtension.getResolver(Constants.HANDLER, Constants.REVERSE_PROXY, Constants.HOST));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singletonList(INSTANCE_ID);
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        ReverseProxyHostAdd add = new ReverseProxyHostAdd();
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(SERVICE_NAME, add) {
            @Override
            protected ServiceName serviceName(String name, final PathAddress address) {
                final String proxyName = address.getElement(address.size() - 2).getValue();
                return SERVICE_NAME.append(proxyName).append(name);
            }
        }, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }


    private final class ReverseProxyHostAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : getAttributes()) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final String name = address.getLastElement().getValue();
            final String proxyName = address.getElement(address.size() - 2).getValue();
            final String jvmRoute;
            if(model.hasDefined(Constants.INSTANCE_ID)) {
                jvmRoute = INSTANCE_ID.resolveModelAttribute(context, model).asString();
            } else {
                jvmRoute = null;
            }
            ReverseProxyHostService service = new ReverseProxyHostService(name, jvmRoute);
            ServiceBuilder<ReverseProxyHostService> builder = context.getServiceTarget().addService(SERVICE_NAME.append(proxyName).append(name), service)
                    .addDependency(UndertowService.HANDLER.append(proxyName), ProxyHandler.class, service.proxyHandler);
            if (verificationHandler != null) {
                builder.addListener(verificationHandler);
            }
            ServiceController<ReverseProxyHostService> controller = builder.install();
            if (newControllers != null) {
                newControllers.add(controller);
            }
        }
    }

    private static final class ReverseProxyHostService implements Service<ReverseProxyHostService> {

        private final InjectedValue<ProxyHandler> proxyHandler = new InjectedValue<>();

        private final String name;
        private final String instanceId;

        private ReverseProxyHostService(String name, String instanceId) {
            this.name = name;
            this.instanceId = instanceId;
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getValue().getProxyClient();
            try {
                client.addHost(new URI(name), instanceId);
            } catch (URISyntaxException e) {
                throw new StartException(e);
            }
        }

        @Override
        public void stop(StopContext stopContext) {
            final LoadBalancingProxyClient client = (LoadBalancingProxyClient) proxyHandler.getValue().getProxyClient();
            try {
                client.removeHost(new URI(name));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e); //impossible
            }
        }

        @Override
        public ReverseProxyHostService getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }
    }

}
