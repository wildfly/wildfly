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

package org.jboss.as.security.plugins;

import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.security.SecurityMessages;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.identitytrust.IdentityTrustManager;
import org.jboss.security.mapping.MappingManager;

/**
 * An encapsulation of the JNDI security context information
 *
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityDomainContext {

    static final String ACTIVE_SUBJECT = "subject";
    static final String AUTHENTICATION_MGR = "authenticationMgr";
    static final String AUTHORIZATION_MGR = "authorizationMgr";
    static final String AUDIT_MGR = "auditMgr";
    static final String MAPPING_MGR = "mappingMgr";
    static final String IDENTITY_TRUST_MGR = "identityTrustMgr";
    static final String DOMAIN_CONTEXT = "domainContext";
    static final String JSSE = "jsse";

    AuthenticationManager authenticationMgr;
    AuthorizationManager authorizationMgr;
    AuditManager auditMgr;
    MappingManager mappingMgr;
    IdentityTrustManager identityTrustMgr;
    JSSESecurityDomain jsseSecurityDomain;

    private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";

    public SecurityDomainContext(AuthenticationManager authenticationMgr) {
        this.authenticationMgr = authenticationMgr;
    }

    public Object lookup(String name) throws NamingException {
        Object binding = null;
        if (name == null || name.length() == 0)
            throw SecurityMessages.MESSAGES.nullName();

        if (name.equals(ACTIVE_SUBJECT))
            binding = getSubject();
        else if (name.equals(AUTHENTICATION_MGR))
            binding = getAuthenticationManager();
        else if (name.equals(AUTHORIZATION_MGR))
            binding = getAuthorizationManager();
        else if (name.equals(AUDIT_MGR))
            binding = getAuditManager();
        else if (name.equals(MAPPING_MGR))
            binding = getMappingManager();
        else if (name.equals(IDENTITY_TRUST_MGR))
            binding = getIdentityTrustManager();
        else if (name.equals(DOMAIN_CONTEXT))
            binding = this;
        else if (name.equals(JSSE))
            binding = getJSSE();

        return binding;
    }

    public Subject getSubject() {
        Subject subject = null;
        try {
            subject = (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
        } catch (PolicyContextException pce) {
        }
        return subject;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationMgr;
    }

    public void setAuthenticationManager(AuthenticationManager am) {
        this.authenticationMgr = am;
    }

    public void setAuthorizationManager(AuthorizationManager am) {
        this.authorizationMgr = am;
    }

    public AuthorizationManager getAuthorizationManager() {
        return authorizationMgr;
    }

    public AuditManager getAuditManager() {
        return auditMgr;
    }

    public void setAuditManager(AuditManager auditMgr) {
        this.auditMgr = auditMgr;
    }

    public MappingManager getMappingManager() {
        return mappingMgr;
    }

    public void setMappingManager(MappingManager mappingMgr) {
        this.mappingMgr = mappingMgr;
    }

    public IdentityTrustManager getIdentityTrustManager() {
        return identityTrustMgr;
    }

    public void setIdentityTrustManager(IdentityTrustManager identityTrustMgr) {
        this.identityTrustMgr = identityTrustMgr;
    }

    public JSSESecurityDomain getJSSE() {
        return jsseSecurityDomain;
    }

    public void setJSSE(JSSESecurityDomain jsseSecurityDomain) {
        this.jsseSecurityDomain = jsseSecurityDomain;
    }
}
