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
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.plugin.AuthorizationPlugIn;
import org.jboss.as.domain.management.plugin.PlugInConfigurationSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;

/**
 * The {@link SubjectSupplementalService} for Plug-Ins
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PlugInSubjectSupplemental extends AbstractPlugInService implements Service<SubjectSupplementalService>,
        SubjectSupplementalService {

    private static final String SERVICE_SUFFIX = "plug-in-authorization";

    PlugInSubjectSupplemental(final String realmName, final String name, final Map<String, String> properties) {
        super(realmName, name, properties);
    }

    /*
     * Service Methods
     */

    // The start/stop methods in the base class are sufficient.

    @Override
    public SubjectSupplementalService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public SubjectSupplemental getSubjectSupplemental(Map<String, Object> sharedState) {
        final String name = getPlugInName();
        final AuthorizationPlugIn ap = getPlugInLoader().loadAuthorizationPlugIn(name);
        if (ap instanceof PlugInConfigurationSupport) {
            PlugInConfigurationSupport pcf = (PlugInConfigurationSupport) ap;
            try {
                pcf.init(getConfiguration(), sharedState);
            } catch (IOException e) {
                throw MESSAGES.unableToInitialisePlugIn(name, e.getMessage());
            }
        }

        return new SubjectSupplemental() {

            public void supplementSubject(Subject subject) throws IOException {
                Set<RealmUser> users = subject.getPrincipals(RealmUser.class);
                Set<Principal> principals = subject.getPrincipals();
                // In general we expect exactly one RealmUser, however we could cope with multiple
                // identities so load the roles for them all.
                for (RealmUser current : users) {
                    principals.addAll(loadGroups(current));
                }
            }

            private Set<RealmGroup> loadGroups(final RealmUser user) throws IOException {
                Set<RealmGroup> response;
                String[] groups = ap.loadRoles(user.getName(), getRealmName());
                response = new HashSet<RealmGroup>(groups.length);
                for (String current : groups) {
                    RealmGroup newGroup = new RealmGroup(getRealmName(), current);
                    SECURITY_LOGGER.tracef("Adding group '%s' for user '%s'.", newGroup, user);
                    response.add(newGroup);
                }
                return response;
            }

        };

    }

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }
    }

}
