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

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.constraint.ApplicationTypeConstraint;
import org.jboss.as.controller.access.constraint.AuditConstraint;
import org.jboss.as.controller.access.constraint.Constraint;
import org.jboss.as.controller.access.constraint.ConstraintFactory;
import org.jboss.as.controller.access.constraint.HostEffectConstraint;
import org.jboss.as.controller.access.constraint.NonAuditConstraint;
import org.jboss.as.controller.access.constraint.ScopingConstraint;
import org.jboss.as.controller.access.constraint.SensitiveTargetConstraint;
import org.jboss.as.controller.access.constraint.SensitiveVaultExpressionConstraint;
import org.jboss.as.controller.access.constraint.ServerGroupEffectConstraint;
import org.jboss.as.controller.access.constraint.TopRoleConstraint;
import org.jboss.as.controller.access.permission.CombinationManagementPermission;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.permission.JmxPermissionFactory;
import org.jboss.as.controller.access.permission.ManagementPermission;
import org.jboss.as.controller.access.permission.ManagementPermissionCollection;
import org.jboss.as.controller.access.permission.PermissionFactory;
import org.jboss.as.controller.access.permission.SimpleManagementPermission;

/**
 * Default {@link org.jboss.as.controller.access.permission.PermissionFactory} implementation that supports
 * the WildFly default role-based access control permission scheme.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class DefaultPermissionFactory implements PermissionFactory, JmxPermissionFactory, AuthorizerConfiguration.ScopedRoleListener {

    private static final PermissionCollection NO_PERMISSIONS = new NoPermissionsCollection();
    private final RoleMapper roleMapper;
    private final SortedSet<ConstraintFactory> constraintFactories = new TreeSet<ConstraintFactory>();
    private final Map<String, ManagementPermissionCollection> permissionsByRole = new HashMap<String, ManagementPermissionCollection>();
    private final Map<String, ScopedBase> scopedBaseMap = new HashMap<String, ScopedBase>();
    private final AuthorizerConfiguration authorizerConfiguration;
    private PermsHolder permsHolder;
    private boolean rolePermissionsConfigured;

    /**
     * Creates a new {@code DefaultPermissionFactory}
     * @param roleMapper  the role mapper. Cannot be {@code null}
     * @param authorizerConfiguration the configuration for the {@link org.jboss.as.controller.access.Authorizer} that
     *                                is using this factory. Cannot be {@code null}
     */
    public DefaultPermissionFactory(RoleMapper roleMapper, AuthorizerConfiguration authorizerConfiguration) {
        this(roleMapper, getStandardConstraintFactories(), authorizerConfiguration);
    }

    /** Only for testing use, other than the delegation from the primary constructor */
    DefaultPermissionFactory(RoleMapper roleMapper,
                             Set<ConstraintFactory> constraintFactories, AuthorizerConfiguration authorizerConfiguration) {
        this.roleMapper = roleMapper;
        this.constraintFactories.addAll(constraintFactories);
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
        return getUserPermissions(roleMapper.mapRoles(caller, callEnvironment, action, target));
    }

    @Override
    public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
        return getUserPermissions(roleMapper.mapRoles(caller, callEnvironment, action, target));
    }

    @Override
    public Set<String> getUserRoles(Caller caller, Environment callEnvironment, Action action, TargetResource target){
        return roleMapper.mapRoles(caller, callEnvironment, action, target);
    }

    public boolean isNonFacadeMBeansSensitive() {
        return authorizerConfiguration.isNonFacadeMBeansSensitive();
    }

    private PermissionCollection getUserPermissions(Set<String> roles) {
        PermsHolder currentPerms = configureRolePermissions();
        PermissionCollection result = currentPerms.getPermissions(roles);
        if (result != null) {
            return result;
        }
        final CombinationPolicy combinationPolicy = authorizerConfiguration.getPermissionCombinationPolicy();
        ManagementPermissionCollection simple = null;
        Map<Action.ActionEffect, CombinationManagementPermission> combined = null;
        for (String roleName : roles) {
            if (combinationPolicy == CombinationPolicy.REJECTING && simple != null) {
                throw ControllerMessages.MESSAGES.illegalMultipleRoles();
            }
            ManagementPermissionCollection role = currentPerms.permsByRole.get(getOfficialForm(roleName));
            if (role == null) {
                throw ControllerMessages.MESSAGES.unknownRole(roleName);
            }
            if (simple == null) {
                simple = role;
            } else {
                if (combined == null) {
                    combined = new HashMap<Action.ActionEffect, CombinationManagementPermission>();
                    Enumeration<Permission> permissionEnumeration = simple.elements();
                    String firstName = simple.getName();
                    while (permissionEnumeration.hasMoreElements()) {
                        ManagementPermission mperm = (ManagementPermission) permissionEnumeration.nextElement();
                        Action.ActionEffect actionEffect = mperm.getActionEffect();
                        CombinationManagementPermission cmp = new CombinationManagementPermission(combinationPolicy, actionEffect);
                        cmp.addUnderlyingPermission(firstName, mperm);
                        combined.put(actionEffect, cmp);
                    }
                }
                Enumeration<Permission> permissionEnumeration = role.elements();
                String officialForm = getOfficialForm(roleName);
                while (permissionEnumeration.hasMoreElements()) {
                    ManagementPermission mperm = (ManagementPermission) permissionEnumeration.nextElement();
                    Action.ActionEffect actionEffect = mperm.getActionEffect();
                    CombinationManagementPermission cmp = combined.get(actionEffect);
                    if (cmp == null) {
                        cmp = new CombinationManagementPermission(combinationPolicy, actionEffect);
                        combined.put(actionEffect, cmp);
                    }
                    cmp.addUnderlyingPermission(officialForm, mperm);
                }

            }
        }
        if (combined == null) {
            result = simple != null ? simple : NO_PERMISSIONS;
        } else {
            result = new ManagementPermissionCollection("MULTIPLE ROLES", CombinationManagementPermission.class);
            for (CombinationManagementPermission cmp : combined.values()) {
                result.add(cmp);
            }
        }
        currentPerms.storePermissions(roles, result);
        return result;
    }

    @Override
    public PermissionCollection getRequiredPermissions(Action action, TargetAttribute target) {
        PermsHolder currentPerms = configureRolePermissions();
        ConstraintFactory[] currentFactories = currentPerms.constraintFactories;
        ManagementPermissionCollection result = new ManagementPermissionCollection(SimpleManagementPermission.class);
        for (Action.ActionEffect actionEffect : action.getActionEffects()) {
            Constraint[] constraints = new Constraint[currentFactories.length];
            for (int i = 0; i < constraints.length; i++) {
                constraints[i] = currentFactories[i].getRequiredConstraint(actionEffect, action, target);
            }
            result.add(new SimpleManagementPermission(actionEffect, constraints));
        }
        return result;
    }

    @Override
    public PermissionCollection getRequiredPermissions(Action action, TargetResource target) {
        PermsHolder currentPerms = configureRolePermissions();
        ConstraintFactory[] currentFactories = currentPerms.constraintFactories;
        ManagementPermissionCollection result = new ManagementPermissionCollection(SimpleManagementPermission.class);
        for (Action.ActionEffect actionEffect : action.getActionEffects()) {
            Constraint[] constraints = new Constraint[currentFactories.length];
            for (int i = 0; i < constraints.length; i++) {
                constraints[i] = currentFactories[i].getRequiredConstraint(actionEffect, action, target);
            }
            result.add(new SimpleManagementPermission(actionEffect, constraints));
        }
        return result;
    }

    /** Hook for the access control management layer to add a new constraint factory */
    void addConstraintFactory(ConstraintFactory factory) {
        synchronized (this) {
            if (constraintFactories.add(factory)) {
                // Throw away our permission sets
                rolePermissionsConfigured = false;
            }
        }
    }

    @Override
    public synchronized void scopedRoleAdded(AuthorizerConfiguration.ScopedRole added) {
        String roleName = added.getName();
        String officialForm = getOfficialForm(roleName);
        if (permissionsByRole.containsKey(officialForm)) {
            throw ControllerMessages.MESSAGES.roleIsAlreadyRegistered(roleName);
        }
        String baseName = added.getBaseRoleName();
        String officialBase = getOfficialForm(baseName);
        if (rolePermissionsConfigured && !permissionsByRole.containsKey(officialBase)) {
            throw ControllerMessages.MESSAGES.unknownBaseRole(baseName);
        }
        ScopingConstraint constraint = added.getScopingConstraint();
        addConstraintFactory(constraint.getFactory());
        scopedBaseMap.put(officialForm, new ScopedBase(StandardRole.valueOf(officialBase), constraint));
        rolePermissionsConfigured = false;
    }

    @Override
    public synchronized void scopedRoleRemoved(AuthorizerConfiguration.ScopedRole removed) {
        String officialForm = getOfficialForm(removed.getName());
        StandardRole standard;
        try {
            standard = StandardRole.valueOf(officialForm);
        } catch (RuntimeException ignored) {
            // wasn't a standard role
            standard = null;
        }
        if (standard != null) {
            throw ControllerMessages.MESSAGES.cannotRemoveStandardRole(standard.toString());
        }
        synchronized (this) {
            scopedBaseMap.remove(officialForm);
            rolePermissionsConfigured = false;
        }
    }

    private synchronized PermsHolder configureRolePermissions() {
        if (!rolePermissionsConfigured) {
            this.permissionsByRole.clear();
            this.permissionsByRole.putAll(configureDefaultPermissions());
            for (Map.Entry<String, ScopedBase> entry : scopedBaseMap.entrySet()) {
                addScopedRoleInternal(entry.getKey(), entry.getValue().base, entry.getValue().constraint);
            }
            permsHolder = new PermsHolder(permissionsByRole, constraintFactories);
            rolePermissionsConfigured = true;
        }
        return permsHolder;
    }

    private synchronized Map<String, ManagementPermissionCollection> configureDefaultPermissions() {

        Map<String, ManagementPermissionCollection> result = new HashMap<String, ManagementPermissionCollection>();
        for (StandardRole standardRole : StandardRole.values()) {
            String officialForm = getOfficialForm(standardRole);
            ManagementPermissionCollection rolePerms = new ManagementPermissionCollection(officialForm, SimpleManagementPermission.class);
            for (Action.ActionEffect actionEffect : Action.ActionEffect.values()) {
                if (standardRole.isActionEffectAllowed(actionEffect)) {
                    Constraint[] constraints = new Constraint[constraintFactories.size()];
                    int i = 0;
                    for (ConstraintFactory factory : this.constraintFactories) {
                        constraints[i] = factory.getStandardUserConstraint(standardRole, actionEffect);
                        i++;
                    }
                    rolePerms.add(new SimpleManagementPermission(actionEffect, constraints));
                }
            }
            result.put(officialForm, rolePerms);
        }
        return result;
    }

    private synchronized void addScopedRoleInternal(String officialForm, StandardRole base, ScopingConstraint scopingConstraint) {

        ManagementPermissionCollection baseCollection = permissionsByRole.get(getOfficialForm(base));
        int constraintIndex = getConstraintIndex(scopingConstraint.getFactory());

        Map<Action.ActionEffect, SimpleManagementPermission> monitorPermissions = new HashMap<Action.ActionEffect, SimpleManagementPermission>();
        ManagementPermissionCollection monitorCollection = permissionsByRole.get(getOfficialForm(StandardRole.MONITOR));
        Enumeration<Permission> monitorEnumeration = monitorCollection.elements();
        while (monitorEnumeration.hasMoreElements()) {
            SimpleManagementPermission monitorPerm = (SimpleManagementPermission) monitorEnumeration.nextElement();
            monitorPermissions.put(monitorPerm.getActionEffect(), monitorPerm);
        }

        ManagementPermissionCollection scopedPermissions = null;
        Enumeration<Permission> permissionEnumeration = baseCollection.elements();
        String scopedBaseName = officialForm + " (" + getOfficialForm(base) + " permissions)";
        while (permissionEnumeration.hasMoreElements()) {
            SimpleManagementPermission basePerm = (SimpleManagementPermission) permissionEnumeration.nextElement();
            Action.ActionEffect actionEffect = basePerm.getActionEffect();
            CombinationManagementPermission combinedPermission = new CombinationManagementPermission(CombinationPolicy.PERMISSIVE, actionEffect);
            if (scopedPermissions == null) {
                scopedPermissions = new ManagementPermissionCollection(officialForm, CombinationManagementPermission.class);
            }
            ManagementPermission scopedPerm = basePerm.createScopedPermission(scopingConstraint.getStandardConstraint(), constraintIndex);
            combinedPermission.addUnderlyingPermission(scopedBaseName, scopedPerm);

            SimpleManagementPermission monitorPerm = monitorPermissions.get(actionEffect);
            String scopedReadOnlyName = officialForm + " (READ-ONLY permissions)";
            if (monitorPerm != null) {
                combinedPermission.addUnderlyingPermission(scopedReadOnlyName,
                        monitorPerm.createScopedPermission(scopingConstraint.getOutofScopeReadConstraint(), constraintIndex));
            }
            scopedPermissions.add(combinedPermission);
        }

        permissionsByRole.put(officialForm, scopedPermissions);
    }

    private int getConstraintIndex(ConstraintFactory factory) {
        int i = 0;
        for (Iterator<ConstraintFactory> iter = constraintFactories.iterator(); iter.hasNext(); i++) {
            if (factory.equals(iter.next())) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private static Set<ConstraintFactory> getStandardConstraintFactories() {
        final Set<ConstraintFactory> result = new HashSet<ConstraintFactory>();
        result.add(ApplicationTypeConstraint.FACTORY);
        result.add(AuditConstraint.FACTORY);
        result.add(NonAuditConstraint.FACTORY);
        result.add(HostEffectConstraint.FACTORY);
        result.add(SensitiveTargetConstraint.FACTORY);
        result.add(SensitiveVaultExpressionConstraint.FACTORY);
        result.add(ServerGroupEffectConstraint.FACTORY);
        result.add(TopRoleConstraint.FACTORY);
        return result;
    }

    private static String getOfficialForm(StandardRole role) {
        return role.toString().toUpperCase(Locale.ENGLISH);
    }

    private static String getOfficialForm(String role) {
        return role.toUpperCase(Locale.ENGLISH);
    }

    /** Data holder class */
    private class ScopedBase {
        private final StandardRole base;
        private final ScopingConstraint constraint;

        private ScopedBase(StandardRole base, ScopingConstraint constraint) {
            this.base = base;
            this.constraint = constraint;
        }
    }

    private static class NoPermissionsCollection extends PermissionCollection {

        private static final long serialVersionUID = 426277167342589940L;

        private NoPermissionsCollection() {
            super.setReadOnly();
        }

        @Override
        public void add(Permission permission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean implies(Permission permission) {
            return false;
        }

        @Override
        public Enumeration<Permission> elements() {
            return new Enumeration<Permission>() {

                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public Permission nextElement() {
                    throw new NoSuchElementException();
                }

            };
        }

    }

    private static class PermsHolder {
        private final Map<Set<String>, PermissionCollection> permsByRoleSet =
                Collections.synchronizedMap(new HashMap<Set<String>, PermissionCollection>());
        private final Map<String, ManagementPermissionCollection> permsByRole =
                new HashMap<String, ManagementPermissionCollection>();
        private final ConstraintFactory[] constraintFactories;

        private PermsHolder(Map<String, ManagementPermissionCollection> permsByRole, SortedSet<ConstraintFactory> constraintFactories) {
            this.permsByRole.putAll(permsByRole);
            this.constraintFactories = constraintFactories.toArray(new ConstraintFactory[constraintFactories.size()]);
        }

        private PermissionCollection getPermissions(Set<String> roleSet) {
            return permsByRoleSet.get(roleSet);
        }

        private void storePermissions(Set<String> roleSet, PermissionCollection perms) {
            permsByRoleSet.put(roleSet, perms);
        }
    }

}
