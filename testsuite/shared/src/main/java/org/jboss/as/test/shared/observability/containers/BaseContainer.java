/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.containers;

import java.util.List;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@TestcontainersRequired
public abstract class BaseContainer<SELF extends GenericContainer<SELF>> extends GenericContainer<SELF> {
    private final String containerName;

    private static final int STARTUP_ATTEMPTS = Integer.parseInt(
            System.getProperty("testsuite.integration.container.startup.attempts", "5"));
    protected Boolean loggingEnabled; // Default: null/false

    public BaseContainer(String containerName,
                         String imageName,
                         String imageVersion,
                         List<Integer> exposedPorts) {
        super(DockerImageName.parse(imageName + ":" + imageVersion));

        this.containerName = containerName;

        setExposedPorts(exposedPorts);
        setStartupAttempts(STARTUP_ATTEMPTS);
        setNetwork(Network.SHARED);

        checkForLogging(containerName);

        if (loggingEnabled) {
            setLogConsumers(List.of(outputFrame -> {
                byte[] bytes = outputFrame.getBytes();
                if (bytes != null) {
                    debugLog(new String(bytes));
                }
            }));
        }
    }

    private void checkForLogging(String containerName) {
        loggingEnabled =
                Boolean.parseBoolean(System.getenv().get("TC_LOGGING")) ||
                Boolean.parseBoolean(System.getProperty("testsuite.integration.container.logging")) ||
                Boolean.parseBoolean(System.getProperty("testsuite.integration.container." + containerName.toLowerCase() + ".logging"));
    }

    protected void debugLog(String message) {
        debugLog(containerName, message);
    }

    protected void debugLog(String prefix, String message) {
        if (loggingEnabled) {
            System.err.println("[" + prefix + "] " + message);
        }
    }
}
