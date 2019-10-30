/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2016 Red Hat Inc.
 */
public final class DefaultDeploymentMappingProvider {

    private static DefaultDeploymentMappingProvider INSTANCE = new DefaultDeploymentMappingProvider();

    public static DefaultDeploymentMappingProvider instance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, Map.Entry<String, String>> mappings;

    private DefaultDeploymentMappingProvider() {
        this.mappings = new ConcurrentHashMap<>();
    }


    public Map.Entry<String, String> getMapping(String deploymentName) {
        return mappings.get(deploymentName);
    }

    public void removeMapping(String deploymentName) {
        mappings.remove(deploymentName);
    }

    public void addMapping(String deploymentName, String serverName, String hostName) {
        if (mappings.putIfAbsent(deploymentName, new AbstractMap.SimpleEntry<>(serverName, hostName)) != null) {
            throw UndertowLogger.ROOT_LOGGER.duplicateDefaultWebModuleMapping(deploymentName, serverName, hostName);
        }
    }
    public void clear(){
        mappings.clear();
    }

}
