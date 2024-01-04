/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.controller.OperationContext;

/**
 * Installs any runtime services necessary to support distributable web applications on a given server.
 * @author Paul Ferraro
 */
public interface DistributableServerRuntimeHandler {
    void execute(OperationContext context, String serverName);
}
