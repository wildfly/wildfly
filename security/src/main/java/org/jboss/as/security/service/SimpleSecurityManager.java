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

import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextUtil;
import org.jboss.security.SubjectInfo;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleIdentity;
import org.picketbox.factories.SecurityFactory;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.security.AccessController.doPrivileged;

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

    public Principal getCallerPrincipal() {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null)
            throw new IllegalStateException("No security context established");
        /*
        final Principal principal = getPrincipal(securityContext.getUtil().getSubject());
        */
        Principal principal = securityContext.getIncomingRunAs();
        if (principal == null)
            principal = getPrincipal(securityContext.getSubjectInfo().getAuthenticatedSubject());
        if (principal == null)
            throw new IllegalStateException("No principal available");
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
     * @param roleNames
     * @return true if the user is in any one of the roles listed
     */
    public boolean isCallerInRole(final String... roleNames) {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null)
            throw new IllegalStateException("No security context established");

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

        boolean userNotInRole = Collections.disjoint(requiredRoles, actualRoles);

        return userNotInRole == false;
    }

    /**
     * Must be called from within a privileged action.
     *
     * @param securityDomain
     * @param runAs
     * @param runAsPrincipal
     */
    public void push(final String securityDomain, final String runAs, final String runAsPrincipal) {
        // TODO - Handle a null securityDomain here?  Yes I think so.
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        contexts.push(previous);
        SecurityContext current = SecurityFactory.establishSecurityContext(securityDomain);
        if (previous != null) {
            current.setSubjectInfo(previous.getSubjectInfo());
            current.setIncomingRunAs(previous.getOutgoingRunAs());
        }

        RunAs currentRunAs = current.getIncomingRunAs();
        boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;

        // TODO - Set unauthenticated identity if no auth to occur
        if (trusted == false) {
            // If we have a trusted identity no need for a re-auth.
            boolean authenticated = authenticate(current);
            if (authenticated == false) {
                // TODO - Better type needed.
                throw new SecurityException("Invalid User");
            }
        }

        if (runAs != null) {
            RunAs runAsIdentity = new RunAsIdentity(runAs, null);
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
}
