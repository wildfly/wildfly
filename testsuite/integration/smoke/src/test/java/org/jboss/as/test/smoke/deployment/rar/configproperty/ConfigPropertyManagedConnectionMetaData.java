/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.smoke.deployment.rar.configproperty;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * ConfigPropertyManagedConnectionMetaData
 *
 * @version $Revision: $
 */
public class ConfigPropertyManagedConnectionMetaData implements ManagedConnectionMetaData {
    /**
     * Default constructor
     */
    public ConfigPropertyManagedConnectionMetaData() {
    }

    /**
     * Returns Product name of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product name of the EIS instance
     * @throws javax.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return null;
    }

    /**
     * Returns Product version of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product version of the EIS instance
     * @throws javax.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return null;
    }

    /**
     * Returns maximum limit on number of active concurrent connections
     *
     * @return Maximum limit for number of active concurrent connections
     * @throws javax.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    /**
     * Returns name of the user associated with the ManagedConnection instance
     *
     * @return Name of the user
     * @throws javax.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getUserName() throws ResourceException {
        return null;
    }
}
