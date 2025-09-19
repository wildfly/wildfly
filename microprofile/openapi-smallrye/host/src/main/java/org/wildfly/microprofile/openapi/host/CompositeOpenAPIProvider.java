/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.wildfly.microprofile.openapi.OpenAPIProvider;
import org.wildfly.microprofile.openapi.OpenAPIRegistry;

/**
 * Provides an OpenAPI model composed of registered models.
 * @author Paul Ferraro
 */
public class CompositeOpenAPIProvider implements OpenAPIProvider, OpenAPIRegistry {
    private final OpenAPI defaultModel;
    private final Map<String, OpenAPI> models = new ConcurrentHashMap<>();

    CompositeOpenAPIProvider(OpenAPI defaultModel) {
        this.defaultModel = defaultModel;
    }

    @Override
    public Map<String, OpenAPI> getModels() {
        return Collections.unmodifiableMap(CompositeOpenAPIProvider.this.models);
    }

    @Override
    public Registration register(String key, OpenAPI deploymentModel) {
        this.models.put(key, deploymentModel);
        return () -> this.models.remove(key);
    }

    @Override
    public OpenAPI get() {
        List<OpenAPI> models = List.copyOf(this.models.values());
        // Single deployment use case
        if (models.size() == 1) return models.get(0);

        // If there are OpenAPI models for multiple deployments, unite into single model
        OpenAPI result = OASFactory.createOpenAPI();
        Components resultComponents = OASFactory.createComponents();
        Paths resultPaths = OASFactory.createPaths();
        result.setComponents(resultComponents);
        result.setExternalDocs(this.distinct(models, OpenAPI::getExternalDocs));
        result.setInfo(this.distinct(models, OpenAPI::getInfo));
        result.setOpenapi(this.distinct(models, OpenAPI::getOpenapi));
        result.setPaths(resultPaths);
        // Combine properties from registered models
        for (OpenAPI model : models) {
            Components components = model.getComponents();
            if (components != null) {
                addAll(components.getCallbacks(), resultComponents::addCallback);
                addAll(components.getExamples(), resultComponents::addExample);
                addAll(components.getExtensions(), resultComponents::addExtension);
                addAll(components.getHeaders(), resultComponents::addHeader);
                addAll(components.getLinks(), resultComponents::addLink);
                addAll(components.getParameters(), resultComponents::addParameter);
                addAll(components.getPathItems(), resultComponents::addPathItem);
                addAll(components.getRequestBodies(), resultComponents::addRequestBody);
                addAll(components.getResponses(), resultComponents::addResponse);
                addAll(components.getSchemas(), resultComponents::addSchema);
                addAll(components.getSecuritySchemes(), resultComponents::addSecurityScheme);
            }
            addAll(model.getExtensions(), result::addExtension);
            Paths paths = model.getPaths();
            if (paths != null) {
                addAll(paths.getExtensions(), resultPaths::addExtension);
                addAll(paths.getPathItems(), resultPaths::addPathItem);
            }
            addAll(model.getSecurity(), result::addSecurityRequirement);
            addAll(model.getServers(), result::addServer);
            addAll(model.getTags(), result::addTag);
            addAll(model.getWebhooks(), result::addWebhook);
        }
        return result;
    }

    private <T> T distinct(Collection<OpenAPI> models, Function<OpenAPI, T> function) {
        // If registered models contain conflicting values use value from default model
        List<T> list = models.stream().map(function).filter(Objects::nonNull).distinct().toList();
        return (list.size() == 1) ? list.get(0) : function.apply(this.defaultModel);
    }

    private static <T> void addAll(List<T> list, Consumer<T> consumer) {
        if (list != null) {
            list.forEach(consumer);
        }
    }

    private static <T> void addAll(Map<String, T> map, BiConsumer<String, T> consumer) {
        if (map != null) {
            map.forEach(consumer);
        }
    }
}
