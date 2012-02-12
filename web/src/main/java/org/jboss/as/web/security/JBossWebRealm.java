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

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.CacheableManager;
import org.jboss.security.CertificatePrincipal;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.audit.AuditEvent;
import org.jboss.security.audit.AuditLevel;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;
import org.jboss.security.auth.callback.DigestCallbackHandler;
import org.jboss.security.auth.certs.SubjectDNMapping;
import org.jboss.security.authorization.ResourceKeys;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.javaee.AbstractWebAuthorizationHelper;
import org.jboss.security.javaee.SecurityHelperFactory;
import org.jboss.security.mapping.MappingContext;
import org.jboss.security.mapping.MappingManager;
import org.jboss.security.mapping.MappingType;

/**
 * A {@code RealmBase} implementation
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 */
public class JBossWebRealm extends RealmBase {

    protected static final String name = "JBossWebRealm";

    /**
     * The {@code AuditManager} instance that can audit security events
     */
    protected AuditManager auditManager = null;

    /**
     * The {@code AuthenticationManager} instance that can perform authentication
     */
    protected AuthenticationManager authenticationManager = null;

    /**
     * The {@code AuthorizationManager} instance that is used for authorization as well as get roles
     */
    protected AuthorizationManager authorizationManager = null;

    /**
     * The {@code MappingManager} instance to perform principal, role, attribute and credential mapping
     */
    protected MappingManager mappingManager = null;

    /**
     * The converter from X509 certificate chain to Principal
     */
    protected CertificatePrincipal certMapping = new SubjectDNMapping();

    /**
     * The {@code DeploymentUnit} associated with the Realm
     */
    protected DeploymentUnit deploymnetUnit;

    /**
     * MetaData associated with the DeploymentUnit
     */
    protected JBossWebMetaData metaData;

    /**
     * JBoss specific role mapping set in the MetaData
     */
    protected Map<String, Set<String>> principalVersusRolesMap;

    /**
     * Is JBoss authorization framework enabled?
     */
    protected boolean useJBossAuthorization = false;

    /**
     * Is Audit disabled?
     */
    protected boolean disableAudit = false;

    /**
     * Set the {@code AuthenticationManager}
     *
     * @param authenticationManager
     */
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Set the {@code AuditManager}
     *
     * @param auditManager
     */
    public void setAuditManager(AuditManager auditManager) {
        this.auditManager = auditManager;
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
     * Set the {@code MappingManager}
     *
     * @param mappingManager
     */
    public void setMappingManager(MappingManager mappingManager) {
        this.mappingManager = mappingManager;
    }

    /**
     * Set the {@code DeploymentUnit}
     *
     * @param deploymentUnit
     */
    public void setDeploymentUnit(DeploymentUnit deploymentUnit) {
        this.deploymnetUnit = deploymentUnit;
        metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY).getMergedJBossWebMetaData();
        principalVersusRolesMap = metaData.getSecurityRoles().getPrincipalVersusRolesMap();
        useJBossAuthorization = metaData.isUseJBossAuthorization();
        disableAudit = metaData.isDisableAudit();
    }

