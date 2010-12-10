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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Remove a deployment from the domain.
 *
 * @author Brian Stansberry
 */
public final class DomainDeploymentRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String uniqueName;

    /**
     * Construct a new instance.
     *
     * @param uniqueName the user-supplied unique name of the deployment
     */
    public DomainDeploymentRemove(final String uniqueName) {
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
        Set<String> mappings = element.getServerGroupDeploymentsMappings(uniqueName);
        if (mappings.size() > 0) {
            Iterator<String> iter = mappings.iterator();
            StringBuffer sb = new StringBuffer(iter.next());
            while (iter.hasNext()) {
                sb.append(", ");
                sb.append(iter.next());
            }
            throw new UpdateFailedException("Deployment " + uniqueName + " cannot be removed as it is mapped to server groups " + sb.toString());
        }
        if (! element.removeDeployment(uniqueName)) {
            throw new UpdateFailedException("Deployment '" + uniqueName + "' is not configured");
        }
    }

    @Override
    public DomainDeploymentAdd getCompensatingUpdate(final DomainModel original) {
        DeploymentUnitElement due = original.getDeployment(uniqueName);
        return due == null ? null : new DomainDeploymentAdd(uniqueName, due.getRuntimeName(), due.getSha1Hash(), due.isStart());
    }

    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {
        return Collections.emptyList();
    }
}
