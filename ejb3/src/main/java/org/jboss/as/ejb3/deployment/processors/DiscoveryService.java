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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedSetValue;
import org.wildfly.discovery.Discovery;
import org.wildfly.discovery.spi.DiscoveryProvider;

/**
 * A service that provides discovery services.
 * <p>
 * Note: this service is going to move elsewhere in the future.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DiscoveryService implements Service<Discovery> {
    private static final DiscoveryProvider[] NO_PROVIDERS = new DiscoveryProvider[0];

    public static final ServiceName BASE_NAME = ServiceName.JBOSS.append("deployment", "discovery");

    private final InjectedSetValue<DiscoveryProvider> providerInjectors = new InjectedSetValue<>();

    private volatile Discovery discovery;

    public void start(final StartContext context) throws StartException {
        discovery = Discovery.create(providerInjectors.getValue().toArray(NO_PROVIDERS));
    }

    public void stop(final StopContext context) {
        discovery = null;
    }

    public Injector<DiscoveryProvider> getDiscoveryProviderInjector() {
        return providerInjectors.injector();
    }

    public Discovery getValue() throws IllegalStateException, IllegalArgumentException {
        return discovery;
    }
}
