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
import java.util.List;

/**
 * Remove a profile from the domain.
 *
 * @author Brian Stansberry
 */
public final class DomainProfileRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String profileName;

    /**
     * Construct a new instance.
     *
     * @param profileName the name of the profile to remove
     */
    public DomainProfileRemove(final String profileName) {
        this.profileName = profileName;
    }

    /**
     * Get the profile name to remove.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        // Validate empty
        ProfileElement pe = element.getProfile(profileName);
        if (pe != null && (pe.getIncludedProfiles().size() > 0 || pe.getSubsystems().size() > 0)) {
            throw new UpdateFailedException("Profile '" + profileName + "' is not empty and cannot be removed");
        }
        // Validate unused
        StringBuilder illegal = null;
        for (String serverGroupName : element.getServerGroupNames()) {
            ServerGroupElement sge = element.getServerGroup(serverGroupName);
            if (profileName.equals(sge.getName())) {
                if (illegal == null) {
                    illegal = new StringBuilder(sge.getName());
                }
                else {
                    illegal.append(", ");
                    illegal.append(sge.getName());
                }
            }
        }
        if (illegal != null) {
            throw new UpdateFailedException(String.format("Profile %s cannot be removed as it is still used by server groups %s", profileName, illegal.toString()));
        }

        element.removeProfile(profileName);
    }

    @Override
    public DomainProfileAdd getCompensatingUpdate(final DomainModel original) {
        return new DomainProfileAdd(profileName);
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
