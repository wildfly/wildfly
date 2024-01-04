/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.naming.java.permission;

import java.io.Serializable;

class SerializedJndiPermissionCollection implements Serializable {

    private static final long serialVersionUID = 315106751231586701L;

    private final boolean readOnly;
    private final JndiPermission[] permissions;

    SerializedJndiPermissionCollection(final boolean readOnly, final JndiPermission[] permissions) {
        this.readOnly = readOnly;
        this.permissions = permissions;
    }

    Object readResolve() {
        final JndiPermissionCollection collection = new JndiPermissionCollection(permissions);
        if (readOnly) collection.setReadOnly();
        return collection;
    }
}
