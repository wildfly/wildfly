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

import javax.net.ssl.SSLContext;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ElytronSSLContextRegistryService implements Service {

    private static final ElytronSSLContextRegistryService INSTANCE = new ElytronSSLContextRegistryService();
    private static final ServiceName BASE_CLIENT_SSL_CONTEXT_NAME = ServiceName.of("org", "wildfly", "security", "ssl-context");
    private static final ServiceName BASE_CLIENT_KEY_STORE_NAME = ServiceName.of("org", "wildfly", "security", "key-store");
    private static final ServiceName BASE_CLIENT_TRUST_MANAGER_NAME = ServiceName.of("org", "wildfly", "security", "trust-manager");

    private volatile ServiceRegistry serviceRegistry;

    private ElytronSSLContextRegistryService() {
    }

    public static void install(ServiceTarget target, ServiceRegistry unmodifiableServiceRegistry) {
        ServiceName name = ServiceName.JBOSS
                .append("wildfly", "reactive", "messaging", "kafka", "ssl", "context", "registry");
        target.addService(name)
                .setInstance(INSTANCE)
                .install();
        INSTANCE.serviceRegistry = unmodifiableServiceRegistry;
    }

    static SSLContext getInstalledSSLContext(String name) {
        ServiceController<SSLContext> controller = (ServiceController<SSLContext>)INSTANCE.serviceRegistry
                .getService(getSSLContextName(name));
        return controller.getValue();
    }

    static ServiceName getSSLContextName(String name) {
        return BASE_CLIENT_SSL_CONTEXT_NAME.append(name);
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}
