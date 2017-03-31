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

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class AjpListenerAdd extends ListenerAdd {

    AjpListenerAdd(AjpListenerResourceDefinition def) {
        super(def);
    }

    @Override
    ListenerService createService(String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        ModelNode schemeNode = AjpListenerResourceDefinition.SCHEME.resolveModelAttribute(context, model);
        String scheme = null;
        if (schemeNode.isDefined()) {
            scheme = schemeNode.asString();
        }
        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        AjpListenerResourceDefinition.MAX_AJP_PACKET_SIZE.resolveOption(context, model,listenerBuilder);
        return new AjpListenerService(name, scheme, listenerBuilder.getMap(), socketOptions);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<? extends UndertowListener> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException {
        ModelNode redirectBindingRef = ListenerResourceDefinition.REDIRECT_SOCKET.resolveModelAttribute(context, model);
        if (redirectBindingRef.isDefined()) {
            serviceBuilder.addCapabilityRequirement(REF_SOCKET_BINDING, SocketBinding.class, service.getRedirectSocket(), redirectBindingRef.asString());
        }
    }
}
