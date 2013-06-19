/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
