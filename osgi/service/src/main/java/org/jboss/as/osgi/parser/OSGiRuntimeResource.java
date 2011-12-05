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
package org.jboss.as.osgi.parser;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author David Bosschaert
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OSGiRuntimeResource implements Resource {
    private final Resource delegate;
    private SystemBundleControllerHolder systemBundleControllerHolder;

    private static class SystemBundleControllerHolder {
        ServiceController<Bundle> systemBundleController;
    }

    public OSGiRuntimeResource() {
        this(Resource.Factory.create(), new SystemBundleControllerHolder());
    }

    public OSGiRuntimeResource(Resource delegate, SystemBundleControllerHolder holder) {
        this.delegate = delegate;
        this.systemBundleControllerHolder = holder;
    }

    public void setBundleContextServiceController(ServiceController<Bundle> serviceController) {
        systemBundleControllerHolder.systemBundleController = serviceController;
    }

    @Override
    public ModelNode getModel() {
        return delegate.getModel();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        delegate.writeModel(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return delegate.isModelDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        if (ModelConstants.BUNDLE.equals(element.getKey()))
            return hasBundle(element);
        else
            return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        if (ModelConstants.BUNDLE.equals(element.getKey()))
            return hasBundle(element) ? OSGiBundleResource.INSTANCE : null;
        else
            return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(PathElement element) {
        if (ModelConstants.BUNDLE.equals(element.getKey())) {
            if (hasBundle(element))
                return OSGiBundleResource.INSTANCE;
            throw new NoSuchResourceException(element);
        } else {
            return delegate.requireChild(element);
        }
    }

    @Override
    public boolean hasChildren(String childType) {
        if (ModelConstants.BUNDLE.equals(childType))
            return getChildrenNames(ModelConstants.BUNDLE).size() > 0;
        else
            return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() > 0 && ModelConstants.BUNDLE.equals(address.getElement(0).getKey())) {
            if (address.size() > 1) {
                throw new NoSuchResourceException(address.getElement(1));
            }
            return OSGiBundleResource.INSTANCE;
        } else {
            return delegate.navigate(address);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        Set<String> result = new HashSet<String>(delegate.getChildTypes());
        result.add(ModelConstants.BUNDLE);
        return result;
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (ModelConstants.BUNDLE.equals(childType))
            return getBundleIDs();
        else
            return delegate.getChildrenNames(childType);
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        if (ModelConstants.BUNDLE.equals(childType)) {
            Set<ResourceEntry> result = new HashSet<Resource.ResourceEntry>();
            for (String id : getBundleIDs()) {
                result.add(new OSGiBundleResource.OSGiBundleResourceEntry(id));
            }
            return result;
        } else {
            return delegate.getChildren(childType);
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (ModelConstants.BUNDLE.equals(address.getKey())) {
            throw new UnsupportedOperationException();
        } else {
            delegate.registerChild(address, resource);
        }
    }

    @Override
    public Resource removeChild(PathElement address) {
        if (ModelConstants.BUNDLE.equals(address.getKey())) {
            throw new UnsupportedOperationException();
        } else {
            return delegate.removeChild(address);
        }
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
    public Resource clone() {
        return new OSGiRuntimeResource(delegate.clone(), systemBundleControllerHolder);
    }

    private boolean hasBundle(PathElement element) {
        BundleContext ctx = getBundleContext();
        if (ctx == null)
            return false;

        try {
            long id = Long.parseLong(element.getValue());
            return ctx.getBundle(id) != null;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private Set<String> getBundleIDs() {
        BundleContext ctx = getBundleContext();

        if (ctx == null) {
            return Collections.emptySet();
        } else {
            Set<String> result = new TreeSet<String>();
            for (Bundle b : ctx.getBundles()) {
                result.add(Long.toString(b.getBundleId()));
            }
            return result;
        }
    }

    private BundleContext getBundleContext() {
        if (systemBundleControllerHolder.systemBundleController == null ||
            systemBundleControllerHolder.systemBundleController.getState() != ServiceController.State.UP) {
            return null;
        } else {
            return systemBundleControllerHolder.systemBundleController.getValue().getBundleContext();
        }
    }
}
