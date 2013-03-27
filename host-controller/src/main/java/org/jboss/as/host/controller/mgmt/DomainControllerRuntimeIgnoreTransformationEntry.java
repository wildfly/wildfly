/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller.mgmt;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.RuntimeIgnoreTransformation;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil.ServerConfigInfo;
import org.jboss.util.collection.ConcurrentSet;

/**
 * Used on the DC. Maintains the known domain level resource addresses and server groups for a particular host
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainControllerRuntimeIgnoreTransformationEntry implements RuntimeIgnoreTransformation {

    private final HostInfo hostInfo;
    private final IgnoredNonAffectedServerGroupsUtil util;
    private volatile ConcurrentSet<PathElement> knownRootAddresses;

    private DomainControllerRuntimeIgnoreTransformationEntry(HostInfo hostInfo, ExtensionRegistry extensionRegistry) {
        this.hostInfo = hostInfo;
        this.util = IgnoredNonAffectedServerGroupsUtil.create(extensionRegistry);
    }

    /**
     * Static factory
     *
     * @param hostInfo the host info for the slave
     * @param extensionRegistry the DC extension registry
     * @return the created instance
     */
    static DomainControllerRuntimeIgnoreTransformationEntry create(HostInfo hostInfo, ExtensionRegistry extensionRegistry) {
        return new DomainControllerRuntimeIgnoreTransformationEntry(hostInfo, extensionRegistry);
    }

    /**
     * Callback for {@link DomainControllerRuntimeIgnoreTransformationRegistry} to initialize the known addresses map
     *
     * @param knownRootAddresses the known addresses map
     */
    void setKnownRootAddresses(ConcurrentSet<PathElement> knownRootAddresses) {
        this.knownRootAddresses = knownRootAddresses;
    }

    /**
     * Whether the slave host is set up to ignore unaffected config
     *
     * @return {@code true} if the slave is set up to ignore unaffected config
     */
    boolean isIgnoreUnaffactedConfig() {
        return hostInfo.isIgnoreUnaffectedConfig();
    }

    /**
     * Whether the host should ignore the domain resource
     *
     * @param domainResource the root domain resource
     * @param the address to check
     * @return {@code true} if we are to ignore the resource
     */
    @Override
    public boolean ignoreResource(Resource domainResource, PathAddress address) {
        if (address.size() != 1) {
            return false;
        }
        if (hostInfo.isIgnoreUnaffectedConfig()) {
            if (knownRootAddresses != null && address.size() >= 0 && knownRootAddresses.contains(address.getElement(0))) {
                return false;
            }
            return util.ignoreResource(domainResource, hostInfo.getServerConfigInfos(), address);
        }
        return false;
    }

    /**
     * Add/update a server config info
     *
     * @param serverInfo the server config info
     */
    void updateSlaveServerConfig(ServerConfigInfo serverInfo) {
        if (hostInfo.isIgnoreUnaffectedConfig()) {
            hostInfo.updateSlaveServerConfigInfo(serverInfo);
        }
    }

    /**
     * Gets all the unknown extensions for a profile's subsystems
     *
     * @param domainResource the root domain resource
     * @param profileElement the profile address to check
     * @return the unknown extensions
     */
    Set<PathElement> getUnknownExtensionsForProfile(Resource domainResource, PathElement profileElement) {
        Set<PathElement> allExtensions = util.getAllExtensionsForProfile(domainResource, profileElement);
        Set<PathElement> unknownExtensions = new HashSet<>();
        for (PathElement extensionElement : allExtensions) {
            if (!knownRootAddresses.contains(extensionElement)) {
                unknownExtensions.add(extensionElement);
            }
        }
        return unknownExtensions;
    }
}
