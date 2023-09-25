/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import javax.net.ssl.SSLContext;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ElytronSSLContextRegistry {

    private static final ElytronSSLContextRegistry INSTANCE = new ElytronSSLContextRegistry();
    private static final ServiceName BASE_CLIENT_SSL_CONTEXT_NAME = ServiceName.of("org", "wildfly", "security", "ssl-context");

    private volatile ServiceRegistry serviceRegistry;

    private ElytronSSLContextRegistry() {
    }

    public static void setServiceRegistry(ServiceRegistry serviceRegistry) {
        INSTANCE.serviceRegistry = serviceRegistry;
    }

    static boolean isSSLContextInstalled(String name) {
        return INSTANCE.getSSLContextController(name) != null;
    }

    static SSLContext getInstalledSSLContext(String name) {
        ServiceController<SSLContext> controller = INSTANCE.getSSLContextController(name);
        if (controller == null) {
            throw LOGGER.noElytronClientSSLContext(name);
        }
        return controller.getValue();
    }

    private ServiceController<SSLContext> getSSLContextController(String name) {
        return (ServiceController<SSLContext>)INSTANCE.serviceRegistry.getService(getSSLContextName(name));
    }

    static ServiceName getSSLContextName(String name) {
        return BASE_CLIENT_SSL_CONTEXT_NAME.append(name);
    }

}
