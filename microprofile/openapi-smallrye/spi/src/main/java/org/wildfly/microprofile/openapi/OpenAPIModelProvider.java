/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;

/**
 * Provider of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIModelProvider {
    NullaryServiceDescriptor<Void> SUBSYSTEM_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.microprofile.openapi", Void.class);

    BinaryServiceDescriptor<OpenAPIModelProvider> DEFAULT_SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.open-api.default-model-provider", OpenAPIModelProvider.class);
    TernaryServiceDescriptor<OpenAPIModelProvider> SERVICE_DESCRIPTOR = TernaryServiceDescriptor.of("org.wildfly.open-api.model-provider", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Returns an OpenAPI model.
     * @return an OpenAPI model.
     */
    OpenAPI getModel();

    static OpenAPIModelProvider of(OpenAPI model) {
        return new OpenAPIModelProvider() {
            @Override
            public OpenAPI getModel() {
                return model;
            }
        };
    }
}
