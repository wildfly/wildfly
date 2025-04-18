/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

/**
 * Encapsulates the configuration of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIModelConfiguration extends OpenAPIEndpointConfiguration {

    /**
     * Returns the name of this model, or null, if this the default model.
     * @return a model name
     */
    default String getModelName() {
        return null;
    }
}
