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

package org.jboss.as.patching.management;

import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.Layer;


/**
 * @author Alexey Loubyansky
 */
abstract class LayerAttributeReadHandler extends ElementProviderAttributeReadHandler {

    @Override
    protected Layer getProvider(final String name, final InstalledIdentity identity) throws OperationFailedException {
        final List<Layer> layers = identity.getLayers();
        if (layers == null) {
            throw new OperationFailedException("no layers for " + name);
        }
        Layer target = null;
        for (Layer layer : layers) {
            if (layer.getName().equals(name)) {
                target = layer;
                break;
            }
        }
        if (target == null) {
            throw new OperationFailedException("Target layer not found: " + name);
        }
        return target;
    }
}
