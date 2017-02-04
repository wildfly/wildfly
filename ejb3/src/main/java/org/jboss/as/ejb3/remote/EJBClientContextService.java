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

import org.jboss.as.ejb3.subsystem.EJBClientConfiguratorService;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The EJB client context service.
 *
 * @author Stuart Douglas
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EJBClientContextService implements Service<EJBClientContextService> {

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext");

    public static final ServiceName DEPLOYMENT_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("deployment");

    public static final ServiceName DEFAULT_SERVICE_NAME = BASE_SERVICE_NAME.append("default");

    private EJBClientContext clientContext;

    private final InjectedValue<EJBClientConfiguratorService> configuratorServiceInjector = new InjectedValue<>();
    private final InjectedValue<EJBTransportProvider> localProviderInjector = new InjectedValue<>();
    private final InjectedValue<RemotingProfileService> profileServiceInjector = new InjectedValue<>();

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

        // apply subsystem-level configuration that applies to all EJB client contexts
        configuratorServiceInjector.getValue().accept(builder);

        final EJBTransportProvider localTransport = localProviderInjector.getOptionalValue();
        if (localTransport != null) {
            builder.addTransportProvider(localTransport);
        }

        final RemotingProfileService profileService = profileServiceInjector.getOptionalValue();
        if (profileService != null) for (RemotingProfileService.ConnectionSpec spec : profileService.getConnectionSpecs()) {
            final EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
            connBuilder.setDestination(spec.getInjector().getValue().getDestinationUri());
            // connBuilder.setConnectionTimeout(timeout);
            builder.addClientConnection(connBuilder.build());
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

    public EJBClientContextService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public EJBClientContext getClientContext() {
        return clientContext;
    }

    public InjectedValue<EJBClientConfiguratorService> getConfiguratorServiceInjector() {
        return configuratorServiceInjector;
    }

    public InjectedValue<EJBTransportProvider> getLocalProviderInjector() {
        return localProviderInjector;
    }

    public InjectedValue<RemotingProfileService> getProfileServiceInjector() {
        return profileServiceInjector;
    }
}
