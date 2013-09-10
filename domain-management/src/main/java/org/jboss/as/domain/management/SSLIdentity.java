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
package org.jboss.as.domain.management;

import javax.net.ssl.SSLContext;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Interface for services providing SSL identities through pre-configured SSLContexts.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SSLIdentity {

    /**
     * Obtain the full SSLContext comprising of both any defined key and trust stores.
     *
     * @return The SSLContext.
     */
    SSLContext getFullContext();

    /**
     * Obtain the SSLContext but only comprising the trust store definitions.
     *
     * @return The SSLContext.
     */
    SSLContext getTrustOnlyContext();

    public static final class ServiceUtil {

        private static final String SERVICE_SUFFIX = "ssl";

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<SSLIdentity> injector,
                String realmName, boolean optional) {
            ServiceBuilder.DependencyType type = optional ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, createServiceName(realmName), SSLIdentity.class, injector);

            return sb;
        }

    }

}
