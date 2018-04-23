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

import static org.wildfly.extension.undertow.Capabilities.REF_SOCKET_BINDING;

import io.undertow.server.ListenerRegistry;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HttpListenerAdd extends ListenerAdd {

    static final ServiceName REGISTRY_SERVICE_NAME = ServiceName.JBOSS.append("http", "listener", "registry");

    HttpListenerAdd(ListenerResourceDefinition definition) {
        super(definition);
    }

    @Override
    ListenerService createService(String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        final boolean proxyProtocol = HttpListenerResourceDefinition.PROXY_PROTOCOL.resolveModelAttribute(context, model).asBoolean();
        final boolean certificateForwarding = HttpListenerResourceDefinition.CERTIFICATE_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        final boolean proxyAddressForwarding = HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        HttpListenerResourceDefinition.ENABLE_HTTP2.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.resolveOption(context, model,listenerBuilder);


        handleHttp2Options(context, model, listenerBuilder);

        return new HttpListenerService(name, serverName, listenerBuilder.getMap(), socketOptions, certificateForwarding, proxyAddressForwarding, proxyProtocol);
    }

    static void handleHttp2Options(OperationContext context, ModelNode model, OptionMap.Builder listenerBuilder) throws OperationFailedException {
        HttpListenerResourceDefinition.HTTP2_ENABLE_PUSH.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE.resolveOption(context, model,listenerBuilder);
        HttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE.resolveOption(context, model,listenerBuilder);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<? extends UndertowListener> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException {
        ModelNode redirectBindingRef = ListenerResourceDefinition.REDIRECT_SOCKET.resolveModelAttribute(context, model);
        if (redirectBindingRef.isDefined()) {
            ServiceName serviceName = context.getCapabilityServiceName(REF_SOCKET_BINDING, redirectBindingRef.asString(), SocketBinding.class);
            serviceBuilder.addDependency(serviceName, SocketBinding.class, service.getRedirectSocket());
        }
        serviceBuilder.addDependency(REGISTRY_SERVICE_NAME, ListenerRegistry.class, ((HttpListenerService) service).getHttpListenerRegistry());
    }
}
