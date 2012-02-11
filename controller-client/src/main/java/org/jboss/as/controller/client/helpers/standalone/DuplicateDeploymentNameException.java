/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.helpers.standalone;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

/**
 * Exception indicating an attempt to add deployment content to a domain or
 * server that has the same name as existing content.
 *
 * @author Brian Stansberry
 */
public class DuplicateDeploymentNameException extends Exception {

    private static final long serialVersionUID = -7207529184499737454L;

    private final String name;

    /**
     * @param name
     * @param fullDomain
     */
    public DuplicateDeploymentNameException(String name, boolean fullDomain) {
        super(fullDomain ? MESSAGES.domainDeploymentAlreadyExists(name) : MESSAGES.serverDeploymentAlreadyExists(name));
        this.name = name;
    }

    public String getDeploymentName() {
        return name;
    }

}
