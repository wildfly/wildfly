/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.util;

import org.jboss.wsf.spi.management.EndpointRegistry;

/**
 * JBoss AS 7 WS Endpoint registry factory
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @since 25-Jan-2012
 *
 */
public final class EndpointRegistryFactory extends org.jboss.wsf.spi.management.EndpointRegistryFactory {

    private static final EndpointRegistry registry = new ServiceContainerEndpointRegistry();

    public EndpointRegistryFactory() {
        super();
    }

    /**
     * Retrieves endpoint registry through the corresponding Service
     *
     * @return endpoint registry
     */
    public EndpointRegistry getEndpointRegistry() {
        return registry;
    }

}
