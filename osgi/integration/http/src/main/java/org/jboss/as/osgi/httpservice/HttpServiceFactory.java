/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.httpservice;


import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.jboss.as.osgi.OSGiMessages;
import org.jboss.as.web.WebServer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * A {@link ServiceFactory} for {@link HttpService}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jul-2012
 */
final class HttpServiceFactory implements ServiceFactory {

    private final GlobalRegistry registry;
    private final WebServer webServer;
    private final Host virtualHost;
    private StandardContext context;

    HttpServiceFactory(WebServer webServer, Host virtualHost, StandardContext context) {
        this.registry= GlobalRegistry.INSTANCE;
        this.webServer = webServer;
        this.virtualHost = virtualHost;
        this.context = context;
    }

    @Override
    public Object getService(final Bundle bundle, final ServiceRegistration registration) {
        synchronized (registry) {
            return new HttpServiceImpl(context, webServer, virtualHost, bundle);
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        synchronized (registry) {
            HttpServiceImpl httpService = (HttpServiceImpl) service;
            for (Registration reg : registry.unregister(bundle)) {
                httpService.unregisterInternal(reg);
            }
        }
    }

    static class GlobalRegistry {

        static GlobalRegistry INSTANCE = new GlobalRegistry();

        private Map<String, Registration> registrations = new HashMap<String, Registration>();

        private GlobalRegistry() {
        }

        synchronized Registration register(String alias, Bundle bundle, Wrapper wrapper, Registration.Type type) throws NamespaceException {

            if (exists(alias))
                throw new NamespaceException(OSGiMessages.MESSAGES.aliasMappingAlreadyExists(alias));

            LOGGER.infoRegisterHttpServiceAlias(alias);

            Registration result = new Registration(alias, bundle, wrapper, type);
            registrations.put(alias, result);

            return result;
        }

        synchronized boolean exists(String alias) {
            return registrations.get(alias) != null;
        }

        synchronized Registration unregister(String alias, Bundle bundle) {

            if (!exists(alias)) {
                LOGGER.errorf(MESSAGES.aliasMappingDoesNotExist(alias));
                return null;
            }

            Registration reg = registrations.get(alias);
            if (bundle != reg.bundle) {
                LOGGER.errorf(MESSAGES.aliasMappingNotOwnedByBundle(alias, bundle));
                return null;
            }

            LOGGER.infoUnregisterHttpServiceAlias(alias);
            return registrations.remove(reg.alias);
        }

        synchronized Set<Registration> unregister(Bundle bundle) {
            Set<Registration> result = new HashSet<Registration>();
            for (Registration reg : new HashSet<Registration>(registrations.values())) {
                if (bundle == reg.bundle) {
                    registrations.remove(reg.alias);
                    result.add(reg);
                }
            }
            return result;
        }
    }

    static class Registration {
        enum Type {
            SERVLET, RESOURCE
        }

        private final String alias;
        private final Bundle bundle;
        private final Wrapper wrapper;
        private final Type type;

        Registration(String alias, Bundle bundle, Wrapper wrapper, Type type) {
            this.alias = alias;
            this.bundle = bundle;
            this.wrapper = wrapper;
            this.type = type;
        }

        String getAlias() {
            return alias;
        }

        Bundle getBundle() {
            return bundle;
        }

        Wrapper getWrapper() {
            return wrapper;
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Registration [alias=" + alias + ",bundle=" + bundle + ",type=" + type + "]";
        }
    }
}
