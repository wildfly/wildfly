/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.discovery.impl.StaticDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;

/**
 * A service which sets up the {@link EJBClientContext} with appropriate remoting receivers and local receivers.
 * The receivers and the client context are configured in a jboss-ejb-client.xml.
 *
 * TODO emulate old configuration using discovery
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class DescriptorBasedEJBClientContextService implements Service<EJBClientContext> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "dd-based-ejb-client-context");

    private final InjectedValue<RemotingProfileService> profileServiceValue = new InjectedValue<RemotingProfileService>();

    /**
     * The client context
     */
    private volatile EJBClientContext ejbClientContext;

    @Override
    public synchronized void start(final StartContext startContext) throws StartException {
        // setup the context with the receivers

        final EJBClientContext.Builder contextBuilder = new EJBClientContext.Builder();
        final RemotingProfileService profileService=profileServiceValue.getValue();

        for(Value<EJBTransportProvider> transportProvider: profileService.getTransportProviders()){
            contextBuilder.addTransportProvider(transportProvider.getValue());
        }
        if(!profileService.getServiceUrls().isEmpty()) {
            DiscoveryProvider staticDiscoveryProvider = new StaticDiscoveryProvider(profileService.getServiceUrls());
            contextBuilder.addDiscoveryProvider(staticDiscoveryProvider);
        }
        this.ejbClientContext = contextBuilder.build();
    }

    @Override
    public synchronized void stop(StopContext context) {
    }

    @Override
    public EJBClientContext getValue() throws IllegalStateException, IllegalArgumentException {
        return this.ejbClientContext;
    }

    public Injector<RemotingProfileService> getProfileServiceInjector() {
        return this.profileServiceValue;
    }
}
