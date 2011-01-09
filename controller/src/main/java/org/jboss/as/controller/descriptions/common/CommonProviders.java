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
package org.jboss.as.controller.descriptions.common;

import org.jboss.as.controller.descriptions.ModelDescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * {@link ModelDescriptionProvider} implementations for sub-models that occur
 * across different types of models.
 *
 * @author Brian Stansberry
 *
 */
public final class CommonProviders {

    // Prevent instantiation
    private CommonProviders() {}

    /**
     * Provider for a sub-model that names a "path" but doesn't require
     * the actual path to be specified.
     */
    public static final ModelDescriptionProvider NAMED_PATH_PROVIDER = new ModelDescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final boolean recursive) {
            return PathDescription.getNamedPathDescription();
        }
    };

    /**
     * Provider for a sub-model that names a "path" and specifies the actual path.
     */
    public static final ModelDescriptionProvider SPECIFIED_PATH_PROVIDER = new ModelDescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final boolean recursive) {
            return PathDescription.getSpecifiedPathDescription();
        }
    };
}
