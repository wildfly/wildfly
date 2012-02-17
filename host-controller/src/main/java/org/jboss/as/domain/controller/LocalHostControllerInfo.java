/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import org.jboss.as.controller.ControlledProcessState;

/**
 * Core information about the local host controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface LocalHostControllerInfo {

    /**
     * Gets the host controller's name.
     *
     * @return the name
     *
     * @throws IllegalStateException if {@link #getProcessState()} is {@link org.jboss.as.controller.ControlledProcessState.State#STARTING}
     */
    String getLocalHostName();

    /**
     * Gets whether the local host controllers is acting as the master domain controller.
     *
     * @return {@code true} if the local host controller is the master
     *
     * @throws IllegalStateException if {@link #getProcessState()} is {@link org.jboss.as.controller.ControlledProcessState.State#STARTING}
     */
     boolean isMasterDomainController();

    /**
     * Gets the name of the interface on which the host listens for native management requests.
     *
     * @return the logical interface name
     */
    String getNativeManagementInterface();

    /**
     * Gets the name of the port on which the host listens for native management requests.
     * @return  the port number
     */
    int getNativeManagementPort();

    /**
     * Gets the name of the security realm to use for for native management requests.
     *
     * @return the logical interface name
     */
    String getNativeManagementSecurityRealm();

    /**
     * Gets the name of the interface on which the host listens for HTTP management requests.
     *
     * @return the logical interface name
     */
    String getHttpManagementInterface();

    /**
     * Gets the name of the port on which the host listens for HTTP management requests.
     * @return  the port number
     */
    int getHttpManagementPort();

    /**
     * Gets the name of the port on which the host listens for HTTPS management requests.
     * @return  the port number
     */
    int getHttpManagementSecurePort();

    /**
     * Gets the name of the security realm to use for for HTTP management requests.
     *
     * @return the logical interface name
     */
    String getHttpManagementSecurityRealm();

    /**
     *  Gets the host name of the remote domain controller
     *  @return the host name
     */
    String getRemoteDomainControllerHost();

    /**
     * Gets the port of the remote domain controller
     * @return the port number
     */
    int getRemoteDomainControllerPort();

    /**
     * Gets the current state of the host controller process.
     * @return the state
     */
    ControlledProcessState.State getProcessState();
}
