/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.hornetq.core.security.CheckType;
import org.hornetq.core.security.Role;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SimplePrincipal;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

public class HornetQSecurityManagerAS7 implements org.hornetq.spi.core.security.HornetQSecurityManager {
    private SecurityDomainContext securityDomainContext;
    private String defaultUser = null;
    private String defaultPassword = null;

    public HornetQSecurityManagerAS7(SecurityDomainContext sdc) {
        securityDomainContext = sdc;
        defaultUser = HornetQDefaultCredentials.getUsername();
        defaultPassword = HornetQDefaultCredentials.getPassword();
    }

    @Override
    public boolean validateUser(String username, String password) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        if (securityDomainContext == null)
            throw MESSAGES.securityDomainContextNotSet();

        return securityDomainContext.getAuthenticationManager().isValid(new SimplePrincipal(username), password, new Subject());
    }

    @Override
    public boolean validateUserAndRole(String username, String password, Set<Role> roles, CheckType checkType) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        if (securityDomainContext == null)
            throw MESSAGES.securityDomainContextNotSet();

        Subject subject = new Subject();

        // The authentication call here changes the subject and that subject must be used later.  That is why we don't call validateUser(String, String) here.
        boolean authenticated = securityDomainContext.getAuthenticationManager().isValid(new SimplePrincipal(username), password, subject);

        if (authenticated) {
            SecurityContext securityContext = pushSecurityContext(subject, new SimplePrincipal(username), password);
            Set<Principal> principals = new HashSet<Principal>();
            for (Role role : roles) {
                if (checkType.hasRole(role)) {
                    principals.add(new SimplePrincipal(role.getName()));
                }
            }

            authenticated = securityDomainContext.getAuthorizationManager().doesUserHaveRole(new SimplePrincipal(username), principals);

            // restore the previous security context if any
            setSecurityContextOnAssociation(securityContext);
        }

        return authenticated;
    }

    private SecurityContext pushSecurityContext(final Subject subject, final Principal principal, final Object credential) {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            public SecurityContext run() {
                final SecurityContext currentSecurityContext = SecurityContextAssociation.getSecurityContext();
                final SecurityContext securityContext;
                if (currentSecurityContext == null) {
                    securityContext = createSecurityContext(subject, principal, credential, securityDomainContext.getAuthenticationManager().getSecurityDomain());
                } else {
                    securityContext = currentSecurityContext;
                    securityContext.getUtil().createSubjectInfo(principal, credential, subject);
                }
                setSecurityContextOnAssociation(securityContext);
                return currentSecurityContext;
            }
        });
    }

    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    private static SecurityContext createSecurityContext(final Subject subject, final Principal principal, final Object credential, final String domain) {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            @Override
            public SecurityContext run() {
                try {
                    return SecurityContextFactory.createSecurityContext(principal, credential, subject, domain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void addUser(String s, String s1) {
    }

    @Override
    public void removeUser(String s) {
    }

    @Override
    public void addRole(String s, String s1) {
    }

    @Override
    public void removeRole(String s, String s1) {
    }

    @Override
    public void setDefaultUser(String s) {
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
