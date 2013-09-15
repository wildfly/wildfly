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

package org.jboss.as.controller.access.management;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.rbac.StandardRBACAuthorizer;

/**
 * Standard {@link AuthorizerConfiguration} implementation that also exposes mutator APIs for use by
 * the WildFly management layer.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class WritableAuthorizerConfiguration implements AuthorizerConfiguration {

    private volatile Map<String, RoleMappingImpl> roleMappings = new HashMap<String, RoleMappingImpl>();
    private final Map<Object, RoleMappingImpl> removedRoles = new WeakHashMap<Object, RoleMappingImpl>();
    private volatile CombinationPolicy combinationPolicy = CombinationPolicy.PERMISSIVE;
    private volatile boolean useRealmRoles;
    private volatile boolean nonFacadeMBeansSensitive;
    private volatile Authorizer.AuthorizerDescription authorizerDescription;
    private volatile RoleMaps roleMaps;
    private final Set<ScopedRoleListener> scopedRoleListeners = new LinkedHashSet<ScopedRoleListener>();

    public WritableAuthorizerConfiguration(Authorizer.AuthorizerDescription authorizerDescription) {
        this.authorizerDescription = authorizerDescription;
        this.roleMaps = new RoleMaps(authorizerDescription.getStandardRoles(), Collections.<String, ScopedRole>emptyMap());
    }

    /**
     * Reset the internal state of this object back to what it originally was.
     * Only to be used in a slave host controller following a post-boot reconnect
     * to the master.
     */
    public synchronized void domainReconnectReset() {
        this.authorizerDescription = StandardRBACAuthorizer.AUTHORIZER_DESCRIPTION;
        this.useRealmRoles = this.nonFacadeMBeansSensitive = false;
        this.roleMappings = new HashMap<String, RoleMappingImpl>();
        RoleMaps oldRoleMaps = this.roleMaps;
        this.roleMaps = new RoleMaps(authorizerDescription.getStandardRoles(), Collections.<String, ScopedRole>emptyMap());
        for (ScopedRole role : oldRoleMaps.scopedRoles.values()) {
            for (ScopedRoleListener listener : scopedRoleListeners) {
                try {
                    listener.scopedRoleRemoved(role);
                } catch (Exception ignored) {
                    // TODO log an ERROR
                }
            }
        }
    }

    public synchronized void registerScopedRoleListener(ScopedRoleListener listener) {
        scopedRoleListeners.add(listener);
    }

    public synchronized void unregisterScopedRoleListener(ScopedRoleListener listener) {
        scopedRoleListeners.remove(listener);
    }

    @Override
    public CombinationPolicy getPermissionCombinationPolicy() {
        return combinationPolicy;
    }

    @Override
    public boolean isRoleBased() {
        return authorizerDescription.isRoleBased();
    }

    @Override
    public boolean isMapUsingRealmRoles() {
        return useRealmRoles;
    }

    @Override
    public Set<String> getStandardRoles() {
        return roleMaps.standardRoles;
    }

    @Override
    public Map<String, ScopedRole> getScopedRoles() {
        return roleMaps.scopedRoles;
    }

    @Override
    public Set<String> getAllRoles() {
        return roleMaps.allRoles;
    }

    @Override
    public boolean hasRole(String roleName) {
        final Set<String> canonicalRoles = roleMaps.canonicalRoles;
        return canonicalRoles.contains(getOfficialForm(roleName));
    }

    @Override
    public Map<String, RoleMapping> getRoleMappings() {
        return Collections.<String, RoleMapping>unmodifiableMap(roleMappings);
    }

    public synchronized void addScopedRole(ScopedRole toAdd) {
        for (ScopedRoleListener listener : scopedRoleListeners) {
            listener.scopedRoleAdded(toAdd);
        }
        Map<String, ScopedRole> newScopedRoles = new HashMap<String, ScopedRole>(roleMaps.scopedRoles);
        newScopedRoles.put(toAdd.getName(), toAdd);
        roleMaps = new RoleMaps(roleMaps.standardRoles, newScopedRoles);
    }

    public synchronized void removeScopedRole(String toRemove) {
        Map<String, ScopedRole> newScopedRoles = new HashMap<String, ScopedRole>(roleMaps.scopedRoles);
        ScopedRole removed = newScopedRoles.remove(toRemove);
        roleMaps = new RoleMaps(roleMaps.standardRoles, newScopedRoles);
        if (removed != null) {
            for (ScopedRoleListener listener : scopedRoleListeners) {
                listener.scopedRoleRemoved(removed);
            }
        }
    }

    @Override
    public boolean isNonFacadeMBeansSensitive() {
        return nonFacadeMBeansSensitive;
    }

    public void addRoleMappingImmediate(final String roleName) {
        roleMappings.put(roleName, new RoleMappingImpl(roleName));
    }

    /**
     * Adds a new role to the list of defined roles.
     *
     * @param roleName - The name of the role being added.
     */
    public synchronized void addRoleMapping(final String roleName) {
        HashMap<String, RoleMappingImpl> newRoles = new HashMap<String, RoleMappingImpl>(roleMappings);
        if (newRoles.containsKey(roleName) == false) {
            newRoles.put(roleName, new RoleMappingImpl(roleName));
            roleMappings = Collections.unmodifiableMap(newRoles);
        }
    }

    /**
     * Remove a role from the list of defined roles.
     *
     * @param roleName - The name of the role to be removed.
     * @return A key that can be used to undo the removal.
     */
    public synchronized Object removeRoleMapping(final String roleName) {
        /*
         * Would not expect this to happen during boot so don't offer the 'immediate' optimisation.
         */
        HashMap<String, RoleMappingImpl> newRoles = new HashMap<String, RoleMappingImpl>(roleMappings);
        if (newRoles.containsKey(roleName)) {
            RoleMappingImpl removed = newRoles.remove(roleName);
            Object removalKey = new Object();
            removedRoles.put(removalKey, removed);
            roleMappings = Collections.unmodifiableMap(newRoles);

            return removalKey;
        }

        return null;
    }

    /**
     * Undo a prior removal using the supplied undo key.
     *
     * @param removalKey - The key returned from the call to removeRoleMapping.
     * @return true if the undo was successfull, false otherwise.
     */
    public synchronized boolean undoRoleMappingRemove(final Object removalKey) {
        HashMap<String, RoleMappingImpl> newRoles = new HashMap<String, RoleMappingImpl>(roleMappings);
        RoleMappingImpl toRestore = removedRoles.remove(removalKey);
        if (toRestore != null && newRoles.containsKey(toRestore.getName()) == false) {
            newRoles.put(toRestore.getName(), toRestore);
            roleMappings = Collections.unmodifiableMap(newRoles);
            return true;
        }

        return false;
    }

    public void setRoleMappingIncludeAll(final String roleName, final boolean includeAll) {
        RoleMappingImpl role = roleMappings.get(roleName);
        role.setIncludeAll(includeAll);
    }

    public boolean addRoleMappingPrincipal(final String roleName, final PrincipalType principalType, final MatchType matchType,
                                           final String name, final String realm, final boolean immediate) {
        RoleMappingImpl role = roleMappings.get(roleName);
        if (role != null) {
            if (immediate) {
                return role.addPrincipalImmediate(createPrincipal(principalType, name, realm), matchType);
            } else {
                return role.addPrincipal(createPrincipal(principalType, name, realm), matchType);
            }
        }
        return false;
    }

    public boolean removeRoleMappingPrincipal(final String roleName, final PrincipalType principalType, final MatchType matchType,
                                              final String name, final String realm) {
        RoleMappingImpl role = roleMappings.get(roleName);
        if (role != null) {
            return role.removePrincipal(createPrincipal(principalType, name, realm), matchType);
        }
        return false;
    }

    public MappingPrincipal createPrincipal(final PrincipalType principalType, final String name, final String realm) {
        return new MappingPrincipalImpl(principalType, name, realm);
    }

    public void setPermissionCombinationPolicy(CombinationPolicy combinationPolicy) {
        assert combinationPolicy != null : "combinationPolicy is null";
        this.combinationPolicy = combinationPolicy;
    }

    void setNonFacadeMBeansSensitive(boolean nonFacadeMBeansSensitive) {
        this.nonFacadeMBeansSensitive = nonFacadeMBeansSensitive;
    }

    synchronized void setAuthorizerDescription(Authorizer.AuthorizerDescription authorizerDescription) {
        this.authorizerDescription = authorizerDescription;
        this.roleMaps = new RoleMaps(authorizerDescription.getStandardRoles(), roleMaps.scopedRoles);
    }

    private static String getOfficialForm(String roleName) {
        return roleName == null ? null : roleName.toUpperCase(Locale.ENGLISH);
    }

    /**
     * Types of matching strategies used in {@link org.jboss.as.controller.access.Caller} to {@link org.jboss.as.controller.access.AuthorizerConfiguration.RoleMapping} mapping.
     */
    public static enum MatchType {
        /** Any exclusive match, where a match precludes mapping a {@link org.jboss.as.controller.access.Caller} to a {@link org.jboss.as.controller.access.AuthorizerConfiguration.RoleMapping} */
        EXCLUDE,
        /** Any inclusive match, where a match allows mapping a {@link org.jboss.as.controller.access.Caller} to a {@link org.jboss.as.controller.access.AuthorizerConfiguration.RoleMapping} */
        INCLUDE
    }

    private static final class RoleMappingImpl implements RoleMapping {

        private final String name;
        private boolean includeAll;
        private volatile HashSet<MappingPrincipal> includes = new HashSet<MappingPrincipal>();
        private volatile HashSet<MappingPrincipal> excludes = new HashSet<MappingPrincipal>();

        private RoleMappingImpl(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[Role name='" + name + "' ");
            sb.append("{Includes = ");
            for (MappingPrincipal current : includes) {
                sb.append(current.toString());
            }
            sb.append("}");
            sb.append("{Excludes = ");
            for (MappingPrincipal current : excludes) {
                sb.append(current.toString());
            }
            sb.append("}");
            sb.append("]");

            return sb.toString();
        }

        private boolean addPrincipalImmediate(final MappingPrincipal principal, final MatchType matchType) {
            HashSet<MappingPrincipal> set = getSet(matchType, true);
            try {
                return set.add(principal);
            } finally {
                setSet(set, matchType, true);
            }
        }

        private synchronized boolean addPrincipal(final MappingPrincipal principal, final MatchType matchType) {
            HashSet<MappingPrincipal> set = getSet(matchType, false);
            try {
                return set.add(principal);
            } finally {
                setSet(set, matchType, false);
            }
        }

        private void setIncludeAll(final boolean includeAll) {
            this.includeAll = includeAll;
        }

        public boolean includeAllAuthedUsers() {
            return includeAll;
        }

        private synchronized boolean removePrincipal(final MappingPrincipal principal, final MatchType matchType) {
            HashSet<MappingPrincipal> set = getSet(matchType, false);
            try {
                return set.remove(principal);
            } finally {
                setSet(set, matchType, false);
            }
        }

        @Override
        public MappingPrincipal isIncluded(Caller caller) {
            return isInSet(caller, includes);
        }

        @Override
        public MappingPrincipal isExcluded(Caller caller) {
            return isInSet(caller, excludes);
        }

        private MappingPrincipal isInSet(Caller caller, HashSet<MappingPrincipal> theSet) {
            // One match is all it takes - return true on first match found.

            String accountName = null;
            String realm = null;
            Set<String> groups = null;

            for (MappingPrincipal current : theSet) {
                String expectedRealm = current.getRealm();
                switch (current.getType()) {
                    case USER:
                        if (expectedRealm != null) {
                            if (current.getName().equals((accountName = getAccountName(caller, accountName)))
                                    && expectedRealm.equals((realm = getRealmName(caller, realm)))) {
                                return current;
                            }
                        } else {
                            if (current.getName().equals((accountName = getAccountName(caller, accountName)))) {
                                return current;
                            }
                        }
                        break;
                    case GROUP:
                        if (expectedRealm != null) {
                            if ((groups = getGroups(caller, groups)).contains(current.getName())
                                    && expectedRealm.equals((realm = getRealmName(caller, realm)))) {
                                return current;
                            }
                        } else {
                            if ((groups = getGroups(caller, groups)).contains(current.getName())) {
                                return current;
                            }
                        }

                        break;
                }
            }

            return null;
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

        private HashSet<MappingPrincipal> getSet(final MatchType matchType, final boolean immediate) {
            HashSet<MappingPrincipal> set;
            switch (matchType) {
                case INCLUDE:
                    set = includes;
                    break;
                default:
                    set = excludes;
            }

            return immediate ? set : new HashSet<MappingPrincipal>(set);
        }

        private void setSet(final HashSet<MappingPrincipal> set, final MatchType matchType, final boolean immediate) {
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

    private static final class MappingPrincipalImpl implements MappingPrincipal {
        private final PrincipalType type;
        private final String realm;
        private final String name;

        private final int hashCode; // Doesn't change and we know it is needed.

        private MappingPrincipalImpl(final PrincipalType type, final String name, final String realm) {
            this.type = type;
            this.name = name;
            this.realm = realm;

            hashCode = type.ordinal() * name.hashCode() * (realm == null ? 31 : realm.hashCode());
        }

        @Override
        public PrincipalType getType() {
            return type;
        }

        @Override
        public String getRealm() {
            return realm;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MappingPrincipalImpl && this.equals((MappingPrincipalImpl) obj);
        }

        private boolean equals(MappingPrincipalImpl obj) {
            return type == obj.type && name.equals(obj.name) && (realm == null ? obj.realm == null : realm.equals(obj.realm));
        }

        @Override
        public String toString() {
            return "Principal [type=" + type + ", realm=" + realm + ", name=" + name + "]";
        }

    }

    private static class RoleMaps {
        private final Set<String> standardRoles;
        private final Map<String, ScopedRole> scopedRoles;
        private final Set<String> allRoles;
        private final Set<String> canonicalRoles;

        private RoleMaps(Set<String> standardRoles, Map<String, ScopedRole> scopedRoles) {
            this.standardRoles = standardRoles;
            this.scopedRoles = scopedRoles;
            Set<String> allRoles = new HashSet<String>();
            allRoles.addAll(standardRoles);
            allRoles.addAll(scopedRoles.keySet());
            this.allRoles = Collections.unmodifiableSet(allRoles);
            Set<String> canonicalRoles = new HashSet<String>();
            for (String r : allRoles) {
                canonicalRoles.add(getOfficialForm(r));
            }
            this.canonicalRoles = Collections.unmodifiableSet(canonicalRoles);
        }
    }
}
