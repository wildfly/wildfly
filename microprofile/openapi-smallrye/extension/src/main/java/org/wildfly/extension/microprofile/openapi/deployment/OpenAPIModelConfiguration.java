/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

import io.smallrye.openapi.api.SmallRyeOpenAPI;

import org.eclipse.microprofile.config.Config;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;

/**
 * Encapsulates the configuration of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIModelConfiguration extends OpenAPIEndpointConfiguration {
    TernaryServiceDescriptor<SmallRyeOpenAPI> SERVICE_DESCRIPTOR = TernaryServiceDescriptor.of("org.wildfly.open-api.model", SmallRyeOpenAPI.class);

    /**
     * Indicates whether or not an OpenAPI endpoint is enabled for this deployment.
     * @return true, if an OpenAPI endpoint is enabled for this deployment, false otherwise.
     */
    boolean isEnabled();

    /**
     * Returns the MicroProfile configuration for this deployment.
     * @return the MicroProfile configuration for this deployment.
     */
    Config getMicroProfileConfig();

    /**
     * Returns an optional static file, only present if the deployment defines one.
     * @return an optional static file, only present if the deployment defines one.
     */
    Optional<URL> getStaticFile();

    /**
     * Returns a function that resolves a relative deployment path to a URL.
     * @return a function that resolves a relative deployment path to a URL.
     */
    Function<String, URL> getResourceResolver();

    /**
     * Indicates whether or not the OpenAPI document should use relative URLs.
     * @return true, if the OpenAPI document for this deployment should use relative URLs, false otherwise.
     */
    boolean useRelativeServerURLs();
}
