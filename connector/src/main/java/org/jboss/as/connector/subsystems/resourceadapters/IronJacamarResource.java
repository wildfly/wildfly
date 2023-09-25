/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.AbstractModelResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Resource maintaining the sub-tree for the iron-jacamar
 *
 * @author Stefano Maestri
 */
public class IronJacamarResource implements Resource {

    private volatile Resource delegate = Factory.create();

    protected void update(final Resource updated) {
        delegate = updated;
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
        return delegate.hasChild(element);
    }

    @Override
    public Resource getChild(PathElement element) {
        return delegate.getChild(element);
    }

    @Override
    public Resource requireChild(PathElement element) {
        return delegate.requireChild(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return delegate.hasChildren(childType);
    }

    @Override
    public Resource navigate(PathAddress address) {
        return delegate.navigate(address);
    }

    @Override
    public Set<String> getChildTypes() {
        return delegate.getChildTypes();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return delegate.getChildrenNames(childType);
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        return delegate.getChildren(childType);
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        assert resource instanceof IronJacamarRuntimeResource;
        delegate.registerChild(address, resource);
    }

    @Override
    public void registerChild(PathElement address, int index, Resource resource) {
        throw ConnectorLogger.ROOT_LOGGER.indexedChildResourceRegistrationNotAvailable(address);
    }

    @Override
    public Resource removeChild(PathElement address) {
        return delegate.removeChild(address);
    }

    @Override
    public Set<String> getOrderedChildTypes() {
        return Collections.emptySet();
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
        return this;
    }

    public static class IronJacamarRuntimeResource extends AbstractModelResource {

        private volatile ModelNode model = new ModelNode();

        public IronJacamarRuntimeResource() {
        }


        @Override
        public ModelNode getModel() {
            return model;
        }

        @Override
        public void writeModel(final ModelNode newModel) {
            model = newModel;
        }

        @Override
        public boolean isModelDefined() {
            return model.isDefined();
        }

        @Override
        public Resource clone() {
            return this;
        }

        @Override
        public boolean isRuntime() {
            return true;
        }
    }
}
