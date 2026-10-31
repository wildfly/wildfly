/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;

/**
 * Workaround for docker-java reusing pooled Docker API connections that the daemon has already closed.
 * Podman closes idle API connections, so when tests run in a reused fork, the first non-idempotent request after an
 * idle period (e.g. {@code POST /containers/create}) fails with {@code NoHttpResponseException: localhost:2375 failed
 * to respond}. Each such failure discards one dead connection, so retrying the start eventually obtains a live one.
 * Container startup attempts are not sufficient on their own, since the number of dead connections in the pool can exceed them.
 * <p>
 * TODO Remove once https://github.com/docker-java/docker-java/issues/2682 is fixed and upgraded in Testcontainers and then here.
 *
 * @author Radoslav Husar
 */
public final class StaleDockerConnectionWorkaround {

    private static final Logger log = Logger.getLogger(StaleDockerConnectionWorkaround.class);
    private static final int MAX_ATTEMPTS = 10;

    /**
     * Starts the given container, retrying if the start failed on a stale Docker API connection.
     * Not to be used with a container whose {@link GenericContainer#start()} itself delegates to this workaround.
     */
    public static void start(GenericContainer<?> container) {
        start(container.getDockerImageName(), container::start);
    }

    /**
     * Runs the given container start action, retrying if it failed on a stale Docker API connection.
     */
    public static void start(String containerName, Runnable start) {
        for (int attempt = 1; ; attempt++) {
            try {
                start.run();
                return;
            } catch (RuntimeException e) {
                if (attempt >= MAX_ATTEMPTS || !isStaleConnection(e)) {
                    throw e;
                }
                log.warnf("Container %s failed to start on a stale Docker API connection, retrying (attempt %d of %d)", containerName, attempt + 1, MAX_ATTEMPTS);
            }
        }
    }

    private static boolean isStaleConnection(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            // The exception class is shaded by docker-java, so match it by simple name
            if (t.getClass().getSimpleName().equals("NoHttpResponseException")) {
                return true;
            }
        }
        return false;
    }

    private StaleDockerConnectionWorkaround() {
        // Utility class
    }
}
