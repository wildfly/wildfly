/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

import io.smallrye.openapi.api.OpenApiConfig;

import org.eclipse.microprofile.config.Config;

/**
 * Encapsulates the configuration of an OpenAPI endpoint.
 * @author Paul Ferraro
 */
public interface OpenAPIEndpointConfiguration {
    String PATH = "mp.openapi.extensions.path";
    String DEFAULT_PATH = "/openapi";
    String ENABLED = "mp.openapi.extensions.enabled";

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
        return DEFAULT_PATH;
    }

    /**
     * Indicates whether or not this OpenAPI endpoint is enabled.
     * @return true, if this OpenAPI endpoint is enabled, false otherwise.
     */
    boolean isEnabled();

    default OpenApiConfig getConfiguration() {
        return OpenApiConfig.fromConfig(this.getMicroProfileConfig());
    }

    /**
     * Returns the MicroProfile configuration used to configure this endpoint.
     * @return the MicroProfile configuration used to configure this endpoint.
     */
    Config getMicroProfileConfig();
}
