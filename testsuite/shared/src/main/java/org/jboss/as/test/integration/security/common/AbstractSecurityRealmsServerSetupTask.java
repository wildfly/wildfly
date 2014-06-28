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
package org.jboss.as.test.integration.security.common;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;


/**
 * {@link org.jboss.as.arquillian.api.ServerSetupTask} instance for security realms setup.
 *
 * @author Josef Cacek
 * @see org.jboss.as.domain.management.SecurityRealm
 */
public abstract class AbstractSecurityRealmsServerSetupTask extends AbstractBaseSecurityRealmsServerSetupTask implements ServerSetupTask {

    protected ManagementClient managementClient;

    // Public methods --------------------------------------------------------

    /**
     * Adds security realms retrieved from {@link #getSecurityRealms()}.
     */
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        this.managementClient = managementClient;
        setup(managementClient.getControllerClient(), containerId);
    }

    /**
     * Removes the security realms from the AS configuration.
     *
     * @param managementClient
     * @param containerId
     */
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        tearDown(managementClient.getControllerClient(), containerId);
        this.managementClient = null;
    }

}

