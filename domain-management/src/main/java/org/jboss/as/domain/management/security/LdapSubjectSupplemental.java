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

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_ATTRIBUTE;
import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class LdapSubjectSupplemental implements Service<SubjectSupplementalService>, SubjectSupplementalService, SubjectSupplemental {
    public static final String SERVICE_SUFFIX = "ldap-authorization";
    protected final int searchTimeLimit = 10000; // TODO - Maybe make configurable.
    private final InjectedValue<ConnectionManager> connectionManager = new InjectedValue<ConnectionManager>();
    private final boolean recursive;
    private final String rolesDn;
    private final String baseDn;
    private final String usernameAttribute;
    private final String advancedFilter;
    private final String pattern;
    private final int group;
    private final String resultPattern;
    private final Boolean reverseGroupMember;
    private final String userDn;
    private final String userFilter;

    public LdapSubjectSupplemental(boolean recursive, String rolesDn, String baseDn, String userDn,String userNameAttribute, String advancedFilter, String pattern, int groups, String resultPattern, Boolean reverseGroupMember) {
        this.recursive = recursive;
        this.rolesDn = rolesDn;
        this.baseDn = baseDn;
        this.userDn = userDn;
        this.pattern = pattern;
        this.group = groups;
        this.resultPattern = resultPattern;
        this.reverseGroupMember = reverseGroupMember;
        if (userNameAttribute == null && advancedFilter == null) {
            throw MESSAGES.oneOfRequired(USERNAME_ATTRIBUTE, ADVANCED_FILTER);
        }
        this.usernameAttribute = userNameAttribute;
        this.advancedFilter = advancedFilter;
        this.userFilter = "(" + usernameAttribute + "={0})";
    }

    /*
     * Service Methods
     */

    public SubjectSupplementalService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    /*
     *  Access to Injectors
     */
    public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
        return connectionManager;
    }

    /*
     * SubjectSupplementalService Method
     */

    public SubjectSupplemental getSubjectSupplemental(Map<String, Object> sharedState) {
        return this;
    }

    /*
     * SubjectSupplementalMethods
     */

    /**
     * @see org.jboss.as.domain.management.security.SubjectSupplemental#supplementSubject(javax.security.auth.Subject)
     */
    public void supplementSubject(Subject subject) throws IOException {
        Set<RealmUser> users = subject.getPrincipals(RealmUser.class);
        Set<Principal> principals = subject.getPrincipals();
        // In general we expect exactly one RealmUser, however we could cope with multiple
        // identities so load the roles for them all.
        for (RealmUser current : users) {
            principals.addAll(getRoles(current));
        }
    }

    private InitialDirContext createSearchContext() throws Exception {
        ConnectionManager connectionManager = this.connectionManager.getValue();
        InitialDirContext searchContext = (InitialDirContext) connectionManager.getConnection();
        return searchContext;
    }

    protected SearchControls createSearchControl() {
        // 2 - Search to identify the DN of the user connecting
        SearchControls searchControls = new SearchControls();
        if (recursive) {
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        } else {
            searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }
        searchControls.setReturningAttributes(new String[]{rolesDn});
        searchControls.setTimeLimit(searchTimeLimit);
        return searchControls;
    }

    private String getDistinguishedName(InitialDirContext searchContext, SearchControls searchControls, Object[] filterArguments) throws NamingException, IOException {
        String distinguishedUserDN = (String) filterArguments[0];
        if ((!reverseGroupMember) && (usernameAttribute != null)) {
            NamingEnumeration<SearchResult> searchEnumeration = searchContext.search(baseDn, userFilter, filterArguments, searchControls);
            if (searchEnumeration.hasMore() == true) {
                SearchResult result = searchEnumeration.next();
                if (userDn == null || userDn.equalsIgnoreCase("dn")) {
                    distinguishedUserDN = result.getNameInNamespace();
                } else {
                    Attributes attributes = result.getAttributes();
                    if (attributes != null) {
                        Attribute dn = attributes.get(userDn);
                        if (dn != null) {
                            distinguishedUserDN = (String) dn.get();
                        } else {
                            ROOT_LOGGER.failedRetrieveLdapAttribute(userDn);
                        }
                    }
                }
            }
        }
        return distinguishedUserDN;
    }

    private String[] createSearchFilter(InitialDirContext searchContext, SearchControls searchControls, String username) throws NamingException, IOException {
        String userDN = getDistinguishedName(searchContext,searchControls, new Object[]{username});
        String[] filter = new String[2];
        if (reverseGroupMember) {
            filter[0] = userFilter;
            filter[1] = userDN;
        } else {
            filter[0] = advancedFilter != null ? advancedFilter : "(member={0})";
            filter[1] = userDN;
        }
        return filter;
    }

    protected Set<String> searchLdap(String username) {
        Set<String> userNames = new HashSet<String>();
        // 1 - Obtain Connection to LDAP
        InitialDirContext searchContext = null;
        NamingEnumeration<SearchResult> searchEnumeration = null;
        try {
            searchContext = createSearchContext();
            SearchControls searchControls = createSearchControl();
            String[] filter = createSearchFilter(searchContext, searchControls, username);
            Object[] filterArguments = new Object[]{filter[1]};

            searchEnumeration = searchContext.search(baseDn, filter[0], filterArguments, searchControls);
            while (searchEnumeration.hasMore()) {
                SearchResult result = searchEnumeration.next();
                if (rolesDn.equalsIgnoreCase("dn")) {
                    userNames.add(result.getNameInNamespace());
                } else {
                    Attributes attributes = result.getAttributes();
                    if (attributes != null) {
                        Attribute dn = attributes.get(rolesDn);
                        if (dn != null) {
                            NamingEnumeration<?> role = dn.getAll();
                            while (role.hasMore()) {
                                userNames.add(role.next().toString());
                            }
                        } else {
                           ROOT_LOGGER.failedRetrieveLdapAttribute(rolesDn);
                        }
                    } else {
                        ROOT_LOGGER.failedRetrieveLdapAttribute(rolesDn);
                    }
                }
            }
        } catch (Exception e) {
            ROOT_LOGGER.failedRetrieveLdapRoles(e);
        } finally {
            safeClose(searchEnumeration);
            safeClose(searchContext);
        }

        return userNames;
    }

    private Set<RealmRole> getRoles(RealmUser user) {
        Set<RealmRole> response = new HashSet<RealmRole>();
        try {
            HashSet<String> ldapRole = (HashSet<String>) searchLdap(user.getName());
            ROOT_LOGGER.trace("Retrieved roles ["+user.getName()+"] :");
            for (String role : ldapRole) {
                if (pattern != null) {
                    Pattern compilePattern = Pattern.compile(pattern);
                    Matcher ldapRoleMatch = compilePattern.matcher(role);
                    if (ldapRoleMatch.groupCount() > 0) {
                        String parsedRole = replaceGroups(ldapRoleMatch);
                        if (parsedRole.length() > 0) {
                            response.add(new RealmRole(parsedRole));
                        }
                    }
                } else {
                    ROOT_LOGGER.trace("   role:"+role);
                    response.add(new RealmRole(role));
                }
            }
        } catch (Exception e) {
            ROOT_LOGGER.failedRetrieveLdapRoles(e);
        }
        return response;
    }

    private String replaceGroups(Matcher ldapRoles) {
        String parsedLdapRoles = new String();
        Pattern rPattern = Pattern.compile("(\\{)(.+?)(\\})");
        if (resultPattern != null) {
            Matcher resultMatch = rPattern.matcher(resultPattern);
            boolean foundGroupMatch = false;
            StringBuffer sb = new StringBuffer();
            while (resultMatch.find()) {
                foundGroupMatch = true;
                try {
                    ldapRoles.reset();
                    int elementNo = Integer.parseInt(resultMatch.group(2));
                    for (int i = 0; i <= elementNo; i++) {
                        ldapRoles.find();
                    }
                    resultMatch.appendReplacement(sb, ldapRoles.group(group));

                } catch (Exception e) {
                    ROOT_LOGGER.failedRetrieveMatchingLdapRoles(e);
                    throw new IllegalStateException(e); // make sure we bail out to prevent return value is not set with incorrect data
                }
            }
            resultMatch.appendTail(sb);
            parsedLdapRoles = sb.toString();
            if (!foundGroupMatch) {
               ROOT_LOGGER.failedRetrieveMatchingGroups();
               parsedLdapRoles = ""; // better reset the return value then sent back rubbish
            }
        } else {
            if (ldapRoles.find()) {
                parsedLdapRoles = ldapRoles.group(group);
            }
        }
        return parsedLdapRoles;
    }

    private void safeClose(Context context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void safeClose(NamingEnumeration ne) {
        if (ne != null) {
            try {
                ne.close();
            } catch (Exception ignored) {
            }
        }
    }
}
