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

import org.jboss.as.SubsystemFactory;

/**
 * Add a subsystem to a domain profile.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainSubsystemAdd extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -9076890219875153928L;

    private final String profileName;
    private final String namespaceUri;

    /**
     * Construct a new instance.
     *
     * @param profileName the name of the profile that the change applies to
     * @param namespaceUri the namespace URI of the subsystem to configure
     */
    public DomainSubsystemAdd(final String profileName, final String namespaceUri) {
        this.profileName = profileName;
        this.namespaceUri = namespaceUri;
    }

    /**
     * Get the profile name to add the subsystem to.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Get the namespace URI of the subsystem.
     *
     * @return the namespace URI
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        final SubsystemFactory<?> factory = element.getSubsystemFactory(namespaceUri);
        if (factory == null) {
            throw new UpdateFailedException("Subsystem '" + namespaceUri + "' is not configured in this domain");
        }
        if (! element.getProfile(profileName).addSubsystem(namespaceUri, factory.createSubsystemElement())) {
            throw new UpdateFailedException("Subsystem '" + namespaceUri + "' is already configured in profile '" + profileName + "'");
        }
    }

    public DomainSubsystemRemove getCompensatingUpdate(final DomainModel original) {
        return new DomainSubsystemRemove(profileName, namespaceUri);
    }

    protected ServerSubsystemAdd getServerModelUpdate() {
        return null;
    }
}
