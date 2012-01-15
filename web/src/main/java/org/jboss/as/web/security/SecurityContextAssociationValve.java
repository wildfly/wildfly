/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.security.Principal;

import javax.security.jacc.PolicyContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.SecurityUtil;

/**
 * A {@code Valve} that creates a {@code SecurityContext} if one doesn't exist and sets the security information based on the
 * authenticated principal in the request's session.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Thomas.Diesler@jboss.org
 * @author Anil.Saldhana@jboss.org
 */
public class SecurityContextAssociationValve extends ValveBase {

    private static final Logger log = Logger.getLogger(SecurityContextAssociationValve.class);

    private final DeploymentUnit deploymentUnit;

    private static final ThreadLocal<Request> activeRequest = new ThreadLocal<Request>();

    public SecurityContextAssociationValve(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    /** {@inheritDoc} */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
        activeRequest.set(request);

        Session session = null;
        // Get the request caller which could be set due to SSO
        Principal caller = request.getPrincipal();
        // The cached web container principal
        JBossGenericPrincipal principal = null;
        HttpSession hsession = request.getSession(false);

        log.tracef("Begin invoke, caller=" + caller);

        boolean createdSecurityContext = false;
        SecurityContext sc = SecurityActions.getSecurityContext();
        if (sc == null) {
            createdSecurityContext = true;
            String securityDomain = SecurityUtil.unprefixSecurityDomain(metaData.getSecurityDomain());
            if (securityDomain == null)
                securityDomain = SecurityConstants.DEFAULT_WEB_APPLICATION_POLICY;
            sc = SecurityActions.createSecurityContext(securityDomain);
            SecurityActions.setSecurityContextOnAssociation(sc);
        }

        try {
            Wrapper servlet = null;
            try {
                servlet = request.getWrapper();
                if (servlet != null) {
                    String name = servlet.getName();
                    RunAsIdentityMetaData identity = metaData.getRunAsIdentity(name);
                    RunAsIdentity runAsIdentity = null;
                    if (identity != null) {
                        log.tracef(name + ", runAs: " + identity);
                        runAsIdentity = new RunAsIdentity(identity.getRoleName(), identity.getPrincipalName(),
                                identity.getRunAsRoles());
                    }
                    SecurityActions.pushRunAsIdentity(runAsIdentity);
                }

                // If there is a session, get the tomcat session for the principal
                Manager manager = container.getManager();
                if (manager != null && hsession != null) {
                    try {
                        session = manager.findSession(hsession.getId());
                    } catch (IOException ignore) {
                    }
                }

                if (caller == null || !(caller instanceof JBossGenericPrincipal)) {
                    // Look to the session for the active caller security context
                    if (session != null) {
                        principal = (JBossGenericPrincipal) session.getPrincipal();
                    }
                    if (principal == null) {
                        Session sessionInternal = request.getSessionInternal(false);
                        if (sessionInternal != null) {
                           principal = (JBossGenericPrincipal) sessionInternal.getNote(Constants.FORM_PRINCIPAL_NOTE);
                        }
                    }
                } else {
                    // Use the request principal as the caller identity
                    principal = (JBossGenericPrincipal) caller;
                }

                // If there is a caller use this as the identity to propagate
                if (principal != null) {
                    log.tracef("Restoring principal info from cache");
                    if (createdSecurityContext) {
                        sc.getUtil().createSubjectInfo(principal.getUserPrincipal(), principal.getCredentials(),
                                principal.getSubject());
                    }
                }
            } catch (Throwable e) {
                //TODO:decide whether to log this as info or warn
                log.debug("Failed to determine servlet", e);
            }
            // set JACC contextID
            PolicyContext.setContextID(deploymentUnit.getName());

            // Perform the request
            getNext().invoke(request, response);
            if (servlet != null) {
                SecurityActions.popRunAsIdentity();
            }
        } finally {
            log.tracef("End invoke, caller=" + caller);
            SecurityActions.clearSecurityContext();
            SecurityRolesAssociation.setSecurityRoles(null);
            PolicyContext.setContextID(null);
            activeRequest.set(null);
        }
    }

    public static Request getActiveRequest() {
        return activeRequest.get();
    }

}
