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

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

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
public final class SecurityDomainContext {

    private final AuthenticationManager authenticationMgr;
    private final AuthorizationManager authorizationMgr;
    private final AuditManager auditMgr;
    private final MappingManager mappingMgr;
    private final IdentityTrustManager identityTrustMgr;
    private final JSSESecurityDomain jsseSecurityDomain;

    private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";

    public SecurityDomainContext(AuthenticationManager authenticationMgr,
                                 AuthorizationManager authorizationMgr,
                                 AuditManager auditMgr,
                                 IdentityTrustManager identityTrustMgr, MappingManager mappingMgr,
                                 JSSESecurityDomain jsseSecurityDomain) {
        this.authenticationMgr = authenticationMgr;
        this.authorizationMgr = authorizationMgr;
        this.auditMgr = auditMgr;
        this.mappingMgr = mappingMgr;
        this.identityTrustMgr = identityTrustMgr;
        this.jsseSecurityDomain = jsseSecurityDomain;
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

    public AuthorizationManager getAuthorizationManager() {
        return authorizationMgr;
    }

    public AuditManager getAuditManager() {
        return auditMgr;
    }

    public MappingManager getMappingManager() {
        return mappingMgr;
    }

    public IdentityTrustManager getIdentityTrustManager() {
        return identityTrustMgr;
    }

    public JSSESecurityDomain getJSSE() {
        return jsseSecurityDomain;
    }
}
