/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
