/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.jboss.logging.Logger;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.CertificatePrincipal;
import org.jboss.security.SecurityContext;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.certs.SubjectDNMapping;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;

/**
 * A {@code RealmBase} implementation
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 14, 2011
 */
public class JBossWebRealm extends RealmBase {

    private static Logger log = Logger.getLogger(JBossWebRealm.class);

    protected static final String name = "JBossWebRealm";

    /**
     * The {@code AuthenticationManager} instance that can perform authentication
     */
    protected AuthenticationManager authenticationManager = null;

    /**
     * The {@code AuthorizationManager} instance that is used for authorization as well as get roles
     */
    protected AuthorizationManager authorizationManager = null;

    /**
     * Set the {@code AuthenticationManager}
     *
     * @param authenticationManager
     */
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Set the {@code AuthorizationManager}
     *
     * @param authorizationManager
     */
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    /**
     * The converter from X509 certificate chain to Principal
     */
    protected CertificatePrincipal certMapping = new SubjectDNMapping();

    @Override
    public Principal authenticate(String username, String credentials) {
        if (username == null && credentials == null)
            return null;

        if (authenticationManager == null)
            throw new IllegalStateException("Authentication Manager has not been set");
        if (authorizationManager == null)
            throw new IllegalStateException("Authorization Manager has not been set");

        Principal userPrincipal = new SimplePrincipal(username);
        Subject subject = new Subject();
        boolean isValid = authenticationManager.isValid(userPrincipal, credentials, subject);
        if (isValid) {
            if (log.isTraceEnabled()) {
                log.trace("User: " + userPrincipal + " is authenticated");
            }
            SecurityContext sc = SecurityActions.createSecurityContext(authenticationManager.getSecurityDomain());
            sc.getUtil().createSubjectInfo(userPrincipal, credentials, subject);
            SecurityActions.setSecurityContextOnAssociation(sc);
            SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
            RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
            List<Role> rolesAsList = roles.getRoles();
            List<String> rolesAsStringList = new ArrayList<String>();
            for (Role role : rolesAsList) {
                rolesAsStringList.add(role.getRoleName());
            }
            return new GenericPrincipal(this, username, credentials, rolesAsStringList);
        }

        return super.authenticate(username, credentials);
    }

    @Override
    public Principal authenticate(X509Certificate[] certs) {
        if ((certs == null) || (certs.length < 1))
            return (null);
        if (authenticationManager == null)
            throw new IllegalStateException("Authentication Manager has not been set");
        if (authorizationManager == null)
            throw new IllegalStateException("Authorization Manager has not been set");

        Principal userPrincipal = null;
        try {
            userPrincipal = certMapping.toPrincipal(certs);
            Subject subject = new Subject();
            boolean isValid = authenticationManager.isValid(userPrincipal, certs, subject);
            if (isValid) {
                if (log.isTraceEnabled()) {
                    log.trace("User: " + userPrincipal + " is authenticated");
                }
                SecurityContext sc = SecurityActions.createSecurityContext(authenticationManager.getSecurityDomain());
                sc.getUtil().createSubjectInfo(userPrincipal, certs, subject);
                SecurityActions.setSecurityContextOnAssociation(sc);
                SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
                RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
                List<Role> rolesAsList = roles.getRoles();
                List<String> rolesAsStringList = new ArrayList<String>();
                for (Role role : rolesAsList) {
                    rolesAsStringList.add(role.getRoleName());
                }
                userPrincipal = new GenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("User: " + userPrincipal + " is NOT authenticated");
                }
                userPrincipal = null;
            }
        } catch (Exception e) {
            log.error("Error during authenticate(X509Certificate[])");
        }

        return userPrincipal;
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String getPassword(String username) {
        return null;
    }

    @Override
    protected Principal getPrincipal(String username) {
        return new SimplePrincipal(username);
    }
}