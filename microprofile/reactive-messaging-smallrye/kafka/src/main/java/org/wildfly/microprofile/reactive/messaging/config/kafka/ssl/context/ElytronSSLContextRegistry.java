/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
