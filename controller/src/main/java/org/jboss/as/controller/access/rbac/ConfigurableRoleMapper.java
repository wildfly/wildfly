/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access.rbac;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * A {@link RoleMapper} that supports configuration from the WildFly management API.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConfigurableRoleMapper implements RoleMapper {

    private volatile HashMap<String, Role> roles = new HashMap<String, Role>();
    private volatile boolean useRealmRoles;

    public enum PrincipalType {
        GROUP, USER
    }

    public enum MatchType {
        EXCLUDE, INCLUDE
    }

    // TODO - May want to consider COMPOSITE operations so all updates are applied simultaneously.

    // TODO - Split immediate into different methods, synchronization isn't needed either.

    /**
     * Adds a new role to the list of defined roles.
     *
     * @param roleName - The name of the role being added.
     * @param immediate - Should the change be applied immediately without cloning the internal collection.
     */
    public synchronized void addRole(final String roleName, final boolean immediate) {
        HashMap<String, Role> newRoles = immediate ? roles : new HashMap<String, Role>(roles);
        if (newRoles.containsKey(roleName) == false) {
            newRoles.put(roleName, new Role(roleName));
            if (immediate == false) {
                roles = newRoles;
            }
        }
    }

    /**
     * Remove a role from the list of defined roles.
     *
     * @param roleName - The name of the role to be removed.
     */
    public synchronized void removeRole(final String roleName) {
        /*
         * Would not expect this to happen during boot so don't offer the 'immediate' optimisation.
         */
        HashMap<String, Role> newRoles = new HashMap<String, Role>(roles);
        if (newRoles.containsKey(roleName)) {
            newRoles.remove(roleName);
            roles = newRoles;
        }
    }

    public void addPrincipal(final String roleName, final PrincipalType principalType, final MatchType matchType,
            final String name, final String realm, final boolean immediate) {
        Role role = roles.get(roleName);
        role.addPrincipal(createPrincipal(principalType, name, realm), matchType, immediate);
    }

    public void removePrincipal(final String roleName, final PrincipalType principalType, final MatchType matchType,
            final String name, final String realm) {
        Role role = roles.get(roleName);
        role.removePrincipal(createPrincipal(principalType, name, realm), matchType);
    }

    private Principal createPrincipal(final PrincipalType principalType, final String name, final String realm) {
        return new Principal(principalType, name, realm);
    }

    public void setUseRealmRoles(final boolean useRealmRoles) {
        this.useRealmRoles = useRealmRoles;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute) {
        return mapRoles(caller);
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource) {
        return mapRoles(caller);
    }

    private Set<String> mapRoles(final Caller caller) {
        Set<String> mappedRoles = new HashSet<String>();

        HashMap<String, Role> rolesToCheck;
        if (useRealmRoles) {
            rolesToCheck = new HashMap<String, Role>(roles);
            Set<String> realmRoles = caller.getAssociatedRoles();
            for (String current : realmRoles) {
                String roleName = current.toUpperCase();
                if (rolesToCheck.containsKey(roleName)) {
                    Role role = rolesToCheck.remove(roleName);
                    if (role.isExcluded(caller) == false) {
                        mappedRoles.add(roleName);
                    }
                } else {
                    mappedRoles.add(roleName);
                }
            }
        } else {
            // A clone is not needed here as the whole set of values is to be iterated with no need for removal.
            rolesToCheck = roles;
        }

        for (Role current : rolesToCheck.values()) {
            if (current.isIncluded(caller) && (current.isExcluded(caller) == false)) {
                mappedRoles.add(current.getName());
            }
        }

        // TODO - We could consider something along the lines of a WeakHashMap to hold this result keyed on the Caller.
        // The contents of the Caller are not expected to change during a call and we could clear the cache on a config change.
        return Collections.unmodifiableSet(mappedRoles);
    }

    private class Role {

        private final String name;
        private volatile HashSet<Principal> includes = new HashSet<Principal>();
        private volatile HashSet<Principal> excludes = new HashSet<Principal>();

        private Role(final String name) {
            this.name = name;
        }

        private String getName() {
            return name;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[Role name='" + name + "' ");
            sb.append("{Includes = ");
            for (Principal current : includes) {
                sb.append(current.toString());
            }
            sb.append("}");
            sb.append("{Excludes = ");
            for (Principal current : excludes) {
                sb.append(current.toString());
            }
            sb.append("}");
            sb.append("]");

            return sb.toString();
        }

        private synchronized void addPrincipal(final Principal principal, final MatchType matchType, final boolean immediate) {
            HashSet<Principal> set = getSet(matchType, immediate);
            set.add(principal); // TODO - Work out how to handle duplicates.

            setSet(set, matchType, immediate);
        }

        private synchronized void removePrincipal(final Principal principal, final MatchType matchType) {
            HashSet<Principal> set = getSet(matchType, false);
            set.remove(principal); // TODO - Work out how to handle missing entries.

            setSet(set, matchType, false);
        }

        private boolean isIncluded(Caller caller) {
            return isInSet(caller, includes);
        }

        private boolean isExcluded(Caller caller) {
            return isInSet(caller, excludes);
        }

        private boolean isInSet(Caller caller, HashSet<Principal> theSet) {
            // One match is all it takes - return true on first match found.

            String accountName = null;
            String realm = null;
            Set<String> groups = null;

            for (Principal current : theSet) {
                String expectedRealm = current.getRealm();
                switch (current.getType()) {
                    case USER:
                        if (expectedRealm != null) {
                            if (current.getName().equals((accountName = getAccountName(caller, accountName)))
                                    && expectedRealm.equals((realm = getRealmName(caller, realm)))) {
                                return true;
                            }
                        } else {
                            if (current.getName().equals((accountName = getAccountName(caller, accountName)))) {
                                return true;
                            }
                        }
                        break;
                    case GROUP:
                        if (expectedRealm != null) {
                            if ((groups = getGroups(caller, groups)).contains(current.getName())
                                    && expectedRealm.equals((realm = getRealmName(caller, realm)))) {
                                return true;
                            }
                        } else {
                            if ((groups = getGroups(caller, groups)).contains(current.getName())) {
                                return true;
                            }
                        }

                        break;
                }
            }

            return false;
        }

        private String getAccountName(final Caller caller, final String currentValue) {
            return currentValue != null ? currentValue : caller.getName();
        }

        private String getRealmName(final Caller caller, final String currentValue) {
            return currentValue != null ? currentValue : caller.getRealm();
        }

        private Set<String> getGroups(final Caller caller, final Set<String> currentValue) {
            return currentValue != null ? currentValue : caller.getAssociatedGroups();
        }

        private HashSet<Principal> getSet(final MatchType matchType, final boolean immediate) {
            HashSet<Principal> set;
            switch (matchType) {
                case INCLUDE:
                    set = includes;
                    break;
                default:
                    set = excludes;
            }

            return immediate ? set : new HashSet<Principal>(set);
        }

        private void setSet(final HashSet<Principal> set, final MatchType matchType, final boolean immediate) {
            if (immediate == false) {
                switch (matchType) {
                    case INCLUDE:
                        includes = set;
                        break;
                    case EXCLUDE:
                        excludes = set;
                }
            }
        }
    }

    private final class Principal {
        private final PrincipalType type;
        private final String realm;
        private final String name;

        private final int hashCode; // Doesn't change and we know it is needed.

        private Principal(final PrincipalType type, final String name, final String realm) {
            this.type = type;
            this.name = name;
            this.realm = realm;

            hashCode = type.ordinal() * name.hashCode() * (realm == null ? 31 : realm.hashCode());
        }

        public PrincipalType getType() {
            return type;
        }

        public String getRealm() {
            return realm;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Principal ? equals((Principal) obj) : false;
        }

        public boolean equals(Principal obj) {
            return type == obj.type && name.equals(obj.name) && (realm == null ? obj.realm == null : realm.equals(obj.realm));
        }

        @Override
        public String toString() {
            return "Principal [type=" + type + ", realm=" + realm + ", name=" + name + "]";
        }

    }

}
