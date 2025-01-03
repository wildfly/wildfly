/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

/**
 * Enumeration of valid login module flags.
 *
 * @author Jason T. Greene
 */
enum ModuleFlag {
    REQUIRED("required"),
    REQUISITE("requisite"),
    SUFFICIENT("sufficient"),
    OPTIONAL("optional");

    private final String name;

    ModuleFlag(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
