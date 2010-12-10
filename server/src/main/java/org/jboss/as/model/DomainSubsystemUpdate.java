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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainSubsystemUpdate<E extends AbstractSubsystemElement<E>, R> extends AbstractDomainModelUpdate<R> {
    private static final long serialVersionUID = -7899677507905342444L;

    private final String profileName;
    private final AbstractSubsystemUpdate<E, R> update;

    /**
     * Construct a new instance.
     *
     * @param profileName the name of the profile to modify
     * @param update the subsystem update to apply to the profile
     */
    public DomainSubsystemUpdate(final String profileName, final AbstractSubsystemUpdate<E, R> update) {
        this.profileName = profileName;
        this.update = update;
    }

    /**
     * Construct a new instance.
     *
     * @param profileName the name of the profile to modify
     * @param update the subsystem update to apply to the profile
     * @param <E>
     * @param <R>
     * @return the new instance
     */
    public static <E extends AbstractSubsystemElement<E>, R> DomainSubsystemUpdate<E, R> create(final String profileName, final AbstractSubsystemUpdate<E, R> update) {
        return new DomainSubsystemUpdate<E,R>(profileName, update);
    }

    public String getProfileName() {
        return profileName;
    }

    public AbstractSubsystemUpdate<?, ? extends R> getUpdate() {
        return update;
    }

    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        final ProfileElement profileElement = element.getProfile(profileName);
        final String namespaceUri = update.getSubsystemNamespaceUri();
        final E subsystemElement = update.getModelElementType().cast(profileElement.getSubsystem(namespaceUri));
        if (subsystemElement == null) {
            throw new UpdateFailedException("No such subsystem '" + namespaceUri + "' declared on profile '" + profileName + "'");
        }
        update.applyUpdate(subsystemElement);
    }

    @Override
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(final DomainModel original) {
        final String namespaceUri = update.getSubsystemNamespaceUri();
        final E element = update.getModelElementType().cast(original.getProfile(profileName).getSubsystem(namespaceUri));
        if (element == null) {
            throw new IllegalArgumentException("No such subsystem '" + namespaceUri + "' declared on profile '" + profileName + "'");
        }
        final AbstractSubsystemUpdate<E, ?> compensatingUpdate = update.getCompensatingUpdate(element);
        return createUpdate(profileName, compensatingUpdate);
    }

    private static <E extends AbstractSubsystemElement<E>, R> DomainSubsystemUpdate<E, R> createUpdate(final String profileName, final AbstractSubsystemUpdate<E, R> update) {
        return new DomainSubsystemUpdate<E,R>(profileName, update);
    }

    @Override
    public AbstractServerModelUpdate<R> getServerModelUpdate() {
        return new ServerSubsystemUpdate<E, R>(update);
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {
        if (getServerModelUpdate() == null) {
            return Collections.emptyList();
        }
        else {
            List<String> result = new ArrayList<String>();

            // Relevant profiles keyed by the name of those that include them
            Map<String, Set<String>> includedProfilesByRoot = new HashMap<String, Set<String>>();

            for (String server : hostModel.getActiveServerNames()) {
                String serverGroupName =  hostModel.getServer(server).getServerGroup();
                String sgProfileName = domainModel.getServerGroup(serverGroupName).getProfileName();

                if (!includedProfilesByRoot.containsKey(sgProfileName)) {
                    // Find all the profiles related to this server group's profile
                    includedProfilesByRoot.put(sgProfileName, domainModel.getProfiles(sgProfileName).keySet());
                }

                if (includedProfilesByRoot.get(sgProfileName).contains(profileName)) {
                    result.add(server);
                }
            }
            return result;
        }
    }
}
