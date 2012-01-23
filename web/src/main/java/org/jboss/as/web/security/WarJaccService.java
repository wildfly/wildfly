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

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;

/**
 * A service that creates JACC permissions for a web deployment
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 */
public class WarJaccService extends JaccService<WarMetaData> {

    /** An prefix pattern "/prefix/*" */
    private static final int PREFIX = 1;

    /** An extension pattern "*.ext" */
    private static final int EXTENSION = 2;

    /** The "/" default pattern */
    private static final int DEFAULT = 3;

    /** An prefix pattern "/prefix/*" */
    private static final int EXACT = 4;

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    public WarJaccService(String contextId, WarMetaData metaData, Boolean standalone) {
        super(contextId, metaData, standalone);
    }

    /** {@inheritDoc} */
    @Override
    public void createPermissions(WarMetaData metaData, PolicyConfiguration pc) throws PolicyContextException {
        if (context == null) {
            throw MESSAGES.noCatalinaContextForJacc();
        }
        HashMap<String, PatternInfo> patternMap = qualifyURLPatterns(context);
        log.debugf("Qualified url patterns: " + patternMap);

        SecurityConstraint[] constraints = context.findConstraints();
        for (int i = 0; i < constraints.length; i++) {
            SecurityConstraint sc = constraints[i];
            SecurityCollection[] resources = sc.findCollections();
            String transport = sc.getUserConstraint();
            if ((sc.getAuthConstraint() && (sc.findAuthRoles().length == 0 && !sc.getAllRoles())) || !sc.getAuthConstraint()) {
                // Process the permissions for the excluded/unchecked resources
                for (int j = 0; j < resources.length; j++) {
                    SecurityCollection wrc = resources[j];
                    String[] httpMethods = wrc.findMethods();
                    String[] urlPatterns = wrc.findPatterns();
                    for (int n = 0; n < urlPatterns.length; n++) {
                        String url = urlPatterns[n];
                        PatternInfo info = (PatternInfo) patternMap.get(url);
                        // Add the excluded methods
                        if (sc.getAuthConstraint() && (sc.findAuthRoles().length == 0 && !sc.getAllRoles())) {
                            info.addExcludedMethods(Arrays.asList(httpMethods));
                        }
                        // SECURITY-63: Missing auth-constraint needs unchecked policy
                        if (!sc.getAuthConstraint())
                            info.isMissingAuthConstraint = true;
                    }
                }
            } else {
                // Process the permission for the resources x roles
                for (int j = 0; j < resources.length; j++) {
                    SecurityCollection wrc = resources[j];
                    String[] httpMethods = wrc.findMethods();
                    String[] urlPatterns = wrc.findPatterns();
                    for (int n = 0; n < urlPatterns.length; n++) {
                        String url = urlPatterns[n];
                        // Get the qualified url pattern
                        PatternInfo info = (PatternInfo) patternMap.get(url);
                        HashSet<String> mappedRoles = new HashSet<String>();
                        String[] authRoles = sc.findAuthRoles();
                        for (int k = 0; k < authRoles.length; k++) {
                            String role = authRoles[k];
                            mappedRoles.add(role);
                        }
                        // was a wildcard role included?
                        if (authRoles.length == 0 && sc.getAllRoles()) {
                            // JBAS-1824: Allow "*" to provide configurable authorization bypass
                            if (metaData.getMergedJBossWebMetaData().isJaccAllStoreRole())
                                mappedRoles.add("*");
                            else {
                                // The wildcard ref maps to all declared security-role names
                                String[] roles = context.findSecurityRoles();
                                for (int l = 0; l < roles.length; l++) {
                                    String role = roles[l];
                                    mappedRoles.add(role);
                                }
                            }
                        }
                        info.addRoles(mappedRoles, Arrays.asList(httpMethods));
                        // Add the transport to methods
                        info.addTransport(transport, Arrays.asList(httpMethods));
                    }
                }
            }
        }

        // Create the permissions
        for (PatternInfo info : patternMap.values()) {
            String qurl = info.getQualifiedPattern();
            if (info.isOverriden) {
                log.debugf("Dropping overriden pattern: " + info);
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

                // !(excluded methods) [JACC 1.1]
                String excludedString = "!" + getCommaSeparatedString(httpMethods);
                WebResourcePermission wrp1 = new WebResourcePermission(info.pattern, excludedString);
                WebUserDataPermission wudp1 = new WebUserDataPermission(info.pattern, excludedString);
                pc.addToUncheckedPolicy(wrp1);
                pc.addToUncheckedPolicy(wudp1);
            }

            // Create the role permissions
            Iterator<Map.Entry<String, Set<String>>> roles = info.getRoleMethods();
            while (roles.hasNext()) {
                Map.Entry<String, Set<String>> roleMethods = roles.next();
                String role = (String) roleMethods.getKey();
                WebResourcePermission wrp;
                if ("*".equals(role)) {
                    // JBAS-1824: <role-name>*</role-name>
                    wrp = new WebResourcePermission(qurl, (String) null);
                } else {
                    Set<String> methods = roleMethods.getValue();
                    httpMethods = new String[methods.size()];
                    methods.toArray(httpMethods);
                    wrp = new WebResourcePermission(qurl, httpMethods);
                }
                pc.addToRole(role, wrp);

                // JACC 1.1: create !(httpmethods) in unchecked perms
                if (httpMethods != null) {
                    WebResourcePermission wrpUnchecked = new WebResourcePermission(info.pattern, "!"
                            + getCommaSeparatedString(httpMethods));
                    pc.addToUncheckedPolicy(wrpUnchecked);
                }
            }

            // Create the unchecked permissions
            String[] missingHttpMethods = info.getMissingMethods();
            if (missingHttpMethods.length > 0) {
                // Create the unchecked permissions WebResourcePermissions
                WebResourcePermission wrp = new WebResourcePermission(qurl, missingHttpMethods);
                pc.addToUncheckedPolicy(wrp);
            } else
                pc.addToUncheckedPolicy(new WebResourcePermission(qurl, (String) null));

            // SECURITY-63: Missing auth-constraint needs unchecked policy
            if (info.isMissingAuthConstraint) {
                pc.addToUncheckedPolicy(new WebResourcePermission(qurl, (String) null));
            }

            // Create the unchecked permissions WebUserDataPermissions
            Iterator<Map.Entry<String, Set<String>>> transportContraints = info.getTransportMethods();
            while (transportContraints.hasNext()) {
                Map.Entry<String, Set<String>> transportMethods = transportContraints.next();
                String transport = transportMethods.getKey();
                Set<String> methods = transportMethods.getValue();
                httpMethods = new String[methods.size()];
                methods.toArray(httpMethods);
                WebUserDataPermission wudp = new WebUserDataPermission(qurl, httpMethods, transport);
                pc.addToUncheckedPolicy(wudp);

                // If the transport is "NONE", then add an exlusive WebUserDataPermission
                // with the url pattern and null
                if ("NONE".equals(transport)) {
                    WebUserDataPermission wudp1 = new WebUserDataPermission(info.pattern, null);
                    pc.addToUncheckedPolicy(wudp1);
                } else {
                    // JACC 1.1: Transport is CONFIDENTIAL/INTEGRAL, add a !(http methods)
                    if (httpMethods != null) {
                        WebUserDataPermission wudpNonNull = new WebUserDataPermission(info.pattern, "!"
                                + getCommaSeparatedString(httpMethods));
                        pc.addToUncheckedPolicy(wudpNonNull);
                    }
                }
            }
        }

        String[] unreferencedRoles = context.findSecurityRoles();
        List<String> unRefRoles = new ArrayList<String>();
        for (int i = 0; i < unreferencedRoles.length; i++) {
            unRefRoles.add(unreferencedRoles[i]);
        }

        /*
         * Create WebRoleRefPermissions for all servlet/security-role-refs along with all the cross product of servlets and
         * security-role elements that are not referenced via a security-role-ref as described in JACC section 3.1.3.2
         */
        Container[] servlets = context.findChildren();
        for (int i = 0; i < servlets.length; i++) {
            Wrapper servlet = (Wrapper) servlets[i];
            String servletName = servlet.getName();
            String[] roleRefs = servlet.findSecurityReferences();
            // Perform the unreferenced roles processing for every servlet name
            for (int j = 0; j < roleRefs.length; j++) {
                String roleRef = roleRefs[j];
                String roleName = servlet.findSecurityReference(roleRef);
                WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, roleRef);
                pc.addToRole(roleName, wrrp);
                /*
                 * A bit of a hack due to how tomcat calls out to its Realm.hasRole() with a role name that has been mapped to
                 * the role-link value. We may need to handle this with a custom request wrapper.
                 */
                wrrp = new WebRoleRefPermission(servletName, roleName);
                pc.addToRole(roleRef, wrrp);
                // Remove the role from the unreferencedRoles
                unRefRoles.remove(roleName);
            }

            // Spec 3.1.3.2: For each servlet element in the deployment descriptor
            // a WebRoleRefPermission must be added to each security-role of the
            // application whose name does not appear as the rolename
            // in a security-role-ref within the servlet element.
            for (String unrefRole : unRefRoles) {
                WebRoleRefPermission unrefP = new WebRoleRefPermission(servletName, unrefRole);
                pc.addToRole(unrefRole, unrefP);
            }
        }

