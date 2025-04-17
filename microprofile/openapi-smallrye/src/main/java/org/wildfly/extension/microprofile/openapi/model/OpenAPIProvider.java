/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.model;

import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;

/**
 * Provider of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIProvider extends Supplier<OpenAPI> {
    BinaryServiceDescriptor<OpenAPIProvider> DEFAULT_SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.open-api.default-model-provider", OpenAPIProvider.class);
    TernaryServiceDescriptor<OpenAPIProvider> SERVICE_DESCRIPTOR = TernaryServiceDescriptor.of("org.wildfly.open-api.model-provider", DEFAULT_SERVICE_DESCRIPTOR);

    static OpenAPIProvider of(OpenAPI model) {
        return new OpenAPIProvider() {
            @Override
            public OpenAPI get() {
                return model;
            }
        };
    }
}
