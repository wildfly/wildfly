/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.SmallRyeOASConfig;

import org.eclipse.microprofile.openapi.OASConfig;

/**
 * Encapsulates the configuration of an OpenAPI model.
 * @author Paul Ferraro
 */
public interface OpenAPIModelConfiguration extends OpenAPIEndpointConfiguration {
    String DELIMITER = ".";
    String SMALLRYE_PREFIX = OASConfig.EXTENSIONS_PREFIX + "smallrye" + DELIMITER;
    String EXTERNAL_DOCUMENTATION = "externalDocs";
    String DESCRIPTION = "description";
    String URL = "url";
    String EXTERNAL_DOCUMENTATION_DESCRIPTION = String.join(DELIMITER, EXTERNAL_DOCUMENTATION, DESCRIPTION);
    String EXTERNAL_DOCUMENTATION_URL = String.join(DELIMITER, EXTERNAL_DOCUMENTATION, URL);
    String VERSION = SmallRyeOASConfig.VERSION.substring(SMALLRYE_PREFIX.length());
    String INFO_DESCRIPTION = SmallRyeOASConfig.INFO_DESCRIPTION.substring(SMALLRYE_PREFIX.length());
    String INFO_SUMMARY = SmallRyeOASConfig.INFO_SUMMARY.substring(SMALLRYE_PREFIX.length());
    String INFO_TERMS_OF_SERVICE = SmallRyeOASConfig.INFO_TERMS.substring(SMALLRYE_PREFIX.length());
    String INFO_TITLE = SmallRyeOASConfig.INFO_TITLE.substring(SMALLRYE_PREFIX.length());
    String INFO_VERSION = SmallRyeOASConfig.INFO_VERSION.substring(SMALLRYE_PREFIX.length());
    String INFO_CONTACT_EMAIL = SmallRyeOASConfig.INFO_CONTACT_EMAIL.substring(SMALLRYE_PREFIX.length());
    String INFO_CONTACT_NAME = SmallRyeOASConfig.INFO_CONTACT_NAME.substring(SMALLRYE_PREFIX.length());
    String INFO_CONTACT_URL = SmallRyeOASConfig.INFO_CONTACT_URL.substring(SMALLRYE_PREFIX.length());
    String INFO_LICENSE_IDENTIFIER = SmallRyeOASConfig.INFO_LICENSE_IDENTIFIER.substring(SMALLRYE_PREFIX.length());
    String INFO_LICENSE_NAME = SmallRyeOASConfig.INFO_LICENSE_NAME.substring(SMALLRYE_PREFIX.length());
    String INFO_LICENSE_URL = SmallRyeOASConfig.INFO_LICENSE_URL.substring(SMALLRYE_PREFIX.length());

    /**
     * Returns the name of this model, or null, if this the default model.
     * @return a model name
     */
    default String getModelName() {
        return null;
    }

    /**
     * Returns the configuration of this OpenAPI model.
     * @return the configuration of this OpenAPI model.
     */
    default OpenApiConfig getOpenApiConfig() {
        return OpenApiConfig.fromConfig(this.getMicroProfileConfig());
    }
}
