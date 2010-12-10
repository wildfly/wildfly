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
import org.jboss.msc.service.ServiceTarget;

/**
 * Server environment service factory.
 *
 * @author Emanuel Muckenhuber
 */
class ServerEnvironmentServices {

    private static final String USER_DIR = "user.dir";
    private static final String USER_HOME = "user.home";
    private static final String JAVA_HOME = "java.home";

    private ServerEnvironmentServices() {
    }

    static void addServices(final ServerEnvironment environment, final ServiceTarget target) {
        // Add server environment
        addEnvironmentService(environment, target);
        // Add environment paths
        addPathService(ServerEnvironment.HOME_DIR, environment.getHomeDir(), target);
        addPathService(ServerEnvironment.SERVER_BASE_DIR, environment.getServerBaseDir(), target);
        addPathService(ServerEnvironment.SERVER_CONFIG_DIR, environment.getServerConfigurationDir(), target);
        addPathService(ServerEnvironment.SERVER_DATA_DIR, environment.getServerDataDir(), target);
        addPathService(ServerEnvironment.SERVER_LOG_DIR, environment.getServerLogDir(), target);
        addPathService(ServerEnvironment.SERVER_TEMP_DIR, environment.getServerTempDir(), target);
        // Add system paths
        addPathService(USER_DIR, getProperty(USER_DIR), target);
        addPathService(USER_HOME, getProperty(USER_HOME), target);
        addPathService(JAVA_HOME, getProperty(JAVA_HOME), target);
    }

    static void addEnvironmentService(final ServerEnvironment environment, final ServiceTarget target) {
        ServerEnvironmentService.addService(environment, target);
    }

    static void addPathService(final String name, final File file, final ServiceTarget target) {
        addPathService(name, file.getAbsolutePath(), target);
    }

    static void addPathService(final String name, final String path, final ServiceTarget target) {
        AbsolutePathService.addService(name, path, target);
    }

    static String getProperty(final String propertyName) {
        return System.getProperty(propertyName);
    }

}
