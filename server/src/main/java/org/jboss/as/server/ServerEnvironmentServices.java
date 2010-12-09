/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import java.io.File;

import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.msc.service.BatchBuilder;

/**
 * Server environment service factory.
 *
 * @author Emanuel Muckenhuber
 */
class ServerEnvironmentServices {

    private static final String USER_DIR = "user.dir";
    private static final String USER_HOME = "user.home";
    private static final String JAVA_HOME = "java.home";

    static void addServices(final ServerEnvironment environment, final BatchBuilder batch) {
        // Add server environment
        addEnvironmentService(environment, batch);
        // Add environment paths
        addPathService(ServerEnvironment.HOME_DIR, environment.getHomeDir(), batch);
        addPathService(ServerEnvironment.SERVER_BASE_DIR, environment.getServerBaseDir(), batch);
        addPathService(ServerEnvironment.SERVER_CONFIG_DIR, environment.getServerConfigurationDir(), batch);
        addPathService(ServerEnvironment.SERVER_DATA_DIR, environment.getServerDataDir(), batch);
        addPathService(ServerEnvironment.SERVER_LOG_DIR, environment.getServerLogDir(), batch);
        addPathService(ServerEnvironment.SERVER_TEMP_DIR, environment.getServerTempDir(), batch);
        // Add system paths
        addPathService(USER_DIR, getProperty(USER_DIR), batch);
        addPathService(USER_HOME, getProperty(USER_HOME), batch);
        addPathService(JAVA_HOME, getProperty(JAVA_HOME), batch);
    }

    static void addEnvironmentService(final ServerEnvironment environment, final BatchBuilder batch) {
        ServerEnvironmentService.addService(environment, batch);
    }

    static void addPathService(final String name, final File file, final BatchBuilder batch) {
        addPathService(name, file.getAbsolutePath(), batch);
    }

    static void addPathService(final String name, final String path, final BatchBuilder batch) {
        AbsolutePathService.addService(name, path, batch);
    }

    static String getProperty(final String propertyName) {
        return System.getProperty(propertyName);
    }

}
