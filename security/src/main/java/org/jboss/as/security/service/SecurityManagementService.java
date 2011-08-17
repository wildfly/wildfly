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

package org.jboss.as.security.service;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.plugins.JNDIBasedSecurityManagement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.ISecurityManagement;

/**
 * Security Management service for the security container
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityManagementService implements Service<ISecurityManagement> {

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("security-management");

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    private final String authenticationManagerClassName;

    private final boolean deepCopySubjectMode;

    private final String callbackHandlerClassName;

    private final String authorizationManagerClassName;

    private final String auditManagerClassName;

    private final String identityTrustManagerClassName;

    private final String mappingManagerClassName;

    private volatile ISecurityManagement securityManagement;

    public SecurityManagementService(String authenticationManagerClassName, boolean deepCopySubjectMode,
            String callbackHandlerClassName, String authorizationManagerClassName, String auditManagerClassName,
            String identityTrustManagerClassName, String mappingManagerClassName) {
        this.authenticationManagerClassName = authenticationManagerClassName;
        this.deepCopySubjectMode = deepCopySubjectMode;
        this.callbackHandlerClassName = callbackHandlerClassName;
        this.authorizationManagerClassName = authorizationManagerClassName;
        this.auditManagerClassName = auditManagerClassName;
        this.identityTrustManagerClassName = identityTrustManagerClassName;
        this.mappingManagerClassName = mappingManagerClassName;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        if (log.isDebugEnabled())
            log.debug("Starting SecurityManagementService");
        // set properties of JNDIBasedSecurityManagement
        JNDIBasedSecurityManagement securityManagement = new JNDIBasedSecurityManagement();
        securityManagement.setAuthenticationManagerClassName(authenticationManagerClassName);
        securityManagement.setDeepCopySubjectMode(deepCopySubjectMode);
        securityManagement.setCallbackHandlerClassName(callbackHandlerClassName);
        securityManagement.setAuthorizationManagerClassName(authorizationManagerClassName);
        securityManagement.setAuditManagerClassName(auditManagerClassName);
        securityManagement.setIdentityTrustManagerClassName(identityTrustManagerClassName);
        securityManagement.setMappingManagerClassName(mappingManagerClassName);
        this.securityManagement = securityManagement;
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public ISecurityManagement getValue() throws IllegalStateException {
        return securityManagement;
    }

}
