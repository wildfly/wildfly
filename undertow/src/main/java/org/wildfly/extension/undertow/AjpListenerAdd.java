/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

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
class AjpListenerAdd extends ListenerAdd<AjpListenerService> {

    @Override
    AjpListenerService createService(final Consumer<ListenerService> serviceConsumer, final String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        ModelNode schemeNode = AjpListenerResourceDefinition.SCHEME.resolveModelAttribute(context, model);
        String scheme = null;
        if (schemeNode.isDefined()) {
            scheme = schemeNode.asString();
        }
        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        AjpListenerResourceDefinition.MAX_AJP_PACKET_SIZE.resolveOption(context, model,listenerBuilder);
        AjpListenerResourceDefinition.ALLOWED_REQUEST_ATTRIBUTES_PATTERN.resolveOption(context, model,listenerBuilder);
        return new AjpListenerService(serviceConsumer, context.getCurrentAddress(), scheme, listenerBuilder.getMap(), socketOptions);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<?> serviceBuilder, ModelNode model, AjpListenerService service) throws OperationFailedException {
        ModelNode redirectBindingRef = ListenerResourceDefinition.REDIRECT_SOCKET.resolveModelAttribute(context, model);
        if (redirectBindingRef.isDefined()) {
            service.getRedirectSocket().set(serviceBuilder.requires(SocketBinding.SERVICE_DESCRIPTOR, redirectBindingRef.asString()));
        }
    }
}
