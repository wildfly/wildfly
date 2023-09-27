/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.deployment;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;

/**
 * Builder facade to workaround singleton nature of {@link OpenApiDocument}.
 * @author Michael Edgar
 */
public class OpenAPIDocumentBuilder {

    private OpenApiConfig config;
    private OpenAPI annotationsModel;
    private OpenAPI readerModel;
    private OpenAPI staticFileModel;
    private OASFilter filter;
    private String archiveName;

    public OpenAPIDocumentBuilder config(OpenApiConfig config) {
        this.config = config;
        return this;
    }

    public OpenAPIDocumentBuilder archiveName(String archiveName) {
        this.archiveName = archiveName;
        return this;
    }

    public OpenAPIDocumentBuilder staticFileModel(OpenAPI staticFileModel) {
        this.staticFileModel = staticFileModel;
        return this;
    }

    public OpenAPIDocumentBuilder annotationsModel(OpenAPI annotationsModel) {
        this.annotationsModel = annotationsModel;
        return this;
    }

    public OpenAPIDocumentBuilder readerModel(OpenAPI readerModel) {
        this.readerModel = readerModel;
        return this;
    }

    public OpenAPIDocumentBuilder filter(OASFilter filter) {
        this.filter = filter;
        return this;
    }

    public OpenAPI build() {
        OpenAPI result = null;
        OpenApiDocument instance = OpenApiDocument.INSTANCE;

        synchronized (instance) {
            instance.reset();
            instance.config(this.config);
            instance.modelFromReader(this.readerModel);
            instance.modelFromStaticFile(this.staticFileModel);
            instance.modelFromAnnotations(this.annotationsModel);
            instance.filter(this.filter);
            instance.archiveName(this.archiveName);
            instance.initialize();

            result = instance.get();

            // Release statically referenced intermediate objects
            instance.reset();
        }

        return result;
    }
}
