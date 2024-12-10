/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class User implements Comparable<User> {

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

    private Long id;
    private String name;
    private Set<Role> roles;

    public long getId() {
        return id == null ? -1L : id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

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

    @Override
    public int compareTo(@NotNull final User o) {
        return Long.compare(getId(), o.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        return id == ((User) obj).id;
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", name=" + name + ", roles=" + roles + "]";
    }
}
