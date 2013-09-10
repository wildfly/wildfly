/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.security.auth.callback.CallbackHandler;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 *  Interface to represent a CallbackHandlerFactory
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface CallbackHandlerFactory {

    /**
     * Create a CallbackHandler for the specified username.
     *
     * @param username - The username to be set by the created CallbackHandler
     * @return the CallbackHandler
     */
    CallbackHandler getCallbackHandler(final String username);

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<CallbackHandlerFactory> injector,
                ServiceName serviceName, boolean optional) {
            ServiceBuilder.DependencyType type = optional ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, serviceName, CallbackHandlerFactory.class, injector);

            return sb;
        }

    }

}
