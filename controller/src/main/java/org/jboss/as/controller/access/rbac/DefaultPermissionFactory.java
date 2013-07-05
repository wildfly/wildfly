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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
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
import org.jboss.as.controller.access.permission.CombinationManagementPermission;
import org.jboss.as.controller.access.permission.CombinationPolicy;
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
public class DefaultPermissionFactory implements PermissionFactory {

    private final CombinationPolicy combinationPolicy;
    private final RoleMapper roleMapper;
    private final Set<ConstraintFactory> constraintFactories;
    private final Map<String, ManagementPermissionCollection> permissionsByRole = new HashMap<String, ManagementPermissionCollection>();
    private final Map<String, ScopedBase> scopedBaseMap = new HashMap<String, ScopedBase>();
    private boolean rolePermissionsConfigured;

    public DefaultPermissionFactory(CombinationPolicy combinationPolicy, RoleMapper roleMapper) {
        this(combinationPolicy, roleMapper, getStandardConstraintFactories());
    }

    /** Only for testing use, other than the delegation from the primary constructor */
    DefaultPermissionFactory(CombinationPolicy combinationPolicy, RoleMapper roleMapper,
                             Set<ConstraintFactory> constraintFactories) {
        this.combinationPolicy = combinationPolicy;
        this.roleMapper = roleMapper;
        this.constraintFactories = constraintFactories;
    }

