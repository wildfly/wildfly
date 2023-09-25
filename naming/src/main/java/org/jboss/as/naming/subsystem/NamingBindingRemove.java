/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removing a JNDI binding
 */
public class NamingBindingRemove extends ServiceRemoveStepHandler {

    public static final NamingBindingRemove INSTANCE = new NamingBindingRemove();

    private NamingBindingRemove() {
        super(NamingBindingAdd.INSTANCE);
    }

    @Override
    protected ServiceName serviceName(final String name) {
        return ContextNames.bindInfoFor(name).getBinderServiceName();
    }

}
