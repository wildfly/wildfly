/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Installs any runtime services necessary to support distributable web applications on a given server.
 * TODO Relocate this to an SPI module
 * @author Paul Ferraro
 */
public interface ServerServiceInstallerProvider {
    ResourceServiceInstaller getServiceInstaller(String serverName);
}
