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

import java.util.LinkedHashSet;

import static org.jboss.as.host.controller.ignored.IgnoredDomainTypeResourceDefinition.*;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link Resource} implementation for a given type of ignored resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IgnoreDomainResourceTypeResource extends PlaceholderResource.PlaceholderResourceEntry {

    private IgnoredDomainResourceRoot parent;
    private Boolean hasWildcard;
    private final LinkedHashSet<String> model = new LinkedHashSet<String>();

    /**
     * Constructor for use by operation step handlers.
     *
     * @param type the name of the type some of whose resources are to be ignored
     * @param names the specific instances of type that should be ignored. Either {@link ModelType#LIST}
     *              or {@link ModelType#UNDEFINED}; cannot be {@code null}
     */
    public IgnoreDomainResourceTypeResource(String type, final ModelNode names) {
        super(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, type);
        setNames(names);
    }

    private IgnoreDomainResourceTypeResource(IgnoreDomainResourceTypeResource toCopy) {
        super(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, toCopy.getName());
        synchronized (toCopy.model) {
            model.addAll(toCopy.model);
        }
        this.parent = toCopy.parent;
    }

    /** {@inheritDoc */
    @Override
    public ModelNode getModel() {
        synchronized (model) {
            // We return what is effectively a copy; force handlers to use writeModel, setWildcard or setNames to modify
            ModelNode result = new ModelNode();
            ModelNode wildcard = result.get(WILDCARD.getName());
            if (hasWildcard != null) {
                wildcard.set(hasWildcard.booleanValue());
            }
            ModelNode names = result.get(NAMES.getName());
            synchronized (model) {
                for (String name : model) {
                    names.add(name);
                }
            }
            return result;
        }
    }

    /** {@inheritDoc */
    @Override
    public void writeModel(ModelNode newModel) {
        synchronized (model) {
            if (newModel.hasDefined(WILDCARD.getName())) {
                setWildcard(newModel.get(WILDCARD.getName()).asBoolean());
            }
            setNames(newModel.get(NAMES.getName()));
        }
    }

    /** {@inheritDoc */
    @Override
    public boolean isModelDefined() {
        return true;
    }

    /** {@inheritDoc */
    @Override
    public IgnoreDomainResourceTypeResource clone() {
        return new IgnoreDomainResourceTypeResource(this);
    }

    void setParent(IgnoredDomainResourceRoot parent) {
        this.parent = parent;
    }

    void setNames(ModelNode names) {
        synchronized (model) {
            model.clear();
            if (names.isDefined()) {
                for (ModelNode name : names.asList()) {
                    String nameStr = name.asString();
                    model.add(nameStr);
                }
            }
        }
    }

    void publish() {
        parent.publish();
    }

    boolean hasName(String name) {
        synchronized (model) {
            return (hasWildcard != null && hasWildcard.booleanValue()) || model.contains(name);
        }
    }

    public void setWildcard(boolean wildcard) {
        synchronized (model) {
            this.hasWildcard = wildcard;
        }
    }
}
