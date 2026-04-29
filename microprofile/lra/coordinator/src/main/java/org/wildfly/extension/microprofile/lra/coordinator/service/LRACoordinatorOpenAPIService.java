/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.narayana.lra.coordinator.api.Coordinator;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.lra.coordinator._private.MicroProfileLRACoordinatorLogger;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;

/**
 * Service that builds an OpenAPI model from the Narayana LRA Coordinator class annotations
 * and registers it with the OpenAPI model registry.
 */
public final class LRACoordinatorOpenAPIService implements Service {

    private final Supplier<OpenAPIModelRegistry> registrySupplier;
    private final String contextPath;

    private volatile OpenAPIModelRegistry.Registration registration;

    public LRACoordinatorOpenAPIService(Supplier<OpenAPIModelRegistry> registrySupplier, String contextPath) {
        this.registrySupplier = registrySupplier;
        this.contextPath = contextPath;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        try {
            OpenAPI model = buildOpenAPIModel();
            String key = contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
            this.registration = registrySupplier.get().register(key, model);
            MicroProfileLRACoordinatorLogger.LOGGER.debug("Registered LRA Coordinator OpenAPI model");
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    private OpenAPI buildOpenAPIModel() throws IOException {
        ClassLoader classLoader = Coordinator.class.getClassLoader();

        // Build a Jandex index for the Coordinator class
        Indexer indexer = new Indexer();
        try (InputStream stream = classLoader.getResourceAsStream(
                Coordinator.class.getName().replace('.', '/') + ".class")) {
            if (stream != null) {
                indexer.index(stream);
            }
        }
        Index index = indexer.complete();

        SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                .withApplicationClassLoader(classLoader)
                .withScannerClassLoader(AnnotationScanner.class.getClassLoader())
                .withIndex(index);

        // Normalize context path (should not end in "/")
        String normalizedContextPath = contextPath.endsWith("/")
                ? contextPath.substring(0, contextPath.length() - 1)
                : contextPath;

        // Prepend the deployment context path to scanned paths so they match the actual runtime URLs.
        // The Coordinator class is annotated with @Path("lra-coordinator"), so the scanner produces
        // paths like /lra-coordinator/start. Since the coordinator is deployed at context path
        // /lra-coordinator, the actual URL is /lra-coordinator/lra-coordinator/start.
        if (normalizedContextPath.length() > 1) {// ignore if it is /
            builder.addFilter(new OASFilter() {
                @Override
                public void filterOpenAPI(OpenAPI model) {
                    Paths paths = model.getPaths();
                    if (paths != null) {
                        Map<String, PathItem> items = Map.copyOf(
                                Optional.ofNullable(paths.getPathItems()).orElse(Map.of()));
                        for (Map.Entry<String, PathItem> entry : items.entrySet()) {
                            paths.removePathItem(entry.getKey());
                            paths.addPathItem(normalizedContextPath + entry.getKey(), entry.getValue());
                        }
                    }
                }
            });
        }

        return builder.build().model();
    }
}
