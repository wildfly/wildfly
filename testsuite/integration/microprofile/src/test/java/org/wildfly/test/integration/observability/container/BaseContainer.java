/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.container;

import java.time.Duration;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

public abstract class BaseContainer<SELF extends GenericContainer<SELF>> extends GenericContainer<SELF> {
    private final String containerName;
    private final List<Integer> exposedPorts;

    private static final int STARTUP_ATTEMPTS = Integer.parseInt(
            System.getProperty("testsuite.integration.container.startup.attempts", "1"));
    private static final Duration ATTEMPT_DURATION = Duration.parse(
            System.getProperty("testsuite.integration.container.attempt.duration", "PT5S"));
    protected final Boolean loggingEnabled; // Default: null/false

    public BaseContainer(String containerName,
                         String imageName,
                         String imageVersion,
                         List<Integer> exposedPorts,
                         List<WaitStrategy> waitStrategies) {
        super(DockerImageName.parse(imageName + ":" + imageVersion));

        this.containerName = containerName;
        this.exposedPorts = exposedPorts;

        setWaitStrategy(buildWaitStrategy(waitStrategies));
        setExposedPorts(exposedPorts);
        setStartupAttempts(STARTUP_ATTEMPTS);

        loggingEnabled =
                Boolean.parseBoolean(System.getProperty("testsuite.integration.container.logging")) ||
                Boolean.parseBoolean(System.getProperty("testsuite.integration.container." + containerName.toLowerCase() + ".logging"));

        if (loggingEnabled) {
            setLogConsumers(List.of(outputFrame -> {
                byte[] bytes = outputFrame.getBytes();
                if (bytes != null) {
                    debugLog(new String(bytes));
                }
            }));
        }
    }

    protected void debugLog(String message) {
        debugLog(containerName.toUpperCase(), message);
    }

    protected void debugLog(String prefix, String message) {
        if (loggingEnabled) {
            System.err.println("[" + prefix + "] " + message);
        }
    }

    private WaitStrategy buildWaitStrategy(List<WaitStrategy> waitStrategies) {
        WaitAllStrategy strategy = new WaitAllStrategy()
                .withStartupTimeout(ATTEMPT_DURATION);
        waitStrategies.forEach(strategy::withStrategy);

        return strategy;
    }
}
