/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED_RESOURCES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WILDCARD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.RemoteDomainConnectionService;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Registration information provided by a slave Host Controller.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class HostInfo implements TransformationTarget.IgnoredTransformationRegistry {

    /**
     * Create the metadata which gets send to the DC when registering.
     *
     *
     * @param hostInfo the local host info
     * @param productConfig the product config
     * @param ignoredResourceRegistry registry of ignored resources
     * @return the host info
     */
    public static ModelNode createLocalHostHostInfo(final LocalHostControllerInfo hostInfo, final ProductConfig productConfig,
                                             final IgnoredDomainResourceRegistry ignoredResourceRegistry) {
        final ModelNode info = new ModelNode();
        info.get(NAME).set(hostInfo.getLocalHostName());
        info.get(RELEASE_VERSION).set(Version.AS_VERSION);
        info.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        info.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        info.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);
        info.get(MANAGEMENT_MICRO_VERSION).set(Version.MANAGEMENT_MICRO_VERSION);
        final String productName = productConfig.getProductName();
        final String productVersion = productConfig.getProductVersion();
        if(productName != null) {
            info.get(PRODUCT_NAME).set(productName);
        }
        if(productVersion != null) {
            info.get(PRODUCT_VERSION).set(productVersion);
        }
        ModelNode ignoredModel = Resource.Tools.readModel(ignoredResourceRegistry.getRootResource());
        info.get(IGNORED_RESOURCES).set(ignoredModel);
        return info;
    }

    public static HostInfo fromModelNode(final ModelNode hostInfo) {
        return new HostInfo(hostInfo);
    }

    private final String hostName;
    private final String releaseVersion;
    private final String releaseCodeName;
    private final int managementMajorVersion;
    private final int managementMinorVersion;
    private final int managementMicroVersion;
    private final String productName;
    private final String productVersion;
    private final Long remoteConnectionId;
    private final Map<String, IgnoredType> ignoredResources;

    private HostInfo(final ModelNode hostInfo) {
        hostName = hostInfo.require(NAME).asString();
        releaseVersion = hostInfo.require(RELEASE_VERSION).asString();
        releaseCodeName = hostInfo.require(RELEASE_CODENAME).asString();
        managementMajorVersion = hostInfo.require(MANAGEMENT_MAJOR_VERSION).asInt();
        managementMinorVersion = hostInfo.require(MANAGEMENT_MINOR_VERSION).asInt();
        managementMicroVersion = hostInfo.hasDefined(MANAGEMENT_MICRO_VERSION) ? hostInfo.require(MANAGEMENT_MICRO_VERSION).asInt() : 0;
        productName = hostInfo.hasDefined(PRODUCT_NAME) ? hostInfo.require(PRODUCT_NAME).asString() : null;
        productVersion = hostInfo.hasDefined(PRODUCT_VERSION) ? hostInfo.require(PRODUCT_VERSION).asString() : null;
        remoteConnectionId = hostInfo.hasDefined(RemoteDomainConnectionService.DOMAIN_CONNECTION_ID)
                ? hostInfo.get(RemoteDomainConnectionService.DOMAIN_CONNECTION_ID).asLong() : null;

        if (hostInfo.hasDefined(IGNORED_RESOURCES)) {
            ignoredResources = new HashMap<String, IgnoredType>();
            for (Property prop : hostInfo.require(IGNORED_RESOURCES).asPropertyList()) {
                String type = prop.getName();
                ModelNode model = prop.getValue();
                IgnoredType ignoredType = model.get(WILDCARD).asBoolean(false) ? new IgnoredType() : new IgnoredType(model.get(NAMES));
                ignoredResources.put(type, ignoredType);
            }
        } else {
            ignoredResources = null;
        }
    }

    public String getHostName() {
        return hostName;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getReleaseCodeName() {
        return releaseCodeName;
    }

    public int getManagementMajorVersion() {
        return managementMajorVersion;
    }

    public int getManagementMinorVersion() {
        return managementMinorVersion;
    }

    public int getManagementMicroVersion() {
        return managementMicroVersion;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public Long getRemoteConnectionId() {
        return remoteConnectionId;
    }

    public boolean isResourceTransformationIgnored(final PathAddress address) {
        boolean result = false;
        if (ignoredResources != null && address.size() > 0) {
            PathElement firstElement = address.getElement(0);
            IgnoredType ignoredType = ignoredResources.get(firstElement.getKey());
            if (ignoredType != null) {
                result = ignoredType.hasName(firstElement.getValue());
            }
        }
        return result;
    }

    @Override
    public boolean isOperationTransformationIgnored(PathAddress address) {
        // Currently the master receives no notification from slaves when changes are made
        // to the IgnoredResourceRegistry post-boot, so we can't ignore operation transformation
        return false;
    }

    public String getPrettyProductName() {

        final String result;
        if(productName != null) {
            result = ProductConfig.getPrettyVersionString(productName, productVersion, releaseVersion);
        } else {
            result = ProductConfig.getPrettyVersionString(null, releaseVersion, releaseCodeName);
        }
        return result;
    }

    private static class IgnoredType {
        private final boolean wildcard;
        private final Set<String> names;

        private IgnoredType() {
            wildcard = true;
            names = null;
        }

        private IgnoredType(ModelNode names) {
            wildcard = false;
            if (names.isDefined()) {
                this.names = new HashSet<String>();
                for (ModelNode name : names.asList()) {
                    this.names.add(name.asString());
                }
            } else {
                this.names = null;
            }
        }

        private boolean hasName(String name) {
            return wildcard || (names != null && names.contains(name));
        }
    }
}
