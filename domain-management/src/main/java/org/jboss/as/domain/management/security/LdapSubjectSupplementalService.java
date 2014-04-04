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

import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.naming.NamingException;
import javax.security.auth.Subject;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManager;
import org.jboss.as.domain.management.security.BaseLdapGroupSearchResource.GroupName;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A {@link SubjectSupplemental} for loading a users groups from LDAP.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapSubjectSupplementalService implements Service<SubjectSupplementalService>, SubjectSupplementalService {

    private final InjectedValue<LdapConnectionManager> connectionManager = new InjectedValue<LdapConnectionManager>();
    private final InjectedValue<LdapSearcherCache<LdapEntry, String>> userSearcherInjector = new InjectedValue<LdapSearcherCache<LdapEntry, String>>();
    private final InjectedValue<LdapSearcherCache<LdapEntry[], LdapEntry>> groupSearcherInjector = new InjectedValue<LdapSearcherCache<LdapEntry[], LdapEntry>>();

    private LdapSearcherCache<LdapEntry, String> userSearcher;
    private LdapSearcherCache<LdapEntry[], LdapEntry> groupSearcher;

    protected final int searchTimeLimit = 10000; // TODO - Maybe make configurable.

    private final String realmName;
    private final boolean shareConnection;
    private final boolean forceUserDnSearch;
    private final boolean iterative;
    private final GroupName groupName;

    public LdapSubjectSupplementalService(final String realmName, final boolean shareConnection, final boolean forceUserDnSearch, final boolean iterative, final GroupName groupName) {
        this.realmName = realmName;
        this.shareConnection = shareConnection;
        this.forceUserDnSearch = forceUserDnSearch;
        this.iterative = iterative;
        this.groupName = groupName;
    }

    /*
     * Service Methods
     */

    public SubjectSupplementalService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
        userSearcher = userSearcherInjector.getOptionalValue();
        groupSearcher = groupSearcherInjector.getValue();

        if (SECURITY_LOGGER.isTraceEnabled()) {
            SECURITY_LOGGER.tracef("LdapSubjectSupplementalService realmName=%s", realmName);
            SECURITY_LOGGER.tracef("LdapSubjectSupplementalService shareConnection=%b", shareConnection);
            SECURITY_LOGGER.tracef("LdapSubjectSupplementalService forceUserDnSearch=%b", forceUserDnSearch);
            SECURITY_LOGGER.tracef("LdapSubjectSupplementalService iterative=%b", iterative);
            SECURITY_LOGGER.tracef("LdapSubjectSupplementalService groupName=%s", groupName);
        }
    }

    public void stop(StopContext context) {
        groupSearcher = null;
        userSearcher = null;
    }

    /*
     *  Access to Injectors
     */
    public Injector<LdapConnectionManager> getConnectionManagerInjector() {
        return connectionManager;
    }

    public Injector<LdapSearcherCache<LdapEntry, String>> getLdapUserSearcherInjector() {
        return userSearcherInjector;
    }

    public Injector<LdapSearcherCache<LdapEntry[], LdapEntry>> getLdapGroupSearcherInjector() {
        return groupSearcherInjector;
    }

    /*
     * SubjectSupplementalService Method
     */

    public SubjectSupplemental getSubjectSupplemental(Map<String, Object> sharedState) {
        return new LdapSubjectSupplemental(sharedState);
    }

    /*
     * SubjectSupplementalMethods
     */

    public class LdapSubjectSupplemental implements SubjectSupplemental {

        private final Set<LdapEntry> searchedPerformed = new HashSet<LdapEntry>();
        private final Map<String, Object> sharedState;

        protected LdapSubjectSupplemental(final Map<String, Object> sharedState) {
            this.sharedState = sharedState;
        }

        /**
         * @see org.jboss.as.domain.management.security.SubjectSupplemental#supplementSubject(javax.security.auth.Subject)
         */
        public void supplementSubject(Subject subject) throws IOException {
            Set<RealmUser> users = subject.getPrincipals(RealmUser.class);
            Set<Principal> principals = subject.getPrincipals();

            final LdapConnectionHandler connectionHandler;
            if (sharedState.containsKey(LdapConnectionHandler.class.getName())) {
                SECURITY_LOGGER.trace("Using existing LdapConnectionHandler from shared state.");
                connectionHandler = (LdapConnectionHandler) sharedState.remove(LdapConnectionHandler.class.getName());
            } else {
                SECURITY_LOGGER.trace("Creating new LdapConnectionHandler.");
                connectionHandler = LdapConnectionHandler.newInstance(connectionManager.getValue());
            }
            try {
                // In general we expect exactly one RealmUser, however we could cope with multiple
                // identities so load the groups for them all.
                for (RealmUser current : users) {
                    SECURITY_LOGGER.tracef("Loading groups for '%s'", current);
                    principals.addAll(loadGroups(current, connectionHandler));
                }

            } catch (Exception e) {
                SECURITY_LOGGER.trace("Failure supplementing Subject", e);
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException(e);
            } finally {
                connectionHandler.close();
            }
        }

        private Set<RealmGroup> loadGroups(RealmUser user, LdapConnectionHandler connectionHandler) throws IOException, NamingException {
            LdapEntry entry = null;
            if (forceUserDnSearch == false && sharedState.containsKey(LdapEntry.class.getName())) {
                entry = (LdapEntry) sharedState.get(LdapEntry.class.getName());
                SECURITY_LOGGER.tracef("Loaded from sharedState '%s'", entry);
            }
            if (entry == null || user.getName().equals(entry.getSimpleName())==false) {
                entry = userSearcher.search(connectionHandler, user.getName()).getResult();
                SECURITY_LOGGER.tracef("Performed userSearch '%s'", entry);
            }

            return loadGroups(entry, connectionHandler);
        }

        private Set<RealmGroup> loadGroups(LdapEntry entry, LdapConnectionHandler connectionHandler) throws IOException, NamingException {
            Set<RealmGroup> realmGroups = new HashSet<RealmGroup>();

            Stack<LdapEntry[]> entries = new Stack<LdapEntry[]>();
            entries.push(loadGroupEntries(entry, connectionHandler));
            while (entries.isEmpty() == false) {
                LdapEntry[] found = entries.pop();
                for (LdapEntry current : found) {
                    RealmGroup group = new RealmGroup(realmName, groupName == GroupName.SIMPLE ? current.getSimpleName() : current.getDistinguishedName());
                    SECURITY_LOGGER.tracef("Adding RealmGroup '%s'", group);
                    realmGroups.add(group);
                    if (iterative) {
                        SECURITY_LOGGER.tracef("Performing iterative load for %s", current);
                        entries.push(loadGroupEntries(current, connectionHandler));
                    }
                }
            }

            return realmGroups;
        }

        private LdapEntry[] loadGroupEntries(LdapEntry entry, LdapConnectionHandler connectionHandler) throws IOException, NamingException {
            if (searchedPerformed.add(entry) == false) {
                SECURITY_LOGGER.tracef("A search has already been performed for %s", entry);
                return new LdapEntry[0];
            }

            return groupSearcher.search(connectionHandler, entry).getResult();
        }

    }

    public static final class ServiceUtil {

        private static final String SERVICE_SUFFIX = "ldap-authorization";

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }

    }

}
