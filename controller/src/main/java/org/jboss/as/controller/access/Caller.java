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

package org.jboss.as.controller.access;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.core.security.AccountPrincipal;
import org.jboss.as.core.security.GroupPrincipal;
import org.jboss.as.core.security.RealmPrincipal;
import org.jboss.as.core.security.RolePrincipal;

/**
 * Represents the caller in an access control decision.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class Caller {

    private static final String UNDEFINED = "UNDEFINED";

    private static final RuntimePermission CREATE_CALLER_PERMISSION = new RuntimePermission("org.jboss.as.controller.security.CREATE_CALLER");
    private static final RuntimePermission GET_SUBJECT_PERMISSION = new RuntimePermission("org.jboss.as.controller.security.GET_SUBJECT");

    private final Subject subject;

    private volatile String name;
    private volatile String realm = UNDEFINED;
    private volatile Set<String> groups;
    private volatile Set<String> roles;

    private Caller(final Subject subject) {
        this.subject = subject;
    }

    public static Caller createCaller(final Subject subject) {
        checkPermission(CREATE_CALLER_PERMISSION);

        return new Caller(subject);
    }

    /**
     * Obtain the name of the caller, most likely a user but could also be a remote process.
     *
     * @return The name of the caller.
     */
    public String getName() {
        if (name == null && subject != null) {
            Set<AccountPrincipal> accounts = subject.getPrincipals(AccountPrincipal.class);
            if (accounts.size() == 1) {
                name = accounts.iterator().next().getName();
            } else if (accounts.size() >= 1) {
                throw MESSAGES.unexpectedAccountPrincipalCount(accounts.size());
            }
        }

        return name;
    }

    /**
     * Obtain the realm used for authentication.
     *
     * This realm name applies to both the user and the groups.
     *
     * @return The name of the realm used for authentication.
     */
    public String getRealm() {
        if (UNDEFINED.equals(realm)) {
            if (subject != null) {
                Set<RealmPrincipal> realmPrincipals = subject.getPrincipals(RealmPrincipal.class);
                String foundRealm = null;
                for (RealmPrincipal current : realmPrincipals) {
                    if (foundRealm == null) {
                        foundRealm = current.getRealm();
                    } else if (foundRealm.equals(current.getRealm()) == false) {
                        throw MESSAGES.differentRealmsInSubject(foundRealm, current.getRealm());
                    }
                }
                realm = foundRealm; // Note: Initialisation may have now set this to null
            } else {
                this.realm = null;
            }
        }

        return realm;
    }

    /**
     * This method returns a {@link Set} of groups loaded for the user during the authentication step.
     *
     * Note: Groups are also assumed to be specific to the realm.
     *
     * @return The {@link Set} of groups loaded during authentication or an empty {@link Set} if none were loaded.
     */
    public Set<String> getAssociatedGroups() {
        if (groups == null) {
            if (subject != null) {
                Set<GroupPrincipal> groupPrincipals = subject.getPrincipals(GroupPrincipal.class);
                Set<String> groups = new HashSet<String>(groupPrincipals.size());
                for (Principal current : groupPrincipals) {
                    groups.add(current.getName());
                }
                this.groups = Collections.unmodifiableSet(groups);
            } else {
                this.groups = Collections.emptySet();
            }
        }

        return groups;
    }

    /**
     * This method returns the set of roles already associated with the caller.
     *
     * Note: This is the realm mapping of roles and does not automatically mean that these roles will be used for management
     * access control decisions.
     *
     * @return The {@link Set} of associated roles or an empty set if none.
     */
    public Set<String> getAssociatedRoles() {
        if (roles == null) {
            if (subject != null) {
                Set<RolePrincipal> rolePrincipals = subject.getPrincipals(RolePrincipal.class);
                Set<String> roles = new HashSet<String>(rolePrincipals.size());
                for (Principal current : rolePrincipals) {
                    roles.add(current.getName());
                }
                this.roles = Collections.unmodifiableSet(roles);
            } else {
                this.roles = Collections.emptySet();
            }
        }

        return roles;
    }

    /**
     * Check if this {@link Caller} has a {@link Subject} without needing to access it.
     *
     * @return true if this {@link Caller} has a {@link Subject}
     */
    public boolean hasSubject() {
        return subject != null;
    }

    /**
     * Obtain the {@link Subject} used to create this caller.
     *
     * @return The {@link Subject} used to create this caller.
     */
    public Subject getSubject() {
        checkPermission(GET_SUBJECT_PERMISSION);

        return subject;
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

}
