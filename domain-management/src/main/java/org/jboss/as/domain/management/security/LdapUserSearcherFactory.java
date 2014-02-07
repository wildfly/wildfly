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

import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Factory to create searchers for user in LDAP.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class LdapUserSearcherFactory {

    protected static final int searchTimeLimit = 10000; // TODO - Maybe make configurable.

    static LdapSearcher<LdapEntry, String> createForUsernameIsDn() {
        return new LdapSearcher<LdapEntry, String>() {

            @Override
            public LdapEntry search(DirContext dirContext, String suppliedName) {
                return new LdapEntry(suppliedName, suppliedName);
            }
        };
    }

    static LdapSearcher<LdapEntry, String> createForUsernameFilter(final String baseDn, final boolean recursive, final String userDnAttribute, final String attribute, final String usernameLoad) {
        return new LdapUserSearcherImpl(baseDn, recursive, userDnAttribute, attribute, null, usernameLoad);
    }

    static LdapSearcher<LdapEntry, String> createForAdvancedFilter(final String baseDn, final boolean recursive, final String userDnAttribute, final String filter, final String usernameLoad) {
        return new LdapUserSearcherImpl(baseDn, recursive, userDnAttribute, null, filter, usernameLoad);
    }

    private static class LdapUserSearcherImpl implements LdapSearcher<LdapEntry, String> {

        final String baseDn;
        final boolean recursive;
        final String userDnAttribute;
        final String userNameAttribute;
        final String advancedFilter;
        final String usernameLoad;

        private LdapUserSearcherImpl(final String baseDn, final boolean recursive, final String userDnAttribute,
                final String userNameAttribute, final String advancedFilter, final String usernameLoad) {
            this.baseDn = baseDn;
            this.recursive = recursive;
            this.userDnAttribute = userDnAttribute;
            this.userNameAttribute = userNameAttribute;
            this.advancedFilter = advancedFilter;
            this.usernameLoad = usernameLoad;

            if (SECURITY_LOGGER.isTraceEnabled()) {
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl baseDn=%s", baseDn);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl recursive=%b", recursive);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl userDnAttribute=%s", userDnAttribute);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl userNameAttribute=%s", userNameAttribute);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl advancedFilter=%s", advancedFilter);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl usernameLoad=%s", usernameLoad);
            }
        }

        @Override
        public LdapEntry search(DirContext dirContext, String suppliedName) throws IOException, NamingException {
            NamingEnumeration<SearchResult> searchEnumeration = null;

            try {
                SearchControls searchControls = new SearchControls();
                if (recursive) {
                    SECURITY_LOGGER.trace("Performing recursive search");
                    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                } else {
                    SECURITY_LOGGER.trace("Performing single level search");
                    searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                }
                if (usernameLoad == null) {
                    searchControls.setReturningAttributes(new String[] { userDnAttribute });
                } else {
                    searchControls.setReturningAttributes(new String[] { userDnAttribute, usernameLoad });
                }
                searchControls.setTimeLimit(searchTimeLimit);

                Object[] filterArguments = new Object[] { suppliedName };
                String filter = userNameAttribute != null ? "(" + userNameAttribute + "={0})" : advancedFilter;
                SECURITY_LOGGER.tracef("Searching for user '%s' using filter '%s'.", suppliedName, filter);

                searchEnumeration = dirContext.search(baseDn, filter, filterArguments, searchControls);
                if (searchEnumeration.hasMore() == false) {
                    SECURITY_LOGGER.tracef("User '%s' not found in directory.", suppliedName);
                    throw MESSAGES.userNotFoundInDirectory(suppliedName);
                }

                String distinguishedUserDN = null;
                String username = usernameLoad == null ? suppliedName : null;

                SearchResult result = searchEnumeration.next();
                Attributes attributes = result.getAttributes();
                if (attributes != null) {
                    Attribute dn = attributes.get(userDnAttribute);
                    if (dn != null) {
                        distinguishedUserDN = (String) dn.get();
                    }
                    if (usernameLoad != null) {
                        Attribute usernameAttr = attributes.get(usernameLoad);
                        if (usernameAttr != null) {
                            username = (String) usernameAttr.get();
                            SECURITY_LOGGER.tracef("Converted username '%s' to '%s'", suppliedName, username);
                        }
                    }
                }
                if (distinguishedUserDN == null) {
                    if (result.isRelative() == true) {
                        distinguishedUserDN = result.getName() + ("".equals(baseDn) ? "" : "," + baseDn);
                    } else {
                        String name = result.getName();
                        SECURITY_LOGGER.tracef("Can't follow referral for authentication: %s", name);
                        throw MESSAGES.nameNotFound(suppliedName);
                    }
                }
                if (username == null) {
                    throw MESSAGES.usernameNotLoaded(suppliedName);
                }
                SECURITY_LOGGER.tracef("DN '%s' found for user '%s'", distinguishedUserDN, username);

                return new LdapEntry(username, distinguishedUserDN);
            } finally {
                if (searchEnumeration != null) {
                    try {
                        searchEnumeration.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

    }

}
