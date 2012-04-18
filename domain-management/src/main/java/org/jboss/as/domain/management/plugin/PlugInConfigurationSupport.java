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
import java.util.Map;

/**
 * Optional Interface to be implemented by plug-ins to receive configuration and a shared state Map before being used to load
 * identity information.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface PlugInConfigurationSupport {

    /**
     * Initialisation method called before any request to pass in plug-in configuration and a shared state map to share state
     * between multiple plug-ins handling the same request.
     *
     * This method will not be called until we are ready to use the plug-in so unless this method fails one of the plug-in
     * methods will be used to retrieve information.
     *
     * Where an authentication plug-in and an authorization plug-in are defined init will be called on the authentication
     * plug-in first and on the authorization plug-in second. The call on the authorization plug-in will only happen if the
     * authentication step was a success. Updates to the shared state by the authentication plug-in will be visible by the
     * authorization plug-in during the call to it's init method.
     *
     * We reserve the right to include generic items ourselves within the provided configuration, for this reason plug-ins
     * should not reject a configuration just because some contents are not understood.
     *
     * @param configuration - The defined configuration for this plug-in. This Map is shared between all instances of the
     *        plug-in and is not modifiable.
     * @param sharedState - A Map of shared state to be shared by all plug-ins handling the same request.
     * @throws IOException If the plug-in can not complete the initialisation successfully.
     */
    void init(final Map<String, String> configuration, final Map<String, Object> sharedState) throws IOException;

}
