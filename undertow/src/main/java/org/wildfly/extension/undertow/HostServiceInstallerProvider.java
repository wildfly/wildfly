/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Provides a service installer for a host.
 * @author Paul Ferraro
 * TODO Move this to an SPI module.
 */
public interface HostServiceInstallerProvider {
    /**
     * Returns a service installer for the host identified by the specified server name and host name.
     * @param serverName a server name
     * @param hostName a host name
     * @return a service installer
     */
    ResourceServiceInstaller getServiceInstaller(String serverName, String hostName);
}
