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
import java.security.acl.Group;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.catalina.realm.RealmBase;
import org.jboss.logging.Logger;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.CacheableManager;
import org.jboss.security.CertificatePrincipal;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.certs.SubjectDNMapping;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.mapping.MappingContext;
import org.jboss.security.mapping.MappingManager;
import org.jboss.security.mapping.MappingType;

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
     * The {@code MappingManager} instance to perform principal, role, attribute and credential mapping
     */
    protected MappingManager mappingManager = null;

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
     * Set the {@code MappingManager}
     *
     * @param mappingManager
     */
    public void setMappingManager(MappingManager mappingManager) {
        this.mappingManager = mappingManager;
    }

    /**
     * The converter from X509 certificate chain to Principal
     */
    protected CertificatePrincipal certMapping = new SubjectDNMapping();

    protected Map<String, Set<String>> principalVersusRolesMap;

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
        boolean isValid = authenticationManager.isValid(userPrincipal, credentials, subject);
        if (isValid) {
            if (log.isTraceEnabled()) {
                log.trace("User: " + userPrincipal + " is authenticated");
            }
            SecurityContext sc = SecurityActions.createSecurityContext(authenticationManager.getSecurityDomain());
            userPrincipal = getPrincipal(subject);
            sc.getUtil().createSubjectInfo(userPrincipal, credentials, subject);
            SecurityActions.setSecurityContextOnAssociation(sc);
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
                return new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList, userPrincipal, null,
                        cm);
            } else
                return new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList, userPrincipal, null);
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
                userPrincipal = getPrincipal(subject);
                sc.getUtil().createSubjectInfo(userPrincipal, certs, subject);
                SecurityActions.setSecurityContextOnAssociation(sc);
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
                            userPrincipal, null, cm);
                } else
                    userPrincipal = new JBossGenericPrincipal(this, userPrincipal.getName(), null, rolesAsStringList,
                            userPrincipal, null);
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

    public Map<String, Set<String>> getPrincipalVersusRolesMap() {
        return principalVersusRolesMap;
    }

    public void setPrincipalVersusRolesMap(Map<String, Set<String>> principalVersusRolesMap) {
        this.principalVersusRolesMap = principalVersusRolesMap;
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
}