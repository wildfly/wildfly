/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;

/**
 * Configuration of an OpenAPI model for a deployment.
 * @author Paul Ferraro
 */
public interface DeploymentOpenAPIModelConfiguration extends OpenAPIModelConfiguration {
    String RELATIVE_SERVER_URLS = "mp.openapi.extensions.servers.relative";

    /**
     * Returns an optional static file, only present if the deployment defines one.
     * @return an optional static file, only present if the deployment defines one.
     */
    default Optional<URL> getStaticFile() {
        return Optional.empty();
    }

    /**
     *    Returns a function that resolves a relative deployment path to a URL.
     * @return a function that resolves a relative deployment path to a URL.
     */
    default Function<String, URL> getResourceResolver() {
        return new Function<>() {
            @Override
            public URL apply(String path) {
                try {
                    return new URL(path);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(path);
                }
            }
        };
    }

    /**
     * Indicates whether or not the OpenAPI document should use relative URLs.
     * @return true, if the OpenAPI document for this deployment should use relative URLs, false otherwise.
     */
    boolean useRelativeServerURLs();
}
