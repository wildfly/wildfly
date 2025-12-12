/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * A registry of OpenAPI models.
 * @author Paul Ferraro
 */
public interface OpenAPIModelRegistry {
    BinaryServiceDescriptor<OpenAPIModelRegistry> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.open-api.model-registry", OpenAPIModelRegistry.class);

    interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    Registration register(String key, OpenAPI model);

    Optional<OpenAPI> getDefaultModel();

    Map<String, Optional<OpenAPI>> getModels();
}
