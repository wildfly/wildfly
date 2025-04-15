/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.controller.OperationContext;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Installs any runtime services necessary to support distributable web applications on a given server.
 * @author Paul Ferraro
 */
public interface DistributableServerServiceInstallerFactory {
    ResourceServiceInstaller getServiceInstaller(OperationContext context, String serverName);
}
