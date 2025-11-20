/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import javax.net.ssl.SSLContext;

import io.undertow.server.ListenerRegistry;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.xnio.OptionMap;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Add handler for HTTPS listeners.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class HttpsListenerAdd extends ListenerAdd<HttpsListenerService> {

    @Override
    HttpsListenerService createService(final Consumer<ListenerService> serviceConsumer, final String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException {
        OptionMap.Builder builder = OptionMap.builder().addAll(socketOptions);

        ModelNode securityRealmModel = HttpsListenerResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        final boolean proxyProtocol = AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL.resolveModelAttribute(context, model).asBoolean();
        String cipherSuites = null;
        if(securityRealmModel.isDefined()) {
            throw ROOT_LOGGER.runtimeSecurityRealmUnsupported();
        }

        OptionMap.Builder listenerBuilder = OptionMap.builder().addAll(listenerOptions);
        AbstractHttpListenerResourceDefinition.ENABLE_HTTP2.resolveOption(context, model, listenerBuilder);
        HttpListenerAdd.handleHttp2Options(context, model, listenerBuilder);

        logRequireHostHttp1Ineffective(context, model);

        final boolean certificateForwarding = AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING.resolveModelAttribute(context, model).asBoolean();
        final boolean proxyAddressForwarding = AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING.resolveModelAttribute(context, model).asBoolean();


        return new HttpsListenerService(serviceConsumer, context.getCurrentAddress(), serverName, listenerBuilder.getMap(), cipherSuites, builder.getMap(), certificateForwarding, proxyAddressForwarding, proxyProtocol);
    }

    @Override
    void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<?> serviceBuilder, ModelNode model, HttpsListenerService service) throws OperationFailedException {
        service.getHttpListenerRegistry().set(serviceBuilder.requiresCapability(Capabilities.REF_HTTP_LISTENER_REGISTRY, ListenerRegistry.class));

        final ModelNode sslContextModel = HttpsListenerResourceDefinition.SSL_CONTEXT.resolveModelAttribute(context, model);
        final String sslContextRef = sslContextModel.isDefined() ? sslContextModel.asString() : null;
        final Supplier<SSLContext> sslContextInjector = sslContextRef != null ? serviceBuilder.requiresCapability(REF_SSL_CONTEXT, SSLContext.class, sslContextRef) : null;

        service.setSSLContextSupplier(()-> {
            if (sslContextRef != null) {
                return sslContextInjector.get();
            }

            try {
                return SSLContext.getDefault();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

    }

}
