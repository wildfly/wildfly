/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.undertow.service;

import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Provides a service installer for a host.
 * @author Paul Ferraro
 */
public interface HostServiceInstallerProvider {
    /**
     * Returns a service installer for the specified host within the specified server
     * @param serverName a server name
     * @param hostName a host name
     * @return a service installer
     */
    ResourceServiceInstaller getServiceInstaller(String serverName, String hostName);
}
