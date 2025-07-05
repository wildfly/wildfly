/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.container;

import java.time.Duration;
import java.util.OptionalInt;

/**
 * Defines the configuration of a session manager factory for a web deployment.
 * @author Paul Ferraro
 */
public interface SessionManagerFactoryConfiguration extends WebDeploymentConfiguration {

    /**
     * When present, returns the maximum number of sessions that should be active at any given time.
     * If empty, the container imposes no limit on the number of active sessions.
     * @return a positive integer, when present
     */
    OptionalInt getMaxActiveSessions();

    /**
     * Returns the default session timeout.
     * @return the duration after which sessions will timeout.
     */
    Duration getDefaultSessionTimeout();
}
