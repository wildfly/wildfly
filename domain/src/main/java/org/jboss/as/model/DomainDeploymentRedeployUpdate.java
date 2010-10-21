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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;

/**
 * Add a deployment to the domain and replace all occurences of an earlier
 * version of the same deployment.
 *
 * @author Brian Stansberry
 */
public final class DomainDeploymentRedeployUpdate extends AbstractDomainModelUpdate<ServerDeploymentActionResult> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String uniqueName;

    /**
     * Construct a new instance.
     *
     * @param uniqueName the user-supplied unique name of the deployment
     *
     */
    public DomainDeploymentRedeployUpdate(final String uniqueName) {
        if (uniqueName == null)
            throw new IllegalArgumentException("uniqueName is null");
        this.uniqueName = uniqueName;
    }

    /**
     * Get the user-supplied unique name of the deployment.
     *
     * @return the unique name. Will not be {@code null}
     */
    public String getUniqueName() {
        return uniqueName;
    }


    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        // no-op
    }

    @Override
    public DomainDeploymentRedeployUpdate getCompensatingUpdate(final DomainModel original) {
        return this;
    }

    @Override
    public ServerModelDeploymentReplaceUpdate getServerModelUpdate() {
        return new ServerModelDeploymentReplaceUpdate(uniqueName, uniqueName);
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {

        // Finds groups with our deployment
        Set<String> groupNames = new HashSet<String>(domainModel.getServerGroupNames());
        for (Iterator<String> it = groupNames.iterator(); it.hasNext();) {
            String groupName = it.next();
            ServerGroupElement group = domainModel.getServerGroup(groupName);
            ServerGroupDeploymentElement sgde = group.getDeployment(uniqueName);
            if (sgde == null) {
                it.remove();
            }
        }

        // Find servers in those groups
        List<String> result = new ArrayList<String>();
        for (String serverName : hostModel.getActiveServerNames()) {
            ServerElement server = hostModel.getServer(serverName);
            if (groupNames.contains(server.getServerGroup())) {
                result.add(serverName);
            }
        }

        return result;
    }
}
