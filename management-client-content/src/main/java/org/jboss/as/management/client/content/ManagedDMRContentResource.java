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

package org.jboss.as.management.client.content;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * {@link Resource} implementation for a resource that stores managed DMR content (e.g. named rollout plans.)
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagedDMRContentResource implements Resource.ResourceEntry {

    private final PathElement pathElement;
    private ManagedDMRContentTypeResource parent;
    private ModelNode model;

    public ManagedDMRContentResource(PathElement pathElement) {
        this(pathElement, null);
    }

    public ManagedDMRContentResource(final PathElement pathElement, final ManagedDMRContentTypeResource parent) {
        this.pathElement = pathElement;
        this.parent = parent;
    }

    void setParent(final ManagedDMRContentTypeResource parent) {
        this.parent = parent;
    }

    @Override
    public synchronized ModelNode getModel() {
        if (model == null) {
            model = new ModelNode();
            if (parent != null) {
                ManagedDMRContentTypeResource.ManagedContent content = parent.getManagedContent(pathElement.getValue());
                if (content != null) {
                    model.get(ModelDescriptionConstants.HASH).set(content.getHash());
                    model.get(ModelDescriptionConstants.CONTENT).set(content.getContent());
                }
            }
        }
        return model;
    }

    @Override
    public synchronized void writeModel(ModelNode newModel) {

        if (parent == null) {
            throw new IllegalStateException("null parent");
        }

        ModelNode content = newModel.get(ModelDescriptionConstants.CONTENT);
        try {
            byte[] hash = parent.storeManagedContent(pathElement.getValue(), content);
            newModel.get(ModelDescriptionConstants.HASH).set(hash);
            this.model = null; // force reload
        } catch (IOException e) {
            throw new ContentStorageException(e);
        }
    }

    @Override
    public boolean isModelDefined() {
        return getModel().isDefined();
    }

    @Override
    public boolean hasChild(PathElement element) {
        return false;
    }

    @Override
    public Resource getChild(PathElement element) {
        return null;
    }

    @Override
    public Resource requireChild(PathElement element) {
        throw new NoSuchResourceException(element);
    }

    @Override
    public boolean hasChildren(String childType) {
        return false;
    }

    @Override
    public Resource navigate(PathAddress address) {
        return Tools.navigate(this, address);
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceEntry> getChildren(String childType) {
        return Collections.emptySet();
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        throw MESSAGES.immutableResource();
    }

    @Override
    public Resource removeChild(PathElement address) {
        return null;
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
        return new ManagedDMRContentResource(pathElement, parent);
    }

    @Override
    public String getName() {
        return this.pathElement.getValue();
    }

    @Override
    public PathElement getPathElement() {
        return this.pathElement;
    }
}
