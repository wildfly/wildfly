/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.network.OutboundConnection;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.discovery.ServiceURL;
import org.xnio.OptionMap;

/**
 * Service which contains the static configuration data found in a Jakarta Enterprise Beans Remoting profile, either in the subsystem or in a
 * deployment descriptor.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RemotingProfileService implements Service {

    /**
     * There URLs are used to allow discovery to find these connections.
     */
    private final List<ServiceURL> serviceUrls;
    private final Map<String, RemotingConnectionSpec> remotingConnectionSpecMap;
    private final List<HttpConnectionSpec> httpConnectionSpecs;
    private final Consumer<RemotingProfileService> consumer;
    private final Supplier<EJBTransportProvider> localTransportProviderSupplier;

    public RemotingProfileService(final Consumer<RemotingProfileService> consumer, final Supplier<EJBTransportProvider> localTransportProviderSupplier,
                                  final List<ServiceURL> serviceUrls, final Map<String, RemotingConnectionSpec> remotingConnectionSpecMap,
                                  final List<HttpConnectionSpec> httpConnectionSpecs) {
        this.consumer = consumer;
        this.localTransportProviderSupplier = localTransportProviderSupplier;
        this.serviceUrls = serviceUrls;
        this.remotingConnectionSpecMap = remotingConnectionSpecMap;
        this.httpConnectionSpecs = httpConnectionSpecs;
    }

    @Override
    public void start(StartContext context) throws StartException {
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        consumer.accept(null);
    }

    public Supplier<EJBTransportProvider> getLocalTransportProviderSupplier() {
        return localTransportProviderSupplier;
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

    public static final class RemotingConnectionSpec {
        private final String connectionName;
        private final Supplier<OutboundConnection> supplier;
        private final OptionMap connectOptions;
        private final long connectTimeout;

        public RemotingConnectionSpec(final String connectionName, final Supplier<OutboundConnection> supplier, final OptionMap connectOptions, final long connectTimeout) {
            this.connectionName = connectionName;
            this.supplier = supplier;
            this.connectOptions = connectOptions;
            this.connectTimeout = connectTimeout;
        }

        public String getConnectionName() {
            return connectionName;
        }

        public Supplier<OutboundConnection> getSupplier() {
            return supplier;
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
