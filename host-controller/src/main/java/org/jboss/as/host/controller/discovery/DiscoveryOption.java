/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller.discovery;

/**
 * Allow the discovery of a remote domain controller's host and port.
 *
 * @author Farah Juma
 */
public interface DiscoveryOption {

    /**
     *  Allow a domain controller's host name and port number to be discovered.
     *  This method is intended to be called by the domain controller.
     *
     *  @param host the host name of the domain controller
     *  @param port the port number of the domain controller
     */
    void allowDiscovery(String host, int port);

    /**
     *  Determine the host name and port of the remote domain controller.
     *  This method is intended to be called by a slave host controller.
     */
    void discover();

    /**
     * Clean up anything that was created for domain controller discovery.
     */
    void cleanUp();

    /**
     *  Gets the host name of the remote domain controller.
     *
     *  @return the host name
     */
    String getRemoteDomainControllerHost();

    /**
     * Gets the port of the remote domain controller.
     *
     * @return the port number
     */
    int getRemoteDomainControllerPort();
}
