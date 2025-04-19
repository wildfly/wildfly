/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

/**
 * Encapsulates the configuration of an OpenAPI endpoint.
 * @author Paul Ferraro
 */
public interface OpenAPIEndpointConfiguration {

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
    String getPath();
}