    @Override
    public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
        return getUserPermissions(roleMapper.mapRoles(caller, callEnvironment, action, target));
    }

    @Override
    public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
        return getUserPermissions(roleMapper.mapRoles(caller, callEnvironment, action, target));
    }

    private PermissionCollection getUserPermissions(Set<String> user) {
        configureRolePermissions();
        ManagementPermissionCollection simple = null;
        Map<Action.ActionEffect, CombinationManagementPermission> combined = null;
        for (String roleName : user) {
            if (combinationPolicy == CombinationPolicy.REJECTING && simple != null) {
                throw ControllerMessages.MESSAGES.illegalMultipleRoles();
            }
            ManagementPermissionCollection role = null;
            synchronized (this) {
                role = permissionsByRole.get(roleName);
            }
            if (role == null) {
                throw new IllegalArgumentException("unknown role " + role);
            }
            if (simple == null) {
                simple = role;
            } else {
                if (combined == null) {
                    combined = new HashMap<Action.ActionEffect, CombinationManagementPermission>();
                    Enumeration<Permission> permissionEnumeration = simple.elements();
                    while (permissionEnumeration.hasMoreElements()) {
                        ManagementPermission mperm = (ManagementPermission) permissionEnumeration.nextElement();
                        Action.ActionEffect actionEffect = mperm.getActionEffect();
                        CombinationManagementPermission cmp = new CombinationManagementPermission(combinationPolicy, actionEffect);
                        cmp.addUnderlyingPermission(mperm);
                        combined.put(actionEffect, cmp);
                    }
                }
                Enumeration<Permission> permissionEnumeration = role.elements();
                while (permissionEnumeration.hasMoreElements()) {
                    ManagementPermission mperm = (ManagementPermission) permissionEnumeration.nextElement();
                    Action.ActionEffect actionEffect = mperm.getActionEffect();
                    CombinationManagementPermission cmp = combined.get(actionEffect);
                    if (cmp == null) {
                        cmp = new CombinationManagementPermission(combinationPolicy, actionEffect);
                        combined.put(actionEffect, cmp);
                    }
                    cmp.addUnderlyingPermission(mperm);
                }

            }
        }
        PermissionCollection result;
        if (combined == null) {
            result = simple;
        } else {
            result = new ManagementPermissionCollection(CombinationManagementPermission.class);
            for (CombinationManagementPermission cmp : combined.values()) {
                result.add(cmp);
            }
        }
        return result;
    }

    @Override
    public PermissionCollection getRequiredPermissions(Action action, TargetAttribute target) {
        List<ConstraintFactory> factories;
        synchronized (this) {
            factories = new ArrayList<ConstraintFactory>(this.constraintFactories);
        }
        ManagementPermissionCollection result = new ManagementPermissionCollection(SimpleManagementPermission.class);
        for (Action.ActionEffect actionEffect : action.getActionEffects()) {
            Set<Constraint> constraints = new TreeSet<Constraint>();
            for (ConstraintFactory factory : factories) {
                constraints.add(factory.getRequiredConstraint(actionEffect, action, target));
            }
            result.add(new SimpleManagementPermission(actionEffect, constraints.toArray(new Constraint[constraints.size()])));
        }
        return result;
    }

    @Override
    public PermissionCollection getRequiredPermissions(Action action, TargetResource target) {
        List<ConstraintFactory> factories;
        synchronized (this) {
            factories = new ArrayList<ConstraintFactory>(this.constraintFactories);
        }
        ManagementPermissionCollection result = new ManagementPermissionCollection(SimpleManagementPermission.class);
        for (Action.ActionEffect actionEffect : action.getActionEffects()) {
            Set<Constraint> constraints = new TreeSet<Constraint>();
            for (ConstraintFactory factory : factories) {
                constraints.add(factory.getRequiredConstraint(actionEffect, action, target));
            }
            result.add(new SimpleManagementPermission(actionEffect, constraints.toArray(new Constraint[constraints.size()])));
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

    /** Hook for the access control management layer to add a new role */
    void addScopedRole(String roleName, StandardRole base, ScopingConstraint constraint) {
        configureRolePermissions();
        addScopedRoleInternal(roleName, base,constraint);
    }

    private synchronized void configureRolePermissions() {
        if (!rolePermissionsConfigured) {
            this.permissionsByRole.clear();
            this.permissionsByRole.putAll(configureDefaultPermissions());
            for (Map.Entry<String, ScopedBase> entry : scopedBaseMap.entrySet()) {
                addScopedRoleInternal(entry.getKey(), entry.getValue().base, entry.getValue().constraint);
            }
            rolePermissionsConfigured = true;
        }
    }

    private synchronized Map<String, ManagementPermissionCollection> configureDefaultPermissions() {

        Map<String, ManagementPermissionCollection> result = new HashMap<String, ManagementPermissionCollection>();
        for (StandardRole standardRole : StandardRole.values()) {
            ManagementPermissionCollection rolePerms = new ManagementPermissionCollection(SimpleManagementPermission.class);
            for (Action.ActionEffect actionEffect : Action.ActionEffect.values()) {
                if (standardRole.isActionEffectAllowed(actionEffect)) {
                    Set<Constraint> constraints = new TreeSet<Constraint>();
                    for (ConstraintFactory factory : this.constraintFactories) {
                        constraints.add(factory.getStandardUserConstraint(standardRole, actionEffect));
                    }
                    rolePerms.add(new SimpleManagementPermission(actionEffect, constraints.toArray(new Constraint[constraints.size()])));
                }
            }
            result.put(standardRole.toString(), rolePerms);
        }
        return result;
    }

    private synchronized void addScopedRoleInternal(String roleName, StandardRole base, ScopingConstraint constraint) {
        if (permissionsByRole.containsKey(roleName)) {
            throw new IllegalStateException(String.format("role %s is already registered", roleName));
        }
        ManagementPermissionCollection baseCollection = permissionsByRole.get(base.toString());
        if (baseCollection == null) {
            throw new IllegalArgumentException(String.format("Unknown base role %s", base));
        }
        ManagementPermissionCollection scopedPermissions = null;
        Enumeration<Permission> permissionEnumeration = baseCollection.elements();
        while (permissionEnumeration.hasMoreElements()) {
            ManagementPermission basePerm = (ManagementPermission) permissionEnumeration.nextElement();
            ManagementPermission scopedPerm = basePerm.createScopedPermission(constraint);
            if (scopedPermissions == null) {
                scopedPermissions = (ManagementPermissionCollection) scopedPerm.newPermissionCollection();
            }
            scopedPermissions.add(scopedPerm);
        }
        permissionsByRole.put(roleName, scopedPermissions);
        scopedBaseMap.put(roleName, new ScopedBase(base, constraint));
    }

    private static Set<ConstraintFactory> getStandardConstraintFactories() {
        final Set<ConstraintFactory> result = new LinkedHashSet<ConstraintFactory>();
        result.add(ApplicationTypeConstraint.FACTORY);
        result.add(AuditConstraint.FACTORY);
        result.add(NonAuditConstraint.FACTORY);
        result.add(HostEffectConstraint.FACTORY);
        result.add(SensitiveTargetConstraint.FACTORY);
        result.add(SensitiveVaultExpressionConstraint.FACTORY);
        result.add(ServerGroupEffectConstraint.FACTORY);
        return result;
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
}
