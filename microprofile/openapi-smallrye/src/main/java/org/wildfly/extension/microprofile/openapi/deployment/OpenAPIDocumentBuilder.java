package org.wildfly.extension.microprofile.openapi.deployment;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;

public class OpenAPIDocumentBuilder {

    private OpenApiConfig config;
    private OpenAPI annotationsModel;
    private OpenAPI readerModel;
    private OpenAPI staticFileModel;
    private OASFilter filter;
    private String archiveName;

    private OpenAPIDocumentBuilder() {
    }

    public static OpenAPIDocumentBuilder create() {
        return new OpenAPIDocumentBuilder();
    }

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
        OpenApiDocument instance = OpenApiDocument.INSTANCE;
        OpenAPI document = null;

        synchronized (instance) {
            instance.reset();
            instance.config(this.config);
            instance.modelFromReader(this.readerModel);
            instance.modelFromStaticFile(this.staticFileModel);
            instance.modelFromAnnotations(this.annotationsModel);
            instance.filter(this.filter);
            instance.archiveName(this.archiveName);

            instance.initialize();

            document = instance.get();
        }

        return document;
    }
}
