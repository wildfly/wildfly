/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.naming.java.permission;

import java.io.Serializable;

final class SerializedJndiPermission implements Serializable {
    private static final long serialVersionUID = - 7602123815143424767L;

    private final String name;
    private final String actions;

    SerializedJndiPermission(final String name, final String actions) {
        this.name = name;
        this.actions = actions;
    }

    Object readResolve() {
        return new JndiPermission(name, actions);
    }
}
