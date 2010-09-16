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

package org.jboss.as.model;

/**
 * Update used when adding a deployment unit to a server group.
 *
 * @author John E. Bailey
 */
public class ServerGroupAddDeploymentUpdate extends AbstractModelUpdate<ServerGroupElement> {

    private static final long serialVersionUID = -5275795083851579399L;
    private final DeploymentUnitKey key;

    public ServerGroupAddDeploymentUpdate(final String fileName, final byte[] sha1Hash) {
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
    }

    @Override
    protected Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }

    @Override
    protected AbstractModelUpdate<ServerGroupElement> applyUpdate(ServerGroupElement serverGroup) {
        return null; // TODO: return a deployment removal update
    }


}
