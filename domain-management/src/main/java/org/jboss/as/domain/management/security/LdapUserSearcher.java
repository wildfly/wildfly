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

import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Interface for the utility which searches for a users LdapEntry based on the supplied name.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface LdapUserSearcher {

    LdapEntry userSearch(final DirContext dirContext, final String suppliedName) throws IOException, NamingException;

    public static final class ServiceUtil {

        private static final String AUTH_SERVICE_SUFFIX = "ldap_authentication.user_search";
        private static final String AUTHZ_SERVICE_SUFFIX = "ldap_authorization.user_search";

        private ServiceUtil() {
        }

        /**
         * Utility method to create the ServiceName for services that provide {@code LdapUserSearcher} instances.
         *
         * @param realmName - The name of the realm the {@code LdapUserSearcher} is associated with.
         * @param forAuthentication - Is this for user loading during authentication or during authorization / group loading.
         * @return The constructed ServiceName.
         */
        public static ServiceName createServiceName(final boolean forAuthentication, final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(
                    forAuthentication ? AUTH_SERVICE_SUFFIX : AUTHZ_SERVICE_SUFFIX);
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<LdapUserSearcher> injector,
                boolean forAuthentication, String realmName) {
            ServiceBuilder.DependencyType type = ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, createServiceName(forAuthentication, realmName), LdapUserSearcher.class, injector);

            return sb;
        }
    }

}
