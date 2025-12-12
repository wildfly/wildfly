/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASConfig;

/**
 * Encapsulates the configuration of an OpenAPI endpoint.
 * @author Paul Ferraro
 */
public interface OpenAPIEndpointConfiguration {
    String PATH = "path";
    String ENABLED = "enabled";
    String DEFAULT_PATH = "/openapi";
    Boolean DEFAULT_ENABLED = Boolean.TRUE;

    /**
     * Returns the name of the Undertow server to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getServerName();

    /**
     * Returns the name of the Undertow host to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getHostName();

    /**
     * Returns the URL path of the OpenAPI endpoint.
     * @return an endpoint path
     */
    default String getPath() {
        return this.getMicroProfileConfig().getOptionalValue(OASConfig.EXTENSIONS_PREFIX + PATH, String.class).orElse(DEFAULT_PATH);
    }

    /**
     * Indicates whether or not this OpenAPI endpoint is enabled.
     * @return true, if this OpenAPI endpoint is enabled, false otherwise.
     */
    default boolean isEnabled() {
        return this.getMicroProfileConfig().getOptionalValue(OASConfig.EXTENSIONS_PREFIX + ENABLED, Boolean.class).orElse(DEFAULT_ENABLED);
    }

    /**
     * Returns the MicroProfile configuration used to configure this endpoint.
     * @return the MicroProfile configuration used to configure this endpoint.
     */
    Config getMicroProfileConfig();
}
