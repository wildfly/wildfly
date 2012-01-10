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
package org.jboss.as.platform.mbean;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass of {@link Resource} implementations for the platform mbean resources.
 */
abstract class AbstractPlatformMBeanResource implements Resource.ResourceEntry {

    private final PathElement pathElement;

    AbstractPlatformMBeanResource(PathElement pathElement) {
        this.pathElement = pathElement;
    }

    @Override
    public ModelNode getModel() {
        return new ModelNode();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw PlatformMBeanMessages.MESSAGES.modelNotWritable();
    }

    @Override
    public boolean isModelDefined() {
        return false;
    }

    @Override
    public Resource getChild(PathElement element) {
        if (hasChildren(element.getKey())) {
            return getChildEntry(element.getValue());
        }
        return null;
    }

    @Override
    public Resource requireChild(PathElement address) {
        final Resource resource = getChild(address);
        if (resource == null) {
            throw new NoSuchResourceException(address);
        }
        return resource;
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() == 0) {
            return this;
        } else {
            Resource child = requireChild(address.getElement(0));
            return address.size() == 1 ? child : child.navigate(address.subAddress(1));
        }
    }

    @Override
    public Set<Resource.ResourceEntry> getChildren(String childType) {
        if (!hasChildren(childType)) {
            return Collections.emptySet();
        } else {
            Set<Resource.ResourceEntry> result = new HashSet<Resource.ResourceEntry>();
            for (String name : getChildrenNames()) {
                result.add(getChildEntry(name));
            }
            return result;
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        return getChildTypes().contains(childType);
    }

    @Override
    public boolean hasChild(PathElement element) {
        return getChildrenNames(element.getKey()).contains(element.getValue());
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (hasChildren(childType)) {
            return getChildrenNames();
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw PlatformMBeanMessages.MESSAGES.addingChildrenNotSupported();
    }

    @Override
    public Resource removeChild(PathElement address) {
        throw PlatformMBeanMessages.MESSAGES.removingChildrenNotSupported();
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Resource clone() {
        try {
            return Resource.class.cast(super.clone());
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("impossible");
        }
    }

    @Override
    public String getName() {
        return pathElement.getValue();
    }

    @Override
    public PathElement getPathElement() {
        return pathElement;
    }

    abstract ResourceEntry getChildEntry(String name);

    abstract Set<String> getChildrenNames();
}
