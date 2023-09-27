/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
