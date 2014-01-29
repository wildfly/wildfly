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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service for supplying the {@link LdapUserSearcher}
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapUserSearcherService implements Service<LdapUserSearcher> {

    private final LdapUserSearcher searcher;

    protected static final int searchTimeLimit = 10000; // TODO - Maybe make configurable.

    private LdapUserSearcherService(final LdapUserSearcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public LdapUserSearcher getValue() throws IllegalStateException, IllegalArgumentException {
        return searcher;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    static Service<LdapUserSearcher> createForUsernameIsDn() {
        return new LdapUserSearcherService(new LdapUserSearcher() {

            @Override
            public LdapEntry userSearch(DirContext dirContext, String suppliedName) {
                return new LdapEntry(suppliedName, suppliedName);
            }});
    }

    static Service<LdapUserSearcher> createForUsernameFilter(final String baseDn, final boolean recursive, final String userDnAttribute, final String attribute) {
        return new LdapUserSearcherService(new LdapUserSearcherImpl(baseDn, recursive, userDnAttribute, attribute, null));
    }

    static Service<LdapUserSearcher> createForAdvancedFilter(final String baseDn, final boolean recursive, final String userDnAttribute, final String filter) {
        return new LdapUserSearcherService(new LdapUserSearcherImpl(baseDn, recursive, userDnAttribute, null, filter));
    }

    private static class LdapUserSearcherImpl implements LdapUserSearcher {

        final String baseDn;
        final boolean recursive;
        final String userDnAttribute;
        final String userNameAttribute;
        final String advancedFilter;

        private LdapUserSearcherImpl(final String baseDn, final boolean recursive, final String userDnAttribute,
                final String userNameAttribute, final String advancedFilter) {
            this.baseDn = baseDn;
            this.recursive = recursive;
            this.userDnAttribute = userDnAttribute;
            this.userNameAttribute = userNameAttribute;
            this.advancedFilter = advancedFilter;

            if (SECURITY_LOGGER.isTraceEnabled()) {
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl baseDn=%s", baseDn);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl recursive=%b", recursive);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl userDnAttribute=%s", userDnAttribute);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl userNameAttribute=%s", userNameAttribute);
                SECURITY_LOGGER.tracef("LdapUserSearcherImpl advancedFilter=%s", advancedFilter);
            }
        }

        @Override
        public LdapEntry userSearch(DirContext dirContext, String suppliedName) throws IOException, NamingException {
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
                searchControls.setReturningAttributes(new String[] { userDnAttribute });
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

                SearchResult result = searchEnumeration.next();
                Attributes attributes = result.getAttributes();
                if (attributes != null) {
                    Attribute dn = attributes.get(userDnAttribute);
                    if (dn != null) {
                        distinguishedUserDN = (String) dn.get();
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
                SECURITY_LOGGER.tracef("DN '%s' found for user '%s'", distinguishedUserDN, suppliedName);

                return new LdapEntry(suppliedName, distinguishedUserDN);
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
