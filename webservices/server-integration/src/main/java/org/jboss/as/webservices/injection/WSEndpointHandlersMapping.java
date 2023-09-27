/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.injection;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines mapping of Jaxws endpoints and their handlers.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEndpointHandlersMapping {

    private final Map<String, Set<String>> endpointHandlersMap = new HashMap<String, Set<String>>();

    /**
     * Registers endpoint and its associated WS handlers.
     *
     * @param endpointClass WS endpoint
     * @param endpointHandlers WS handlers associated with endpoint
     */
    public void registerEndpointHandlers(final String endpointClass, final Set<String> endpointHandlers) {
        checkNotNullParam("endpointClass", endpointClass);
        checkNotNullParam("endpointHandlers", endpointHandlers);
        endpointHandlersMap.put(endpointClass, Collections.unmodifiableSet(endpointHandlers));
    }

    /**
     * Returns handlers class names associated with WS endpoint.
     *
     * @param endpointClass to get associated handlers for
     * @return associated handlers class names
     */
    public Set<String> getHandlers(final String endpointClass) {
        return endpointHandlersMap.get(endpointClass);
    }

    public boolean isEmpty() {
        return endpointHandlersMap.size() == 0;
    }

}
