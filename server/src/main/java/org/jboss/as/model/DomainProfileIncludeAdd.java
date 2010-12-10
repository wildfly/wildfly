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
 * Add the inclusion of another profile to a profile.
 *
 * @author Brian Stansberry
 */
public final class DomainProfileIncludeAdd extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String profileName;
    private final String includedProfileName;

    /**
     * Construct a new instance.
     *
     * @param profileName the name of the profile to add
     */
    public DomainProfileIncludeAdd(final String profileName, final String includedProfileName) {
        this.profileName = profileName;
        this.includedProfileName = includedProfileName;
    }

    /**
     * Get the profile name to add the included profile to.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Gets the name of the profile that is included.
     * @return the profile name
     */
    public String getIncludedProfileName() {
        return includedProfileName;
    }

    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        ProfileElement pe = element.getProfile(profileName);
        if (pe == null) {
            throw new UpdateFailedException("Profile '" + profileName + "' does not exist");
        }
        if (! pe.addIncludedProfile(includedProfileName)) {
            throw new UpdateFailedException("Profile '" + profileName + "' already includes profile '" + includedProfileName +"'");
        }
    }

    @Override
    public DomainProfileIncludeRemove getCompensatingUpdate(final DomainModel original) {
        return new DomainProfileIncludeRemove(profileName, includedProfileName);
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
