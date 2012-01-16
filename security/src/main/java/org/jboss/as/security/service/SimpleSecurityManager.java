/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security.service;

import static java.security.AccessController.doPrivileged;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;

import org.jboss.as.security.SecurityMessages;
import org.jboss.as.security.remoting.RemotingContext;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SecurityContextUtil;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.SubjectInfo;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleIdentity;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimpleSecurityManager {
    private ThreadLocalStack<SecurityContext> contexts = new ThreadLocalStack<SecurityContext>();

    private static PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }

    private static SecurityContext establishSecurityContext(final String securityDomain) {
        // Do not use SecurityFactory.establishSecurityContext, its static init is broken.
        try {
            final SecurityContext securityContext = SecurityContextFactory.createSecurityContext(securityDomain);
            SecurityContextAssociation.setSecurityContext(securityContext);
            return securityContext;
        } catch (Exception e) {
            throw SecurityMessages.MESSAGES.securityException(e);
        }
    }

    public Principal getCallerPrincipal() {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null) {
            return getUnauthenticatedIdentity().asPrincipal();
        }
        /*
         * final Principal principal = getPrincipal(securityContext.getUtil().getSubject());
         */
        Principal principal = securityContext.getIncomingRunAs();
        if (principal == null)
            principal = getPrincipal(securityContext.getSubjectInfo().getAuthenticatedSubject());
        if (principal == null)
            return getUnauthenticatedIdentity().asPrincipal();
        return principal;
    }

    /**
     * Get the Principal given the authenticated Subject. Currently the first principal that is not of type {@code Group} is
     * considered or the single principal inside the CallerPrincipal group.
     *
     * @param subject
     * @return the authenticated principal
     */
    private Principal getPrincipal(Subject subject) {
        Principal principal = null;
        Principal callerPrincipal = null;
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                for (Principal p : principals) {
                    if (!(p instanceof Group) && principal == null) {
                        principal = p;
                    }
                    if (p instanceof Group) {
                        Group g = Group.class.cast(p);
                        if (g.getName().equals("CallerPrincipal") && callerPrincipal == null) {
                            Enumeration<? extends Principal> e = g.members();
                            if (e.hasMoreElements())
                                callerPrincipal = e.nextElement();
                        }
                    }
                }
            }
        }
        return callerPrincipal == null ? principal : callerPrincipal;
    }

    /**
     *
     * @param mappedRoles The principal vs roles mapping (if any). Can be null.
     * @param roleLinks The role link map where the key is a alias role name and the value is the collection of
     *                  role names, that alias represents. Can be null.
     * @param roleNames The role names for which the caller is being checked for
     * @return true if the user is in <b>any</b> one of the <code>roleNames</code>. Else returns false
     */
    public boolean isCallerInRole(final SecurityRolesMetaData mappedRoles, final Map<String, Collection<String>> roleLinks,
                                  final String... roleNames) {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null) {
            return false;
        }

        RoleGroup roleGroup = null;

        RunAs runAs = securityContext.getIncomingRunAs();
        if (runAs != null && runAs instanceof RunAsIdentity) {
            RunAsIdentity runAsIdentity = (RunAsIdentity) runAs;
            roleGroup = runAsIdentity.getRunAsRolesAsRoleGroup();
        } else {
            AuthorizationManager am = securityContext.getAuthorizationManager();
            SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(securityContext);

            roleGroup = am.getSubjectRoles(securityContext.getSubjectInfo().getAuthenticatedSubject(), scb);
        }

        List<Role> roles = roleGroup.getRoles();

        // TODO - Review most performant way.
        Set<String> requiredRoles = new HashSet<String>();
        for (String current : roleNames) {
            requiredRoles.add(current);
        }
        Set<String> actualRoles = new HashSet<String>();
        for (Role current : roles) {
            actualRoles.add(current.getRoleName());
        }
        // add mapped roles
        if (mappedRoles != null) {
            Principal callerPrincipal = getCallerPrincipal();
            Set<String> mapped = mappedRoles.getSecurityRoleNamesByPrincipal(callerPrincipal.getName());
            if (mapped != null) {
                actualRoles.addAll(mapped);
            }
        }
        // if the actual roles matches any of the required roles, then return true
        if (!Collections.disjoint(requiredRoles, actualRoles)) {
            return true;
        }
        // we did not have a match between the required roles and the actual roles.
        // let's now get the alias role names (if any) for each of the actual role
        // and see if any of those aliases of the actual roles matches the required roles
        if (roleLinks != null) {
            for (final String actualRole : actualRoles) {
                // get aliases (if any) for actual role
                final Set<String> aliases = this.getRoleAliases(actualRole, roleLinks);
                // if there's a match between the required role and an alias of an actual role, then
                // return true indicating that the caller is in one of the required roles
                if (!Collections.disjoint(requiredRoles, aliases)) {
                    return true;
                }
            }
        }
        // caller is not in any of the required roles
        return false;
    }

    /**
     * Must be called from within a privileged action.
     *
     * @param securityDomain
     * @param runAs
     * @param runAsPrincipal
     * @param extraRoles
     */
    public void push(final String securityDomain, final String runAs, final String runAsPrincipal, final Set<String> extraRoles) {
        // TODO - Handle a null securityDomain here? Yes I think so.
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        contexts.push(previous);
        SecurityContext current = establishSecurityContext(securityDomain);
        if (previous != null) {
            current.setSubjectInfo(previous.getSubjectInfo());
            current.setIncomingRunAs(previous.getOutgoingRunAs());
        }

        RunAs currentRunAs = current.getIncomingRunAs();
        boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;

        // TODO - Set unauthenticated identity if no auth to occur
        if (trusted == false) {
            if (RemotingContext.isSet()) {
                // In this case the principal and credential will not have been set to set some random values.
                SecurityContextUtil util = current.getUtil();

                Principal p = new SimplePrincipal(UUID.randomUUID().toString());
                String credential = UUID.randomUUID().toString();

                util.createSubjectInfo(p, credential, null);
            }

            // If we have a trusted identity no need for a re-auth.
            boolean authenticated = authenticate(current);
            if (authenticated == false) {
                // TODO - Better type needed.
                throw SecurityMessages.MESSAGES.invalidUserException();
            }
        }

        if (runAs != null) {
            RunAs runAsIdentity = new RunAsIdentity(runAs, runAsPrincipal, extraRoles);
            current.setOutgoingRunAs(runAsIdentity);
        } else if (previous != null && previous.getOutgoingRunAs() != null) {
            // Ensure the propagation continues.
            current.setOutgoingRunAs(previous.getOutgoingRunAs());
        }
    }

    private boolean authenticate(SecurityContext context) {
        SecurityContextUtil util = context.getUtil();
        SubjectInfo subjectInfo = context.getSubjectInfo();
        Subject subject = new Subject();
        Principal principal = util.getUserPrincipal();
        Object credential = util.getCredential();

        boolean authenticated = false;
        if (principal == null) {
            Identity unauthenticatedIdentity = getUnauthenticatedIdentity();
            subjectInfo.addIdentity(unauthenticatedIdentity);
            subject.getPrincipals().add(unauthenticatedIdentity.asPrincipal());
            authenticated = true;
        }

        if (authenticated == false) {
            AuthenticationManager authenticationManager = context.getAuthenticationManager();
            authenticated = authenticationManager.isValid(principal, credential, subject);
        }
        if (authenticated == true) {
            subjectInfo.setAuthenticatedSubject(subject);
        }

        return authenticated;
    }

    // TODO - Base on configuration.
    // Also the spec requires a container representation of an unauthenticated identity so providing
    // at least a default is not optional.
    private Identity getUnauthenticatedIdentity() {
        return new SimpleIdentity("anonymous");
    }

    /**
     * Must be called from within a privileged action.
     */
    public void pop() {
        final SecurityContext sc = contexts.pop();
        SecurityContextAssociation.setSecurityContext(sc);
    }

    /**
     * Returns the alias role names for the passed <code>roleName</code>. Returns an empty set if the passed
     * role name doesn't have any aliases
     *
     * @param roleName The role name
     * @param roleLinks The role link map where the key is a alias role name and the value is the collection of
     *                  role names, that alias represents
     * @return
     */
    private Set<String> getRoleAliases(final String roleName, final Map<String, Collection<String>> roleLinks) {
        if (roleLinks == null || roleLinks.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> aliases = new HashSet<String>();
        for (final Map.Entry<String, Collection<String>> roleLinkEntry : roleLinks.entrySet()) {
            final String aliasRoleName = roleLinkEntry.getKey();
            final Collection<String> realRoleNames = roleLinkEntry.getValue();
            if (realRoleNames != null && realRoleNames.contains(roleName)) {
                aliases.add(aliasRoleName);
            }
        }
        return aliases;
    }
}
