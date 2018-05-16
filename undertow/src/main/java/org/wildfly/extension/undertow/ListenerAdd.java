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

import static org.wildfly.extension.undertow.Capabilities.REF_IO_WORKER;
import static org.wildfly.extension.undertow.Capabilities.REF_SOCKET_BINDING;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.LISTENER_CAPABILITY;
import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.server.handlers.PeerNameResolvingHandler;
import io.undertow.servlet.handlers.MarkSecureHandler;
import io.undertow.util.HttpString;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.io.OptionList;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
abstract class ListenerAdd extends AbstractAddStepHandler {

    ListenerAdd(ListenerResourceDefinition definition) {
        super(definition.getAttributes());
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);

        String ourCap = LISTENER_CAPABILITY.getDynamicName(context.getCurrentAddress());
        String serverCap = SERVER_CAPABILITY.getDynamicName(context.getCurrentAddress().getParent());
        context.registerAdditionalCapabilityRequirement(serverCap, ourCap, null);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress parent = address.getParent();
        final String name = context.getCurrentAddressValue();
        final String bindingRef = ListenerResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        final String workerName = ListenerResourceDefinition.WORKER.resolveModelAttribute(context, model).asString();
        final String bufferPoolName = ListenerResourceDefinition.BUFFER_POOL.resolveModelAttribute(context, model).asString();
        final boolean enabled = ListenerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        final boolean peerHostLookup = ListenerResourceDefinition.RESOLVE_PEER_ADDRESS.resolveModelAttribute(context, model).asBoolean();
        final boolean secure = ListenerResourceDefinition.SECURE.resolveModelAttribute(context, model).asBoolean();

        OptionMap listenerOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.LISTENER_OPTIONS);
        OptionMap socketOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.SOCKET_OPTIONS);
        String serverName = parent.getLastElement().getValue();
        final ListenerService service = createService(name, serverName, context, model, listenerOptions,socketOptions);
        if (peerHostLookup) {
            service.addWrapperHandler(PeerNameResolvingHandler::new);
        }
        service.setEnabled(enabled);
        if(secure) {
            service.addWrapperHandler(MarkSecureHandler.WRAPPER);
        }
        List<String> disallowedMethods = ListenerResourceDefinition.DISALLOWED_METHODS.unwrap(context, model);
        if(!disallowedMethods.isEmpty()) {
            final Set<HttpString> methodSet = new HashSet<>();
            for (String i : disallowedMethods) {
                HttpString httpString = new HttpString(i.trim());
                methodSet.add(httpString);
            }
            service.addWrapperHandler(handler -> new DisallowedMethodsHandler(handler, methodSet));
        }

        final CapabilityServiceBuilder<? extends UndertowListener> serviceBuilder = context.getCapabilityServiceTarget().addCapability(ListenerResourceDefinition.LISTENER_CAPABILITY, service);
        serviceBuilder.addCapabilityRequirement(REF_IO_WORKER, XnioWorker.class, service.getWorker(), workerName)
                .addCapabilityRequirement(REF_SOCKET_BINDING, SocketBinding.class, service.getBinding(), bindingRef)
                .addCapabilityRequirement(Capabilities.CAPABILITY_BYTE_BUFFER_POOL, ByteBufferPool.class, service.getBufferPool(), bufferPoolName)
                .addCapabilityRequirement(Capabilities.CAPABILITY_SERVER, Server.class, service.getServerService(), serverName)
                .addAliases(UndertowService.listenerName(name))
                ;

        configureAdditionalDependencies(context, serviceBuilder, model, service);
        serviceBuilder.install();

    }

    abstract ListenerService createService(String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException;

    abstract void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<? extends UndertowListener> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException;

}
