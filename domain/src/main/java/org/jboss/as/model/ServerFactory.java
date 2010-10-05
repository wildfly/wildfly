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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerFactory {

    private ServerFactory() {
    }

    /**
     * Combine a domain model and a host model to generate a list of bootstrap updates for a server to run.
     *
     * @param domainModel the domain model
     * @param hostModel the host model
     * @param serverName the name of the server to bootstrap
     * @param list the list to which the updates should be appended
     * @return TBD
     */
    public static Void combine(DomainModel domainModel, HostModel hostModel, String serverName, List<AbstractServerModelUpdate<?>> list) {
        final ServerElement serverElement = hostModel.getServer(serverName);
        final String serverGroupName = serverElement.getServerGroup();
        final ServerGroupElement serverGroup = domainModel.getServerGroup(serverGroupName);
        final String profileName = serverGroup.getProfileName();
        final ProfileElement leafProfile = domainModel.getProfile(profileName);

        // Merge interfaces

        // Merge extensions
        final Set<String> extensionNames = new LinkedHashSet<String>();
        for (String name : domainModel.getExtensions()) {
            extensionNames.add(name);
        }
        for (String name : hostModel.getExtensions()) {
            extensionNames.add(name);
        }
        for (String name : extensionNames) {
            list.add(new ServerExtensionAdd(name));
        }

        // Merge subsystems
        for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystemElement : leafProfile.getSubsystems()) {
            final AbstractSubsystemAdd subsystemAdd = subsystemElement.getAdd();
            list.add(new ServerSubsystemAdd(subsystemAdd));
            final List<AbstractSubsystemUpdate<? extends AbstractSubsystemElement<?>, ?>> subsystemList = new ArrayList<AbstractSubsystemUpdate<? extends AbstractSubsystemElement<?>,?>>();
            subsystemElement.getUpdates(subsystemList);
            for (AbstractSubsystemUpdate<? extends AbstractSubsystemElement<?>, ?> update : subsystemList) {
                list.add(ServerProfileUpdate.create(update));
            }
        }

        // Merge deployer config stuff

        // Merge deployments

        return null;
    }
}
