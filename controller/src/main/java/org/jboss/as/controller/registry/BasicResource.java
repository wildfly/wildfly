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

package org.jboss.as.controller.registry;

import org.jboss.dmr.ModelNode;

import java.util.ConcurrentModificationException;

/**
 * Standard {@link Resource} implementation.
 *
 * <p>Concurrency note: if a thread needs to modify a BasicResource, it must use the clone() method to obtain its
 * own copy of the resource. That instance cannot be made visible to other threads until all writes are complete.</p>
 *
 * @author Emanuel Muckenhuber
 */
class BasicResource extends AbstractModelResource implements Resource {

    /** The local model. */
    private final ModelNode model = new ModelNode();

    protected BasicResource() {
    }

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public void writeModel(ModelNode newModel) {
        model.set(newModel);
    }

    @Override
    public boolean isModelDefined() {
        return model.isDefined();
    }
    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public Resource clone() {
        final Resource clone = new BasicResource();
        for (;;) {
            try {
                clone.writeModel(model);
                break;
            } catch (ConcurrentModificationException ignore) {
                // TODO horrible hack :(
            }
        }
        for(final String childType : getChildTypes()) {
            for(final ResourceEntry child : getChildren(childType)) {
                clone.registerChild(child.getPathElement(), child.clone());
            }
        }
        return clone;
    }

}
