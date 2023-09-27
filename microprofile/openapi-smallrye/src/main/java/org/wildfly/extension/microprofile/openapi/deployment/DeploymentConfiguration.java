/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.Map;

import org.jboss.vfs.VirtualFile;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.io.Format;

/**
 * The configuration of an OpenAPI deployment.
 * @author Paul Ferraro
 */
public interface DeploymentConfiguration {

    /**
     * Returns the config property with the specified name, or the provided default value, if unspecified.
     * @return the config property with the specified name, or the provided default value, if unspecified.
     */
    <T> T getProperty(String name, T defaultValue);

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
     * Returns the name of the Undertow server to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getServerName();

    /**
     * Returns the name of the Undertow host to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getHostName();
}
