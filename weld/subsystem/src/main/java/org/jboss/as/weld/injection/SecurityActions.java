/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.injection;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

import static java.security.AccessController.doPrivileged;

final class SecurityActions {

    private SecurityActions() {
        // forbidden inheritance
    }

    static void setAccessible(final AccessibleObject object) {
        if (! WildFlySecurityManager.isChecking()) {
            object.setAccessible(true);
        } else {
            doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    object.setAccessible(true);
                    return null;
                }
            });
        }
    }
}
