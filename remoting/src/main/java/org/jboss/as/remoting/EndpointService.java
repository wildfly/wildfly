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

package org.jboss.as.remoting;

import static org.xnio.IoUtils.safeClose;

import java.util.concurrent.Executor;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

/**
 * An MSC service for Remoting endpoints.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EndpointService implements Service<Endpoint> {
    private Endpoint endpoint;
    private OptionMap optionMap;
    private Registration providerRegistration;

    private final InjectedValue<Executor> executor = new InjectedValue<Executor>();

    /**
     * Set the option map for this endpoint.
     *
     * @param optionMap the option map
     */
    public synchronized void setOptionMap(final OptionMap optionMap) {
        this.optionMap = optionMap;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        try {
            endpoint = Remoting.createEndpoint("endpoint", executor.getValue(), optionMap);
            Xnio xnio = XnioUtil.getXnio();

            providerRegistration = endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));

        } catch (Exception e) {
            throw new StartException("Failed to start service", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        safeClose(providerRegistration);
        safeClose(endpoint);
    }

    /** {@inheritDoc} */
    public synchronized Endpoint getValue() throws IllegalStateException {
        final Endpoint endpoint = this.endpoint;
        if (endpoint == null) throw new IllegalStateException();
        return endpoint;
    }

    /**
     * Get the injector for the executor dependency.
     *
     * @return the injector
     */
    Injector<Executor> getExecutorInjector() {
        return executor;
    }
}
