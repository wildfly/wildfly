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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesSubjectSupplemental extends PropertiesFileLoader implements Service<SubjectSupplementalService>, SubjectSupplementalService,
        SubjectSupplemental {

    private static final String SERVICE_SUFFIX = "properties_authorization";
    private static final String COMMA = ",";

    private final String realmName;

    public PropertiesSubjectSupplemental(final String realmName, final String path) {
        super(path);
        this.realmName = realmName;
    }

    /*
     * Service Methods
     */

    public SubjectSupplementalService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
        super.start(context);
    }

    public void stop(StopContext context) {
        super.stop(context);
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
        Properties properties = getProperties();
        // In general we expect exactly one RealmUser, however we could cope with multiple
        // identities so load the groups for them all.
        for (RealmUser current : users) {
            principals.addAll(loadGroups(properties, current));
        }
    }

    private Set<RealmGroup> loadGroups(final Properties properties, final RealmUser user) {
        Set<RealmGroup> response;
        String groupString = properties.getProperty(user.getName(), "").trim();
        if (groupString.length() > 0) {
            String[] groups = groupString.split(COMMA);
            response = new HashSet<RealmGroup>(groups.length);
            for (String current : groups) {
                String cleaned = current.trim();
                if (cleaned.length() > 0) {
                    RealmGroup newGroup = new RealmGroup(realmName, cleaned);
                    SECURITY_LOGGER.tracef("Adding group '%s' for user '%s'.", newGroup, user);
                    response.add(newGroup);
                }
            }
        } else {
            SECURITY_LOGGER.tracef("No roles found for user '%s' in properties file.", user);
            response = Collections.emptySet();
        }

        return response;
    }

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }
    }

}
