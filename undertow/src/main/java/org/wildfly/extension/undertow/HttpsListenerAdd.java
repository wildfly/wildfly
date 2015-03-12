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

package org.wildfly.extension.undertow;

import io.undertow.server.ListenerRegistry;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.xnio.OptionMap;

/**
 * Add handler for HTTPS listeners.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpsListenerAdd extends ListenerAdd {

    HttpsListenerAdd(HttpsListenerResourceDefinition def) {
        super(def);
    }

    @Override
    ListenerService<? extends ListenerService> createService(String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        OptionMap.Builder builder = OptionMap.builder().addAll(socketOptions);
        HttpsListenerResourceDefinition.VERIFY_CLIENT.resolveOption(context, model,builder);
        HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES.resolveOption(context, model, builder);
        HttpsListenerResourceDefinition.ENABLED_PROTOCOLS.resolveOption(context, model, builder);

        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        HttpsListenerResourceDefinition.ENABLE_HTTP2.resolveOption(context, model,listenerBuilder);
        HttpsListenerResourceDefinition.ENABLE_SPDY.resolveOption(context, model,listenerBuilder);
        return new HttpsListenerService(name, serverName, listenerBuilder.getMap(), builder.getMap());
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, ServiceBuilder<? extends ListenerService> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException {
        serviceBuilder.addDependency(HttpListenerAdd.REGISTRY_SERVICE_NAME, ListenerRegistry.class, ((HttpListenerService) service).getHttpListenerRegistry());
        final String securityRealm = HttpsListenerResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model).asString();
        SecurityRealm.ServiceUtil.addDependency(serviceBuilder, ((HttpsListenerService) service).getSecurityRealm(), securityRealm, false);
    }

}
