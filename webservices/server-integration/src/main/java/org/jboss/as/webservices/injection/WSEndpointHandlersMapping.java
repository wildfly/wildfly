/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.injection;

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
        if ((endpointClass == null) || (endpointHandlers == null)) {
            throw new IllegalArgumentException();
        }
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
