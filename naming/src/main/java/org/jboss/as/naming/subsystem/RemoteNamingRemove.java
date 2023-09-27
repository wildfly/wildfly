/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.naming.remote.RemoteNamingServerService;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removing a JNDI binding
 */
public class RemoteNamingRemove extends ServiceRemoveStepHandler {

    public static final RemoteNamingRemove INSTANCE = new RemoteNamingRemove();

    private RemoteNamingRemove() {
        super(RemoteNamingAdd.INSTANCE);
    }

    @Override
    protected ServiceName serviceName(final String name) {
        return RemoteNamingServerService.SERVICE_NAME;
    }
}
