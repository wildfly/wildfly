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

package org.jboss.as.host.controller;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * The directory grouping types for the domains; {@code tmp}, {@code log} and {@code data} directories.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum DirectoryGrouping {


    BY_TYPE("by-type"),
    BY_SERVER("by-server");

    private final String localName;

    DirectoryGrouping(final String localName) {
        this.localName = localName;
    }

    @Override
    public String toString() {
        return localName;
    }

    /**
     * Converts the value of the directory grouping to a model node.
     *
     * @return a new model node for the value.
     */
    public ModelNode toModelNode() {
        return new ModelNode().set(toString());
    }

    /**
     * Returns the default directory grouping.
     *
     * @return the default directory grouping.
     */
    public static DirectoryGrouping defaultValue() {
        return BY_SERVER;
    }

    /**
     * Returns the value of found in the model.
     *
     * @param model the model that contains the key and value.
     *
     * @return the directory grouping found in the model.
     *
     * @throws IllegalArgumentException if the {@link ModelDescriptionConstants#DIRECTORY_GROUPING directory grouping}
     *                                  was not found in the model.
     */
    public static DirectoryGrouping fromModel(final ModelNode model) {
        if (model.hasDefined(ModelDescriptionConstants.DIRECTORY_GROUPING)) {
            return DirectoryGrouping.valueOf(model.get(ModelDescriptionConstants.DIRECTORY_GROUPING).asString());
        }
        return defaultValue();
    }
}
