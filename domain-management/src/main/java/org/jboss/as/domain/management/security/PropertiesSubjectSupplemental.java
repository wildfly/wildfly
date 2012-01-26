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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.domain.management.RealmRole;
import org.jboss.as.domain.management.RealmUser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesSubjectSupplemental extends PropertiesFileLoader implements Service<SubjectSupplemental>,
        SubjectSupplemental {

    public static final String SERVICE_SUFFIX = "properties_authorization";
    private static final String COMMA = ",";

    public PropertiesSubjectSupplemental(ModelNode properties) {
        super(properties.require(PATH).asString());
    }

    /*
     * Service Methods
     */

    public SubjectSupplemental getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
        super.start(context);
    }

    public void stop(StopContext context) {
        super.stop(context);
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
        // identities so load the roles for them all.
        for (RealmUser current : users) {
            principals.addAll(loadRoles(properties, current));
        }
    }

    private Set<RealmRole> loadRoles(final Properties properties, final RealmUser user) {
        Set<RealmRole> response;
        String rolesString = properties.getProperty(user.getName(), "").trim();
        if (rolesString.length() > 0) {
            String[] roles = rolesString.split(COMMA);
            response = new HashSet<RealmRole>(roles.length);
            for (String current : roles) {
                String cleaned = current.trim();
                if (cleaned.length() > 0) {
                    response.add(new RealmRole(cleaned));
                }
            }
        } else {
            response = Collections.emptySet();
        }

        return response;
    }

}
