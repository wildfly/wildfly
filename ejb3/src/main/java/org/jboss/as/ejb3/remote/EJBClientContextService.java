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

package org.jboss.as.ejb3.remote;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedSetValue;
import org.wildfly.discovery.ServiceRegistry;
import org.wildfly.discovery.impl.AggregateRegistryProvider;
import org.wildfly.discovery.spi.RegistryProvider;

/**
 * The EJB client context service.
 *
 * @author Stuart Douglas
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EJBClientContextService implements Service<EJBClientContext> {

    private static final RegistryProvider[] NO_PROVIDERS = new RegistryProvider[0];

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext");

    public static final ServiceName DEPLOYMENT_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("deployment");

    public static final ServiceName DEFAULT_SERVICE_NAME = BASE_SERVICE_NAME.append("default");

    private volatile EJBClientContext clientContext;

    private final InjectedSetValue<RegistryProvider> registryProviderInjector = new InjectedSetValue<>();
    private final InjectedSetValue<List<EJBTransportProvider>> transportProvidersInjector = new InjectedSetValue<>();

    /**
     * TODO: possibly move to using a per-thread solution for embedded support
     */
    private final boolean makeGlobal;

    public EJBClientContextService(final boolean makeGlobal) {
        this.makeGlobal = makeGlobal;
    }

    public EJBClientContextService() {
        this(false);
    }

    public void start(final StartContext context) throws StartException {
        final EJBClientContext.Builder builder = new EJBClientContext.Builder();
        final Set<RegistryProvider> registryProviders = this.registryProviderInjector.getValue();
        final int size = registryProviders.size();
        if (size > 0) {
            final RegistryProvider registryProvider;
            if (size == 1) {
                registryProvider = registryProviders.iterator().next();
            } else {
                registryProvider = new AggregateRegistryProvider(registryProviders.toArray(NO_PROVIDERS));
            }
            builder.setServiceRegistry(ServiceRegistry.create(registryProvider));
        }
        for (List<EJBTransportProvider> transportProviderList : transportProvidersInjector.getValue()) {
            for (EJBTransportProvider transportProvider : transportProviderList) {
                builder.addTransportProvider(transportProvider);
            }
        }
        clientContext = builder.build();
        if (makeGlobal) {
            doPrivileged((PrivilegedAction<Void>) () -> {
                EJBClientContext.getContextManager().setGlobalDefault(clientContext);
                return null;
            });
        }
    }

    public void stop(final StopContext context) {
        clientContext = null;
        if (makeGlobal) {
            doPrivileged((PrivilegedAction<Void>) () -> {
                EJBClientContext.getContextManager().setGlobalDefault(null);
                return null;
            });
        }
    }

    public EJBClientContext getValue() throws IllegalStateException, IllegalArgumentException {
        return clientContext;
    }

    public Injector<List<EJBTransportProvider>> createEJBTransportProviderListInjector() {
        return transportProvidersInjector.injector();
    }

    public Injector<EJBTransportProvider> createEJBTransportProviderInjector() {
        final Injector<List<EJBTransportProvider>> injector = transportProvidersInjector.injector();
        return new Injector<EJBTransportProvider>() {
            public void inject(final EJBTransportProvider value) throws InjectionException {
                injector.inject(Collections.singletonList(value));
            }

            public void uninject() {
                injector.uninject();
            }
        };
    }

    public Injector<RegistryProvider> createRegistryProviderInjector() {
        return registryProviderInjector.injector();
    }
}
