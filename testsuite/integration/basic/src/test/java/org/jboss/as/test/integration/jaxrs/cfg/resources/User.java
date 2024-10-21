/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.util.Locale;
import java.util.Set;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class User {
    public enum Role {
        ADMIN,
        MANAGER,
        USER,
        ;


        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private String name;
    private Set<Role> roles;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(final Set<Role> roles) {
        this.roles = Set.copyOf(roles);
    }
}
