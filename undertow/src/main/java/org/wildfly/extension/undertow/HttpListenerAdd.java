/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.server.ListenerRegistry;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.xnio.OptionMap;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class HttpListenerAdd extends ListenerAdd<HttpListenerService> {

    @Override
    HttpListenerService createService(final Consumer<ListenerService> serviceConsumer, final String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        final boolean proxyProtocol = AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL.resolveModelAttribute(context, model).asBoolean();
        final boolean certificateForwarding = AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        final boolean proxyAddressForwarding = AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        AbstractHttpListenerResourceDefinition.ENABLE_HTTP2.resolveOption(context, model,listenerBuilder);

        handleHttp2Options(context, model, listenerBuilder);

        return new HttpListenerService(serviceConsumer, context.getCurrentAddress(), serverName, listenerBuilder.getMap(), socketOptions, certificateForwarding, proxyAddressForwarding, proxyProtocol);
    }

    static void handleHttp2Options(OperationContext context, ModelNode model, OptionMap.Builder listenerBuilder) throws OperationFailedException {
        AbstractHttpListenerResourceDefinition.HTTP2_ENABLE_PUSH.resolveOption(context, model,listenerBuilder);
        AbstractHttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE.resolveOption(context, model,listenerBuilder);
        AbstractHttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE.resolveOption(context, model,listenerBuilder);
        AbstractHttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS.resolveOption(context, model,listenerBuilder);
        AbstractHttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE.resolveOption(context, model,listenerBuilder);
        AbstractHttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE.resolveOption(context, model,listenerBuilder);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<?> serviceBuilder, ModelNode model, HttpListenerService service) throws OperationFailedException {
        ModelNode redirectBindingRef = ListenerResourceDefinition.REDIRECT_SOCKET.resolveModelAttribute(context, model);
        if (redirectBindingRef.isDefined()) {
            service.getRedirectSocket().set(serviceBuilder.requires(SocketBinding.SERVICE_DESCRIPTOR, redirectBindingRef.asString()));
        }
        service.getHttpListenerRegistry().set(serviceBuilder.requiresCapability(Capabilities.REF_HTTP_LISTENER_REGISTRY, ListenerRegistry.class));
    }
}
