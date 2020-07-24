package org.wildfly.extension.vertx;

import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;

class SecurityActions {
    static String getSystemProperty(String name) {
        if (WildFlySecurityManager.isChecking()) {
            return doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(name);
                }
            });
        } else {
            return System.getProperty(name);
        }
    }
}
