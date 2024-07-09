/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.Map;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.io.Format;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.vfs.VirtualFile;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;

/**
 * Encapsulates the configuration of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIModelConfiguration extends OpenAPIEndpointConfiguration {
    TernaryServiceDescriptor<OpenAPI> SERVICE_DESCRIPTOR = TernaryServiceDescriptor.of("org.wildfly.open-api.model", OpenAPI.class);

    /**
     * Indicates whether or not an OpenAPI endpoint is enabled for this deployment.
     * @return true, if an OpenAPI endpoint is enabled for this deployment, false otherwise.
     */
    boolean isEnabled();

    /**
     * Returns the OpenAPI configuration for this deployment.
     * @return the OpenAPI configuration for this deployment.
     */
    OpenApiConfig getOpenApiConfig();

    /**
     * Returns a tuple containing the static file and its format, or null, if the deployment does not define a static file.
     * @return a tuple containing the static file and its format, or null, if the deployment does not define a static file.
     */
    Map.Entry<VirtualFile, Format> getStaticFile();

    /**
     * Indicates whether or not the OpenAPI document should use relative URLs.
     * @return true, if the OpenAPI document for this deployment should use relative URLs, false otherwise.
     */
    boolean useRelativeServerURLs();
}
