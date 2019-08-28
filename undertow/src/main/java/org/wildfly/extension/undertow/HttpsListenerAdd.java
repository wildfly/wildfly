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

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;

import javax.net.ssl.SSLContext;

import io.undertow.server.ListenerRegistry;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.OptionMap;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Add handler for HTTPS listeners.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class HttpsListenerAdd extends ListenerAdd {

    HttpsListenerAdd(HttpsListenerResourceDefinition def) {
        super(def);
    }

    @Override
    ListenerService createService(final Consumer<ListenerService> serviceConsumer, final String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        OptionMap.Builder builder = OptionMap.builder().addAll(socketOptions);

        ModelNode securityRealmModel = HttpsListenerResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        final boolean proxyProtocol = HttpListenerResourceDefinition.PROXY_PROTOCOL.resolveModelAttribute(context, model).asBoolean();
        String cipherSuites = null;
        if(securityRealmModel.isDefined()) {
            //we only support setting these options for security realms
            HttpsListenerResourceDefinition.VERIFY_CLIENT.resolveOption(context, model, builder);

            ModelNode value = HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES.resolveModelAttribute(context, model);
            cipherSuites = value.isDefined() ? value.asString() : null;

            HttpsListenerResourceDefinition.ENABLED_PROTOCOLS.resolveOption(context, model, builder);
            HttpsListenerResourceDefinition.SSL_SESSION_CACHE_SIZE.resolveOption(context, model, builder);
            HttpsListenerResourceDefinition.SSL_SESSION_TIMEOUT.resolveOption(context, model, builder);
        }

        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        HttpsListenerResourceDefinition.ENABLE_HTTP2.resolveOption(context, model, listenerBuilder);
        HttpListenerAdd.handleHttp2Options(context, model, listenerBuilder);

        HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.resolveOption(context, model,listenerBuilder);

        final boolean certificateForwarding = HttpListenerResourceDefinition.CERTIFICATE_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        final boolean proxyAddressForwarding = HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING.resolveModelAttribute(context, model).asBoolean();


        return new HttpsListenerService(serviceConsumer, context.getCurrentAddress(), serverName, listenerBuilder.getMap(), cipherSuites, builder.getMap(), certificateForwarding, proxyAddressForwarding, proxyProtocol);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<?> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException {
        ((HttpListenerService) service).getHttpListenerRegistry().set(serviceBuilder.requiresCapability(Capabilities.REF_HTTP_LISTENER_REGISTRY, ListenerRegistry.class));

        final ModelNode sslContextModel = HttpsListenerResourceDefinition.SSL_CONTEXT.resolveModelAttribute(context, model);
        final ModelNode securityRealmModel = HttpsListenerResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        final String sslContextRef = sslContextModel.isDefined() ? sslContextModel.asString() : null;
        final Supplier<SSLContext> sslContextInjector = sslContextRef != null ? serviceBuilder.requiresCapability(REF_SSL_CONTEXT, SSLContext.class, sslContextRef) : null;
        final String securityRealmRef = securityRealmModel.isDefined() ? securityRealmModel.asString() : null;
        final Supplier<SecurityRealm> securityRealmInjector = securityRealmRef != null ? SecurityRealm.ServiceUtil.requires(serviceBuilder, securityRealmRef) : null;

        ((HttpsListenerService)service).setSSLContextSupplier(()-> {
            if (sslContextRef != null) {
                return sslContextInjector.get();
            }

            if (securityRealmRef != null) {
                 SSLContext sslContext = securityRealmInjector.get().getSSLContext();

                 if (sslContext == null) {
                     throw UndertowLogger.ROOT_LOGGER.noSslContextInSecurityRealm(securityRealmRef);
                 }
                 return sslContext;
            }

            try {
                return SSLContext.getDefault();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

    }

}
