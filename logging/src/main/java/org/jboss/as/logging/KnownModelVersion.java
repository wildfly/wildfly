/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.controller.ModelVersion;

/**
 * Known model versions for the logging extension.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
enum KnownModelVersion {
    VERSION_1_1_0(ModelVersion.create(1, 1, 0), true),
    VERSION_1_2_0(ModelVersion.create(1, 2, 0), true),
    VERSION_1_3_0(ModelVersion.create(1, 3, 0), true),
    VERSION_1_4_0(ModelVersion.create(1, 4, 0), false),
    ;
    private final ModelVersion modelVersion;
    private final boolean hasTransformers;

    private KnownModelVersion(final ModelVersion modelVersion, final boolean hasTransformers) {
        this.modelVersion = modelVersion;
        this.hasTransformers = hasTransformers;
    }

    /**
     * Returns {@code true} if transformers should be registered against the model version.
     *
     * @return {@code true} if transformers should be registered, otherwise {@code false}
     */
    public boolean hasTransformers() {
        return hasTransformers;
    }

    /**
     * The model version.
     *
     * @return the model version
     */
    public ModelVersion getModelVersion() {
        return modelVersion;
    }
}
