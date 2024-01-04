/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful.cache;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Provides a {@link ServiceName} for a statful session bean cache provider.
 * @author Paul Ferraro
 */
public class StatefulSessionBeanCacheProviderServiceNameProvider extends SimpleServiceNameProvider {

    private static final ServiceName BASE_CACHE_SERVICE_NAME = ServiceName.JBOSS.append("ejb", "cache");
    public static final ServiceName DEFAULT_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("sfsb-default");
    public static final ServiceName DEFAULT_PASSIVATION_DISABLED_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("sfsb-default-passivation-disabled");

    protected static final ServiceName BASE_CACHE_PROVIDER_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("provider");

    public StatefulSessionBeanCacheProviderServiceNameProvider(String cacheName) {
        this(BASE_CACHE_PROVIDER_SERVICE_NAME.append(cacheName));
    }

    protected StatefulSessionBeanCacheProviderServiceNameProvider(ServiceName name) {
        super(name);
    }
}
