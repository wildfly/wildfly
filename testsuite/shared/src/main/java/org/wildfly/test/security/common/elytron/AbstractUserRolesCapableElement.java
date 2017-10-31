/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.security.common.elytron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract parent for {@link ConfigurableElement} implementations which are able to configure (and provide) users and roles.
 * It extends {@link AbstractConfigurableElement} and holds user list to be created.
 *
 * @author Josef Cacek
 */
public abstract class AbstractUserRolesCapableElement extends AbstractConfigurableElement implements UsersRolesCapableElement {

    private final List<UserWithRoles> usersWithRoles;

    protected AbstractUserRolesCapableElement(Builder<?> builder) {
        super(builder);
        this.usersWithRoles = Collections.unmodifiableList(new ArrayList<>(builder.usersWithRoles));
    }

    @Override
    public List<UserWithRoles> getUsersWithRoles() {
        return usersWithRoles;
    }

    /**
     * Builder to build {@link AbstractUserRolesCapableElement}.
     */
    public abstract static class Builder<T extends Builder<T>> extends AbstractConfigurableElement.Builder<T> {
        private List<UserWithRoles> usersWithRoles = new ArrayList<>();

        protected Builder() {
        }

        /**
         * Adds the given user to list of users in the domain.
         *
         * @param userWithRoles not-null {@link UserWithRoles} instance
         */
        public final T withUser(UserWithRoles userWithRoles) {
            this.usersWithRoles.add(Objects.requireNonNull(userWithRoles, "Provided user must not be null."));
            return self();
        }

        /**
         * Shortcut method for {@link #withUser(UserWithRoles)} one.
         *
         * @param username must not be null
         * @param password must not be null
         * @param roles roles to be assigned to user (may be null)
         */
        public final T withUser(String username, String password, String... roles) {
            this.usersWithRoles.add(UserWithRoles.builder().withName(username).withPassword(password).withRoles(roles).build());
            return self();
        }
    }

}
