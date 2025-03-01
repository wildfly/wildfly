/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.container;

import java.time.Duration;
import java.util.OptionalInt;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;

/**
 * Defines the container configuration for a session manager factory of a deployment.
 * @author Paul Ferraro
 */
public interface SessionManagerFactoryConfiguration extends WebDeploymentConfiguration {

    /**
     * Returns the maximum number of sessions that should be active at any given time.
     * @return a positive integer; or null if there should be no limit to the number of active sessions.
     */
    OptionalInt getMaxActiveSessions();

    /**
     * Returns the default session timeout.
     * @return the duration after which sessions will timeout.
     */
    Duration getDefaultSessionTimeout();

    /**
     * The deployment unit with which this session manager factory is to be associated.
     * @return a deployment unit
     */
    DeploymentUnit getDeploymentUnit();

    @Override
    default Module getModule() {
        return this.getDeploymentUnit().getAttachment(Attachments.MODULE);
    }
}
