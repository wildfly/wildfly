/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.Map;

import org.jboss.vfs.VirtualFile;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.io.Format;

/**
 * The configuration of an OpenAPI deployment.
 * @author Paul Ferraro
 */
public interface DeploymentConfiguration {

    /**
     * Returns the config property with the specified name, or the provided default value, if unspecified.
     * @return the config property with the specified name, or the provided default value, if unspecified.
     */
    <T> T getProperty(String name, T defaultValue);

    /**
     * Returns the OpenAPI configuration for this deployment.
     * @return the OpenAPI configuration for this deployment.
     */
    OpenApiConfig getOpenApiConfig();

    /**
     * Returns a tuple containing the static file and its format, or null, if the deployment does not define a static file.
     * @return a tuple containing the static file and its format, or null, if the deployment does not define a static file.
     */
    Map.Entry<VirtualFile, Format> getStaticFile();

    /**
     * Returns the name of the Undertow server to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getServerName();

    /**
     * Returns the name of the Undertow host to which this application is deployed.
     * @return the name of an Undertow server
     */
    String getHostName();
}
