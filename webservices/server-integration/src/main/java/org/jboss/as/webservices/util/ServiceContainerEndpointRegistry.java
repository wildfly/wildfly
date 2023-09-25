/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.jboss.msc.service.ServiceName;
import org.jboss.ws.common.ObjectNameFactory;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.management.EndpointResolver;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ServiceContainerEndpointRegistry implements EndpointRegistry {

    private static final Map<ServiceName, Endpoint> endpoints = new ConcurrentHashMap<>();

    @Override
    public Set<ObjectName> getEndpoints() {
        String contextPath;
        String endpointName;
        StringBuilder name;
        final Set<ObjectName> retVal = new CopyOnWriteArraySet<>();
        for (final ServiceName sname : endpoints.keySet()) {
            contextPath = sname.getParent().getSimpleName().substring(8);
            endpointName = sname.getSimpleName();
            name = new StringBuilder(Endpoint.SEPID_DOMAIN + ":");
            name.append(Endpoint.SEPID_PROPERTY_CONTEXT + "=").append(contextPath).append(",");
            name.append(Endpoint.SEPID_PROPERTY_ENDPOINT + "=").append(endpointName);
            retVal.add(ObjectNameFactory.create(name.toString()));
        }
        return retVal;
    }

    @Override
    public Endpoint getEndpoint(final ObjectName epName) {
        final String context = epName.getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT);
        final String name = epName.getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
        final ServiceName endpointName = WSServices.ENDPOINT_SERVICE.append("context=" + context).append(name);
        return getEndpoint(endpointName);
    }

    @Override
    public Endpoint resolve(final EndpointResolver resolver) {
        return resolver.query(new CopyOnWriteArraySet<>(endpoints.values()).iterator());
    }

    @Override
    public boolean isRegistered(final ObjectName epName) {
        return getEndpoint(epName) != null;
    }

    public static void register(final ServiceName endpointName, final Endpoint endpoint) {
        endpoints.put(endpointName, endpoint);
    }

    public static void unregister(final ServiceName endpointName, final Endpoint endpoint) {
        endpoints.remove(endpointName, endpoint);
    }

    public static Endpoint getEndpoint(final ServiceName endpointName) {
        return endpoints.get(endpointName);
    }
}
