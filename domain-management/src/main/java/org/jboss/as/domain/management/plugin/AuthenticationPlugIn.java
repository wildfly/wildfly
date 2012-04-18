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
 * The interface to be implemented by a plug-in providing identity information during the authentication process.
 *
 * Plug-Ins can also optionally implement either the {@link PlugInConfigurationSupport} or {@link PlugInLifecycleSupport}
 * interfaces to be provided with configuration and a shared state map and optionally to recieve a notification once the
 * authentication process has completed.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface AuthenticationPlugIn<T extends Credential> {

    /**
     * Load the identity for the given username and realm.
     *
     * The returned identity contains the credential that will be used for verification of the remote user in the authentication
     * process.
     *
     * @param userName - The username supplied by the user.
     * @param realm - Either the realm supplied by the user or the name of the realm this plug-in is executing within.
     * @return The identity requested.
     * @throws IOException - If there is any problem loading the identity or if the identity is not found.
     */
    Identity<T> loadIdentity(final String userName, final String realm) throws IOException;

}
