/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import org.jboss.as.controller.PathAddress;

/**
 * utility class that tracks components registered with the management API
 *
 * @author Stuart Douglas
 */
public class InstalledComponent {
    private final EJBComponentType type;
    private final PathAddress address;

    public InstalledComponent(final EJBComponentType type, final PathAddress address) {
        this.type = type;
        this.address = address;
    }

    public EJBComponentType getType() {
        return type;
    }

    public PathAddress getAddress() {
        return address;
    }
}
