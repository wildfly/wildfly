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
package org.wildfly.clustering.web.container;

import java.time.Duration;

import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Defines the container configuration for a session manager factory of a deployment.
 * @author Paul Ferraro
 */
public interface SessionManagerFactoryConfiguration extends WebDeploymentConfiguration {

    /**
     * Returns the maximum number of sessions that should be active at any given time.
     * @return a positive integer; or null if there should be no limit to the number of active sessions.
     */
    Integer getMaxActiveSessions();

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
}
