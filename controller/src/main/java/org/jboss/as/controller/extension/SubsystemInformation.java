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

package org.jboss.as.controller.extension;

import java.util.List;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
*/
public interface SubsystemInformation {

    /**
     * Gets the URIs of the XML namespaces the subsystem can parse.
     *
     * @return list of XML namespace URIs. Will not return {@code null}
     */
    List<String> getXMLNamespaces();

    /**
     * Gets the major version of the subsystem's management interface, if available.
     *
     * @return the major interface version, or {@code null} if the subsystem does not have a versioned interface
     */
    Integer getManagementInterfaceMajorVersion();

    /**
     * Gets the minor version of the subsystem's management interface, if available.
     *
     * @return the minor interface version, or {@code null} if the subsystem does not have a versioned interface
     */
    Integer getManagementInterfaceMinorVersion();

    /**
     * Gets the micro version of the subsystem's management interface, if available.
     *
     * @return the micro interface version, or {@code null} if the subsystem does not have a versioned interface
     */
    Integer getManagementInterfaceMicroVersion();
}
