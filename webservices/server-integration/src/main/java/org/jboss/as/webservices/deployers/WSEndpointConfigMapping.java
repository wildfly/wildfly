/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.deployers;

import java.util.HashMap;
import java.util.Map;

import org.jboss.wsf.spi.metadata.config.EndpointConfig;

/**
 * Defines mapping of endpoints and their config.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSEndpointConfigMapping {

    private final Map<String, EndpointConfig> endpointConfigMap = new HashMap<String, EndpointConfig>();

    /**
     * Registers endpoint and its config.
     *
     * @param endpointClass WS endpoint
     * @param config Config with endpoint
     */
    public void registerEndpointConfig(final String endpointClass, final EndpointConfig config) {
        if ((endpointClass == null) || (config == null)) {
            throw new IllegalArgumentException();
        }
        endpointConfigMap.put(endpointClass, config);
    }

    /**
     * Returns config associated with WS endpoint.
     *
     * @param endpointClass to get associated config
     * @return associated config
     */
    public EndpointConfig getConfig(final String endpointClass) {
        return endpointConfigMap.get(endpointClass);
    }

    public boolean isEmpty() {
        return endpointConfigMap.size() == 0;
    }

}
