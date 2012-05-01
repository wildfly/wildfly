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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.domain.management.plugin.AuthorizationPlugIn;
import org.jboss.as.domain.management.plugin.PlugInConfigurationSupport;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PlugInSubjectSupplemental extends AbstractPlugInService implements Service<SubjectSupplementalService>,
        SubjectSupplementalService {

    public static final String SERVICE_SUFFIX = "plug-in-authorization";

    private final String realmName;

    PlugInSubjectSupplemental(final String realmName, final ModelNode model) {
        super(model);
        this.realmName = realmName;
    }

    /*
     * Service Methods
     */

    // The methods in the base class are sufficient.

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
                    principals.addAll(loadRoles(current));
                }
            }

            private Set<RealmRole> loadRoles(final RealmUser user) throws IOException {
                Set<RealmRole> response;
                String[] roles = ap.loadRoles(user.getName(), realmName);
                response = new HashSet<RealmRole>(roles.length);
                for (String current : roles) {
                    response.add(new RealmRole(current));
                }
                return response;
            }

        };

    }

}
