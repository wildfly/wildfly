/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.deployers;

import static org.wildfly.common.Assert.checkNotNullParam;

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
        checkNotNullParam("endpointClass", endpointClass);
        checkNotNullParam("config", config);

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
