/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.metadata;

import static org.jboss.as.patching.Constants.ADD_ONS;
import static org.jboss.as.patching.Constants.LAYERS;

/**
 * The layer type.
 *
 * @author Emanuel Muckenhuber
 */
public enum LayerType {

    Layer("layer", LAYERS),
    AddOn("add-on", ADD_ONS),
    // Maybe add identity, since we could have also changes that affect the identity itself
    ;

    private final String name;
    private final String dirName;
    LayerType(String name, String dirName) {
        this.name = name;
        this.dirName = dirName;
    }

    public String getName() {
        return name;
    }

    public String getDirName() {
        return dirName;
    }
}
