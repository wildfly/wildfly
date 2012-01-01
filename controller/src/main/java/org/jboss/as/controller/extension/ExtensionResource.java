/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * {@link Resource} representing an {@link Extension}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionResource implements Resource.ResourceEntry {

    private static final Set<String> CHILD_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(SUBSYSTEM)));

    private final String moduleName;
    private final ExtensionRegistry extensionRegistry;

    public ExtensionResource(String moduleName, ExtensionRegistry extensionRegistry) {
        assert moduleName != null : "moduleName is null";
        assert extensionRegistry != null : "extensionRegistry is null";

        this.moduleName = moduleName;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return moduleName;
    }

    @Override
    public PathElement getPathElement() {
        return PathElement.pathElement(EXTENSION, moduleName);
    }

    @Override
    public ModelNode getModel() {
        final ModelNode model = new ModelNode();
        model.get(ExtensionResourceDefinition.MODULE.getName()).set(moduleName);
        return model;
    }

    @Override
    public void writeModel(ModelNode newModel) {
        // ApplyRemoteMasterDomainModelHandler does this. Just ignore it.
    }

    @Override
    public boolean isModelDefined() {
        return true;
    }

    @Override
    public boolean hasChild(PathElement element) {
        return getChildrenNames(element.getKey()).contains(element.getValue());
    }

    @Override
    public Resource getChild(PathElement element) {
        Resource result = null;
        if (SUBSYSTEM.equals(element.getKey())) {
            Map<String, ExtensionRegistry.SubsystemInformation> entry = extensionRegistry.getAvailableSubsystems(moduleName);
            ExtensionRegistry.SubsystemInformation info = entry != null ? entry.get(element.getValue()) : null;
            if (info != null) {
                return new SubsystemResource(element.getValue(), info);
            }
        }
        return result;
    }

    @Override
    public Resource requireChild(PathElement element) {
        Resource child = getChild(element);
        if (child == null) {
            throw new NoSuchResourceException(element);
        }
        return child;
    }

    @Override
    public boolean hasChildren(String childType) {
        return getChildren(childType).size() > 0;
    }

    @Override
    public Resource navigate(PathAddress address) {
        int size = address.size();
        if (size == 0) {
            return this;
        }
        PathElement pe = address.getElement(0);
        Resource child = getChild(pe);
        if (child != null) {
            return size == 1 ? child : child.navigate(address.subAddress(1));
        } else {
            throw new NoSuchResourceException(pe);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        Set<String> result = null;
        if (SUBSYSTEM.equals(childType)) {
            Map<String, ExtensionRegistry.SubsystemInformation> info = extensionRegistry.getAvailableSubsystems(moduleName);
            result = info != null ? new HashSet<String>(info.keySet()) : null;
        }
        return result != null ? result : Collections.<String>emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        Set<ResourceEntry> result = null;
        if (SUBSYSTEM.equals(childType)) {
            result = new HashSet<ResourceEntry>();
            Map<String, ExtensionRegistry.SubsystemInformation> entry = extensionRegistry.getAvailableSubsystems(moduleName);
            if (entry != null) {
                for (Map.Entry<String, ExtensionRegistry.SubsystemInformation> subsys : entry.entrySet()) {
                    result.add(new SubsystemResource(subsys.getKey(), subsys.getValue()));
                }
            }
        }
        return result;
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public ExtensionResource clone() {
        return new ExtensionResource(moduleName, extensionRegistry);
    }

    private static class SubsystemResource extends PlaceholderResource.PlaceholderResourceEntry {

        private final ExtensionRegistry.SubsystemInformation subsystemInformation;

        public SubsystemResource(final String name, ExtensionRegistry.SubsystemInformation subsystemInformation) {
            super(SUBSYSTEM, name);
            this.subsystemInformation = subsystemInformation;
        }

        @Override
        public ModelNode getModel() {
            final ModelNode model = new ModelNode();

            final ModelNode majorNode = model.get(ExtensionSubsystemResourceDefinition.MAJOR_VERSION.getName());
            Integer major = subsystemInformation.getManagementInterfaceMajorVersion();
            if (major != null) {
                majorNode.set(major.intValue());
            }
            final ModelNode minorNode = model.get(ExtensionSubsystemResourceDefinition.MINOR_VERSION.getName());
            Integer minor = subsystemInformation.getManagementInterfaceMinorVersion();
            if (minor != null) {
                minorNode.set(minor.intValue());
            }

            final ModelNode xmlNode = model.get(ExtensionSubsystemResourceDefinition.XML_NAMESPACES.getName()).setEmptyList();
            List<String> namespaces = subsystemInformation.getXMLNamespaces();
            if (namespaces != null) {
                for (String namespace : namespaces) {
                    xmlNode.add(namespace);
                }
            }

            return model;
        }

        @Override
        public boolean isModelDefined() {
            return true;
        }

        @Override
        public PlaceholderResourceEntry clone() {
            return new SubsystemResource(getName(), subsystemInformation);
        }
    }
}