        // JACC 1.1:Spec 3.1.3.2: For each security-role defined in the deployment descriptor, an
        // additional WebRoleRefPermission must be added to the corresponding role by
        // calling the addToRole method on the PolicyConfiguration object. The
        // name of all such permissions must be the empty string, and the actions of each
        // such permission must be the role-name of the corresponding role.
        for (int i = 0; i < unreferencedRoles.length; i++) {
            String unreferencedRole = unreferencedRoles[i];
            WebRoleRefPermission wrrep = new WebRoleRefPermission("", unreferencedRole);
            pc.addToRole(unreferencedRole, wrrep);
        }

        // Now build the cross product of the unreferencedRoles and servlets
        for (int i = 0; i < servlets.length; i++) {
            Wrapper servlet = (Wrapper) servlets[i];
            String servletName = servlet.getName();
            for (int j = 0; j < unreferencedRoles.length; j++) {
                String role = unreferencedRoles[j];
                WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, role);
                pc.addToRole(role, wrrp);
            }
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
    static HashMap<String, PatternInfo> qualifyURLPatterns(Context metaData) {
        ArrayList<PatternInfo> prefixList = new ArrayList<PatternInfo>();
        ArrayList<PatternInfo> extensionList = new ArrayList<PatternInfo>();
        ArrayList<PatternInfo> exactList = new ArrayList<PatternInfo>();
        HashMap<String, PatternInfo> patternMap = new HashMap<String, PatternInfo>();
        PatternInfo defaultInfo = null;

        SecurityConstraint[] constraints = metaData.findConstraints();
        for (int i = 0; i < constraints.length; i++) {
            SecurityConstraint sc = constraints[i];
            SecurityCollection[] resources = sc.findCollections();
            for (int j = 0; j < resources.length; j++) {
                SecurityCollection wrc = resources[j];
                String[] urlPatterns = wrc.findPatterns();
                for (int n = 0; n < urlPatterns.length; n++) {
                    String url = urlPatterns[n];
                    int type = getPatternType(url);
                    PatternInfo info = (PatternInfo) patternMap.get(url);
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

        // Qualify all prefix patterns
        for (int i = 0; i < prefixList.size(); i++) {
            PatternInfo info = (PatternInfo) prefixList.get(i);
            // Qualify by every other prefix pattern matching this pattern
            for (int j = 0; j < prefixList.size(); j++) {
                if (i == j)
                    continue;
                PatternInfo other = (PatternInfo) prefixList.get(j);
                if (info.matches(other))
                    info.addQualifier(other);
            }
            // Qualify by every exact pattern that is matched by this pattern
            for (int j = 0; j < exactList.size(); j++) {
                PatternInfo other = (PatternInfo) exactList.get(j);
                if (info.matches(other))
                    info.addQualifier(other);
            }
        }

        // Qualify all extension patterns
        for (int i = 0; i < extensionList.size(); i++) {
            PatternInfo info = (PatternInfo) extensionList.get(i);
            // Qualify by every path prefix pattern
            for (int j = 0; j < prefixList.size(); j++) {
                PatternInfo other = (PatternInfo) prefixList.get(j);
                {
                    // Any extension
                    info.addQualifier(other);
                }
            }
            // Qualify by every matching exact pattern
            for (int j = 0; j < exactList.size(); j++) {
                PatternInfo other = (PatternInfo) exactList.get(j);
                if (info.isExtensionFor(other))
                    info.addQualifier(other);
            }
        }

        // Qualify the default pattern
        if (defaultInfo == null) {
            defaultInfo = new PatternInfo("/", DEFAULT);
            patternMap.put("/", defaultInfo);
        }
        Iterator<PatternInfo> iter = patternMap.values().iterator();
        while (iter.hasNext()) {
            PatternInfo info = iter.next();
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
        boolean isOverriden;

        /**
         * A Security Constraint is missing an <auth-constraint/>
         */
        boolean isMissingAuthConstraint;

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
         * @param httpMethods
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
            Iterator<Map.Entry<String, Set<String>>> iter = tmp.entrySet().iterator();
            return iter;
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
            Iterator<Map.Entry<String, Set<String>>> iter = tmp.entrySet().iterator();
            return iter;
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
                    isOverriden = true;
                qualifiers.add(info);
            }
        }

        /**
         * Get the url pattern with its qualifications
         *
         * @see WebPermissionMapping#qualifyURLPatterns(org.jboss.metadata.WebMetaData)
         * @return the qualified form of the url pattern
         */
        public String getQualifiedPattern() {
            if (qpattern == null) {
                StringBuffer tmp = new StringBuffer(pattern);
                for (int n = 0; n < qualifiers.size(); n++) {
                    tmp.append(':');
                    PatternInfo info = (PatternInfo) qualifiers.get(n);
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
            StringBuffer tmp = new StringBuffer("PatternInfo[");
            tmp.append("pattern=");
            tmp.append(pattern);
            tmp.append(",type=");
            tmp.append(type);
            tmp.append(",isOverriden=");
            tmp.append(isOverriden);
            tmp.append(",qualifiers=");
            tmp.append(qualifiers);
            tmp.append("]");
            return tmp.toString();
        }
    }
}