    /**
     * Returns the principal versus roles map
     *
     * @return map
     */
    public Map<String, Set<String>> getPrincipalVersusRolesMap() {
        return principalVersusRolesMap;
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        if (username == null && credentials == null)
            return null;

        if (authenticationManager == null)
            throw new IllegalStateException("Authentication Manager has not been set");
        if (authorizationManager == null)
            throw new IllegalStateException("Authorization Manager has not been set");

        Principal userPrincipal = getPrincipal(username);
        Subject subject = new Subject();
        try {
            boolean isValid = authenticationManager.isValid(userPrincipal, credentials, subject);
            if (isValid) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("User: " + userPrincipal + " is authenticated");
                SecurityContext sc = SecurityActions.getSecurityContext();
                if (sc == null)
                    throw new IllegalStateException("No SecurityContext found!");
                userPrincipal = getPrincipal(subject);
                sc.getUtil().createSubjectInfo(userPrincipal, credentials, subject);
                SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
                if (mappingManager != null) {
                    // if there are mapping modules let them handle the role mapping
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc != null && mc.hasModules()) {
                        SecurityRolesAssociation.setSecurityRoles(principalVersusRolesMap);
                    }
                }
                RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
                List<Role> rolesAsList = roles.getRoles();
                List<String> rolesAsStringList = new ArrayList<String>();
                for (Role role : rolesAsList) {
                    rolesAsStringList.add(role.getRoleName());
                }
                if (mappingManager != null) {
                    // if there are no mapping modules handle role mapping here
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc == null || !mc.hasModules()) {
                        rolesAsStringList = mapUserRoles(rolesAsStringList);
                    }
                } else
                    // if mapping manager is not set, handle role mapping here too
                    rolesAsStringList = mapUserRoles(rolesAsStringList);
                if (authenticationManager instanceof CacheableManager) {
                    @SuppressWarnings("unchecked")
                    CacheableManager<?, Principal> cm = (CacheableManager<?, Principal>) authenticationManager;
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, credentials, cm, subject);
                    successAudit(userPrincipal, null);
                    return userPrincipal;
                } else {
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, credentials, null, subject);
                    successAudit(userPrincipal, null);
                    return userPrincipal;
                }
            }
        } catch (Exception e) {
            WebLogger.WEB_SECURITY_LOGGER.authenticateError(e);
            userPrincipal = null;
            exceptionAudit(userPrincipal, null, e);
        }

        userPrincipal = super.authenticate(username, credentials);
        if (userPrincipal != null) {
            successAudit(userPrincipal, null);
        } else {
            failureAudit(userPrincipal, null);
        }
        return userPrincipal;
    }

    @Override
    public Principal authenticate(X509Certificate[] certs) {
        if ((certs == null) || (certs.length < 1))
            return (null);
        if (authenticationManager == null)
            throw MESSAGES.noAuthenticationManager();
        if (authorizationManager == null)
            throw MESSAGES.noAuthorizationManager();

        Principal userPrincipal = null;
        try {
            userPrincipal = certMapping.toPrincipal(certs);
            Subject subject = new Subject();
            boolean isValid = authenticationManager.isValid(userPrincipal, certs, subject);
            if (isValid) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("User: " + userPrincipal + " is authenticated");
                SecurityContext sc = SecurityActions.getSecurityContext();
                if (sc == null)
                    throw new IllegalStateException("No SecurityContext found!");
                userPrincipal = getPrincipal(subject);
                sc.getUtil().createSubjectInfo(userPrincipal, certs, subject);
                SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
                if (mappingManager != null) {
                    // if there are mapping modules let them handle the role mapping
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc != null && mc.hasModules()) {
                        SecurityRolesAssociation.setSecurityRoles(principalVersusRolesMap);
                    }
                }
                RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
                List<Role> rolesAsList = roles.getRoles();
                List<String> rolesAsStringList = new ArrayList<String>();
                for (Role role : rolesAsList) {
                    rolesAsStringList.add(role.getRoleName());
                }
                if (mappingManager != null) {
                    // if there are no mapping modules handle role mapping here
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc == null || !mc.hasModules()) {
                        rolesAsStringList = mapUserRoles(rolesAsStringList);
                    }
                } else
                    // if mapping manager is not set, handle role mapping here too
                    rolesAsStringList = mapUserRoles(rolesAsStringList);
                if (authenticationManager instanceof CacheableManager) {
                    @SuppressWarnings("unchecked")
                    CacheableManager<?, Principal> cm = (CacheableManager<?, Principal>) authenticationManager;
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, certs, cm, subject);
                } else
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, certs, null, subject);
            } else {
                WebLogger.WEB_SECURITY_LOGGER.tracef("User: " + userPrincipal + " is NOT authenticated");
                userPrincipal = null;
            }
        } catch (Exception e) {
            WebLogger.WEB_SECURITY_LOGGER.authenticateErrorCert(e);
            exceptionAudit(userPrincipal, null, e);
        }

        if (userPrincipal != null) {
            successAudit(userPrincipal, null);
        }

        return userPrincipal;
    }

    @Override
    public Principal authenticate(String username, byte[] credentials) {
        return authenticate(username, new String(credentials));
    }

    @Override
    public Principal authenticate(String username, String clientDigest, String nOnce, String nc, String cnonce, String qop,
            String realm, String md5a2) {
        if (authenticationManager == null)
            throw MESSAGES.noAuthenticationManager();
        if (authorizationManager == null)
            throw MESSAGES.noAuthorizationManager();
        Principal userPrincipal = null;
        SecurityContext sc = SecurityActions.getSecurityContext();
        if (sc == null)
            throw MESSAGES.noSecurityContext();
        Principal caller = sc.getUtil().getUserPrincipal();
        if (caller == null && username == null && clientDigest == null) {
            return null;
        }
        try {
            DigestCallbackHandler handler = new DigestCallbackHandler(username, nOnce, nc, cnonce, qop, realm, md5a2);
            CallbackHandlerPolicyContextHandler.setCallbackHandler(handler);
            userPrincipal = getPrincipal(username);
            Subject subject = new Subject();
            boolean isValid = authenticationManager.isValid(userPrincipal, clientDigest, subject);
            if (isValid) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("User: " + userPrincipal + " is authenticated");
                userPrincipal = getPrincipal(subject);
                sc.getUtil().createSubjectInfo(userPrincipal, clientDigest, subject);
                SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
                if (mappingManager != null) {
                    // if there are mapping modules let them handle the role mapping
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc != null && mc.hasModules()) {
                        SecurityRolesAssociation.setSecurityRoles(principalVersusRolesMap);
                    }
                }
                RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
                List<Role> rolesAsList = roles.getRoles();
                List<String> rolesAsStringList = new ArrayList<String>();
                for (Role role : rolesAsList) {
                    rolesAsStringList.add(role.getRoleName());
                }
                if (mappingManager != null) {
                    // if there are no mapping modules handle role mapping here
                    MappingContext<RoleGroup> mc = mappingManager.getMappingContext(MappingType.ROLE.name());
                    if (mc == null || !mc.hasModules()) {
                        rolesAsStringList = mapUserRoles(rolesAsStringList);
                    }
                } else
                    // if mapping manager is not set, handle role mapping here too
                    rolesAsStringList = mapUserRoles(rolesAsStringList);
                if (authenticationManager instanceof CacheableManager) {
                    @SuppressWarnings("unchecked")
                    CacheableManager<?, Principal> cm = (CacheableManager<?, Principal>) authenticationManager;
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, clientDigest, cm, subject);
                } else
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null, clientDigest, null, subject);
            } else {
                WebLogger.WEB_SECURITY_LOGGER.tracef("User: " + userPrincipal + " is NOT authenticated");
                userPrincipal = null;
            }
        } catch (Exception e) {
            WebLogger.WEB_SECURITY_LOGGER.authenticateErrorDigest(e);
        }

        if (userPrincipal != null) {
            successAudit(userPrincipal, null);
        } else {
            failureAudit(userPrincipal, null);
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

    protected List<String> mapUserRoles(List<String> rolesList) {
        if (principalVersusRolesMap != null && principalVersusRolesMap.size() > 0) {
            List<String> mappedRoles = new ArrayList<String>();
            for (String role : rolesList) {
                Set<String> roles = principalVersusRolesMap.get(role);
                if (roles != null && roles.size() > 0) {
                    for (String r : roles) {
                        if (!mappedRoles.contains(r))
                            mappedRoles.add(r);
                    }
                } else {
                    if (!mappedRoles.contains(role))
                        mappedRoles.add(role);
                }
            }
            return mappedRoles;
        }

        return rolesList;
    }

    /**
     * Get the Principal given the authenticated Subject. Currently the first principal that is not of type {@code Group} is
     * considered or the single principal inside the CallerPrincipal group.
     *
     * @param subject
     * @return the authenticated principal
     */
    protected Principal getPrincipal(Subject subject) {
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
                        if (g.getName().equals(SecurityConstants.CALLER_PRINCIPAL_GROUP) && callerPrincipal == null) {
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

    @Override
    public boolean hasResourcePermission(Request request, Response response, SecurityConstraint[] constraints, Context context)
            throws IOException {
        boolean authzDecision = true;
        boolean baseDecision = super.hasResourcePermission(request, response, constraints, context);

        // if the RealmBase check has passed, then we can go to authz framework
        if (baseDecision && useJBossAuthorization) {
            SecurityContext sc = SecurityActions.getSecurityContext();
            Subject caller = sc.getUtil().getSubject();
            if (caller == null)
                caller = getSubjectFromRequestPrincipal(request.getPrincipal());
            Map<String, Object> contextMap = new HashMap<String, Object>();
            contextMap.put(ResourceKeys.RESOURCE_PERM_CHECK, Boolean.TRUE);
            contextMap.put("securityConstraints", constraints);

            AbstractWebAuthorizationHelper helper = null;
            try {
                helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
            } catch (Exception e) {
                WebLogger.WEB_SECURITY_LOGGER.noAuthorizationHelper(e);
                return false;
            }

            authzDecision = helper.checkResourcePermission(contextMap, request, response, caller, PolicyContext.getContextID(),
                    requestURI(request), getPrincipalRoles(request));
        }
        boolean finalDecision = baseDecision && authzDecision;
        WebLogger.WEB_SECURITY_LOGGER.tracef("hasResourcePermission:RealmBase says:" + baseDecision + "::Authz framework says:" + authzDecision
                + ":final=" + finalDecision);
        if (!finalDecision) {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasResourcePermission");
                failureAudit(request.getUserPrincipal(), entries);
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, sm.getString("realmBase.forbidden"));
        } else {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasResourcePermission");
                successAudit(request.getUserPrincipal(), entries);
            }
        }
        return finalDecision;
    }

    @Override
    public boolean hasRole(Principal principal, String role) {
        boolean authzDecision = true;
        boolean baseDecision = super.hasRole(principal, role);
        // if the RealmBase check has passed, then we can go to authz framework
        if (baseDecision && useJBossAuthorization) {
            Request request = SecurityContextAssociationValve.getActiveRequest();
            String servletName = null;
            Wrapper servlet = request.getWrapper();
            if (servlet != null) {
                servletName = getServletName(servlet);
            }
            if (servletName == null)
                throw new IllegalStateException("servletName is null");
            String roleName = role;
            ServletMetaData servletMD = metaData.getServlets().get(servletName);
            SecurityRoleRefsMetaData roleRefs = null;
            if (servletMD != null)
                roleRefs = servletMD.getSecurityRoleRefs();
            if (roleRefs != null) {
                for (SecurityRoleRefMetaData ref : roleRefs) {
                    if (ref.getRoleLink().equals(role)) {
                        roleName = ref.getName();
                        break;
                    }
                }
            }

            SecurityContext sc = SecurityActions.getSecurityContext();
            AbstractWebAuthorizationHelper helper = null;
            try {
                helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
            } catch (Exception e) {
                WebLogger.WEB_SECURITY_LOGGER.noAuthorizationHelper(e);
            }
            Subject callerSubject = sc.getUtil().getSubject();
            if (callerSubject == null) {
                // During hasResourcePermission check, Catalina calls hasRole. But we have not established
                // a subject yet in the security context. So we will get the subject from the cached principal
                callerSubject = getSubjectFromRequestPrincipal(principal);
            }

            authzDecision = helper.hasRole(roleName, principal, servletName, getPrincipalRoles(principal),
                    PolicyContext.getContextID(), callerSubject, getPrincipalRoles(request));
        }
        boolean finalDecision = baseDecision && authzDecision;
        WebLogger.WEB_SECURITY_LOGGER.tracef("hasRole:RealmBase says:" + baseDecision + "::Authz framework says:" + authzDecision + ":final="
                + finalDecision);
        if (finalDecision) {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasRole");
                successAudit(principal, entries);
            }
        } else {
            if (!disableAudit) {
                Map<String, Object> entries = new HashMap<String, Object>();
                entries.put("Step", "hasRole");
                failureAudit(principal, entries);
            }
        }

        return finalDecision;
    }

    @Override
    public boolean hasUserDataPermission(Request request, Response response, SecurityConstraint[] constraints)
            throws IOException {
        boolean ok = super.hasUserDataPermission(request, response, constraints);
        // if the RealmBase check has passed, then we can go to authz framework
        if (ok && useJBossAuthorization) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("securityConstraints", constraints);
            map.put(ResourceKeys.USERDATA_PERM_CHECK, Boolean.TRUE);

            SecurityContext sc = SecurityActions.getSecurityContext();
            AbstractWebAuthorizationHelper helper = null;
            try {
                helper = SecurityHelperFactory.getWebAuthorizationHelper(sc);
            } catch (Exception e) {
                WebLogger.WEB_SECURITY_LOGGER.noAuthorizationHelper(e);
            }

            Subject callerSubject = sc.getUtil().getSubject();
            // JBAS-6419:CallerSubject has no bearing on the user data permission check
            if (callerSubject == null)
                callerSubject = new Subject();

            ok = helper.hasUserDataPermission(map, request, response, PolicyContext.getContextID(), callerSubject,
                    getPrincipalRoles(request));
        }

        return ok;
    }

    /**
     * Retrieve the Subject stored in the Principal
     *
     * @param principal the Principal present in the Request
     * @return the authenticated Subject
     */
    protected Subject getSubjectFromRequestPrincipal(Principal principal) {
        Subject subject = null;
        if (principal instanceof JBossGenericPrincipal) {
            subject = JBossGenericPrincipal.class.cast(principal).getSubject();
        }
        return subject;
    }

    /**
     * Access the set of role Principals associated with the given caller principal.
     *
     * @param principal - the Principal mapped from the authentication principal and visible from the
     *        HttpServletRequest.getUserPrincipal
     * @return a possible null Set<Principal> for the caller roles
     */
    protected Set<Principal> getPrincipalRoles(Principal principal) {
        if (!(principal instanceof GenericPrincipal))
            throw MESSAGES.illegalPrincipalType(principal.getClass());
        GenericPrincipal gp = GenericPrincipal.class.cast(principal);
        String[] roleNames = gp.getRoles();
        Set<Principal> userRoles = new HashSet<Principal>();
        if (roleNames != null) {
            for (int n = 0; n < roleNames.length; n++) {
                Principal sp = getPrincipal(roleNames[n]);
                userRoles.add(sp);
            }
        }
        return userRoles;
    }

    /**
     * Get the roles that is stored in the authenticated {@link GenericPrincipal}
     *
     * @param request
     * @return
     */
    protected List<String> getPrincipalRoles(Request request) {
        List<String> roles = null;
        Principal principal = request.getPrincipal();
        if (principal != null) {
            if (principal instanceof GenericPrincipal) {
                GenericPrincipal gc = GenericPrincipal.class.cast(principal);
                roles = Arrays.asList(gc.getRoles());
            }
        }

        return roles;
    }

    /**
     * Get the canonical request URI from the request mapping data requestPath
     *
     * @param request
     * @return the request URI path
     */
    protected String requestURI(Request request) {
        String uri = request.getMappingData().requestPath.getString();
        if (uri == null || uri.equals("/")) {
            uri = "";
        }
        return uri;
    }

    /**
     * Jacc Specification : Appendix B.19 Calling isUserInRole from JSP not mapped to a Servlet Checking a WebRoleRefPermission
     * requires the name of a Servlet to identify the scope of the reference to role translation. The name of a scoping servlet
     * has not been established for an unmapped JSP.
     *
     * Resolution- For every security role in the web application add a WebRoleRefPermission to the corresponding role. The name
     * of all such permissions shall be the empty string, and the actions of each permission shall be the corresponding role
     * name. When checking a WebRoleRefPermission from a JSP not mapped to a servlet, use a permission with the empty string as
     * its name and with the argument to is UserInRole as its actions.
     *
     * @param servlet Wrapper
     * @return empty string if it is for an unmapped jsp or name of the servlet for others
     */
    private String getServletName(Wrapper servlet) {
        // For jsp, the mapping will be (*.jsp, *.jspx)
        String[] mappings = servlet.findMappings();
        WebLogger.WEB_SECURITY_LOGGER.tracef("[getServletName:servletmappings=" + mappings + ":servlet.getName()=" + servlet.getName() + "]");
        if ("jsp".equals(servlet.getName()) && (mappings != null && mappings[0].indexOf("*.jsp") > -1))
            return "";
        else
            return servlet.getName();
    }

    /**
     * Get the original http request using the JACC mandated api
     *
     * @return
     */
    private HttpServletRequest getServletRequest() {
        try {
            return (HttpServletRequest) PolicyContext.getContext(SecurityConstants.WEB_REQUEST_KEY);
        } catch (Exception e) {
            WebLogger.WEB_SECURITY_LOGGER.tracef("Exception in getting servlet request:", e);
        }
        return null;
    }

    // Audit Methods
    private void successAudit(Principal userPrincipal, Map<String, Object> entries) {
        if (userPrincipal != null && !disableAudit) {
            if (auditManager != null) {
                AuditEvent auditEvent = new AuditEvent(AuditLevel.SUCCESS);
                Map<String, Object> ctxMap = new HashMap<String, Object>();
                ctxMap.put("principal", userPrincipal);
                HttpServletRequest hsr = getServletRequest();
                if (hsr != null) {
                    ctxMap.put("request", WebUtil.deriveUsefulInfo(hsr));
                }
                ctxMap.put("Source", getClass().getCanonicalName());
                if (entries != null) {
                    ctxMap.putAll(entries);
                }
                auditEvent.setContextMap(ctxMap);
                auditManager.audit(auditEvent);
            }
        }
    }

    private void failureAudit(Principal userPrincipal, Map<String, Object> entries) {
        if (auditManager != null && !disableAudit) {
            AuditEvent auditEvent = new AuditEvent(AuditLevel.FAILURE);
            Map<String, Object> ctxMap = new HashMap<String, Object>();
            ctxMap.put("principal", userPrincipal);
            HttpServletRequest hsr = getServletRequest();
            if (hsr != null) {
                ctxMap.put("request", WebUtil.deriveUsefulInfo(hsr));
            }
            ctxMap.put("Source", getClass().getCanonicalName());
            if (entries != null) {
                ctxMap.putAll(entries);
            }
            auditEvent.setContextMap(ctxMap);
            auditManager.audit(auditEvent);
        }
    }

    private void exceptionAudit(Principal userPrincipal, Map<String, Object> entries, Exception e) {
        if (auditManager != null && !disableAudit) {
            AuditEvent auditEvent = new AuditEvent(AuditLevel.ERROR);
            Map<String, Object> ctxMap = new HashMap<String, Object>();
            ctxMap.put("principal", userPrincipal);
            ctxMap.putAll(entries);
            HttpServletRequest hsr = getServletRequest();
            if (hsr != null) {
                ctxMap.put("request", WebUtil.deriveUsefulInfo(hsr));
            }
            ctxMap.put("source", getClass().getCanonicalName());
            if (entries != null) {
                ctxMap.putAll(entries);
            }
            auditEvent.setContextMap(ctxMap);
            auditEvent.setUnderlyingException(e);
            auditManager.audit(auditEvent);
        }
    }
}
