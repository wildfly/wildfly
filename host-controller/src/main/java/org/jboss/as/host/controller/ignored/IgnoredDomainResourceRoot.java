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

package org.jboss.as.host.controller.ignored;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * {@link Resource} that is registered in the {@code /host=*} resource under subaddress {@link #PATH_ELEMENT}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class IgnoredDomainResourceRoot implements Resource.ResourceEntry {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.IGNORED_RESOURCES);

    private IgnoredDomainResourceRegistry ignoredRegistry;
    private final Map<String, IgnoreDomainResourceTypeResource> children = new LinkedHashMap<String, IgnoreDomainResourceTypeResource>();

    IgnoredDomainResourceRoot(IgnoredDomainResourceRegistry ignoredRegistry) {
        this.ignoredRegistry = ignoredRegistry;
    }

    @Override
    public String getName() {
        return PATH_ELEMENT.getValue();
    }

    @Override
    public PathElement getPathElement() {
        return PATH_ELEMENT;
    }

    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        // this resource has no meaningful model
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    @Override
    public boolean hasChild(PathElement element) {
        synchronized (children) {
            return !isMaster() && IGNORED_RESOURCE_TYPE.equals(element.getKey())
                    && children.containsKey(element.getValue());
        }
    }

    @Override
    public Resource getChild(PathElement element) {
        Resource result = null;
        if (!isMaster() && IGNORED_RESOURCE_TYPE.equals(element.getKey())) {
            result = getChildInternal(element.getValue());
        }
        return result;
    }

    @Override
    public Resource requireChild(PathElement address) {
        final Resource resource = getChild(address);
        if(resource == null) {
            throw new NoSuchResourceException(address);
        }
        return resource;
    }

    @Override
    public boolean hasChildren(String childType) {
        boolean result = !isMaster() && IGNORED_RESOURCE_TYPE.equals(childType);
        if (result) {
            synchronized (children) {
                result = children.size() > 0;
            }
        }
        return result;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Resource.Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(IGNORED_RESOURCE_TYPE);
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        Set<String> result;
        if (!isMaster() && IGNORED_RESOURCE_TYPE.equals(childType)) {
            synchronized (children) {
                result = new HashSet<String>(children.keySet());
            }
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        Set<ResourceEntry> result;
        if (!isMaster() && IGNORED_RESOURCE_TYPE.equals(childType)) {
            synchronized (children) {
                result = new HashSet<ResourceEntry>(children.values());
            }
        } else {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (!isMaster() && IGNORED_RESOURCE_TYPE.equals(address.getKey())) {
            synchronized (children) {
                if (children.containsKey(address.getValue())) {
                    throw MESSAGES.duplicateResource(address.getValue());
                }
                registerChildInternal(IgnoreDomainResourceTypeResource.class.cast(resource));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        Resource result = null;
        if (IGNORED_RESOURCE_TYPE.equals(address.getKey())) {
            synchronized (children) {
                result = children.remove(address.getValue());
            }
        }
        return result;
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
    public IgnoredDomainResourceRoot clone() {
        IgnoredDomainResourceRoot clone = new IgnoredDomainResourceRoot(ignoredRegistry);
        synchronized (children) {
            for (IgnoreDomainResourceTypeResource child : children.values()) {
                IgnoreDomainResourceTypeResource childClone = child.clone();
                clone.registerChildInternal(childClone);
            }
        }
        return clone;
    }

    void publish() {
        ignoredRegistry.publish(this);
    }

    IgnoreDomainResourceTypeResource getChildInternal(String resourceType) {
        synchronized (children) {
            return IgnoreDomainResourceTypeResource.class.cast(children.get(resourceType));
        }
    }

    // call with lock on 'children' held
    private void registerChildInternal(IgnoreDomainResourceTypeResource child) {
        child.setParent(this);
        children.put(child.getName(), child);
    }

    private boolean isMaster() {
        return ignoredRegistry.isMaster();
    }

}
