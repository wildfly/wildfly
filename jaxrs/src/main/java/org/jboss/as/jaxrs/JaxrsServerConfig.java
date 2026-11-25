/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public class JaxrsServerConfig {

    private final Map<String, String> contextParameters;

    public JaxrsServerConfig() {
        contextParameters = new LinkedHashMap<>();
    }

    /**
     * Adds the value to the context parameters.
     *
     * @param name  the name of the context parameter
     * @param value the value for the context parameter
     */
    protected void putContextParameter(final String name, final String value) {
        contextParameters.put(name, value);
    }

    /**
     * Returns a copy of the context parameters.
     *
     * @return the context parameters
     */
    public Map<String, String> getContextParameters() {
        return Map.copyOf(contextParameters);
    }
}
