/*
 * JBoss, Home of Professional Open Source. Copyright 2019, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi._private.MicroProfileOpenAPILogger.LOGGER;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;

/**
 * This listener instantiates OASModelReader and OASFilter and triggers OpenAPI
 * document initialization during the startup of the application, in the context
 * of the application.
 *
 * @author Martin Kouba
 * @author Michael Edgar (adapted from Thorntail to Wildfly)
 */
@WebListener
public class OpenApiServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        OpenApiConfig config = new OpenApiConfigImpl(ConfigProvider.getConfig(getContextClassLoader()));

        // Instantiate OASModelReader and OASFilter
        OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;
        OpenAPI model = modelFromReader(config);

        if (model != null && LOGGER.isDebugEnabled()) {
            LOGGER.modelReaderSuccessful(model.getClass().getName());
        }

        openApiDocument.modelFromReader(model);

        OASFilter filter = getFilter(config);

        if (filter != null && LOGGER.isDebugEnabled()) {
            LOGGER.filterImplementationLoaded(filter.getClass().getName());
        }

        openApiDocument.filter(filter);

        // Now we're ready to initialize the final model
        openApiDocument.initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No action when application is stopped.
    }

    /**
     * Instantiate the configured {@link OASModelReader} and invoke it. If no
     * reader is configured, then return null. If a class is configured but
     * there is an error either instantiating or invoking it, a
     * {@link RuntimeException} is thrown.
     */
    private OpenAPIImpl modelFromReader(OpenApiConfig config) {
        ClassLoader cl = getContextClassLoader();

        if (cl == null) {
            cl = OpenApiServletContextListener.class.getClassLoader();
        }

        return OpenApiProcessor.modelFromReader(config, cl);
    }

    /**
     * Instantiate the {@link OASFilter} configured by the application. May be
     * null.
     */
    private OASFilter getFilter(OpenApiConfig config) {
        ClassLoader cl = getContextClassLoader();

        if (cl == null) {
            cl = OpenApiServletContextListener.class.getClassLoader();
        }

        return OpenApiProcessor.getFilter(config, cl);
    }

    /**
     * Gets the current context class loader.
     */
    private static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread()
                                                                                         .getContextClassLoader());
    }
}
