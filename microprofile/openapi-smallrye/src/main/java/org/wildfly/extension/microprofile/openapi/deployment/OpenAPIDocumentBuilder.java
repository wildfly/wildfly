/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
