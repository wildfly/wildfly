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

package org.jboss.as.domain.management.plugin;

import java.io.IOException;

/**
 * Interface to be implemented by a plug-in providing role information used for authorization decisions.
 *
 * Plug-Ins can also optionally implement either the {@link PlugInConfigurationSupport} or {@link PlugInLifecycleSupport}
 * interfaces to be provided with configuration and a shared state map and optionally to recieve a notification once the
 * authentication process has completed.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface AuthorizationPlugIn {

    /**
     * Load and return a list of the roles for the given username and realm.
     *
     * The username and realm supplied here will be the same as supplied to the AuthenticationPlugIn, the reason they are
     * supplied again is that plug-ins are not required to work together so there may be no shared state.
     *
     * For plug-ins working together or for when the same plug-in is used for authentication and authorization it is safe to
     * assume that this method is being called for the same user.
     *
     * @param userName - The username supplied by the user.
     * @param realm - Either the realm supplied by the user or the name of the realm this plug-in is executing within.
     * @return A list of the users roles.
     * @throws IOException - If an error occurs loading the roles or if the identity can not be found.
     */
    String[] loadRoles(final String userName, final String realm) throws IOException;

}
