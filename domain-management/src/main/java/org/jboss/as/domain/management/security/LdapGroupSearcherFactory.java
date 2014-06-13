/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.logging.DomainManagementLogger.SECURITY_LOGGER;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapReferralException;

import org.jboss.as.domain.management.security.BaseLdapGroupSearchResource.GroupName;

/**
 * Factory for supplying LDAP searches for groups.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapGroupSearcherFactory {

    private static final int searchTimeLimit = 10000;

    static LdapSearcher<LdapEntry[], LdapEntry> createForGroupToPrincipal(final String baseDn, final String groupDnAttribute,
            final String groupNameAttribute, final String principalAttribute, final boolean recursive,
            final GroupName searchBy, final boolean preferOriginalConnection) {
        return new GroupToPrincipalSearcher(baseDn, groupDnAttribute, groupNameAttribute, principalAttribute, recursive, searchBy, preferOriginalConnection);
    }

    static LdapSearcher<LdapEntry[], LdapEntry> createForPrincipalToGroup(final String groupAttribute, final String groupNameAttribute, final boolean preferOriginalConnection) {
        return new PrincipalToGroupSearcher(groupAttribute, groupNameAttribute, preferOriginalConnection);
    }

    private static SearchControls createSearchControl(final boolean recursive, final String[] attributes) {
        if (SECURITY_LOGGER.isTraceEnabled()) {
            SECURITY_LOGGER.tracef("createSearchControl recursive=%b,  attributes=%s", recursive, Arrays.toString(attributes));
        }
        // 2 - Search to identify the DN of the user connecting
        SearchControls searchControls = new SearchControls();
        if (recursive) {
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        } else {
            searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }
        searchControls.setReturningAttributes(attributes);
        searchControls.setTimeLimit(searchTimeLimit);
        return searchControls;
    }

    private static String[] createArray(final String... elements) {
        ArrayList<String> elementList = new ArrayList<String>(elements.length);
        for (String current : elements) {
            if (current != null) {
                elementList.add(current);
            }
        }

        return elementList.toArray(new String[elementList.size()]);
    }

    private static class GroupToPrincipalSearcher implements LdapSearcher<LdapEntry[], LdapEntry> {

        private final String baseDn;
        private final String groupDnAttribute;
        private final String groupNameAttribute;
        private final String[] attributeArray;
        private final String filterString;
        private final boolean recursive;
        private final GroupName searchBy;
        private final boolean preferOriginalConnection;

        private GroupToPrincipalSearcher(final String baseDn, final String groupDnAttribute, final String groupNameAttribute,
                final String principalAttribute, final boolean recursive, final GroupName searchBy, final boolean preferOriginalConnection) {
            this.baseDn = baseDn;
            this.groupDnAttribute = groupDnAttribute;
            this.groupNameAttribute = groupNameAttribute;
            this.attributeArray = createArray(groupDnAttribute, groupNameAttribute);
            this.filterString = String.format("(%s={0})", principalAttribute);
            this.recursive = recursive;
            this.searchBy = searchBy;
            this.preferOriginalConnection = preferOriginalConnection;

            if (SECURITY_LOGGER.isTraceEnabled()) {
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher baseDn=%s", baseDn);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher groupDnAttribute=%s", groupDnAttribute);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher groupNameAttribute=%s", groupNameAttribute);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher attributeArray=%s", Arrays.toString(attributeArray));
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher filterString=%s", filterString);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher recursive=%b", recursive);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher searchBy=%s", searchBy);
                SECURITY_LOGGER.tracef("GroupToPrincipalSearcher preferOriginalConnection=%b", preferOriginalConnection);
            }
        }

        @Override
        public LdapEntry[] search(LdapConnectionHandler originalConnectionHandler, LdapEntry entry) throws IOException, NamingException {
            SearchControls searchControls = createSearchControl(recursive, attributeArray); // TODO - Can we create this in
                                                                                            // advance?
            Set<LdapEntry> foundEntries = new HashSet<LdapEntry>();

            LdapConnectionHandler connectionHandler = originalConnectionHandler;
            URI referralAddress = null;
            if ((referralAddress = entry.getReferralUri()) != null && preferOriginalConnection==false) {
                connectionHandler = connectionHandler.findForReferral(referralAddress);
                if (connectionHandler == null) {
                    SECURITY_LOGGER.tracef("Unable to obtain connection handler for referral URI %s", referralAddress);
                    return foundEntries.toArray(new LdapEntry[foundEntries.size()]);
                }
            } else {
                // Be sure it is back to null to prevent leaking into LdapEntry for results that may be used for an iterative search.
                referralAddress = null;
            }

            Object[] searchParameter = getSearchParameter(entry);
            boolean trace = SECURITY_LOGGER.isTraceEnabled();
            if (trace) {
                SECURITY_LOGGER.tracef("Performing search baseDn=%s, filterString=%s, searchParameter=%s", baseDn,
                        filterString, Arrays.toString(searchParameter));
            }
            NamingEnumeration<SearchResult> searchResults = connectionHandler.getConnection().search(baseDn, filterString, searchParameter, searchControls);
            if (trace && searchResults.hasMore() == false) {
                SECURITY_LOGGER.trace("No search results found.");
            }
            while (searchResults.hasMore()) {
                SearchResult current = searchResults.next();
                Attributes attributes = current.getAttributes();
                if (attributes != null) {
                    LdapEntry newEntry = convertToLdapEntry(current, attributes, referralAddress);
                    SECURITY_LOGGER.tracef("Adding %s", newEntry);
                    foundEntries.add(newEntry);
                } else {
                    SECURITY_LOGGER.tracef("No attributes found for %s", current);
                }
            }

            return foundEntries.toArray(new LdapEntry[foundEntries.size()]);
        }

        private LdapEntry convertToLdapEntry(SearchResult searchResult, Attributes attributes, final URI referralAddress) throws NamingException {
            String simpleName = null;
            String distinguishedName = null;

            if (groupNameAttribute != null) {
                SECURITY_LOGGER.tracef("Getting groupNameAttribute=%s", groupNameAttribute);
                Attribute groupNameAttr = attributes.get(groupNameAttribute);
                if (groupNameAttr != null) {
                    simpleName = (String) groupNameAttr.get();
                }
            }

            if (groupDnAttribute != null) {
                if ("dn".equals(groupDnAttribute)) {
                    SECURITY_LOGGER.trace("Obtaining dn using getNameInNamespace()");
                    distinguishedName = searchResult.getNameInNamespace();
                } else {
                    SECURITY_LOGGER.tracef("Getting groupDnAttribute=%s", groupDnAttribute);
                    Attribute groupDnAttr = attributes.get(groupDnAttribute);
                    if (groupDnAttr != null) {
                        distinguishedName = (String) groupDnAttr.get();
                    }
                }
            }

            return new LdapEntry(simpleName, distinguishedName, referralAddress);
        }

        private Object[] getSearchParameter(final LdapEntry entry) {
            switch (searchBy) {
                case SIMPLE:
                    return new String[] { entry.getSimpleName() };
                default:
                    return new String[] { entry.getDistinguishedName() };
            }
        }

    }

    private static class PrincipalToGroupSearcher implements LdapSearcher<LdapEntry[], LdapEntry> {

        private final String groupAttribute; // The attribute on the principal that references the group it is a member of.
        private final String groupNameAttribute; // The attribute on the group that is it's simple name.
        private final boolean preferOriginalConnection; // After a referral should we still prefer the original connection?
        private static final boolean IGNORE_NONEXISTENT_ROLE =
              Boolean.valueOf(System.getProperty("org.jboss.as.domain.management.security.LdapGroupSearcherService.IGNORE_NONEXISTENT_ROLE", "false")).booleanValue();

        private PrincipalToGroupSearcher(final String groupAttribute, final String groupNameAttribute, final boolean preferOriginalConnection) {
            this.groupAttribute = groupAttribute;
            this.groupNameAttribute = groupNameAttribute;
            this.preferOriginalConnection = preferOriginalConnection;

            if (SECURITY_LOGGER.isTraceEnabled()) {
                SECURITY_LOGGER.tracef("PrincipalToGroupSearcher groupAttribute=%s", groupAttribute);
                SECURITY_LOGGER.tracef("PrincipalToGroupSearcher groupNameAttribute=%s", groupNameAttribute);
                SECURITY_LOGGER.tracef("PrincipalToGroupSearcher preferOriginalConnection=%b", preferOriginalConnection);
            }
        }

        @Override
        public LdapEntry[] search(LdapConnectionHandler originalConnectionHandler, LdapEntry entry) throws IOException, NamingException {
            Set<LdapEntry> foundEntries = new HashSet<LdapEntry>();

            LdapConnectionHandler connectionHandler = originalConnectionHandler;
            URI originalReferralAddress = null;
            if ((originalReferralAddress = entry.getReferralUri()) != null) {
                // To load the list of groups references we will always need to have followed the referral.
                connectionHandler = connectionHandler.findForReferral(originalReferralAddress);
                if (connectionHandler == null) {
                    SECURITY_LOGGER.tracef("Unable to obtain connection handler for referral URI %s", originalReferralAddress);
                    return foundEntries.toArray(new LdapEntry[foundEntries.size()]);
                }
            }

            DirContext dirContext = connectionHandler.getConnection();
            // Load the list of group - before reaching this point any referrals should have already been followed so we
            // do not prepare to follow a referral here.
            Attributes groups = dirContext.getAttributes(entry.getDistinguishedName(), new String[] { groupAttribute });
            Attribute groupRef = groups.get(groupAttribute);

            if (preferOriginalConnection) {
                // If needed reset the connection handler back to the original in
                // preparation for loading the actual groups.
                connectionHandler = originalConnectionHandler;
                originalReferralAddress = null;
            }

            if (groupRef != null && groupRef.size() > 0) {
                NamingEnumeration<String> groupRefValues = (NamingEnumeration<String>) groupRef.getAll();
                while (groupRefValues.hasMore()) {
                    String distingushedName = groupRefValues.next().replace("\\", "\\\\").replace("/", "\\/");
                    SECURITY_LOGGER.tracef("Group found with distinguishedName=%s", distingushedName);

                    LdapConnectionHandler groupLoadHandler = connectionHandler;
                    URI groupReferralAddress = originalReferralAddress;

                    boolean retry = false;

                    String simpleName = null;

                    do {
                        retry = false;

                        try {
                            dirContext = groupLoadHandler.getConnection();
                            // Load the Name
                            Attributes groupNameAttrs = dirContext.getAttributes(distingushedName,
                                    groupNameAttribute != null ? new String[] { groupNameAttribute } : new String[] {});

                            if (groupNameAttribute != null) {
                                Attribute groupNameAttr = groupNameAttrs.get(groupNameAttribute);
                                simpleName = (String) groupNameAttr.get();
                                SECURITY_LOGGER.tracef("simpleName %s loaded for group with distinguishedName=%s", simpleName,
                                        distingushedName);
                            } else {
                                SECURITY_LOGGER.trace("No groupNameAttribute to load simpleName");
                            }
                            foundEntries.add(new LdapEntry(simpleName, distingushedName, groupReferralAddress));
                        } catch (NameNotFoundException e) {
                            SECURITY_LOGGER.tracef("Failed to query roleNameAttrName: %s", e.getMessage());
                            if( !this.IGNORE_NONEXISTENT_ROLE )
                                throw e;
                        } catch (LdapReferralException e) {
                            Object info = e.getReferralInfo();
                            try {
                                URI fullUri = new URI(info.toString());
                                groupReferralAddress = new URI(fullUri.getScheme(), null, fullUri.getHost(), fullUri.getPort(),
                                        null, null, null);
                                distingushedName = fullUri.getPath().substring(1);
                                SECURITY_LOGGER.tracef("Received referral with address '%s' for dn '%s'",
                                        groupReferralAddress.toString(), distingushedName);

                                groupLoadHandler = groupLoadHandler.findForReferral(groupReferralAddress);
                                if (groupLoadHandler == null) {
                                    SECURITY_LOGGER.tracef("Unable to follow referral to '%s'", fullUri);
                                }
                                retry = true;
                            } catch (URISyntaxException ue) {
                                SECURITY_LOGGER.tracef("Unable to construct URI from referral: %s", info);
                            }
                        }
                    } while (retry);
                }
            } else {
                SECURITY_LOGGER.tracef("No groups found for %s", entry);
            }

            return foundEntries.toArray(new LdapEntry[foundEntries.size()]);
        }

    }

}
