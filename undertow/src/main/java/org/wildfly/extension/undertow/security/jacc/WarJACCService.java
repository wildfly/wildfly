/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security.jacc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import org.jboss.as.security.service.JaccService;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.UserDataConstraintMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;

/**
 * A service that creates JACC permissions for a web deployment
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class WarJACCService extends JaccService<WarMetaData> {

    /** A prefix pattern "/prefix/*" */
    private static final int PREFIX = 1;

    /** An extension pattern "*.ext" */
    private static final int EXTENSION = 2;

    /** The "/" default pattern */
    private static final int DEFAULT = 3;

    /** A prefix pattern "/prefix/*" */
    private static final int EXACT = 4;

    private static final String ANY_AUTHENTICATED_USER_ROLE = "**";

    public WarJACCService(String contextId, WarMetaData metaData, Boolean standalone) {
        super(contextId, metaData, standalone);
    }

    /** {@inheritDoc} */
    @Override
    public void createPermissions(WarMetaData metaData, PolicyConfiguration pc) throws PolicyContextException {

        JBossWebMetaData jbossWebMetaData = metaData.getMergedJBossWebMetaData();
        HashMap<String, PatternInfo> patternMap = qualifyURLPatterns(jbossWebMetaData);

        List<SecurityConstraintMetaData> secConstraints = jbossWebMetaData.getSecurityConstraints();

        if (secConstraints != null) {
            for (SecurityConstraintMetaData secConstraint : secConstraints) {
                WebResourceCollectionsMetaData resourceCollectionsMetaData = secConstraint.getResourceCollections();
                UserDataConstraintMetaData userDataConstraintMetaData = secConstraint.getUserDataConstraint();

                if (resourceCollectionsMetaData != null) {
                    if (secConstraint.isExcluded() || secConstraint.isUnchecked()) {
                        // Process the permissions for the excluded/unchecked resources
                        for (WebResourceCollectionMetaData resourceCollectionMetaData : resourceCollectionsMetaData) {
                            List<String> httpMethods = new ArrayList<>(resourceCollectionMetaData.getHttpMethods());
                            List<String> ommisions = resourceCollectionMetaData.getHttpMethodOmissions();
                            if(httpMethods.isEmpty() && !ommisions.isEmpty()) {
                                httpMethods.addAll(WebResourceCollectionMetaData.ALL_HTTP_METHODS);
                                httpMethods.removeAll(ommisions);
                            }
                            List<String> urlPatterns = resourceCollectionMetaData.getUrlPatterns();
                            for (String urlPattern : urlPatterns) {
                                PatternInfo info = patternMap.get(urlPattern);
                                info.descriptor=true;
                                // Add the excluded methods
                                if (secConstraint.isExcluded()) {
                                    info.addExcludedMethods(httpMethods);
                                }

                                // SECURITY-63: Missing auth-constraint needs unchecked policy
                                if (secConstraint.isUnchecked() && httpMethods.isEmpty()) {
                                    info.isMissingAuthConstraint = true;
                                } else {
                                    info.missingAuthConstraintMethods.addAll(httpMethods);
                                }
                            }
                        }
                    } else {
                        // Process the permission for the resources x roles
                        for (WebResourceCollectionMetaData resourceCollectionMetaData : resourceCollectionsMetaData) {
                            List<String> httpMethods = new ArrayList<>(resourceCollectionMetaData.getHttpMethods());
                            List<String> methodOmissions = resourceCollectionMetaData.getHttpMethodOmissions();
                            if(httpMethods.isEmpty() && !methodOmissions.isEmpty()) {
                                httpMethods.addAll(WebResourceCollectionMetaData.ALL_HTTP_METHODS);
                                httpMethods.removeAll(methodOmissions);
                            }
                            List<String> urlPatterns = resourceCollectionMetaData.getUrlPatterns();
                            for (String urlPattern : urlPatterns) {
                                // Get the qualified url pattern
                                PatternInfo info = patternMap.get(urlPattern);
                                info.descriptor=true;
                                HashSet<String> mappedRoles = new HashSet<String>();
                                secConstraint.getAuthConstraint().getRoleNames();
                                List<String> authRoles = secConstraint.getAuthConstraint().getRoleNames();
                                for (String role : authRoles) {
                                    if ("*".equals(role)) {
                                        // The wildcard ref maps to all declared security-role names
                                        mappedRoles.addAll(jbossWebMetaData.getSecurityRoleNames());
                                    }
                                    else {
                                        mappedRoles.add(role);
                                    }
                                }
                                info.addRoles(mappedRoles, httpMethods);
                                // Add the transport to methods
                                if (userDataConstraintMetaData != null && userDataConstraintMetaData.getTransportGuarantee() != null)
                                    info.addTransport(userDataConstraintMetaData.getTransportGuarantee().name(), httpMethods);
                            }
                        }
                    }
                }
            }
        }

        JBossServletsMetaData servlets = jbossWebMetaData.getServlets();
        List<ServletMappingMetaData> mappings = jbossWebMetaData.getServletMappings();
        if(servlets != null && mappings != null) {

            Map<String, List<String>> servletMappingMap = new HashMap<>();
            for(ServletMappingMetaData mapping : mappings) {
                List<String> list = servletMappingMap.get(mapping.getServletName());
                if(list == null) {
                    servletMappingMap.put(mapping.getServletName(), list = new ArrayList<>());
                }
                list.addAll(mapping.getUrlPatterns());
            }
            if(!jbossWebMetaData.isMetadataComplete()) {
                for (JBossServletMetaData servlet : servlets) {
                    ServletSecurityMetaData security = servlet.getServletSecurity();
                    if (security != null) {
                        List<String> servletMappings = servletMappingMap.get(servlet.getServletName());
                        if (servletMappings != null) {

                            if (security.getHttpMethodConstraints() != null) {
                                for (HttpMethodConstraintMetaData s : security.getHttpMethodConstraints()) {
                                    if (s.getRolesAllowed() == null || s.getRolesAllowed().isEmpty()) {
                                        for (String urlPattern : servletMappings) {
                                            // Get the qualified url pattern
                                            PatternInfo info = patternMap.get(urlPattern);
                                            if (info.descriptor) {
                                                continue;
                                            }
                                            // Add the excluded methods
                                            if (s.getEmptyRoleSemantic() == null || s.getEmptyRoleSemantic() == EmptyRoleSemanticType.PERMIT) {
                                                info.missingAuthConstraintMethods.add(s.getMethod());
                                            } else {
                                                info.addExcludedMethods(Collections.singletonList(s.getMethod()));
                                            }
                                            // Add the transport to methods
                                            if (s.getTransportGuarantee() != null)
                                                info.addTransport(s.getTransportGuarantee().name(), Collections.singletonList(s.getMethod()));
                                        }
                                    } else {
                                        for (String urlPattern : servletMappings) {
                                            // Get the qualified url pattern
                                            PatternInfo info = patternMap.get(urlPattern);
                                            if (info.descriptor) {
                                                continue;
                                            }
                                            HashSet<String> mappedRoles = new HashSet<String>();
                                            List<String> authRoles = s.getRolesAllowed();
                                            for (String role : authRoles) {
                                                if ("*".equals(role)) {
                                                    // The wildcard ref maps to all declared security-role names
                                                    mappedRoles.addAll(jbossWebMetaData.getSecurityRoleNames());
                                                } else {
                                                    mappedRoles.add(role);
                                                }
                                            }
                                            info.addRoles(mappedRoles, Collections.singletonList(s.getMethod()));
                                            // Add the transport to methods
                                            if (s.getTransportGuarantee() != null)
                                                info.addTransport(s.getTransportGuarantee().name(), Collections.singletonList(s.getMethod()));
                                        }
                                    }
                                }
                            }
                            if (security.getRolesAllowed() == null || security.getRolesAllowed().isEmpty()) {
                                for (String urlPattern : servletMappings) {
                                    // Get the qualified url pattern
                                    PatternInfo info = patternMap.get(urlPattern);
                                    if (info.descriptor) {
                                        continue;
                                    }
                                    // Add the excluded methods
                                    if (security.getEmptyRoleSemantic() == null || security.getEmptyRoleSemantic() == EmptyRoleSemanticType.PERMIT) {
                                        info.isMissingAuthConstraint = true;
                                    } else {
                                        Set<String> methods = new HashSet<>(WebResourceCollectionMetaData.ALL_HTTP_METHODS);
                                        if (security.getHttpMethodConstraints() != null) {
                                            for (HttpMethodConstraintMetaData method : security.getHttpMethodConstraints()) {
                                                methods.remove(method.getMethod());
                                            }
                                        }
                                        info.addExcludedMethods(new ArrayList<>(methods));
                                    }
                                    // Add the transport to methods
                                    if (security.getTransportGuarantee() != null)
                                        info.addTransport(security.getTransportGuarantee().name(), Collections.emptyList());
                                }
                            } else {
                                for (String urlPattern : servletMappings) {
                                    // Get the qualified url pattern
                                    PatternInfo info = patternMap.get(urlPattern);
                                    if (info.descriptor) {
                                        continue;
                                    }
                                    HashSet<String> mappedRoles = new HashSet<String>();
                                    List<String> authRoles = security.getRolesAllowed();
                                    for (String role : authRoles) {
                                        if ("*".equals(role)) {
                                            // The wildcard ref maps to all declared security-role names
                                            mappedRoles.addAll(jbossWebMetaData.getSecurityRoleNames());
                                        } else {
                                            mappedRoles.add(role);
                                        }
                                    }
                                    info.addRoles(mappedRoles, Collections.emptyList());
                                    // Add the transport to methods
                                    if (security.getTransportGuarantee() != null)
                                        info.addTransport(security.getTransportGuarantee().name(), Collections.emptyList());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create the permissions
        for (PatternInfo info : patternMap.values()) {
            String qurl = info.getQualifiedPattern();
            if (info.isOverridden) {
                continue;
            }
            // Create the excluded permissions
            String[] httpMethods = info.getExcludedMethods();
            if (httpMethods != null) {
                // There were excluded security-constraints
                WebResourcePermission wrp = new WebResourcePermission(qurl, httpMethods);
                WebUserDataPermission wudp = new WebUserDataPermission(qurl, httpMethods, null);
                pc.addToExcludedPolicy(wrp);
                pc.addToExcludedPolicy(wudp);

            }

            // Create the role permissions
            Iterator<Map.Entry<String, Set<String>>> roles = info.getRoleMethods();
            Set<String> seenMethods = new HashSet<>();
            while (roles.hasNext()) {
                Map.Entry<String, Set<String>> roleMethods = roles.next();
                String role = roleMethods.getKey();
                Set<String> methods = roleMethods.getValue();
                seenMethods.addAll(methods);
                httpMethods = methods.toArray(new String[methods.size()]);
                pc.addToRole(role, new WebResourcePermission(qurl, httpMethods));

            }

            //there are totally 7 http methods from the jacc spec (See WebResourceCollectionMetaData.ALL_HTTP_METHOD_NAMES)
            final int NUMBER_OF_HTTP_METHODS = 7;
            // JACC 1.1: create !(httpmethods) in unchecked perms
            if(jbossWebMetaData.getDenyUncoveredHttpMethods() == null) {
                if (seenMethods.size() != NUMBER_OF_HTTP_METHODS) {
                    WebResourcePermission wrpUnchecked = new WebResourcePermission(qurl, "!"
                            + getCommaSeparatedString(seenMethods.toArray(new String[seenMethods.size()])));
                    pc.addToUncheckedPolicy(wrpUnchecked);
                }
            }
            if (jbossWebMetaData.getDenyUncoveredHttpMethods() == null) {
                // Create the unchecked permissions
                String[] missingHttpMethods = info.getMissingMethods();
                int length = missingHttpMethods.length;
                roles = info.getRoleMethods();
                if (length > 0 && !roles.hasNext()) {
                    // Create the unchecked permissions WebResourcePermissions
                    WebResourcePermission wrp = new WebResourcePermission(qurl, missingHttpMethods);
                    pc.addToUncheckedPolicy(wrp);
                } else if (!roles.hasNext()) {
                    pc.addToUncheckedPolicy(new WebResourcePermission(qurl, (String) null));
                }

                // SECURITY-63: Missing auth-constraint needs unchecked policy
                if (info.isMissingAuthConstraint) {
                    pc.addToUncheckedPolicy(new WebResourcePermission(qurl, (String) null));
                } else if (!info.allMethods.containsAll(WebResourceCollectionMetaData.ALL_HTTP_METHODS)) {
                    List<String> methods = new ArrayList<>(WebResourceCollectionMetaData.ALL_HTTP_METHODS);
                    methods.removeAll(info.allMethods);
                    pc.addToUncheckedPolicy(new WebResourcePermission(qurl, methods.toArray(new String[methods.size()])));

                }
                if (!info.missingAuthConstraintMethods.isEmpty()) {
                    pc.addToUncheckedPolicy(new WebResourcePermission(qurl, info.missingAuthConstraintMethods.toArray(new String[info.missingAuthConstraintMethods.size()])));
                }
            }

            // Create the unchecked permissions WebUserDataPermissions
            Iterator<Map.Entry<String, Set<String>>> transportConstraints = info.getTransportMethods();
            while (transportConstraints.hasNext()) {
                Map.Entry<String, Set<String>> transportMethods = transportConstraints.next();
                String transport = transportMethods.getKey();
                Set<String> methods = transportMethods.getValue();
                httpMethods = new String[methods.size()];
                methods.toArray(httpMethods);
                WebUserDataPermission wudp = new WebUserDataPermission(qurl, httpMethods, transport);
                pc.addToUncheckedPolicy(wudp);

                // If the transport is "NONE", then add an exclusive WebUserDataPermission
                // with the url pattern and null
                if ("NONE".equals(transport)) {
                    WebUserDataPermission wudp1 = new WebUserDataPermission(qurl, null);
                    pc.addToUncheckedPolicy(wudp1);
                } else {
                    // JACC 1.1: Transport is CONFIDENTIAL/INTEGRAL, add a !(http methods)
                    WebUserDataPermission wudpNonNull = new WebUserDataPermission(qurl, "!"
                            + getCommaSeparatedString(httpMethods));
                    pc.addToUncheckedPolicy(wudpNonNull);
                }
            }
        }

        Set<String> declaredRoles = jbossWebMetaData.getSecurityRoleNames();
        declaredRoles.add(ANY_AUTHENTICATED_USER_ROLE);

        /*
         * Create WebRoleRefPermissions for all servlet/security-role-refs along with all the cross product of servlets and
         * security-role elements that are not referenced via a security-role-ref as described in JACC section 3.1.3.2
         */
        JBossServletsMetaData servletsMetaData = jbossWebMetaData.getServlets();
        for (JBossServletMetaData servletMetaData : servletsMetaData) {
            Set<String> unrefRoles = new HashSet<String>(declaredRoles);
            String servletName = servletMetaData.getName();
            SecurityRoleRefsMetaData roleRefsMetaData = servletMetaData.getSecurityRoleRefs();
            // Perform the unreferenced roles processing for every servlet name
            if (roleRefsMetaData != null) {
                for (SecurityRoleRefMetaData roleRefMetaData : roleRefsMetaData) {
                    String roleRef = roleRefMetaData.getRoleLink();
                    String roleName = roleRefMetaData.getRoleName();
                    WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, roleName);
                    pc.addToRole(roleRef, wrrp);

                    // Remove the role from the unreferencedRoles
                    unrefRoles.remove(roleName);
                }
            }
            // Spec 3.1.3.2: For each servlet element in the deployment descriptor
            // a WebRoleRefPermission must be added to each security-role of the
            // application whose name does not appear as the rolename
            // in a security-role-ref within the servlet element.
            for (String unrefRole : unrefRoles) {
                WebRoleRefPermission unrefP = new WebRoleRefPermission(servletName, unrefRole);
                pc.addToRole(unrefRole, unrefP);
            }
        }

        // JACC 1.1:Spec 3.1.3.2: For each security-role defined in the deployment descriptor, an
        // additional WebRoleRefPermission must be added to the corresponding role by
        // calling the addToRole method on the PolicyConfiguration object. The
        // name of all such permissions must be the empty string, and the actions of each
        // such permission must be the role-name of the corresponding role.
        for (String role : declaredRoles) {
            WebRoleRefPermission wrrep = new WebRoleRefPermission("", role);
            pc.addToRole(role, wrrep);
        }
    }

    static String getCommaSeparatedString(String[] str) {
        int len = str.length;
        Arrays.sort(str);

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0)
                buf.append(",");
            buf.append(str[i]);
        }
        return buf.toString();
    }

    /**
     * Determine the url-pattern type
     *
     * @param urlPattern - the raw url-pattern value
     * @return one of EXACT, EXTENSION, PREFIX, DEFAULT
     */
    static int getPatternType(String urlPattern) {
        int type = EXACT;
        if (urlPattern.startsWith("*."))
            type = EXTENSION;
        else if (urlPattern.startsWith("/") && urlPattern.endsWith("/*"))
            type = PREFIX;
        else if (urlPattern.equals("/"))
            type = DEFAULT;
        return type;
    }

    /**
     * JACC url pattern Qualified URL Pattern Names.
     *
     * The rules for qualifying a URL pattern are dependent on the rules for determining if one URL pattern matches another as
     * defined in Section 3.1.3.3, Servlet URL-Pattern Matching Rules, and are described as follows: - If the pattern is a path
     * prefix pattern, it must be qualified by every path-prefix pattern in the deployment descriptor matched by and different
     * from the pattern being qualified. The pattern must also be qualified by every exact pattern appearing in the deployment
     * descriptor that is matched by the pattern being qualified. - If the pattern is an extension pattern, it must be qualified
     * by every path-prefix pattern appearing in the deployment descriptor and every exact pattern in the deployment descriptor
     * that is matched by the pattern being qualified. - If the pattern is the default pattern, "/", it must be qualified by
     * every other pattern except the default pattern appearing in the deployment descriptor. - If the pattern is an exact
     * pattern, its qualified form must not contain any qualifying patterns.
     *
     * URL patterns are qualified by appending to their String representation, a colon separated representation of the list of
     * patterns that qualify the pattern. Duplicates must not be included in the list of qualifying patterns, and any qualifying
     * pattern matched by another qualifying pattern may5 be dropped from the list.
     *
     * Any pattern, qualified by a pattern that matches it, is overridden and made irrelevant (in the translation) by the
     * qualifying pattern. Specifically, all extension patterns and the default pattern are made irrelevant by the presence of
     * the path prefix pattern "/*" in a deployment descriptor. Patterns qualified by the "/*" pattern violate the
     * URLPatternSpec constraints of WebResourcePermission and WebUserDataPermission names and must be rejected by the
     * corresponding permission constructors.
     *
     * @param metaData - the web deployment metadata
     * @return HashMap<String, PatternInfo>
     */
    static HashMap<String, PatternInfo> qualifyURLPatterns(JBossWebMetaData metaData) {
        ArrayList<PatternInfo> prefixList = new ArrayList<PatternInfo>();
        ArrayList<PatternInfo> extensionList = new ArrayList<PatternInfo>();
        ArrayList<PatternInfo> exactList = new ArrayList<PatternInfo>();
        HashMap<String, PatternInfo> patternMap = new HashMap<String, PatternInfo>();
        PatternInfo defaultInfo = null;

        List<SecurityConstraintMetaData> constraints = metaData.getSecurityConstraints();
        if (constraints != null) {
            for (SecurityConstraintMetaData constraint : constraints) {
                WebResourceCollectionsMetaData resourceCollectionsMetaData = constraint.getResourceCollections();
                if (resourceCollectionsMetaData != null) {
                    for (WebResourceCollectionMetaData resourceCollectionMetaData : resourceCollectionsMetaData) {
                        List<String> urlPatterns = resourceCollectionMetaData.getUrlPatterns();
                        for (String url : urlPatterns) {
                            int type = getPatternType(url);
                            PatternInfo info = patternMap.get(url);
                            if (info == null) {
                                info = new PatternInfo(url, type);
                                patternMap.put(url, info);
                                switch (type) {
                                    case PREFIX:
                                        prefixList.add(info);
                                        break;
                                    case EXTENSION:
                                        extensionList.add(info);
                                        break;
                                    case EXACT:
                                        exactList.add(info);
                                        break;
                                    case DEFAULT:
                                        defaultInfo = info;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
        JBossServletsMetaData servlets = metaData.getServlets();
        List<ServletMappingMetaData> mappings = metaData.getServletMappings();
        if(!metaData.isMetadataComplete() && servlets != null && mappings != null) {

            Map<String, List<String>> servletMappingMap = new HashMap<>();
            for(ServletMappingMetaData mapping : mappings) {
                List<String> list = servletMappingMap.get(mapping.getServletName());
                if(list == null) {
                    servletMappingMap.put(mapping.getServletName(), list = new ArrayList<>());
                }
                list.addAll(mapping.getUrlPatterns());
            }
            for (JBossServletMetaData servlet : servlets) {
                ServletSecurityMetaData security = servlet.getServletSecurity();
                if(security != null) {
                    List<String> servletMappings = servletMappingMap.get(servlet.getServletName());
                    if(servletMappings != null) {
                        for (String url : servletMappings) {
                            int type = getPatternType(url);
                            PatternInfo info = patternMap.get(url);
                            if (info == null) {
                                info = new PatternInfo(url, type);
                                patternMap.put(url, info);
                                switch (type) {
                                    case PREFIX:
                                        prefixList.add(info);
                                        break;
                                    case EXTENSION:
                                        extensionList.add(info);
                                        break;
                                    case EXACT:
                                        exactList.add(info);
                                        break;
                                    case DEFAULT:
                                        defaultInfo = info;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Qualify all prefix patterns
        for (int i = 0; i < prefixList.size(); i++) {
            PatternInfo info = prefixList.get(i);
            // Qualify by every other prefix pattern matching this pattern
            for (int j = 0; j < prefixList.size(); j++) {
                if (i == j)
                    continue;
                PatternInfo other = prefixList.get(j);
                if (info.matches(other))
                    info.addQualifier(other);
            }
            // Qualify by every exact pattern that is matched by this pattern
            for (PatternInfo other : exactList) {
                if (info.matches(other))
                    info.addQualifier(other);
            }
        }

        // Qualify all extension patterns
        for (PatternInfo info : extensionList) {
            // Qualify by every path prefix pattern
            for (PatternInfo other : prefixList) {
                // Any extension
                info.addQualifier(other);
            }
            // Qualify by every matching exact pattern
            for (PatternInfo other : exactList) {
                if (info.isExtensionFor(other))
                    info.addQualifier(other);
            }
        }

        // Qualify the default pattern
        if (defaultInfo == null) {
            defaultInfo = new PatternInfo("/", DEFAULT);
            patternMap.put("/", defaultInfo);
        }
        for (PatternInfo info : patternMap.values()) {
            if (info == defaultInfo)
                continue;
            defaultInfo.addQualifier(info);
        }

        return patternMap;
    }

    /**
     * A representation of all security-constraint mappings for a unique url-pattern
     */
    static class PatternInfo {

        static final HashMap<String, Set<String>> ALL_TRANSPORTS = new HashMap<String, Set<String>>();

        static {
            ALL_TRANSPORTS.put("NONE", WebResourceCollectionMetaData.ALL_HTTP_METHODS);
        }

        /** The raw url-pattern string from the web.xml */
        String pattern;

        /** The qualified url pattern as determined by qualifyURLPatterns */
        String qpattern;

        /** The list of qualifying patterns as determined by qualifyURLPatterns */
        ArrayList<PatternInfo> qualifiers = new ArrayList<PatternInfo>();

        /** One of PREFIX, EXTENSION, DEFAULT, EXACT */
        int type;

        /** HashSet<String> Union of all http methods seen in excluded statements */
        HashSet<String> excludedMethods;

        /** HashMap<String, HashSet<String>> role to http methods */
        HashMap<String, Set<String>> roles;

        /** HashMap<String, HashSet<String>> transport to http methods */
        HashMap<String, Set<String>> transports;

        /** The url pattern to http methods for patterns for */
        HashSet<String> allMethods = new HashSet<String>();

        /**
         * Does a qualifying pattern match this pattern and make this pattern obsolete?
         */
        boolean isOverridden;

        /**
         * A Security Constraint is missing an <auth-constraint/>
         */
        boolean isMissingAuthConstraint;

        Set<String> missingAuthConstraintMethods = new HashSet<>();

        boolean descriptor = false;

        /**
         * @param pattern - the url-pattern value
         * @param type - one of EXACT, EXTENSION, PREFIX, DEFAULT
         */
        PatternInfo(String pattern, int type) {
            this.pattern = pattern;
            this.type = type;
        }

        /**
         * Augment the excluded methods associated with this url
         *
         * @param httpMethods the list of excluded methods
         */
        void addExcludedMethods(List<String> httpMethods) {
            Collection<String> methods = httpMethods;
            if (methods.size() == 0)
                methods = WebResourceCollectionMetaData.ALL_HTTP_METHODS;
            if (excludedMethods == null)
                excludedMethods = new HashSet<String>();
            excludedMethods.addAll(methods);
            allMethods.addAll(methods);
        }

        /**
         * Get the list of excluded http methods
         *
         * @return excluded http methods if the exist, null if there were no excluded security constraints
         */
        public String[] getExcludedMethods() {
            String[] httpMethods = null;
            if (excludedMethods != null) {
                httpMethods = new String[excludedMethods.size()];
                excludedMethods.toArray(httpMethods);
            }
            return httpMethods;
        }

        /**
         * Update the role to http methods mapping for this url.
         *
         * @param mappedRoles - the role-name values for the auth-constraint
         * @param httpMethods - the http-method values for the web-resource-collection
         */
        public void addRoles(HashSet<String> mappedRoles, List<String> httpMethods) {
            Collection<String> methods = httpMethods;
            if (methods.size() == 0)
                methods = WebResourceCollectionMetaData.ALL_HTTP_METHODS;
            allMethods.addAll(methods);
            if (roles == null)
                roles = new HashMap<String, Set<String>>();

            for (String role : mappedRoles) {
                Set<String> roleMethods = roles.get(role);
                if (roleMethods == null) {
                    roleMethods = new HashSet<String>();
                    roles.put(role, roleMethods);
                }
                roleMethods.addAll(methods);
            }
        }

        /**
         * Get the role to http method mappings
         *
         * @return Iterator<Map.Entry<String, Set<String>>> for the role to http method mappings.
         */
        public Iterator<Map.Entry<String, Set<String>>> getRoleMethods() {
            HashMap<String, Set<String>> tmp = roles;
            if (tmp == null)
                tmp = new HashMap<String, Set<String>>(0);
            return tmp.entrySet().iterator();
        }

        /**
         * Update the role to http methods mapping for this url.
         *
         * @param transport - the transport-guarantee value
         * @param httpMethods - the http-method values for the web-resource-collection
         */
        void addTransport(String transport, List<String> httpMethods) {
            Collection<String> methods = httpMethods;
            if (methods.size() == 0)
                methods = WebResourceCollectionMetaData.ALL_HTTP_METHODS;
            if (transports == null)
                transports = new HashMap<String, Set<String>>();

            Set<String> transportMethods = transports.get(transport);
            if (transportMethods == null) {
                transportMethods = new HashSet<String>();
                transports.put(transport, transportMethods);
            }
            transportMethods.addAll(methods);
        }

        /**
         * Get the transport to http method mappings
         *
         * @return Iterator<Map.Entry<String, Set<String>>> for the transport to http method mappings.
         */
        public Iterator<Map.Entry<String, Set<String>>> getTransportMethods() {
            HashMap<String, Set<String>> tmp = transports;
            if (tmp == null)
                tmp = ALL_TRANSPORTS;
            return tmp.entrySet().iterator();
        }

        /**
         * Get the list of http methods that were not associated with an excluded or role based mapping of this url.
         *
         * @return the subset of http methods that should be unchecked
         */
        public String[] getMissingMethods() {
            String[] httpMethods = {};
            if (allMethods.size() == 0) {
                // There were no excluded or role based security-constraints
                httpMethods = WebResourceCollectionMetaData.ALL_HTTP_METHOD_NAMES;
            } else {
                httpMethods = WebResourceCollectionMetaData.getMissingHttpMethods(allMethods);
            }
            return httpMethods;
        }

        /**
         * Add the qualifying pattern. If info is a prefix pattern that matches this pattern, it overrides this pattern and will
         * exclude it from inclusion in the policy.
         *
         * @param info - a url pattern that should qualify this pattern
         */
        void addQualifier(PatternInfo info) {
            if (qualifiers.contains(info) == false) {
                // See if this pattern is matched by the qualifier
                if (info.type == PREFIX && info.matches(this))
                    isOverridden = true;
                qualifiers.add(info);
            }
        }

        /**
         * Get the url pattern with its qualifications
         *
         * @return the qualified form of the url pattern
         */
        public String getQualifiedPattern() {
            if (qpattern == null) {
                StringBuilder tmp = new StringBuilder(pattern);
                for (int n = 0; n < qualifiers.size(); n++) {
                    tmp.append(':');
                    PatternInfo info = qualifiers.get(n);
                    tmp.append(info.pattern);
                }
                qpattern = tmp.toString();
            }
            return qpattern;
        }

        public int hashCode() {
            return pattern.hashCode();
        }

        public boolean equals(Object obj) {
            PatternInfo pi = (PatternInfo) obj;
            return pattern.equals(pi.pattern);
        }

        /**
         * See if this pattern is matches the other pattern
         *
         * @param other - another pattern
         * @return true if the other pattern starts with this pattern less the "/*", false otherwise
         */
        public boolean matches(PatternInfo other) {
            int matchLength = pattern.length() - 2;
            boolean matches = pattern.regionMatches(0, other.pattern, 0, matchLength);
            return matches;
        }

        /**
         * See if this is an extension pattern that matches other
         *
         * @param other - another pattern
         * @return true if is an extension pattern and other ends with this pattern
         */
        public boolean isExtensionFor(PatternInfo other) {
            int offset = other.pattern.lastIndexOf('.');
            int length = pattern.length() - 1;
            boolean isExtensionFor = false;
            if (offset > 0) {
                isExtensionFor = pattern.regionMatches(1, other.pattern, offset, length);
            }
            return isExtensionFor;
        }

        public String toString() {
            StringBuilder tmp = new StringBuilder("PatternInfo[");
            tmp.append("pattern=");
            tmp.append(pattern);
            tmp.append(",type=");
            tmp.append(type);
            tmp.append(",isOverridden=");
            tmp.append(isOverridden);
            tmp.append(",qualifiers=");
            tmp.append(qualifiers);
            tmp.append("]");
            return tmp.toString();
        }
    }
}
