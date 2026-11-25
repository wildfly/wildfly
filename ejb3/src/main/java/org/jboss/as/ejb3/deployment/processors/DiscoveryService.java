/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.profile.RemotingProfileService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.discovery.Discovery;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.impl.StaticDiscoveryProvider;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A service that provides discovery services.
 * <p>
 * Note: this service is going to move elsewhere in the future.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DiscoveryService implements Service {
    private static final DiscoveryProvider[] NO_PROVIDERS = new DiscoveryProvider[0];
    public static final ServiceName BASE_NAME = ServiceName.JBOSS.append("deployment", "discovery");

    private final Collection<DiscoveryProvider> providers = new LinkedHashSet<>();
    private final Consumer<Discovery> discoveryConsumer;
    private final Supplier<RemotingProfileService> remotingProfileServiceSupplier;
    private final Supplier<DiscoveryProvider> localEJBDiscoveryProviderSupplier;

    private volatile DiscoveryProvider remotingDP;
    private volatile DiscoveryProvider localDP;

    public DiscoveryService(final Consumer<Discovery> discoveryConsumer, final Supplier<RemotingProfileService> remotingProfileServiceSupplier, final Supplier<DiscoveryProvider> localEJBDiscoveryProviderSupplier) {
        this.discoveryConsumer = discoveryConsumer;
        this.remotingProfileServiceSupplier = remotingProfileServiceSupplier;
        this.localEJBDiscoveryProviderSupplier = localEJBDiscoveryProviderSupplier;
    }

    public void start(final StartContext context) throws StartException {
        final RemotingProfileService remotingProfileService = remotingProfileServiceSupplier.get();
        final DiscoveryProvider localEJBDiscoveryProvider = localEJBDiscoveryProviderSupplier.get();

        if (remotingProfileService != null) {
            remotingDP = new StaticDiscoveryProvider(remotingProfileService.getServiceUrls());
        }
        if (localEJBDiscoveryProvider != null) {
            localDP = localEJBDiscoveryProvider;
        }
        final DiscoveryProvider[] providersArray;
        synchronized (providers) {
            if (remotingDP != null) providers.add(remotingDP);
            if (localDP != null) providers.add(localDP);
            providersArray = providers.toArray(NO_PROVIDERS);
        }
        discoveryConsumer.accept(Discovery.create(providersArray));
    }

    public void stop(final StopContext context) {
        discoveryConsumer.accept(null);
        synchronized (providers) {
            if (remotingDP != null) {
                providers.remove(remotingDP);
                remotingDP = null;
            }
            if (localDP != null) {
                providers.remove(localDP);
                localDP = null;
            }
        }
    }

    Consumer<DiscoveryProvider> getDiscoveryProviderConsumer() {
        return new Consumer<DiscoveryProvider>() {
            @Override
            public void accept(final DiscoveryProvider discoveryProvider) {
                synchronized (providers) {
                    providers.add(discoveryProvider);
                }
            }
        };
    }
}
