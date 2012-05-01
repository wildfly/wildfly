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
 * An abstract plug-in implementation that can be extended as an alternative to implementing the interfaces.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class AbstractPlugIn implements AuthenticationPlugIn<Credential>, AuthorizationPlugIn, PlugInConfigurationSupport {

    protected Map<String, String> configuration;
    protected Map<String, Object> sharedState;

    protected String[] roles = new String[0];

    /**
     * @see org.jboss.as.domain.management.plugin.PlugInConfigurationSupport#init(java.util.Map, java.util.Map)
     */
    public void init(Map<String, String> configuration, Map<String, Object> sharedState) throws IOException {
        this.configuration = configuration;
        this.sharedState = sharedState;
    }

    /**
     * @see org.jboss.as.domain.management.plugin.AuthenticationPlugIn#loadIdentity(java.lang.String, java.lang.String)
     */
    public Identity<Credential> loadIdentity(String userName, String realm) throws IOException {
        throw new IOException();
    }

    /**
     * @see org.jboss.as.domain.management.plugin.AuthorizationPlugIn#loadRoles(java.lang.String, java.lang.String)
     */
    public String[] loadRoles(String userName, String realm) throws IOException {
        return roles;
    }

}
