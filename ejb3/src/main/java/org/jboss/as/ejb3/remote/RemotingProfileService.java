/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.as.network.OutboundConnection;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.discovery.ServiceURL;
import org.xnio.OptionMap;

/**
 * Service which contains the static configuration data found in an EJB Remoting profile, either in the subsystem or in a
 * deployment descriptor.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RemotingProfileService implements Service<RemotingProfileService> {

    /**
     * There URLs are used to allow discovery to find these connections.
     */
    private final List<ServiceURL> serviceUrls;
    private final Map<String, RemotingConnectionSpec> remotingConnectionSpecMap;
    private final List<HttpConnectionSpec> httpConnectionSpecs;
    private final InjectedValue<EJBTransportProvider> localTransportProviderInjector = new InjectedValue<>();

    public RemotingProfileService(final List<ServiceURL> serviceUrls, final Map<String, RemotingConnectionSpec> remotingConnectionSpecMap,
                                  final List<HttpConnectionSpec> httpConnectionSpecs) {
        this.serviceUrls = serviceUrls;
        this.remotingConnectionSpecMap = remotingConnectionSpecMap;
        this.httpConnectionSpecs = httpConnectionSpecs;
    }

    @Override
    public RemotingProfileService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    public Collection<RemotingConnectionSpec> getConnectionSpecs() {
        return remotingConnectionSpecMap.values();
    }

    public Collection<HttpConnectionSpec> getHttpConnectionSpecs() {
        return httpConnectionSpecs;
    }

    public List<ServiceURL> getServiceUrls() {
        return serviceUrls;
    }

    public InjectedValue<EJBTransportProvider> getLocalTransportProviderInjector() {
        return localTransportProviderInjector;
    }

    public static final class RemotingConnectionSpec {
        private final String connectionName;
        private final InjectedValue<OutboundConnection> injector;
        private final OptionMap connectOptions;
        private final long connectTimeout;

        public RemotingConnectionSpec(final String connectionName, final InjectedValue<OutboundConnection> injector, final OptionMap connectOptions, final long connectTimeout) {
            this.connectionName = connectionName;
            this.injector = injector;
            this.connectOptions = connectOptions;
            this.connectTimeout = connectTimeout;
        }

        public String getConnectionName() {
            return connectionName;
        }

        public InjectedValue<OutboundConnection> getInjector() {
            return injector;
        }

        public OptionMap getConnectOptions() {
            return connectOptions;
        }

        public long getConnectTimeout() {
            return connectTimeout;
        }
    }

    public static final class HttpConnectionSpec {
        private final String uri;

        public HttpConnectionSpec(final String uri) {
            this.uri = uri;
        }

        public URI getUri() {
            try {
                return new URI(uri);
            } catch(Exception e) {
                return null;
            }
        }
    }
}
